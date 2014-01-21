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
package org.modeshape.jcr.value.basic;

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
     * @see org.modeshape.jcr.value.basic.AbstractNamespaceRegistryTest#createNamespaceRegistry()
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
