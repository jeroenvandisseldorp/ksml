package io.axual.ksml.python;

/*-
 * ========================LICENSE_START=================================
 * KSML
 * %%
 * Copyright (C) 2021 - 2023 Axual B.V.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */


import io.axual.ksml.data.mapper.DataObjectConverter;
import io.axual.ksml.data.object.DataNull;
import io.axual.ksml.data.object.DataObject;
import io.axual.ksml.data.object.DataString;
import io.axual.ksml.definition.FunctionDefinition;
import io.axual.ksml.definition.InjectedParameter;
import io.axual.ksml.definition.ParameterDefinition;
import io.axual.ksml.exception.ExecutionException;
import io.axual.ksml.exception.TopologyException;
import io.axual.ksml.execution.FatalError;
import io.axual.ksml.proxy.store.StoreIteratorProxy;
import io.axual.ksml.user.UserFunction;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.axual.ksml.type.UserType.DEFAULT_NOTATION;
import static io.axual.ksml.user.UserFunction.INJECT_STORES;

@Slf4j
public class PythonFunction extends UserFunction {
    // Every function can get the stores; a call without stores gets an empty dict
    private static final InjectedParameter STORES = new InjectedParameter(INJECT_STORES);
    private static final Map<String, Object> EMPTY_STORES = Map.of();
    private static final PythonNativeMapper NATIVE_MAPPER = new PythonNativeMapper();
    private static final String QUOTE = "\"";
    private final DataObjectConverter converter;
    private final Context context;
    private final PythonDataObjectMapper dataObjectMapper;
    private final Value function;
    // The variables a call can inject: Python globals that start as None. A call sets only those it is given.
    private final List<InjectedParameter> injectedParameters;

    public static PythonFunction forFunction(PythonContext context, String namespace, String name, FunctionDefinition definition) {
        return new PythonFunction(context, namespace, "function", name, definition);
    }

    public static PythonFunction forGenerator(PythonContext context, String namespace, String name, FunctionDefinition definition) {
        return new PythonFunction(context, namespace, "generator", name, definition);
    }

    public static PythonFunction forPredicate(PythonContext context, String namespace, String name, FunctionDefinition definition) {
        return new PythonFunction(context, namespace, "condition", name, definition);
    }

    private PythonFunction(PythonContext context, String namespace, String type, String name, FunctionDefinition definition) {
        super(namespace, name, definition.parameters(), definition.resultType(), definition.storeNames());
        converter = context.converter();
        this.context = context.context();
        dataObjectMapper = new PythonDataObjectMapper(true, this.context);
        injectedParameters = injectedParametersOf(definition);
        final var pyCode = generatePythonCode(namespace, type, name, definition);
        function = context.registerFunction(pyCode, name + "_caller");
        if (function == null) {
            final var pyCodeLines = pyCode.split("\n");
            final var builder = new StringBuilder();
            for (int index = 1; index <= pyCodeLines.length; index++) {
                builder.append(index).append("  ").append(pyCodeLines[index - 1]).append("\n");
            }
            log.error("""
                    Function {} {}
                    Error in generated Python code:
                    
                    {}
                    """, namespace, name, builder);
            throw new ExecutionException("Error in function: %s.%s".formatted(namespace, name));
        }
    }

