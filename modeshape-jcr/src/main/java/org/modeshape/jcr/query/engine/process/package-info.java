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
 * When executing a query, the <em>processing</em> phase involves "running" each branch in the optimized query plan, determining
 * the set of nodes that satisfies each branch, and then joining each branch into a single result. This package defines several
 * {@link org.modeshape.jcr.query.NodeSequence} implementations that perform various aspects of this processing.
 */

package org.modeshape.jcr.query.engine.process;
