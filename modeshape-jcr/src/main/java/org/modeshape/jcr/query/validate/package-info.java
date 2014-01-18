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
 * This package provides the interfaces that define the tables and columns that can be queried.  Though the query framework does not prescribe 
 * the semantics of a table or column, it does provide
 * a {@link org.modeshape.jcr.query.validate.Schemata} interface that provides access to the immutable
 * {@link org.modeshape.jcr.query.validate.Schemata.Table} and {@link org.modeshape.jcr.query.validate.Schemata.View}
 * definitions (that then contain the {@link org.modeshape.jcr.query.validate.Schemata.Column} 
 * and {@link org.modeshape.jcr.query.validate.Schemata.Key} definitions).
 * <p>
 * Although any Schemata implementaiton can be used, the query framework provides an {@link org.modeshape.jcr.query.validate.ImmutableSchemata}
 * class with a {@link org.modeshape.jcr.query.validate.ImmutableSchemata.Builder builder} with a fluent API
 * that can create the corresponding immutable table, column and key definitions.
 */

package org.modeshape.jcr.query.validate;

