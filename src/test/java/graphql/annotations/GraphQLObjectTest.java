/**
 * Copyright 2016 Yurii Rashkovskii
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 */
package graphql.annotations;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.schema.*;
import graphql.schema.GraphQLType;
import lombok.*;
import lombok.experimental.Accessors;
import org.testng.annotations.Test;

import javax.validation.constraints.NotNull;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Supplier;

import static graphql.Scalars.GraphQLString;
import static graphql.annotations.DefaultTypeFunction.instance;
import static graphql.schema.GraphQLFieldDefinition.*;
import static graphql.schema.GraphQLSchema.newSchema;
import static org.testng.Assert.*;

public class GraphQLObjectTest {

    public static class DefaultAValue implements Supplier<Object> {

        @Override
        public Object get() {
            return "default";
        }
    }

    @GraphQLDescription("TestObject object")
    @GraphQLName("TestObject")
    private static class TestObject {
        @GraphQLField
        @GraphQLName("field0")
        @GraphQLDescription("field")
        public
        @NotNull
        String field() {
            return "test";
        }

        @GraphQLField
        public String fieldWithArgs(@NotNull String a, @GraphQLDefaultValue(DefaultAValue.class) @GraphQLDescription("b") String b) {
            return b;
        }

        @GraphQLField
        public String fieldWithArgsAndEnvironment(DataFetchingEnvironment env, String a, String b) {
            return a;
        }

        @GraphQLField
        @Deprecated
        public String deprecated() {
            return null;
        }

        @GraphQLField
        @GraphQLDeprecate("Reason")
        public String deprecate() {
            return null;
        }

        @GraphQLField
        public String publicTest = "public";

        @Getter
        @Setter
        @GraphQLField
        private String privateTest = "private";

    }

    private static class TestDefaults {
    }

    private static class TestObjectNamedArgs {
        @GraphQLField
        public String fieldWithNamedArgs(@GraphQLName("namedArg") String firstArgument) {
            return firstArgument;
        }
    }

    @Test @SneakyThrows
    public void namedFields() {
        GraphQLObjectType object = GraphQLAnnotations.object(TestObjectNamedArgs.class);
        List<GraphQLFieldDefinition> fields = object.getFieldDefinitions();
        assertEquals(fields.size(), 1);

        List<GraphQLArgument> args = fields.get(0).getArguments();
        assertEquals(args.size(), 1);

        GraphQLArgument arg = args.get(0);
        assertEquals(arg.getName(), "namedArg");
    }

    @Test @SneakyThrows
    public void metainformation() {
        GraphQLObjectType object = GraphQLAnnotations.object(TestObject.class);
        assertEquals(object.getName(), "TestObject");
        assertEquals(object.getDescription(), "TestObject object");
    }

    @Test @SneakyThrows
    public void fields() {
        GraphQLObjectType object = GraphQLAnnotations.object(TestObject.class);
        List<GraphQLFieldDefinition> fields = object.getFieldDefinitions();
        assertEquals(fields.size(), 7);

        fields.sort((o1, o2) -> o1.getName().compareTo(o2.getName()));

        assertEquals(fields.get(2).getName(), "field0");
        assertEquals(fields.get(2).getDescription(), "field");
        assertTrue(fields.get(2).getType() instanceof graphql.schema.GraphQLNonNull);
        assertEquals(((graphql.schema.GraphQLNonNull) fields.get(2).getType()).getWrappedType(), GraphQLString);

        assertEquals(fields.get(3).getName(), "fieldWithArgs");
        List<GraphQLArgument> args = fields.get(3).getArguments();
        assertEquals(args.size(), 2);
        assertEquals(args.get(0).getName(), "a");
        assertTrue(args.get(0).getType() instanceof graphql.schema.GraphQLNonNull);
        assertEquals(((graphql.schema.GraphQLNonNull) args.get(0).getType()).getWrappedType(), GraphQLString);
        assertEquals(args.get(1).getName(), "b");
        assertEquals(args.get(1).getType(), GraphQLString);
        assertEquals(args.get(1).getDescription(), "b");

        assertEquals(fields.get(4).getName(), "fieldWithArgsAndEnvironment");
        args = fields.get(4).getArguments();
        assertEquals(args.size(), 2);

        assertEquals(fields.get(1).getName(), "deprecated");
        assertTrue(fields.get(1).isDeprecated());

        assertEquals(fields.get(0).getName(), "deprecate");
        assertTrue(fields.get(0).isDeprecated());
        assertEquals(fields.get(0).getDeprecationReason(), "Reason");

        assertEquals(fields.get(5).getName(), "privateTest");
        assertEquals(fields.get(6).getName(), "publicTest");

    }

