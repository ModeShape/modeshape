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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.jcr.api;

import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import java.util.Date;

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
     * Creates a JCR {@link Binary} value from the given byte array.
     *
     * @param value a non-null byte array
     * @return a Binary implementation instance
     */
    public Binary createBinary( byte[] value );

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
     *
     * @return a name-compliant string
     */
    public String createName( String namespaceUri,
                              String localName );
}
