/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr.query;

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

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.QueryParser#getLanguage()
     */
    @Override
    public String getLanguage() {
        return LANGUAGE;
    }
}
