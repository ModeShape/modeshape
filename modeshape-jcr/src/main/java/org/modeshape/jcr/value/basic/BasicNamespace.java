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

import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.NamespaceRegistry.Namespace;

/**
 * Basic implementation of a {@link NamespaceRegistry} namespace.
 */
@Immutable
public class BasicNamespace implements NamespaceRegistry.Namespace {
    private final String prefix;
    private final String namespaceUri;

    /**
     * Create a namespace instance.
     * 
     * @param prefix the namespace prefix; may not be null (this is not checked)
     * @param namespaceUri the namespace URI; may not be null (this is not checked)
     */
    public BasicNamespace( String prefix,
                           String namespaceUri ) {
        assert prefix != null;
        assert namespaceUri != null;
        this.prefix = prefix;
        this.namespaceUri = namespaceUri;
    }

    @Override
    public String getNamespaceUri() {
        return namespaceUri;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public int hashCode() {
        return namespaceUri.hashCode();
    }

    @Override
    public int compareTo( Namespace that ) {
        if (that == null) return 1;
        if (this == that) return 0;
        return this.getNamespaceUri().compareTo(that.getNamespaceUri());
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Namespace) {
            Namespace that = (Namespace)obj;
            if (!this.namespaceUri.equals(that.getNamespaceUri())) return false;
            // if (!this.prefix.equals(that.getPrefix())) return false;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return prefix + "=" + namespaceUri;
    }
}