    private static class TestObjectInherited extends TestObject {
        @Override @GraphQLName("field1") // Test overriding field
        public String field() {
            return "inherited";
        }
    }

    @Test @SneakyThrows
    public void methodInheritance() {
        GraphQLObjectType object = GraphQLAnnotations.object(TestObject.class);
        GraphQLObjectType objectInherited = GraphQLAnnotations.object(TestObjectInherited.class);
        assertEquals(object.getFieldDefinitions().size(), objectInherited.getFieldDefinitions().size());

        GraphQLSchema schema = newSchema().query(object).build();
        GraphQLSchema schemaInherited = newSchema().query(objectInherited).build();

        ExecutionResult result = new GraphQL(schema).execute("{field0}", new TestObject());
        assertEquals(((Map<String, Object>)result.getData()).get("field0"), "test");
        result = new GraphQL(schemaInherited).execute("{field1}", new TestObjectInherited());
        assertEquals(((Map<String, Object>)result.getData()).get("field1"), "inherited");
    }

    public interface Iface {
        @GraphQLField
        default String field() {
            return "field";
        }
    }

    public static class IfaceImpl implements Iface {}

    @Test @SneakyThrows
    public void interfaceInheritance() {
        GraphQLObjectType object = GraphQLAnnotations.object(IfaceImpl.class);
        assertEquals(object.getFieldDefinitions().size(), 1);
        assertEquals(object.getFieldDefinition("field").getType(), GraphQLString);

    }

    private static class TestAccessors {
        @GraphQLField
        public String getValue() {
            return "hello";
        }

        @GraphQLField
        public String setAnotherValue(String s) {
            return s;
        }
    }

    @Test @SneakyThrows
    public void accessors() {
        GraphQLObjectType object = GraphQLAnnotations.object(TestAccessors.class);
        List<GraphQLFieldDefinition> fields = object.getFieldDefinitions();
        assertEquals(fields.size(), 2);
        fields.sort((o1, o2) -> o1.getName().compareTo(o2.getName()));

        assertEquals(fields.get(1).getName(), "value");
        assertEquals(fields.get(0).getName(), "anotherValue");
    }


    @Test @SneakyThrows
    public void defaults() {
        GraphQLObjectType object = GraphQLAnnotations.object(TestDefaults.class);
        assertEquals(object.getName(), "TestDefaults");
        assertNull(object.getDescription());
    }

    public static class TestField {
        @GraphQLField @GraphQLName("field1")
        public String field = "test";
    }

    @Test @SneakyThrows
    public void field() {
        GraphQLObjectType object = GraphQLAnnotations.object(TestField.class);
        List<GraphQLFieldDefinition> fields = object.getFieldDefinitions();
        assertEquals(fields.size(), 1);
        assertEquals(fields.get(0).getName(), "field1");
    }

    private static class LombokTest {
        @Getter(onMethod = @__(@GraphQLField)) @Accessors(fluent = true)
        private String value;
    }

    @Test @SneakyThrows
    public void lombok() {
        GraphQLObjectType object = GraphQLAnnotations.object(LombokTest.class);
        List<GraphQLFieldDefinition> fields = object.getFieldDefinitions();
        assertEquals(fields.size(), 1);
        assertEquals(fields.get(0).getName(), "value");
    }

    public static class TestFetcher implements DataFetcher {

        @Override
        public Object get(DataFetchingEnvironment environment) {
            return "test";
        }
    }
    private static class TestDataFetcher {

        @GraphQLField
        @GraphQLDataFetcher(TestFetcher.class)
        public String field;

        @GraphQLField
        @GraphQLDataFetcher(TestFetcher.class)
        public String someField() {
            return "not test";
        }

    }

    @Test @SneakyThrows
    public void dataFetcher() {
        GraphQLObjectType object = GraphQLAnnotations.object(TestDataFetcher.class);
        GraphQLSchema schema = newSchema().query(object).build();

        ExecutionResult result = new GraphQL(schema).execute("{field someField}", new TestObject());
        assertTrue(result.getErrors().isEmpty());
        assertEquals(((Map<String, String>)result.getData()).get("field"), "test");
        assertEquals(((Map<String, String>)result.getData()).get("someField"), "test");
    }

