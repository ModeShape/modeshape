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
package org.modeshape.jcr.api;

import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * Extension of the standard {@link javax.jcr.ValueFactory} which allows conversion to the jcr {@link Value} from additional
 * types.
 * 
 * @author Horia Chiorean
 */
public interface ValueFactory extends javax.jcr.ValueFactory {

    /**
     * Creates a JCR compatible {@link Value} from a {@link Date} instance.
     * 
     * @param value a non-null date instance
     * @return a JCR value
     * @throws ValueFormatException if the given value cannot be converted
     */
    public Value createValue( Date value ) throws ValueFormatException;

    /**
     * Creates a JCR compatible {@link Value} from a {@link URI} instance.
     * 
     * @param value a non-null URI instance
     * @return a JCR value
     * @throws ValueFormatException if the given value cannot be converted
     */
    public Value createValue( URI value ) throws ValueFormatException;

    /**
     * Creates a JCR {@link Binary} value from the given byte array.
     * 
     * @param value a non-null byte array
     * @return a Binary implementation instance
     */
    public Binary createBinary( byte[] value );

    /**
     * Creates a JCR {@link org.modeshape.jcr.api.Binary} value from the given input stream with a hint to the factory (which is
     * passed to the storage layer)
     * 
     * @param value a non-null input stream
     * @param hint a hint that the storage layer may use to make persistence decisions
     * @return a Binary implementation instance
     */
    public Binary createBinary( InputStream value,
                                String hint );

    /**
     * Creates a JCR compliant name string, from the given local name.
     * 
     * @param localName a non-null string.
     * @return a name-compliant string
     */
    public String createName( String localName );

    /**
     * Creates a JCR compliant name string, from the given namespace uri and local name.
     * 
     * @param namespaceUri a non-null string.
     * @param localName a non-null string.
     * @return a name-compliant string
     */
    public String createName( String namespaceUri,
                              String localName );

    /**
     * Returns a <code>Value</code> object of {@link PropertyType#SIMPLE_REFERENCE} that holds the identifier of the specified
     * <code>Node</code>. This <code>Value</code> object can then be used to set a property that will be a reference to that
     * <code>Node</code>.
     * 
     * @param node a <code>Node</code>
     * @return a <code>Value</code> of {@link PropertyType#SIMPLE_REFERENCE}
     * @throws javax.jcr.RepositoryException if an error occurs.
     */
    public Value createSimpleReference( Node node ) throws RepositoryException;
}
