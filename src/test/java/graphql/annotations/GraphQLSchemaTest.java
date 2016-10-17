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
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.testng.annotations.Test;

import java.util.*;


import static graphql.schema.GraphQLSchema.newSchema;
import static org.testng.Assert.*;


public class GraphQLSchemaTest {

	@GraphQLSchemaRootType(name="class1", returnType=GraphQLSchemaRootType.SINGLE, description = "Test description.")
	@GraphQLSchemaRootType(name="class1_list", returnType=GraphQLSchemaRootType.LIST)
	@GraphQLTable
	public static class Class1 {
		@Getter
		@Setter
		@GraphQLField
		private OtherClass otherClass;

		@GraphQLField
		public OnlyReference onlyReference;

		@GraphQLField
		private String field = "field1";

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}
	}


	@GraphQLTable
	@GraphQLSchemaRootType(name="NamedRoot", returnType=GraphQLSchemaRootType.SINGLE)
	@GraphQLSchemaRootType(name="NamedRoots", returnType=GraphQLSchemaRootType.LIST)
	public static class OtherClass {

		@Getter
		@Setter
		@GraphQLField
		private Class1 class1;

		@Getter
		@Setter
		@GraphQLField
		private String field = "field2";

		public OtherClass(Class1 class1) {
			this.class1 = class1;
		}
	}


	@GraphQLTable
	@GraphQLSchemaRootType(name="list", returnType=GraphQLSchemaRootType.LIST)
	public  static class OnlyList {
	}

	@GraphQLTable
	@GraphQLSchemaRootType(name="single", returnType=GraphQLSchemaRootType.SINGLE)
	public  static class OnlySingle {
	}

	@GraphQLTable
	public  static class OnlyReference {
		@GraphQLField
		public String f = "field";
	}


	@GraphQLTable
	@GraphQLDataFetcher(DataFetcherTestFetcher.class)
	@GraphQLSchemaRootType(name="dataFetcherTest", returnType=GraphQLSchemaRootType.SINGLE)
	@GraphQLSchemaRootType(name="dataFetcherTests", returnType=GraphQLSchemaRootType.LIST)
	public static class DataFetcherTest {
		@GraphQLField
		public String field;

		public DataFetcherTest(String f) {
			this.field = f;
		}
	}

	static class TestDataFetcher implements DataFetcher {

		public TestDataFetcher(Object object) {
			this.object = object;
		}

		private Object object;

		@Override
		public Object get(DataFetchingEnvironment environment) {

			if(environment.getFieldType().getClass().equals(GraphQLList.class)) {
				//return getList(dataFetchingEnvironment);

				List retList = new ArrayList();

				retList.add(object);

				return retList;
			}

			return object;
		}
	}

	public static class DataFetcherTestFetcher implements DataFetcher {

		public DataFetcherTestFetcher() {}


		@Override
		public Object get(DataFetchingEnvironment environment) {
			if(environment.getFieldType().getClass().equals(GraphQLList.class)) {
				//return getList(dataFetchingEnvironment);

				List retList = new ArrayList();

				retList.add(new DataFetcherTest("listTest"));
				retList.add(new DataFetcherTest("SecondListTest"));

				return retList;
			}

			return new DataFetcherTest("singleTest");
		}
	}

	static class TestDataFetcherFactory implements DataFetcherFactory {

		Map<Class, Object> objects;
		Map<Class, List<GraphQLArgument>> arguments;

		public TestDataFetcherFactory(Map<Class, Object> objects, Map<Class, List<GraphQLArgument>> arguments) {
			this.objects = objects;
			this.arguments = arguments;
		}

		@Override
		public DataFetcher getDataFetcher(Class c, graphql.schema.GraphQLType type) {
			return new TestDataFetcher(objects.get(c));
		}

		@Override
		public List<GraphQLArgument> getSupportedArguments(Class c, graphql.schema.GraphQLType type) {
			if(this.arguments.get(c) == null)
				return new ArrayList<>();

			return this.arguments.get(c);
		}
	}


	@SneakyThrows
	private TestDataFetcherFactory createDataFetcherFactory() {
		Class1 class1 = new Class1();
		OtherClass otherClass = new OtherClass(class1);
		class1.setOtherClass(otherClass);

		class1.onlyReference = new OnlyReference();


		Map<Class, Object> objects = new HashMap<>();
		objects.put(Class1.class, class1);
		objects.put(OtherClass.class, otherClass);

		Map<Class, List<GraphQLArgument>> args = new HashMap<>();
		args.put(Class1.class, Arrays.asList(GraphQLArgument.newArgument()
				.name("arg1")
				.type(Scalars.GraphQLInt)
				.build()));
		args.put(OtherClass.class, Arrays.asList(GraphQLArgument.newArgument()
				.name("arg2")
				.type(Scalars.GraphQLInt)
				.build()));
		args.put(OnlyList.class, new ArrayList<>());
		args.put(OnlyReference.class, new ArrayList<>());
		return new TestDataFetcherFactory(objects, args);
	}


	@SneakyThrows
	@Test
	public void typeAnnotations() {
		GraphQLObjectType testRoot = GraphQLAnnotations.schema("graphql.annotations.GraphQLSchemaTest", createDataFetcherFactory());
		assertNotNull(testRoot);



		assertNotNull(testRoot.getFieldDefinition("class1"));
		assertNotNull(testRoot.getFieldDefinition("class1_list"));
		assertNotNull(testRoot.getFieldDefinition("NamedRoot"));
		assertNotNull(testRoot.getFieldDefinition("NamedRoots"));

		assertNotNull(testRoot.getFieldDefinition("list"));
		assertNotNull(testRoot.getFieldDefinition("single"));

		assertNotNull(testRoot.getFieldDefinition("dataFetcherTest"));
		assertNotNull(testRoot.getFieldDefinition("dataFetcherTests"));
	}

	@Test
	@SneakyThrows
	public void onlyReference() {
		GraphQLObjectType testRoot = GraphQLAnnotations.schema("graphql.annotations.GraphQLSchemaTest", createDataFetcherFactory());


		GraphQL graphql = new GraphQL(newSchema().query(testRoot).build());

		ExecutionResult result;
		String actual;
		result = graphql.execute("{class1 {onlyReference { f } } }");
		actual = result.getData().toString();
		assertEquals(actual, "{class1={onlyReference={f=field}}}");
	}

	@Test
	@SneakyThrows
	public void dataFetcherTest() {
		GraphQLObjectType testRoot = GraphQLAnnotations.schema("graphql.annotations.GraphQLSchemaTest", createDataFetcherFactory());

		assertNotNull(testRoot.getFieldDefinition("dataFetcherTest"));
		assertNotNull(testRoot.getFieldDefinition("dataFetcherTests"));

		GraphQL graphql = new GraphQL(newSchema().query(testRoot).build());

		ExecutionResult result;
		String actual;
		result = graphql.execute("{dataFetcherTest {field}, dataFetcherTests {field} }");
		actual = result.getData().toString();
		assertEquals(actual, "{dataFetcherTest={field=singleTest}, dataFetcherTests=[{field=listTest}, {field=SecondListTest}]}");
	}

	@Test
	@SneakyThrows
	public void basicSchema() {

		GraphQLObjectType testRoot = GraphQLAnnotations.schema("graphql.annotations.GraphQLSchemaTest", createDataFetcherFactory());

		assertNotNull(testRoot);

		List<GraphQLFieldDefinition> fields = testRoot.getFieldDefinitions();
		assertEquals(fields.size(), 8);


		assertNotNull(testRoot.getFieldDefinition("NamedRoot"));
		assertNotNull(testRoot.getFieldDefinition("class1"));
	}


    @Test
    @SneakyThrows
    public void description() {

        GraphQLObjectType testRoot = GraphQLAnnotations.schema("graphql.annotations.GraphQLSchemaTest", createDataFetcherFactory());

        assertEquals(testRoot.getFieldDefinition("class1").getDescription(), "Test description.");


    }

	@Test
	@SneakyThrows
	public void basicData() {

		GraphQLObjectType testRoot = GraphQLAnnotations.schema("graphql.annotations.GraphQLSchemaTest", createDataFetcherFactory());

		GraphQL graphql = new GraphQL(newSchema().query(testRoot).build());

		ExecutionResult result = graphql.execute("{class1 { field } }");
		String actual = result.getData().toString();
		assertEquals(actual, "{class1={field=field1}}");

	}

	@Test
	@SneakyThrows
	public void listData() {

		GraphQLObjectType testRoot = GraphQLAnnotations.schema("graphql.annotations.GraphQLSchemaTest", createDataFetcherFactory());

		GraphQL graphql = new GraphQL(newSchema().query(testRoot).build());

		ExecutionResult result = graphql.execute("{class1_list { field } }");
		String actual = result.getData().toString();
		assertEquals(actual, "{class1_list=[{field=field1}]}");

	}

	@Test
	@SneakyThrows
	public void objectReferences() {

		GraphQLObjectType testRoot = GraphQLAnnotations.schema("graphql.annotations.GraphQLSchemaTest", createDataFetcherFactory());

		GraphQL graphql = new GraphQL(newSchema().query(testRoot).build());

		ExecutionResult result;
		String actual;
		result = graphql.execute("{class1 {otherClass { field } } }");
		actual = result.getData().toString();
		assertEquals(actual, "{class1={otherClass={field=field2}}}");

		result = graphql.execute("{NamedRoot {class1 { field } } } ");
		actual = result.getData().toString();
		assertEquals(actual, "{NamedRoot={class1={field=field1}}}");

		result = graphql.execute("{NamedRoot {class1 {otherClass { field } } } }");
		actual = result.getData().toString();
		assertEquals(actual, "{NamedRoot={class1={otherClass={field=field2}}}}");

	}

	@Test
	@SneakyThrows
	public void arguments() {

		GraphQLObjectType testRoot = GraphQLAnnotations.schema("graphql.annotations.GraphQLSchemaTest", createDataFetcherFactory());
		assertNotNull(testRoot);

		assertEquals(testRoot.getFieldDefinition("class1").getArguments().size(), 1);
		assertEquals(testRoot.getFieldDefinition("NamedRoot").getArguments().size(), 1);

		assertNotNull(testRoot.getFieldDefinition("class1").getArgument("arg1"));
		assertNotNull(testRoot.getFieldDefinition("NamedRoot").getArgument("arg2"));


	}


}
