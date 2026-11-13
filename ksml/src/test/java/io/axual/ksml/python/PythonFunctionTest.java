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

import io.axual.ksml.data.object.DataDouble;
import io.axual.ksml.data.object.DataInteger;
import io.axual.ksml.data.object.DataLong;
import io.axual.ksml.data.object.DataNull;
import io.axual.ksml.data.object.DataString;
import io.axual.ksml.definition.FunctionDefinition;
import io.axual.ksml.definition.ParameterDefinition;
import io.axual.ksml.definition.PythonSource;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.exception.ExecutionException;
import io.axual.ksml.proxy.context.ProcessorContextProxy;
import io.axual.ksml.store.StateStores;
import io.axual.ksml.type.UserType;
import org.apache.kafka.streams.errors.TaskMigratedException;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static io.axual.ksml.user.UserFunction.INJECT_CONTEXT;
import static io.axual.ksml.user.UserFunction.withStores;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class PythonFunctionTest {
    @BeforeAll
    static void warmupGraalVM() {
        // The very first Python context creation in a JVM can fail with a libgraal cold-start
        // error ("Error decoding exception: [B@..."). Build one throwaway context here so the
        // field initializer below always runs against a warm libgraal isolate.
        try {
            new PythonContext(PythonContextConfig.builder().build());
        } catch (Exception _) {
            // Warmup only — the real assertions happen in the @Test methods.
        }
    }

    final PythonContext context = new PythonContext(PythonContextConfig.builder().build());
    final ParameterDefinition one = new ParameterDefinition("one", DataInteger.DATATYPE);
    final ParameterDefinition two = new ParameterDefinition("two", DataInteger.DATATYPE);
    final ParameterDefinition[] params = new ParameterDefinition[]{one, two};
    final UserType resultType = new UserType(UserType.DEFAULT_NOTATION, DataInteger.DATATYPE);
    final UserType stringType = new UserType(UserType.DEFAULT_NOTATION, DataString.DATATYPE);

    @ParameterizedTest
    @CsvSource({"1, 2, 3", "100,100,200", "100, -1, 99", "99, -100, -1"})
    void testAdditionExpression(Integer i1, Integer i2, Integer sum) {
        final var adderDef = FunctionDefinition.as(KSMLDSL.Functions.TYPE_GENERIC, "adder", params, PythonSource.of(null, null, "one + two"), resultType, null);
        final var adder = PythonFunction.forFunction(context, "test", "adder", adderDef);

        final var arg1 = new DataInteger(i1);
        final var arg2 = new DataInteger(i2);

        final var result = adder.call(arg1, arg2);
        assertThat(result).isInstanceOf(DataInteger.class);
        assertThat(((DataInteger) result).value()).isEqualTo(sum);
    }

    /**
     * Test creating a Python function and calling it in the expression.
     */
    @ParameterizedTest
    @CsvSource({"1, 2, 3", "100,100,200", "100, -1, 99", "99, -100, -1"})
    void testAdditionCode(Integer i1, Integer i2, Integer sum) {
        var pythonCode = """
                def myAddFunc(one, two):
                  return one + two
                
                """;
        final var adderDef = FunctionDefinition.as(KSMLDSL.Functions.TYPE_GENERIC, "adder", params, PythonSource.of(null, pythonCode.split("\n"), new String[]{"myAddFunc(one, two)"}), resultType, null);
        final var adder = PythonFunction.forFunction(context, "test", "adder", adderDef);

        final var arg1 = new DataInteger(i1);
        final var arg2 = new DataInteger(i2);

        final var result = adder.call(arg1, arg2);
        assertThat(result).isInstanceOf(DataInteger.class);
        assertThat(((DataInteger) result).value()).isEqualTo(sum);
    }

    /**
     * Test creating a Python function in the global code and calling it in the expression.
     */
    @ParameterizedTest
    @CsvSource({"1, 2, 3", "100,100,200", "100, -1, 99", "99, -100, -1"})
    void testAdditionGlobalCode(Integer i1, Integer i2, Integer sum) {
        var pythonCode = """
                def myAddFunc(one, two):
                  return one + two
                
                """;
        final var adderDef = FunctionDefinition.as(KSMLDSL.Functions.TYPE_GENERIC, "adder", params, PythonSource.of(pythonCode.split("\n"), null, new String[]{"myAddFunc(one, two)"}), resultType, null);
        final var adder = PythonFunction.forFunction(context, "test", "adder", adderDef);

        final var arg1 = new DataInteger(i1);
        final var arg2 = new DataInteger(i2);

        final var result = adder.call(arg1, arg2);
        assertThat(result).isInstanceOf(DataInteger.class);
        assertThat(((DataInteger) result).value()).isEqualTo(sum);
    }

    @Test
    /*
      Test that Null Key/Values are accepted as parameters
     */
    void testNullKeyValue() {
        final var stringResultType = new UserType(UserType.DEFAULT_NOTATION, DataString.DATATYPE);
        final var concatDef = FunctionDefinition.as(KSMLDSL.Functions.TYPE_GENERIC, "concat", params, PythonSource.of(null, null, "str(one is None) + ' ' + str(two is None)"), stringResultType, null);
        final var concat = PythonFunction.forFunction(context, "test", "adder", concatDef);

        final var nullArg = DataNull.INSTANCE;
        final var nonNullArg = new DataInteger(1);

        final var expectedResultNullKey = "True False";
        var resultNullKey = concat.call(nullArg, nonNullArg);
        assertThat(resultNullKey).isInstanceOf(DataString.class);
        assertThat(((DataString) resultNullKey).value()).isEqualTo(expectedResultNullKey);

        final var expectedResultNullValue = "False True";
        var resultNullValue = concat.call(nonNullArg, nullArg);
        assertThat(resultNullValue).isInstanceOf(DataString.class);
        assertThat(((DataString) resultNullValue).value()).isEqualTo(expectedResultNullValue);

        final var expectedResultNullKeyValue = "True True";
        var resultNullKeyValue = concat.call(nullArg, nullArg);
        assertThat(resultNullKeyValue).isInstanceOf(DataString.class);
        assertThat(((DataString) resultNullKeyValue).value()).isEqualTo(expectedResultNullKeyValue);

    }

    @Test
    void injectedVariablesDefaultToNoneAndUnknownNamesAreRejected() {
        final var definition = FunctionDefinition.as(KSMLDSL.Functions.TYPE_GENERIC, "injected", new ParameterDefinition[0], PythonSource.of(null, null, "str(context)"), stringType, null);
        final var function = PythonFunction.forFunction(context, "test", "injected", definition);

        assertThat(function.call()).isEqualTo(new DataString("None"));
        // An injected variable is set during the call only; a later call that does not inject it does not see it
        final var storesDefinition = FunctionDefinition.as(KSMLDSL.Functions.TYPE_GENERIC, "injectedStores", new ParameterDefinition[0], PythonSource.of(null, null, "str(type(stores).__name__)"), stringType, null);
        final var storesFunction = PythonFunction.forFunction(context, "test", "injectedStores", storesDefinition);
        assertThat(storesFunction.call(withStores(new StateStores()))).isEqualTo(new DataString("dict"));
        assertThat(storesFunction.call()).isEqualTo(new DataString("NoneType"));
        assertThatThrownBy(() -> function.call(Map.of("unknown", 1)))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("can not inject unknown variable 'unknown'");
    }

    @Test
    void returnsFloatsWithAFraction() {
        final var doubleType = new UserType(UserType.DEFAULT_NOTATION, DataDouble.DATATYPE);
        final var definition = FunctionDefinition.as(KSMLDSL.Functions.TYPE_GENERIC, "half", new ParameterDefinition[0], PythonSource.of(null, null, "1.5"), doubleType, null);
        final var function = PythonFunction.forFunction(context, "test", "half", definition);
        assertThat(function.call()).isEqualTo(new DataDouble(1.5));
    }

    @ParameterizedTest
    @ValueSource(strings = {"context", "stores", "previous", "globalVars", "args"})
    void parameterCanHaveTheNameOfAnInjectedOrCallerVariable(String parameterName) {
        final var parameters = new ParameterDefinition[]{new ParameterDefinition(parameterName, DataString.DATATYPE)};
        final var definition = FunctionDefinition.as(KSMLDSL.Functions.TYPE_GENERIC, "named", parameters, PythonSource.of(null, null, parameterName), stringType, null);
        final var function = PythonFunction.forFunction(context, "test", "named", definition);

        assertThat(function.call(Map.of(), new DataString("x"))).isEqualTo(new DataString("x"));
    }

    @Test
    void missingOptionalParameterGetsItsDefault() {
        final var parameters = new ParameterDefinition[]{
                new ParameterDefinition("first", DataString.DATATYPE),
                new ParameterDefinition("second", DataString.DATATYPE, true, null)};
        final var definition = FunctionDefinition.as(KSMLDSL.Functions.TYPE_GENERIC, "optional", parameters, PythonSource.of(null, null, "first + str(second)"), stringType, null);
        final var function = PythonFunction.forFunction(context, "test", "optional", definition);

        assertThat(function.call(new DataString("x"))).isEqualTo(new DataString("xNone"));
    }

    @Test
    void globalCodeValueOfAnInjectableVariableSurvivesLaterFunctions() {
        final var ownerDefinition = FunctionDefinition.as(KSMLDSL.Functions.TYPE_GENERIC, "owner", new ParameterDefinition[0], PythonSource.of("context = 'mine'", null, "str(context)"), stringType, null);
        final var owner = PythonFunction.forFunction(context, "test", "owner", ownerDefinition);
        final var laterDefinition = FunctionDefinition.as(KSMLDSL.Functions.TYPE_GENERIC, "later", new ParameterDefinition[0], PythonSource.of(null, null, "'later'"), stringType, null);
        PythonFunction.forFunction(context, "test", "later", laterDefinition);

        assertThat(owner.call()).isEqualTo(new DataString("mine"));
    }

    @Test
    void nullInjectionsMeansNoInjections() {
        final var definition = FunctionDefinition.as(KSMLDSL.Functions.TYPE_GENERIC, "noInjections", new ParameterDefinition[0], PythonSource.of(null, null, "str(stores)"), stringType, null);
        final var function = PythonFunction.forFunction(context, "test", "noInjections", definition);

        assertThat(function.call((Map<String, Object>) null)).isEqualTo(new DataString("None"));
    }

    @Test
    void nestedCallRestoresTheInjectedVariablesOfTheOuterCall() {
        // The inner function runs during the outer one's context.forward, as a filter after punctuate does
        final var innerDefinition = FunctionDefinition.as(KSMLDSL.Functions.TYPE_GENERIC, "inner", new ParameterDefinition[0], PythonSource.of(null, null, "str(len(stores))"), stringType, null);
        final var inner = PythonFunction.forFunction(context, "test", "inner", innerDefinition);
        final var innerStores = new StateStores();
        final var innerResult = new DataString[1];
        @SuppressWarnings("unchecked") final ProcessorContext<Object, Object> processorContext = mock(ProcessorContext.class);
        doAnswer(invocation -> {
            innerResult[0] = (DataString) inner.call(withStores(innerStores));
            return null;
        }).when(processorContext).forward(any(Record.class), anyString());
        final var proxy = new ProcessorContextProxy(processorContext, 0L, new ProcessorContextProxy.Target("next", stringType, new UserType(DataLong.DATATYPE)), Map.of());

        final var outerCode = """
                own_stores = stores
                own_context = context
                context.forward('k', 1)
                result = str(stores is own_stores and context is own_context)
                """;
        final var outerDefinition = FunctionDefinition.as(KSMLDSL.Functions.TYPE_GENERIC, "outer", new ParameterDefinition[0], PythonSource.of(null, outerCode, "result"), stringType, null);
        final var outer = PythonFunction.forFunction(context, "test", "outer", outerDefinition);
        final var injections = new HashMap<>(withStores(new StateStores()));
        injections.put(INJECT_CONTEXT, proxy);

        assertThat(outer.call(injections)).isEqualTo(new DataString("True"));
        assertThat(innerResult[0]).isEqualTo(new DataString("0"));
        // After the outer call, the globals are back to None
        final var afterDefinition = FunctionDefinition.as(KSMLDSL.Functions.TYPE_GENERIC, "after", new ParameterDefinition[0], PythonSource.of(null, null, "str(stores) + str(context)"), stringType, null);
        assertThat(PythonFunction.forFunction(context, "test", "after", afterDefinition).call()).isEqualTo(new DataString("NoneNone"));
    }

    @Test
    void kafkaExceptionFromJavaCodeIsRethrownUnchanged() {
        @SuppressWarnings("unchecked") final ProcessorContext<Object, Object> processorContext = mock(ProcessorContext.class);
        final var error = new TaskMigratedException("fenced");
        doThrow(error).when(processorContext).commit();
        final var proxy = new ProcessorContextProxy(processorContext, 0L, new ProcessorContextProxy.Target(null, stringType, stringType), Map.of());
        final var definition = FunctionDefinition.as(KSMLDSL.Functions.TYPE_GENERIC, "committer", new ParameterDefinition[0], PythonSource.of(null, null, "context.commit()"), stringType, null);
        final var function = PythonFunction.forFunction(context, "test", "committer", definition);

        assertThatThrownBy(() -> function.call(Map.of(INJECT_CONTEXT, proxy))).isSameAs(error);
    }

    @Test
    void pythonErrorIsStillWrapped() {
        final var definition = FunctionDefinition.as(KSMLDSL.Functions.TYPE_GENERIC, "failing", new ParameterDefinition[0], PythonSource.of(null, null, "1 / 0"), stringType, null);
        final var function = PythonFunction.forFunction(context, "test", "failing", definition);

        assertThatThrownBy(function::call)
                .hasMessageContaining("Error while executing function test.failing")
                .hasMessageContaining("division by zero");
    }
}
