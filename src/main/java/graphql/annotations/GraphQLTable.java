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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Tell that class is part of GraphQL schema.
 *
 * You can decide what stuff will be include root of graphql schema with these annotations:
 *  * {@link GraphQLSchemaRootTypeList} List of objects
 *  * {@link GraphQLSchemaRootTypeSingle} Single object
 *  * {@link GraphQLSchemaRootTypeNone} Do not include this to schema root, just let other object reference this.
 *
 *  On default, all possibly stuff is created to graphql schema root from this object.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GraphQLTable {
}
