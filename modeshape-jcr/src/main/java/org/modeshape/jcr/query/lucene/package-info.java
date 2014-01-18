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
 * <p>
 * The core query engine, which currently uses Lucene for indexing and raw queries, and 
 * the Hibernate Search engine (which does not depend on or use Hibernate Core or JPA)
 * for updating and querying the Lucene indexes.
 * </p>
 */

package org.modeshape.jcr.query.lucene;

