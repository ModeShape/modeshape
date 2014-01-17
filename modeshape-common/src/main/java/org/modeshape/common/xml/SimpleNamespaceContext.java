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
package org.modeshape.common.xml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;

/**
 * A simple {@link NamespaceContext} implementation that maintains the namespace and prefix mappings discovered within an XML
 * document.
 */
public class SimpleNamespaceContext implements NamespaceContext {

    private final Map<String, String> prefixToNamespace = new HashMap<String, String>();

    @Override
    public String getNamespaceURI( String prefix ) {
        return this.prefixToNamespace.get(prefix);
    }

    @Override
    public String getPrefix( String namespaceURI ) {
        for (Map.Entry<String, String> entry : this.prefixToNamespace.entrySet()) {
            if (entry.getValue().equals(namespaceURI)) return entry.getKey();
        }
        return null;
    }

    public SimpleNamespaceContext setNamespace( String prefix,
                                                String namespaceUri ) {
        this.prefixToNamespace.put(prefix, namespaceUri);
        return this;
    }

    @Override
    public Iterator<String> getPrefixes( String namespaceURI ) {
        return this.prefixToNamespace.keySet().iterator();
    }

}
