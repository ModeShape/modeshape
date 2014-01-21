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
 * The classes that make up the DDL sequencer, which is capable of parsing the more important DDL statements 
 * from SQL-92, Oracle, Derby, and PostgreSQL, and constructing a graph structure containing a structured 
 * representation of these statements. 
 * The resulting graph structure is largely the same for all dialects, though some dialects have non-standard 
 * additions to their grammar, and thus require dialect-specific additions to the graph structure.
 * <p>
 * The sequencer is designed to behave as intelligently as possible with as little configuration. 
 * Thus, the sequencer automatically determines the dialect used by a given DDL stream. This can be tricky, 
 * of course, since most dialects are very similar and the distinguishing features of a dialect may only be 
 * apparent in some of the statements.
 * </p>
 * <p>
 * To get around this, the sequencer uses a "best fit" algorithm: run the DDL stream through the parser for 
 * each of the dialects, and determine which parser was able to successfully read the greatest number of 
 * statements and tokens.
 * </p>
 * <p>
 * One very interesting capability of this sequencer is that, although only a subset of the (more common) 
 * DDL statements are supported, the sequencer is still extremely functional since it does still add all 
 * statements into the output graph, just without much detail other than just the statement text and the 
 * position in the DDL file. Thus, if a DDL file contains statements the sequencer understands and statements 
 * the sequencer does not understand, the graph will still contain all statements, where those statements 
 * understood by the sequencer will have full detail. Since the underlying parsers are able to operate 
 * upon a single statement, it is possible to go back later (after the parsers have been enhanced to 
 * support additional DDL statements) and re-parse only those incomplete statements in the graph.
 * </p>
 * <p>
 * At this time, the sequencer supports SQL-92 standard DDL as well as dialects from Oracle, Derby, and PostgreSQL. 
 * It supports:
 * <ul>
 * <li>Detailed parsing of CREATE SCHEMA, CREATE TABLE and ALTER TABLE.</li>
 * <li>Partial parsing of DROP statements</li>
 * <li>General parsing of remaining schema definition statements (i.e. CREATE VIEW, CREATE DOMAIN, etc.</li>
 * </ul>
 * Note that the sequencer does not perform detailed parsing of SQL (i.e. SELECT, INSERT, UPDATE, etc....) statements.
 * </p>
 */

package org.modeshape.sequencer.ddl;
