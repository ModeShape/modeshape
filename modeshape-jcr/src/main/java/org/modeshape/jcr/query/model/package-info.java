/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * <p>The Abstract Query Model is a vocabulary that can be used to construct a language-neutral representation of a query.
 * It consists of a family of Java classes each represent the important semantic elements
 * needed to fully define a query.  A {@link org.modeshape.jcr.query.model.QueryCommand} is the interface that represents a top-level query,
 * with the {@link org.modeshape.jcr.query.model.Query} and {@link org.modeshape.jcr.query.model.SetQuery} classes being the two implementations.
 * </p>
 * <p>
 * There are two ways to construct abstract query models.  The first is to programmatically construct a query
 * model using the {@link org.modeshape.jcr.query.QueryBuilder}, which provides a fluent API that makes it easy to create a query
 * with Java code.  The second (and more common) approach is to use a {@link org.modeshape.jcr.query.parse.QueryParser}
 * that parses a query represented in a specific language (like SQL or XPath) and then creates the query's equivalent abstract query model.
 * </p>
 * <p>
 * The abstract query model classes are immutable, making them very easily shared or reused if that is advantageous.
 * </p>
 */

package org.modeshape.jcr.query.model;