    @Override
    public DataObject call(Map<String, Object> injections, DataObject... parameters) {
        // Validate that the defined parameter list matches the amount of passed in parameters
        if (this.fixedParameterCount > parameters.length) {
            throw new TopologyException("Function %s.%s - parameter list does not match function spec: minimally expected %d, got %d".formatted(namespace, name, this.parameters.length, parameters.length));
        }
        if (this.parameters.length < parameters.length) {
            throw new TopologyException("Function %s.%s - parameter list does not match function spec: maximally expected %d, got %d".formatted(namespace, name, this.parameters.length, parameters.length));
        }
        // Validate the parameter types
        for (int index = 0; index < parameters.length; index++) {
            final var declaredParameter = this.parameters[index];
            final var actualParameter = parameters[index];
            final var assignable = declaredParameter.type().isAssignableFrom(actualParameter);
            if (assignable.isNotAssignable()) {
                throw new TopologyException("Function %s.%s expects parameter #%d (\"%s\") to be %s but %s was passed in: %s".formatted(namespace, name, index + 1, declaredParameter.name(), declaredParameter.type(), actualParameter.type(), assignable));
            }
        }

        // Only the variables that the caller injects are set in Python
        if (injections == null) injections = Map.of();
        for (final var variable : injections.keySet()) {
            if (injectedParameters.stream().noneMatch(parameter -> parameter.name().equals(variable)))
                throw new ExecutionException("Python function %s.%s - can not inject unknown variable '%s'".formatted(namespace, name, variable));
        }
        // A function without stores sees an empty dict, not the stores of a call it is nested in
        if (!injections.containsKey(INJECT_STORES)) {
            injections = new HashMap<>(injections);
            injections.put(INJECT_STORES, EMPTY_STORES);
        }
        var arguments = convertParameters(injections, parameters);

        final int iteratorMark = StoreIteratorProxy.beginCall();
        try {
            // Call the prepared function
            log.debug("Calling Python function: namespace={}, name={}, arguments={}", namespace, name, arguments);
            Value pyResult = function.execute(arguments);

            if (pyResult.canExecute()) {
                throw new ExecutionException("Python function %s.%s - Code results in a function instead of a value".formatted(namespace, name));
            }

            // Check if the function is supposed to return a result value; a returned value is ignored otherwise
            if (resultType != null && resultType.dataType() != DataNull.DATATYPE) {
                DataObject result = dataObjectMapper.toDataObject(resultType.dataType(), pyResult);
                logCall(parameters, result);
                if (converter != null)
                    result = converter.convert(DEFAULT_NOTATION, result, resultType);
                checkType(resultType.dataType(), result);
                return result;
            } else {
                logCall(parameters, null);
                return DataNull.INSTANCE;
            }
        } catch (PolyglotException e) {
            logCall(parameters, null);
            // Kafka Streams must get its own exceptions unchanged
            if (e.isHostException() && e.asHostException() instanceof KafkaException kafkaError) throw kafkaError;
            throw FatalError.report(new TopologyException("Error while executing function %s.%s : %s".formatted(namespace, name, e.getMessage()), e));
        } catch (Exception e) {
            logCall(parameters, null);
            throw FatalError.report(new TopologyException("Error while executing function %s.%s : %s".formatted(namespace, name, e.getMessage()), e));
        } finally {
            // Closes store iterators that the function left open, for example after break or return in a loop
            final int closed = StoreIteratorProxy.endCall(iteratorMark);
            if (closed > 0) log.debug("Closed {} store iterator(s) left open by {}.{}", closed, namespace, name);
        }
    }

    private Object[] convertParameters(Map<String, Object> injections, DataObject... parameters) {
        Object[] result = new Object[parameters.length + 1];
        result[0] = NATIVE_MAPPER.toRealPythonValue(context, injections);
        for (var index = 0; index < parameters.length; index++) {
            checkType(this.parameters[index], parameters[index]);
            // Convert DataObject to Python value
            result[index + 1] = dataObjectMapper.fromDataObject(parameters[index]);
        }
        return result;
    }

