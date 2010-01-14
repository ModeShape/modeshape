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
package org.modeshape.graph.property.basic;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class SimpleNamespaceRegistryTest extends AbstractNamespaceRegistryTest<SimpleNamespaceRegistry> {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.basic.AbstractNamespaceRegistryTest#createNamespaceRegistry()
     */
    @Override
    protected SimpleNamespaceRegistry createNamespaceRegistry() {
        return new SimpleNamespaceRegistry();
    }

    @Test
    public void shouldGeneratePrefixIfNamespaceUriIsNotRegistered() {
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri3, false), is(nullValue()));
        // Now get the generated prefix ...
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri3, true), is("ns001"));
        // Change the template ...
        namespaceRegistry.setGeneratedPrefixTemplate("xyz0000abc");
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri2, true), is("xyz0002abc"));
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri2, true), is("xyz0002abc"));
        // Change the template again ...
        namespaceRegistry.setGeneratedPrefixTemplate("xyz####abc");
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri1, true), is("xyz3abc"));
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri1, true), is("xyz3abc"));
    }

}
