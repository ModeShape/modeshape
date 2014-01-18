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
 * This package defines the {@link org.modeshape.jcr.query.parse.QueryParser} interface, which defines a component that can parse a query represented
 * in a specific language and produce the corresponding {@link org.modeshape.jcr.query.model abstract query model} representation.
 * <p>
 * Several parsers are provided, including one that parses a subset of SQL and another that parses the full-text search
 * expressions. However, other query parsers can easily be created and used.
 * </p>
 */

package org.modeshape.jcr.query.parse;