    @Test @SneakyThrows
    public void query() {
        GraphQLObjectType object = GraphQLAnnotations.object(TestObject.class);
        GraphQLSchema schema = newSchema().query(object).build();

        ExecutionResult result = new GraphQL(schema).execute("{field0}", new TestObject());
        assertTrue(result.getErrors().isEmpty());
        assertEquals(((Map<String, String>)result.getData()).get("field0"), "test");

        result = new GraphQL(schema).execute("{fieldWithArgs(a: \"test\", b: \"passed\")}", new TestObject());
        assertTrue(result.getErrors().isEmpty());
        assertEquals(((Map<String, String>)result.getData()).get("fieldWithArgs"), "passed");

        result = new GraphQL(schema).execute("{fieldWithArgsAndEnvironment(a: \"test\", b: \"passed\")}", new TestObject());
        assertTrue(result.getErrors().isEmpty());
        assertEquals(((Map<String, String>)result.getData()).get("fieldWithArgsAndEnvironment"), "test");

    }

    @Test @SneakyThrows
    public void queryField() {
        GraphQLObjectType object = GraphQLAnnotations.object(TestField.class);
        GraphQLSchema schema = newSchema().query(object).build();

        ExecutionResult result = new GraphQL(schema).execute("{field1}", new TestField());
        assertTrue(result.getErrors().isEmpty());
        assertEquals(((Map<String, String>)result.getData()).get("field1"), "test");
    }

    @Test @SneakyThrows
    public void defaultArg() {
        GraphQLObjectType object = GraphQLAnnotations.object(TestObject.class);
        GraphQLSchema schema = newSchema().query(object).build();

        ExecutionResult result = new GraphQL(schema).execute("{fieldWithArgs(a: \"test\")}", new TestObject());
        assertTrue(result.getErrors().isEmpty());
        assertEquals(((Map<String, String>)result.getData()).get("fieldWithArgs"), "default");
    }



    public static class Class1 {
        @GraphQLField
        public Class2 class2;
        @GraphQLField
        public String value;
    }

    public static class Class2 {
        @GraphQLField
        public Class1 class1;
        @GraphQLField
        public String value;
    }

    @Test @SneakyThrows
    public void recursiveTypes() {
        GraphQLObjectType object = GraphQLAnnotations.object(Class1.class);
        GraphQLSchema schema = newSchema().query(object).build();

        Class1 class1 = new Class1();
        Class2 class2 = new Class2();
        class1.class2 = class2;
        class2.class1 = class1;
        class2.value = "hello";
        class1.value = "bye";

        ExecutionResult result = new GraphQL(schema).execute("{ class2 { value } }", class1);
        assertTrue(result.getErrors().isEmpty());
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals(((Map<String, Object>) data.get("class2")).get("value"), "hello");

        result = new GraphQL(schema).execute("{ class2 { class1 { value } } }", class1);
        assertTrue(result.getErrors().isEmpty());
        data = (Map<String, Object>) result.getData();
        Map<String, Object> k1 = (Map<String, Object>)((Map<String, Object>) data.get("class2")).get("class1");
        assertEquals(k1.get("value"), "bye");

        result = new GraphQL(schema).execute("{ class2 { class1 { class2 { value } } } }", class1);
        assertTrue(result.getErrors().isEmpty());
    }

    private static class TestCustomType {
        @GraphQLField
        public UUID id() {
            return UUID.randomUUID();
        }
    }

    @Test @SneakyThrows
    public void customType() {
        DefaultTypeFunction.register(UUID.class, new UUIDTypeFunction());
        GraphQLObjectType object = GraphQLAnnotations.object(TestCustomType.class);
        assertEquals(object.getFieldDefinition("id").getType(), GraphQLString);
    }

    private static class TestCustomTypeFunction {
        @GraphQLField @graphql.annotations.GraphQLType(UUIDTypeFunction.class)
        public UUID id() {
            return UUID.randomUUID();
        }
    }

    @Test @SneakyThrows
    public void customTypeFunction() {
        GraphQLObjectType object = GraphQLAnnotations.object(TestCustomTypeFunction.class);
        assertEquals(object.getFieldDefinition("id").getType(), GraphQLString);
    }

    private static class TestInputArgument {
        @GraphQLField public String a;
        @GraphQLField public int b;

        public TestInputArgument(HashMap<String, Object> args) {
            a = (String) args.get("a");
            b = (int) args.get("b");
        }
    }

    private static class TestObjectInput {
        @GraphQLField
        public String test(int other, TestInputArgument arg) {
            return arg.a;
        }
    }

