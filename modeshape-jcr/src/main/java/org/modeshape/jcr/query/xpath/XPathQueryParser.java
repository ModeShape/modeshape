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
package org.modeshape.jcr.query.xpath;

import javax.jcr.query.Query;
import org.modeshape.common.text.ParsingException;
import org.modeshape.jcr.query.model.QueryCommand;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.query.parse.InvalidQueryException;
import org.modeshape.jcr.query.parse.QueryParser;
import org.modeshape.jcr.query.xpath.XPath.Component;

/**
 * A {@link QueryParser} implementation that accepts XPath expressions and converts them to a {@link QueryCommand ModeShape
 * Abstract Query Model} representation.
 */
public class XPathQueryParser implements QueryParser {

    static final boolean COLLAPSE_INNER_COMPONENTS = true;
    @SuppressWarnings( "deprecation" )
    private static final String LANGUAGE = Query.XPATH;

    @Override
    public String getLanguage() {
        return LANGUAGE;
    }

    @Override
    public String toString() {
        return LANGUAGE;
    }

    @Override
    public int hashCode() {
        return LANGUAGE.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof QueryParser) {
            QueryParser that = (QueryParser)obj;
            return this.getLanguage().equals(that.getLanguage());
        }
        return false;
    }

    @Override
    public QueryCommand parseQuery( String query,
                                    TypeSystem typeSystem ) throws InvalidQueryException, ParsingException {
        Component xpath = new XPathParser(typeSystem).parseXPath(query);
        // Convert the result into a QueryCommand ...
        QueryCommand command = new XPathToQueryTranslator(typeSystem, query).createQuery(xpath);
        return command;
    }
}
