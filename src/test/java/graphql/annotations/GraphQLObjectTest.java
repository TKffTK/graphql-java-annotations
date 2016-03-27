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
import java.util.*;
import java.util.function.Supplier;

import static graphql.Scalars.GraphQLString;
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

    }

    private static class TestDefaults {
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
        assertEquals(fields.size(), 5);

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

    }

    private static class TestObjectInherited extends TestObject {
        @Override // Test overriding field
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

        ExecutionResult result = new GraphQL(schema).execute("{field0}", new TestObject());
        assertEquals(((Map<String, Object>)result.getData()).get("field0"), "test");
        result = new GraphQL(schema).execute("{field0}", new TestObjectInherited());
        assertEquals(((Map<String, Object>)result.getData()).get("field0"), "inherited");
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

    private static class TestObjectInput {
        @GraphQLField
        public TestObject test(TestObject testObject) {
            return testObject;
        }
    }

    @Test @SneakyThrows
    public void inputObjectArgument() {
        GraphQLObjectType object = GraphQLAnnotations.object(TestObjectInput.class);
        GraphQLArgument argument = object.getFieldDefinition("test").getArgument("testObject");
        assertTrue(argument.getType() instanceof GraphQLInputObjectType);
        assertEquals(argument.getName(), "testObject");
    }

    @Test @SneakyThrows
    public void inputObject() {
        GraphQLObjectType object = GraphQLAnnotations.object(TestObjectInput.class);
        GraphQLInputObjectType inputObjectType = GraphQLAnnotations.inputObject(object);
        assertEquals(inputObjectType.getFields().size(), object.getFieldDefinitions().size());
    }
    private static class UUIDTypeFunction implements TypeFunction {
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
}