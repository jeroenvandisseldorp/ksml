package io.axual.ksml.parser;

import io.axual.ksml.data.schema.DataField;
import io.axual.ksml.data.schema.DataSchema;
import io.axual.ksml.data.schema.DataValue;
import io.axual.ksml.data.schema.StructSchema;
import io.axual.ksml.data.schema.UnionSchema;
import io.axual.ksml.data.type.UserType;
import io.axual.ksml.execution.FatalError;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DefinitionParser<T> extends BaseParser<T> implements StructuredParser<T> {
    public static final String SCHEMA_NAMESPACE = "io.axual.ksml";
    private static final DataSchema DURATION_SCHEMA = new UnionSchema(DataSchema.longSchema(), DataSchema.stringSchema());
    // The global namespace for this definition parser. The namespace is the specified name in the runner configuration
    // for this particular KSML definition. It is used to prefix global names in Kafka Streams (eg. processor names) to
    // ensure no conflict between processor names for two separate definitions.
    private final String namespace;
    protected final StructuredParser<String> codeParser = new StringValueParser(true);

    public DefinitionParser(String namespace) {
        this.namespace = namespace;
    }

    public String namespace() {
        if (namespace != null) return namespace;
        throw FatalError.topologyError("Topology namespace not properly initialized. This is a programming error.");
    }

    protected static StructSchema structSchema(Class<?> clazz, String doc, List<DataField> fields) {
        return structSchema(clazz.getSimpleName(), doc, fields);
    }

    protected static StructSchema structSchema(String name, String doc, List<DataField> fields) {
        return new StructSchema(SCHEMA_NAMESPACE, name, doc, fields);
    }

    private static class FieldParser<V> implements SingleSchemaParser<V>, NamedObjectParser {
        private final DataField field;
        @Getter
        private final StructSchema schema;
        private final Parser<V> valueParser;
        @Getter
        @Setter
        private String defaultName;

        public FieldParser(String childName, boolean required, boolean constant, V valueIfNull, String doc, StructuredParser<V> valueParser) {
            field = new DataField(childName, valueParser.schema(), doc, required, constant, valueIfNull != null ? new DataValue(valueIfNull) : null);
            schema = structSchema(childName, doc, List.of(field));
            this.valueParser = valueParser;
        }

        @Override
        public V parse(YamlNode node) {
            try {
                if (node == null) return null;
                final var child = node.get(field.name());
                return child != null ? valueParser.parse(child) : null;
            } catch (Exception e) {
                throw FatalError.parseError(node, e.getMessage());
            }
        }
    }

    private static class ValueStructsParser<V> implements MultiSchemaParser<V> {
        @Getter
        private final List<StructSchema> schemas = new ArrayList<>();
        private final Parser<V> valueParser;

        public ValueStructsParser(String name, String doc, List<MultiSchemaParser<?>> subParsers, Parser<V> valueParser) {
            for (final var subParser : subParsers) {
                Utils.addToSchemas(schemas, name, doc, subParser);
            }
            this.valueParser = valueParser;
        }

        @Override
        public V parse(YamlNode node) {
            try {
                return valueParser.parse(node);
            } catch (Exception e) {
                throw FatalError.parseError(node, e.getMessage());
            }
        }
    }

    protected <V> SingleSchemaParser<V> freeField(String childName, boolean required, V valueIfNull, String doc, StructuredParser<V> parser) {
        return new FieldParser<>(childName, required, false, valueIfNull, doc, parser);
    }

    protected SingleSchemaParser<Boolean> booleanField(String childName, boolean required, String doc) {
        return booleanField(childName, required, false, doc);
    }

    protected SingleSchemaParser<Boolean> booleanField(String childName, boolean required, Boolean valueIfNull, String doc) {
        return freeField(childName, required, valueIfNull, doc, StructuredParser.of(YamlNode::asBoolean, DataSchema.booleanSchema()));
    }

    protected SingleSchemaParser<Duration> durationField(String childName, boolean required, String doc) {
        return freeField(childName, required, null, doc, StructuredParser.of(node -> parseDuration(stringValueParser.parse(node)), DURATION_SCHEMA));
    }

    protected SingleSchemaParser<Integer> integerField(String childName, boolean required, String doc) {
        return integerField(childName, required, null, doc);
    }

    protected SingleSchemaParser<Integer> integerField(String childName, boolean required, Integer valueIfNull, String doc) {
        return freeField(childName, required, valueIfNull, doc, StructuredParser.of(YamlNode::asInt, DataSchema.integerSchema()));
    }

    protected SingleSchemaParser<String> fixedStringField(String childName, boolean required, String fixedValue, String doc) {
        return stringField(childName, required, true, fixedValue, doc + ", fixed value \"" + fixedValue + "\"");
    }

    protected SingleSchemaParser<String> stringField(String childName, boolean required, String doc) {
        return stringField(childName, required, null, doc);
    }

    protected SingleSchemaParser<String> stringField(String childName, boolean required, String valueIfNull, String doc) {
        return stringField(childName, required, false, valueIfNull, doc);
    }

    protected SingleSchemaParser<String> stringField(String childName, boolean required, boolean constant, String valueIfNull, String doc) {
        return new FieldParser<>(childName, required, constant, valueIfNull, doc, stringValueParser);
    }

    protected SingleSchemaParser<String> codeField(String childName, boolean required, String doc) {
        return codeField(childName, required, null, doc);
    }

    protected SingleSchemaParser<String> codeField(String childName, boolean required, String valueIfNull, String doc) {
        return new FieldParser<>(childName, required, false, valueIfNull, doc, codeParser);
    }

    protected SingleSchemaParser<UserType> userTypeField(String childName, boolean required, String doc) {
        final var stringParser = stringField(childName, required, null, doc);
        final var field = new DataField(childName, DataSchema.stringSchema(), doc, required, false, null);
        final var schema = structSchema(childName, doc, List.of(field));
        return new SingleSchemaParser<>() {
            @Override
            public UserType parse(YamlNode node) {
                final var type = UserTypeParser.parse(stringParser.parse(node));
                return type != null ? type : UserType.UNKNOWN;
            }

            @Override
            public StructSchema schema() {
                return schema;
            }
        };
    }

    protected <TYPE> SingleSchemaParser<List<TYPE>> listField(String childName, String whatToParse,
                                                              boolean required, String doc, StructuredParser<TYPE> valueParser) {
        return new FieldParser<>(childName, required, false, new ArrayList<>(), doc, new ListWithSchemaParser<>(whatToParse, valueParser));
    }

    protected <TYPE> SingleSchemaParser<Map<String, TYPE>> mapField(String childName, String whatToParse,
                                                                    boolean required, String doc, StructuredParser<TYPE> valueParser) {
        return new FieldParser<>(childName, required, false, new HashMap<>(), doc, new MapWithSchemaParser<>(whatToParse, valueParser));
    }

    protected <TYPE> SingleSchemaParser<TYPE> customField(String childName, boolean required, String
            doc, MultiSchemaParser<TYPE> valueParser) {
        return new FieldParser<>(childName, required, false, null, doc, valueParser);
    }

    public interface Constructor0<RESULT> {
        RESULT construct();
    }

    public interface Constructor1<RESULT, A> {
        RESULT construct(A a);
    }

    public interface Constructor2<RESULT, A, B> {
        RESULT construct(A a, B b);
    }

    public interface Constructor3<RESULT, A, B, C> {
        RESULT construct(A a, B b, C c);
    }

    public interface Constructor4<RESULT, A, B, C, D> {
        RESULT construct(A a, B b, C c, D d);
    }

    public interface Constructor5<RESULT, A, B, C, D, E> {
        RESULT construct(A a, B b, C c, D d, E e);
    }

    public interface Constructor6<RESULT, A, B, C, D, E, F> {
        RESULT construct(A a, B b, C c, D d, E e, F f);
    }

    public interface Constructor7<RESULT, A, B, C, D, E, F, G> {
        RESULT construct(A a, B b, C c, D d, E e, F f, G g);
    }

    public interface Constructor8<RESULT, A, B, C, D, E, F, G, H> {
        RESULT construct(A a, B b, C c, D d, E e, F f, G g, H h);
    }

    public interface Constructor9<RESULT, A, B, C, D, E, F, G, H, I> {
        RESULT construct(A a, B b, C c, D d, E e, F f, G g, H h, I i);
    }

    public interface Constructor10<RESULT, A, B, C, D, E, F, G, H, I, J> {
        RESULT construct(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j);
    }

    public interface Constructor11<RESULT, A, B, C, D, E, F, G, H, I, J, K> {
        RESULT construct(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k);
    }

    protected <TYPE> MultiSchemaParser<TYPE> structParser(Class<TYPE> resultClass, String doc, Constructor0<TYPE> constructor) {
        return new ValueStructsParser<>(resultClass.getSimpleName(), doc, List.of(), node -> constructor.construct());
    }

    protected <TYPE, A> MultiSchemaParser<TYPE> structParser(Class<TYPE> resultClass, String doc, MultiSchemaParser<A> a, Constructor1<TYPE, A> constructor) {
        return new ValueStructsParser<>(resultClass.getSimpleName(), doc, List.of(a), node -> constructor.construct(a.parse(node)));
    }

    protected <TYPE, A, B> MultiSchemaParser<TYPE> structParser(Class<? extends TYPE> resultClass, String doc, MultiSchemaParser<A> a, MultiSchemaParser<B> b, Constructor2<TYPE, A, B> constructor) {
        return new ValueStructsParser<>(resultClass.getSimpleName(), doc, List.of(a, b), node -> constructor.construct(a.parse(node), b.parse(node)));
    }

    protected <TYPE, A, B, C> MultiSchemaParser<TYPE> structParser(Class<? extends TYPE> resultClass, String doc, MultiSchemaParser<A> a, MultiSchemaParser<B> b, MultiSchemaParser<C> c, Constructor3<TYPE, A, B, C> constructor) {
        return new ValueStructsParser<>(resultClass.getSimpleName(), doc, List.of(a, b, c), node -> constructor.construct(a.parse(node), b.parse(node), c.parse(node)));
    }

    protected <TYPE, A, B, C, D> MultiSchemaParser<TYPE> structParser(Class<? extends TYPE> resultClass, String doc, MultiSchemaParser<A> a, MultiSchemaParser<B> b, MultiSchemaParser<C> c, MultiSchemaParser<D> d, Constructor4<TYPE, A, B, C, D> constructor) {
        return new ValueStructsParser<>(resultClass.getSimpleName(), doc, List.of(a, b, c, d), node -> constructor.construct(a.parse(node), b.parse(node), c.parse(node), d.parse(node)));
    }

    protected <TYPE, A, B, C, D, E> MultiSchemaParser<TYPE> structParser(Class<? extends TYPE> resultClass, String doc, MultiSchemaParser<A> a, MultiSchemaParser<B> b, MultiSchemaParser<C> c, MultiSchemaParser<D> d, MultiSchemaParser<E> e, Constructor5<TYPE, A, B, C, D, E> constructor) {
        return new ValueStructsParser<>(resultClass.getSimpleName(), doc, List.of(a, b, c, d, e), node -> constructor.construct(a.parse(node), b.parse(node), c.parse(node), d.parse(node), e.parse(node)));
    }

    protected <TYPE, A, B, C, D, E, F> MultiSchemaParser<TYPE> structParser(Class<? extends TYPE> resultClass, String doc, MultiSchemaParser<A> a, MultiSchemaParser<B> b, MultiSchemaParser<C> c, MultiSchemaParser<D> d, MultiSchemaParser<E> e, MultiSchemaParser<F> f, Constructor6<TYPE, A, B, C, D, E, F> constructor) {
        return new ValueStructsParser<>(resultClass.getSimpleName(), doc, List.of(a, b, c, d, e, f), node -> constructor.construct(a.parse(node), b.parse(node), c.parse(node), d.parse(node), e.parse(node), f.parse(node)));
    }

    protected <TYPE, A, B, C, D, E, F, G> MultiSchemaParser<TYPE> structParser(Class<? extends TYPE> resultClass, String doc, MultiSchemaParser<A> a, MultiSchemaParser<B> b, MultiSchemaParser<C> c, MultiSchemaParser<D> d, MultiSchemaParser<E> e, MultiSchemaParser<F> f, MultiSchemaParser<G> g, Constructor7<TYPE, A, B, C, D, E, F, G> constructor) {
        return new ValueStructsParser<>(resultClass.getSimpleName(), doc, List.of(a, b, c, d, e, f, g), node -> constructor.construct(a.parse(node), b.parse(node), c.parse(node), d.parse(node), e.parse(node), f.parse(node), g.parse(node)));
    }

    protected <TYPE, A, B, C, D, E, F, G, H> MultiSchemaParser<TYPE> structParser(Class<? extends TYPE> resultClass, String doc, MultiSchemaParser<A> a, MultiSchemaParser<B> b, MultiSchemaParser<C> c, MultiSchemaParser<D> d, MultiSchemaParser<E> e, MultiSchemaParser<F> f, MultiSchemaParser<G> g, MultiSchemaParser<H> h, Constructor8<TYPE, A, B, C, D, E, F, G, H> constructor) {
        return new ValueStructsParser<>(resultClass.getSimpleName(), doc, List.of(a, b, c, d, e, f, g, h), node -> constructor.construct(a.parse(node), b.parse(node), c.parse(node), d.parse(node), e.parse(node), f.parse(node), g.parse(node), h.parse(node)));
    }

    protected <TYPE, A, B, C, D, E, F, G, H, I> MultiSchemaParser<TYPE> structParser(Class<? extends TYPE> resultClass, String doc, MultiSchemaParser<A> a, MultiSchemaParser<B> b, MultiSchemaParser<C> c, MultiSchemaParser<D> d, MultiSchemaParser<E> e, MultiSchemaParser<F> f, MultiSchemaParser<G> g, MultiSchemaParser<H> h, MultiSchemaParser<I> i, Constructor9<TYPE, A, B, C, D, E, F, G, H, I> constructor) {
        return new ValueStructsParser<>(resultClass.getSimpleName(), doc, List.of(a, b, c, d, e, f, g, h, i), node -> constructor.construct(a.parse(node), b.parse(node), c.parse(node), d.parse(node), e.parse(node), f.parse(node), g.parse(node), h.parse(node), i.parse(node)));
    }

    protected <TYPE, A, B, C, D, E, F, G, H, I, J> MultiSchemaParser<TYPE> structParser(Class<? extends TYPE> resultClass, String doc, MultiSchemaParser<A> a, MultiSchemaParser<B> b, MultiSchemaParser<C> c, MultiSchemaParser<D> d, MultiSchemaParser<E> e, MultiSchemaParser<F> f, MultiSchemaParser<G> g, MultiSchemaParser<H> h, MultiSchemaParser<I> i, MultiSchemaParser<J> j, Constructor10<TYPE, A, B, C, D, E, F, G, H, I, J> constructor) {
        return new ValueStructsParser<>(resultClass.getSimpleName(), doc, List.of(a, b, c, d, e, f, g, h, i, j), node -> constructor.construct(a.parse(node), b.parse(node), c.parse(node), d.parse(node), e.parse(node), f.parse(node), g.parse(node), h.parse(node), i.parse(node), j.parse(node)));
    }

    protected <TYPE, A, B, C, D, E, F, G, H, I, J, K> MultiSchemaParser<TYPE> structParser(Class<? extends TYPE> resultClass, String doc, MultiSchemaParser<A> a, MultiSchemaParser<B> b, MultiSchemaParser<C> c, MultiSchemaParser<D> d, MultiSchemaParser<E> e, MultiSchemaParser<F> f, MultiSchemaParser<G> g, MultiSchemaParser<H> h, MultiSchemaParser<I> i, MultiSchemaParser<J> j, MultiSchemaParser<K> k, Constructor11<TYPE, A, B, C, D, E, F, G, H, I, J, K> constructor) {
        return new ValueStructsParser<>(resultClass.getSimpleName(), doc, List.of(a, b, c, d, e, f, g, h, i, j, k), node -> constructor.construct(a.parse(node), b.parse(node), c.parse(node), d.parse(node), e.parse(node), f.parse(node), g.parse(node), h.parse(node), i.parse(node), j.parse(node), k.parse(node)));
    }


}
