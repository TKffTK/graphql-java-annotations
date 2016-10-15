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

	@GraphQLTable
	public class Class1 {
		@Getter
		@Setter
		@GraphQLField
		private NamedClass namedClass;

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
	@GraphQLName("namedTest")
	public class NamedClass {

		@Getter
		@Setter
		@GraphQLField
		private Class1 class1;

		@Getter
		@Setter
		@GraphQLField
		private String field = "field2";

		public NamedClass(Class1 class1) {
			this.class1 = class1;
		}
	}


	@GraphQLTable
	@GraphQLSchemaRootTypeList
	public class OnlyList {
		@GraphQLField
		public OnlyReference onlyReferenceTest;
	}

	@GraphQLTable
	@GraphQLSchemaRootTypeSingle(name = "singleNameTest")
	public class OnlyNamedSingle {
	}

	@GraphQLTable
	@GraphQLSchemaRootTypeNone
	public class OnlyReference {
	}

	@GraphQLTable
	@GraphQLSchemaRootTypeSingle
	@GraphQLSchemaRootTypeList(name = "ListDifferentNames")
	public class DifferentNames {
	}


	class TestDataFetcher implements DataFetcher {

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

	class TestDataFetcherFactory implements DataFetcherFactory {

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
			return this.arguments.get(c);
		}
	}


	@SneakyThrows
	private TestDataFetcherFactory createDataFetcherFactory() {
		Class1 class1 = new Class1();
		NamedClass namedClass = new NamedClass(class1);
		class1.setNamedClass(namedClass);


		Map<Class, Object> objects = new HashMap<>();
		objects.put(Class1.class, class1);
		objects.put(NamedClass.class, namedClass);

		Map<Class, List<GraphQLArgument>> args = new HashMap<>();
		args.put(Class1.class, Arrays.asList(GraphQLArgument.newArgument()
				.name("arg1")
				.type(Scalars.GraphQLInt)
				.build()));
		args.put(NamedClass.class, Arrays.asList(GraphQLArgument.newArgument()
				.name("arg2")
				.type(Scalars.GraphQLInt)
				.build()));
		args.put(OnlyList.class, new ArrayList<>());
		args.put(OnlyNamedSingle.class, new ArrayList<>());
		args.put(OnlyReference.class, new ArrayList<>());
		args.put(DifferentNames.class, new ArrayList<>());
		return new TestDataFetcherFactory(objects, args);
	}


	@SneakyThrows
	@Test
	public void typeAnnotations() {
		GraphQLObjectType testRoot = GraphQLAnnotations.schema("graphql.annotations.GraphQLSchemaTest", createDataFetcherFactory());
		assertNotNull(testRoot);


		assertNotNull(testRoot.getFieldDefinition("namedTest"));
		assertNotNull(testRoot.getFieldDefinition("Class1"));

		assertNotNull(testRoot.getFieldDefinition("OnlyList_list"));
		assertNull(testRoot.getFieldDefinition("OnlyList"));

		assertNotNull(testRoot.getFieldDefinition("singleNameTest"));
		assertNull(testRoot.getFieldDefinition("OnlyNamedSingle"));
		assertNull(testRoot.getFieldDefinition("OnlyNamedSingle_list"));

		assertNull(testRoot.getFieldDefinition("OnlyReference"));
		assertNull(testRoot.getFieldDefinition("OnlyReference_list"));

		assertNotNull(testRoot.getFieldDefinition("DifferentNames"));
		assertNotNull(testRoot.getFieldDefinition("ListDifferentNames"));
	}


	@Test
	@SneakyThrows
	public void basicSchema() {

		GraphQLObjectType testRoot = GraphQLAnnotations.schema("graphql.annotations.GraphQLSchemaTest", createDataFetcherFactory());

		assertNotNull(testRoot);

		List<GraphQLFieldDefinition> fields = testRoot.getFieldDefinitions();
		assertEquals(fields.size(), 8);


		assertNotNull(testRoot.getFieldDefinition("namedTest"));
		assertNotNull(testRoot.getFieldDefinition("Class1"));
	}

	@Test
	@SneakyThrows
	public void basicData() {

		GraphQLObjectType testRoot = GraphQLAnnotations.schema("graphql.annotations.GraphQLSchemaTest", createDataFetcherFactory());

		GraphQL graphql = new GraphQL(newSchema().query(testRoot).build());

		ExecutionResult result = graphql.execute("{Class1 { field } }");
		String actual = result.getData().toString();
		assertEquals(actual, "{Class1={field=field1}}");

	}

	@Test
	@SneakyThrows
	public void listData() {

		GraphQLObjectType testRoot = GraphQLAnnotations.schema("graphql.annotations.GraphQLSchemaTest", createDataFetcherFactory());

		GraphQL graphql = new GraphQL(newSchema().query(testRoot).build());

		ExecutionResult result = graphql.execute("{Class1_list { field } }");
		String actual = result.getData().toString();
		assertEquals(actual, "{Class1_list=[{field=field1}]}");

	}

	@Test
	@SneakyThrows
	public void objectReferences() {

		GraphQLObjectType testRoot = GraphQLAnnotations.schema("graphql.annotations.GraphQLSchemaTest", createDataFetcherFactory());

		GraphQL graphql = new GraphQL(newSchema().query(testRoot).build());

		ExecutionResult result;
		String actual;
		result = graphql.execute("{Class1 {namedClass { field } } }");
		actual = result.getData().toString();
		assertEquals(actual, "{Class1={namedClass={field=field2}}}");

		result = graphql.execute("{namedTest {class1 { field } } } ");
		actual = result.getData().toString();
		assertEquals(actual, "{namedTest={class1={field=field1}}}");

		result = graphql.execute("{namedTest {class1 {namedClass { field } } } }");
		actual = result.getData().toString();
		assertEquals(actual, "{namedTest={class1={namedClass={field=field2}}}}");

	}

	@Test
	@SneakyThrows
	public void arguments() {

		GraphQLObjectType testRoot = GraphQLAnnotations.schema("graphql.annotations.GraphQLSchemaTest", createDataFetcherFactory());

		assertEquals(testRoot.getFieldDefinition("Class1").getArguments().size(), 1);
		assertEquals(testRoot.getFieldDefinition("namedTest").getArguments().size(), 1);

		assertNotNull(testRoot.getFieldDefinition("Class1").getArgument("arg1"));
		assertNotNull(testRoot.getFieldDefinition("namedTest").getArgument("arg2"));


	}


}
