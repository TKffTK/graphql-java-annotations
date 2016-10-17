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

import graphql.schema.*;
import graphql.schema.GraphQLType;

import java.util.List;

public interface DataFetcherFactory {

	/**
	 * Get datafetcher for class and returntype
	 *
	 * @param c Class
	 * @param returnType Desired returntype
	 *
	 * @return Instance of datafetcher
	 */
	public DataFetcher getDataFetcher(Class c, graphql.schema.GraphQLType returnType);

	/**
	 * Get supported arguments for datafetcher what {@link #getDataFetcher(Class, GraphQLType)} gives.
	 *
	 * @param c Class
	 * @param returnType Desired returntype
	 * @return List of arguments
	 */
	public List<GraphQLArgument> getSupportedArguments(Class c, GraphQLType returnType);
}