    @Test @SneakyThrows
    public void inputObjectArgument() {
        GraphQLObjectType object = GraphQLAnnotations.object(TestObjectInput.class);
        GraphQLArgument argument = object.getFieldDefinition("test").getArgument("arg");
        assertTrue(argument.getType() instanceof GraphQLInputObjectType);
        assertEquals(argument.getName(), "arg");

        GraphQLSchema schema = newSchema().query(object).build();
        ExecutionResult result = new GraphQL(schema).execute("{ test(arg: { a:\"ok\", b:2 }, other:0) }", new TestObjectInput());
        assertTrue(result.getErrors().isEmpty());
        Map<String, Object> v = (Map<String, Object>) result.getData();
        assertEquals(v.get("test"), "ok");
    }

    @Test @SneakyThrows
    public void inputObject() {
        GraphQLObjectType object = GraphQLAnnotations.object(TestObjectInput.class);
        GraphQLInputObjectType inputObjectType = GraphQLAnnotations.inputObject(object);
        assertEquals(inputObjectType.getFields().size(), object.getFieldDefinitions().size());
    }
    public static class UUIDTypeFunction implements TypeFunction {
        @Override
        public GraphQLType apply(Class<?> aClass, AnnotatedType annotatedType) {
            return GraphQLString;
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class OptionalTest {
        @GraphQLField
        public Optional<String> empty = Optional.empty();
        @GraphQLField
        public Optional<String> nonempty = Optional.of("test");

    }

    @Test @SneakyThrows
    public void queryOptional() {
        GraphQLObjectType object = GraphQLAnnotations.object(OptionalTest.class);
        GraphQLSchema schema = newSchema().query(object).build();

        ExecutionResult result = new GraphQL(schema, new EnhancedExecutionStrategy()).execute("{empty, nonempty}", new OptionalTest());
        assertTrue(result.getErrors().isEmpty());
        Map<String, Object> v = (Map<String, Object>) result.getData();
        assertNull(v.get("empty"));
        assertEquals(v.get("nonempty"), "test");
    }

    @Test @SneakyThrows
    public void optionalInput() {
        GraphQLObjectType object = GraphQLAnnotations.object(OptionalTest.class);
        GraphQLInputObjectType inputObject = GraphQLAnnotations.inputObject(object);
        GraphQLObjectType mutation = GraphQLObjectType.newObject().name("mut").field(newFieldDefinition().name("test").type(object).
                argument(GraphQLArgument.newArgument().type(inputObject).name("input").build()).dataFetcher(environment -> {
                    Map<String, String> input = environment.getArgument("input");
                    return new OptionalTest(Optional.ofNullable(input.get("empty")), Optional.ofNullable(input.get("nonempty")));
                }).build()).build();
        GraphQLSchema schema = newSchema().query(object).mutation(mutation).build();

        ExecutionResult result = new GraphQL(schema, new EnhancedExecutionStrategy()).execute("mutation {test(input: {empty: \"test\"}) { empty nonempty } }", new OptionalTest());
        assertTrue(result.getErrors().isEmpty());
        Map<String, Object> v = (Map<String, Object>) ((Map<String, Object>) result.getData()).get("test");
        assertEquals(v.get("empty"), "test");
        assertNull(v.get("nonempty"));
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class EnumTest {
        public enum E { A, B };
        @GraphQLField
        public E e;
    }

    @Test @SneakyThrows
    public void queryEnum() {
        GraphQLObjectType object = GraphQLAnnotations.object(EnumTest.class);
        GraphQLSchema schema = newSchema().query(object).build();

        ExecutionResult result = new GraphQL(schema, new EnhancedExecutionStrategy()).execute("{e}", new EnumTest(EnumTest.E.B));
        assertTrue(result.getErrors().isEmpty());
        Map<String, Object> v = (Map<String, Object>) result.getData();
        assertEquals(v.get("e"), "B");
    }

    public static class ParametrizedArgsTest {
        @GraphQLField
        public String first(List<String> l) {
            return l.get(0);
        }
    }

    @Test @SneakyThrows
    public void parametrizedArg() {
        GraphQLObjectType object = GraphQLAnnotations.object(ParametrizedArgsTest.class);
        GraphQLInputType t = object.getFieldDefinition("first").getArguments().get(0).getType();
        assertTrue(t instanceof GraphQLList);
        assertEquals(((GraphQLList)t).getWrappedType(), Scalars.GraphQLString);
    }


}