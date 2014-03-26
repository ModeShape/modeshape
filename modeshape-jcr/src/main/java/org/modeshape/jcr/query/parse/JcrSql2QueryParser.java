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

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import org.modeshape.jcr.GraphI18n;
import org.modeshape.jcr.JcrValueFactory;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.query.JcrTypeSystem;
import org.modeshape.jcr.query.model.LiteralValue;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * An specialization of the {@link BasicSqlQueryParser} that uses a different language name that matches the JCR 2.0
 * specification.
 */
public class JcrSql2QueryParser extends BasicSqlQueryParser {

    public static final String LANGUAGE = Query.JCR_SQL2;

    /**
     * @see org.modeshape.jcr.query.parse.QueryParser#getLanguage()
     */
    @Override
    public String getLanguage() {
        return LANGUAGE;
    }

    /**
     * @see org.modeshape.jcr.query.parse.BasicSqlQueryParser#literal(TypeSystem, Object)
     */
    @Override
    protected LiteralValue literal( TypeSystem typeSystem,
                                    Object value ) throws ValueFormatException {
        JcrValueFactory factory = ((JcrTypeSystem)typeSystem).getValueFactory();
        Value jcrValue = null;
        if (value instanceof String) {
            jcrValue = factory.createValue((String)value);
        } else if (value instanceof Boolean) {
            jcrValue = factory.createValue(((Boolean)value).booleanValue());
        } else if (value instanceof Binary) {
            jcrValue = factory.createValue((Binary)value);
        } else if (value instanceof DateTime) {
            jcrValue = factory.createValue(((DateTime)value).toCalendar());
        } else if (value instanceof Calendar) {
            jcrValue = factory.createValue((Calendar)value);
        } else if (value instanceof BigDecimal) {
            jcrValue = factory.createValue((BigDecimal)value);
        } else if (value instanceof Double) {
            jcrValue = factory.createValue((Double)value);
        } else if (value instanceof Long) {
            jcrValue = factory.createValue((Long)value);
        } else if (value instanceof Reference) {
            jcrValue = factory.createValue((Reference)value);
        } else if (value instanceof URI) {
            jcrValue = factory.createValue((URI)value);
        } else if (value instanceof InputStream) {
            Binary binary = factory.createBinary((InputStream)value);
            jcrValue = factory.createValue(binary);
        } else if (value instanceof Name || value instanceof Path) {
            // Convert first to a string ...
            String strValue = typeSystem.getStringFactory().create(value);
            jcrValue = factory.createValue(strValue);
        } else if (value instanceof Node) {
            try {
                jcrValue = factory.createValue((Node)value);
            } catch (RepositoryException e) {
                throw new ValueFormatException(value, PropertyType.REFERENCE,
                                               GraphI18n.errorConvertingType.text(Node.class.getSimpleName(),
                                                                                  Reference.class.getSimpleName(), value), e);
            }
        } else {
            jcrValue = factory.createValue(value.toString());
        }
        return new LiteralValue(jcrValue, value);
    }

}
