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

import org.jboss.dna.common.text.TextEncoder;

/**
 * A factory for creating {@link Name names}.
 *
 * @author Randall Hauch
 */
public interface NameFactory extends ValueFactory<Name> {

	String JCR_PRIMARY_TYPE = "jcr:primaryType";

	/**
	 * Create a name from the given namespace URI and local name.
	 * <p>
	 * This method is equivalent to calling {@link #create(String, String, TextEncoder)} with a null encoder.
	 * </p>
	 *
	 * @param namespaceUri the namespace URI
	 * @param localName the local name
	 * @return the new name
	 * @throws IllegalArgumentException if the local name is <code>null</code> or empty
	 */
	Name create( String namespaceUri,
	             String localName );

	/**
	 * Create a name from the given namespace URI and local name.
	 *
	 * @param namespaceUri the namespace URI
	 * @param localName the local name
	 * @param encoder the encoder that should be used to decode the qualified name
	 * @return the new name
	 * @throws IllegalArgumentException if the local name is <code>null</code> or empty
	 */
	Name create( String namespaceUri,
	             String localName,
	             TextEncoder encoder );

	/**
	 * Get the namespace registry.
	 *
	 * @return the namespace registry; never <code>null</code>
	 */
	NamespaceRegistry getNamespaceRegistry();
}
