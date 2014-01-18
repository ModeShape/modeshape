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
package org.modeshape.jcr.query.parse;

import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

/**
 * A parser for the JCR Query Object Model language.
 * <p>
 * The JCR 2.0 specification does not explicitly define a textual query language or grammar for the Query Object Model (the
 * grammar in the specification is written more in terms of the code of a Java-like language). However, per the Section 6.9 of the
 * JCR 2.0 specification, the {@link QueryManager#createQuery(String, String)} method <blockquote>"is used for languages that are
 * string-based (i.e., most languages, such as JCR-SQL2) as well as for the for the string serializations of non-string-based
 * languages (such as JCR- JQOM)"</blockquote> Since the string serialization (that is, the <code>toString()</code> form) of our
 * QueryCommand and related abstract query model objects are in fact equivalent to JCR-SQL2, this parser simply extends the
 * {@link JcrSql2QueryParser}.
 * </p>
 */
public class JcrQomQueryParser extends JcrSql2QueryParser {

    public static final String LANGUAGE = Query.JCR_JQOM;

    public JcrQomQueryParser() {
        super();
    }

    @Override
    public String getLanguage() {
        return LANGUAGE;
    }
}
