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

import java.lang.annotation.*;


/**
 * GraphQL Schema root object.
 *
 * When generating graphQL schema with {@link GraphQLAnnotations#schema(String, DataFetcherFactory)},
 * objects with this annotation is generated to be part of the root object.
 *
 *
 */
@Repeatable(GraphQLSchemaRootTypes.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GraphQLSchemaRootType {
    public static int LIST = 1;
    public static int SINGLE = 2;

    String name();
    String description() default "";
    int returnType();

}