    private String generatePythonCode(String namespace, String type, String name, FunctionDefinition definition) {
        // Prepend two spaces of indentation before the function code
        String[] functionCode = getFunctionCode(definition.code(), "  ");
        String[] expressionCode = Arrays.stream(definition.expression()).map(line -> "    " + line).toArray(String[]::new);

        // Prepare a list of parameters for the function definition
        String[] defParams = Arrays.stream(definition.parameters()).map(p -> p.name() + (p.isOptional() ? "=None" : "")).toArray(String[]::new);

        // prepare globalCode from the function definition
        String[] globalCodeLines = getFunctionCode(definition.globalCode(), "");
        final var globalCode = String.join("\n", injectFunctionLocalVariables(namespace, type, globalCodeLines)) + "\n";

        // Code to include all global variables
        final var assignStores = definition.storeNames().stream()
                .map(storeName -> "  " + storeName + " = stores[\"" + storeName + "\"]\n")
                .collect(Collectors.joining());
        // Injected globals start as None; setdefault keeps a value set by globalCode
        final var declareGlobals = injectedParameters.stream()
                .map(parameter -> "globals().setdefault(\"" + parameter.name() + "\", None)\n")
                .collect(Collectors.joining());
        // The Python classes that wrap injected objects
        final var proxyClasses = injectedParameters.stream()
                .map(InjectedParameter::proxyClassCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining("\n"));
        // Wrap injected objects in their Python class
        final var wrapInjections = injectedParameters.stream()
                .filter(parameter -> parameter.proxyClassName() != null)
                .map(parameter -> "  if _ksml_injected.get(\"" + parameter.name() + "\") is not None:\n" +
                        "    _ksml_injected[\"" + parameter.name() + "\"] = " + parameter.proxyClassName() + "(_ksml_injected[\"" + parameter.name() + "\"])\n")
                .collect(Collectors.joining());
        // Code to initialize optional parameters with default values
        final var initializeOptionalParams = Arrays.stream(definition.parameters())
                .filter(ParameterDefinition::isOptional)
                .filter(p -> p.defaultValue() != null)
                .map(p -> "  if " + p.name() + " is None:\n    " + p.name() + " = " + (p.type() == DataString.DATATYPE ? QUOTE : "") + p.defaultValue() + (p.type() == DataString.DATATYPE ? QUOTE : "") + "\n")
                .collect(Collectors.joining());

        // Prepare the return statement
        final var returnStatement = "return" +
                (definition.resultType() != null && definition.resultType().dataType() != DataNull.DATATYPE
                        ? " " + String.join("\n", expressionCode) + "\n"
                        : "")
                + "\n";

        // Compose the function (if any) and the return statement together
        final var functionAndExpression = "def " + name + "(" + String.join(",", defParams) + "):\n" +
                initFunctionLocalVariables(2, loggerName(namespace, type, name)) +
                assignStores +
                initializeOptionalParams +
                String.join("\n", functionCode) + "\n" +
                "  " + returnStatement + "\n";

        // Prepare the actual caller for the code
        // Parameters are now pre-converted to Python types by PythonTypeConverter in Java
        // Return value is passed as-is; PythonDataObjectMapper handles GraalVM Value conversion
        // Restore globals after the call, as calls can nest; *args avoids name clashes. Only stores is injected in
        // most calls, so that takes a shorter path.
        final var pyCallerCode = "def " + name + "_caller(_ksml_injected, *args):\n" +
                "  global " + INJECT_STORES + "\n" +
                "  if _ksml_injected.keys() == {\"" + INJECT_STORES + "\"}:\n" +
                "    _ksml_previous = " + INJECT_STORES + "\n" +
                "    " + INJECT_STORES + " = _ksml_injected[\"" + INJECT_STORES + "\"]\n" +
                "    try:\n" +
                "      return " + name + "(*args)\n" +
                "    finally:\n" +
                "      " + INJECT_STORES + " = _ksml_previous\n" +
                wrapInjections +
                "  _ksml_previous = {variable: globals()[variable] for variable in _ksml_injected}\n" +
                "  globals().update(_ksml_injected)\n" +
                "  try:\n" +
                "    return " + name + "(*args)\n" +
                "  finally:\n" +
                "    globals().update(_ksml_previous)\n";

        // Python code template - no longer needs convert_to_python/convert_from_python
        // as Java-side PythonTypeConverter handles all collection conversion
        final var pythonCodeTemplate =
                """
                        import polyglot
                        
                        %4$s
                        # global Python code goes here (first argument)
                        %1$s
                        
                        # function definition and expression go here (second argument)
                        %2$s
                        
                        # caller definition goes here (third argument)
                        @polyglot.export_value
                        %3$s
                        """;

        return pythonCodeTemplate.formatted(globalCode, functionAndExpression, pyCallerCode, proxyClasses + "\n" + declareGlobals);
    }

    /** The stores, followed by the variables that the definition declares. */
    private static List<InjectedParameter> injectedParametersOf(FunctionDefinition definition) {
        final var result = new ArrayList<InjectedParameter>();
        result.add(STORES);
        definition.injectedParameters().stream()
                .filter(parameter -> !parameter.name().equals(INJECT_STORES))
                .forEach(result::add);
        return List.copyOf(result);
    }

    private static String[] getFunctionCode(String[] code, String spaces) {
        return Arrays.stream(code).map(line -> spaces + line).toArray(String[]::new);
    }

    private String loggerName(String namespace, String type, String name) {
        return namespace + "." + type + "." + name;
    }

    private String[] injectFunctionLocalVariables(String namespace, String type, String[] code) {
        // Look for "def func():" statements and inject log variable code after all occurrences
        final var result = new ArrayList<String>();
        var injectCode = false;
        var defIndent = 0;
        var functionName = "";
        for (final var line : code) {
            if (line.trim().isEmpty()) continue;
            int lineIndent = line.length() - line.stripIndent().length();
            if (injectCode && lineIndent > defIndent) {
                result.add(initFunctionLocalVariables(lineIndent, loggerName(namespace, type, functionName)));
            }
            result.add(line);
            injectCode = false;
            if (line.trim().startsWith("def ") && line.trim().endsWith(":")) {
                final var functionDef = line.trim().substring(4, line.length() - 1).trim();
                if (functionDef.contains("(") && functionDef.endsWith(")")) {
                    injectCode = true;
                    defIndent = lineIndent;
                    functionName = functionDef.substring(0, functionDef.indexOf("("));
                }
            }
        }
        return result.toArray(String[]::new);
    }

    private String initFunctionLocalVariables(int indentCount, String loggerName) {
        final var indent = " ".repeat(indentCount);
        return indent + "global loggerBridge\n" +
                indent + "log = None\n" +
                indent + "if loggerBridge is not None:\n" +
                indent + "  log = loggerBridge.getLogger(\"" + loggerName + "\")\n" +
                indent + "global metrics\n";
    }
}
