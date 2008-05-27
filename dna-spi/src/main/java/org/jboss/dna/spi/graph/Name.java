/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.spi.graph;

import java.io.Serializable;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextEncoder;

/**
 * A qualified name consisting of a namespace and a local name.
 * @author Randall Hauch
 */
@Immutable
public interface Name extends Comparable<Name>, Serializable {

    /**
     * Get the local name part of this qualified name.
     * @return the local name; never null
     */
    String getLocalName();

    /**
     * Get the URI for the namespace used in this qualified name.
     * @return the URI; never null but possibly empty
     */
    String getNamespaceUri();

    /**
     * Get the string form of the name. The {@link Path#DEFAULT_ENCODER default encoder} is used to encode characters in the local
     * name and namespace.
     * @return the encoded string
     * @see #getString(TextEncoder)
     */
    public String getString();

    /**
     * Get the encoded string form of the name, using the supplied encoder to encode characters in the local name and namespace.
     * @param encoder the encoder to use, or null if the {@link Path#DEFAULT_ENCODER default encoder} should be used
     * @return the encoded string
     * @see #getString()
     */
    public String getString( TextEncoder encoder );

    /**
     * Get the string form of the name, using the supplied namespace registry to convert the
     * {@link #getNamespaceUri() namespace URI} to a prefix. The {@link Path#DEFAULT_ENCODER default encoder} is used to encode
     * characters in each of the path segments.
     * @param namespaceRegistry the namespace registry that should be used to obtain the prefix for the
     * {@link Name#getNamespaceUri() namespace URI}
     * @return the encoded string
     * @throws IllegalArgumentException if the namespace registry is null
     * @see #getString(NamespaceRegistry,TextEncoder)
     */
    public String getString( NamespaceRegistry namespaceRegistry );

    /**
     * Get the encoded string form of the name, using the supplied namespace registry to convert the
     * {@link #getNamespaceUri() namespace URI} to a prefix.
     * @param namespaceRegistry the namespace registry that should be used to obtain the prefix for the
     * {@link Name#getNamespaceUri() namespace URI}
     * @param encoder the encoder to use, or null if the {@link Path#DEFAULT_ENCODER default encoder} should be used
     * @return the encoded string
     * @throws IllegalArgumentException if the namespace registry is null
     * @see #getString(NamespaceRegistry)
     */
    public String getString( NamespaceRegistry namespaceRegistry, TextEncoder encoder );
}
