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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.JcrMixLexicon;
import org.modeshape.jcr.JcrNtLexicon;
import org.modeshape.jcr.ModeShapeLexicon;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;

/**
 * A basic implementation of {@link Name}.
 */
@Immutable
public class BasicName implements Name {

    private static final Map<String, String> BUILT_IN_NAMESPACES;

    static {
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put(JcrLexicon.Namespace.URI, JcrLexicon.Namespace.PREFIX);
        namespaces.put(JcrMixLexicon.Namespace.URI, JcrMixLexicon.Namespace.PREFIX);
        namespaces.put(JcrNtLexicon.Namespace.URI, JcrNtLexicon.Namespace.PREFIX);
        namespaces.put(ModeShapeLexicon.Namespace.URI, ModeShapeLexicon.Namespace.PREFIX);
        BUILT_IN_NAMESPACES = Collections.unmodifiableMap(namespaces);
    }

    private String trimNonEmptyStrings( String value ) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.length() == 0 ? value : trimmed;
    }

    /**
     */
    private static final long serialVersionUID = -1737537720336990144L;
    private final String namespaceUri;
    private final String localName;
    private final int hc;

    public BasicName( String namespaceUri,
                      String localName ) {
        CheckArg.isNotNull(localName, "localName");
        this.namespaceUri = namespaceUri != null ? namespaceUri.trim() : "";
        this.localName = trimNonEmptyStrings(localName);
        this.hc = HashCode.compute(this.namespaceUri, this.localName);
    }

    @Override
    public String getLocalName() {
        return this.localName;
    }

    @Override
    public String getNamespaceUri() {
        return this.namespaceUri;
    }

    @Override
    public String getString() {
        return getString(Path.DEFAULT_ENCODER);
    }

    @Override
    public String getString( TextEncoder encoder ) {
        if (this.getNamespaceUri().length() == 0) {
            if (this.getLocalName().equals(Path.SELF)) return Path.SELF;
            if (this.getLocalName().equals(Path.PARENT)) return Path.PARENT;
        }
        if (encoder == null) encoder = Path.DEFAULT_ENCODER;

        if (namespaceUri.length() > 0) {
            String prefix = BUILT_IN_NAMESPACES.get(namespaceUri);
            if (prefix != null) return prefix + ":" + encoder.encode(this.localName);
            return "{" + encoder.encode(this.namespaceUri) + "}" + encoder.encode(this.localName);
        }
        return encoder.encode(this.localName);
    }

    @Override
    public String getString( NamespaceRegistry namespaceRegistry ) {
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        String prefix = namespaceRegistry.getPrefixForNamespaceUri(this.namespaceUri, true);
        if (prefix != null && prefix.length() != 0) {
            return prefix + ":" + this.localName;
        }
        return this.localName;
    }

    @Override
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder ) {
        // This is the most-often used method, so implement it directly
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        String prefix = namespaceRegistry.getPrefixForNamespaceUri(this.namespaceUri, true);
        if (prefix != null && prefix.length() != 0) {
            return encoder.encode(prefix) + ":" + encoder.encode(this.localName);
        }
        return encoder.encode(this.localName);
    }

    @Override
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder,
                             TextEncoder delimiterEncoder ) {
        if (namespaceRegistry == null) {
            if (this.getNamespaceUri().length() == 0) {
                if (this.getLocalName().equals(Path.SELF)) return Path.SELF;
                if (this.getLocalName().equals(Path.PARENT)) return Path.PARENT;
            }
            if (encoder == null) encoder = Path.DEFAULT_ENCODER;
            if (delimiterEncoder != null) {
                String prefix = BUILT_IN_NAMESPACES.get(namespaceUri);
                if (prefix != null) return prefix + ":" + encoder.encode(this.localName);
                return delimiterEncoder.encode("{") + encoder.encode(this.namespaceUri) + delimiterEncoder.encode("}")
                       + encoder.encode(this.localName);
            }
            return "{" + encoder.encode(this.namespaceUri) + "}" + encoder.encode(this.localName);

        }
        if (encoder == null) encoder = Path.DEFAULT_ENCODER;
        String prefix = namespaceRegistry.getPrefixForNamespaceUri(this.namespaceUri, true);
        if (prefix != null && prefix.length() != 0) {
            String delim = delimiterEncoder != null ? delimiterEncoder.encode(":") : ":";
            return encoder.encode(prefix) + delim + encoder.encode(this.localName);
        }
        return encoder.encode(this.localName);
    }

    @Override
    public int compareTo( Name that ) {
        if (that == this) return 0;
        int diff = this.getLocalName().compareTo(that.getLocalName());
        if (diff != 0) return diff;
        diff = this.getNamespaceUri().compareTo(that.getNamespaceUri());
        return diff;
    }

    @Override
    public int hashCode() {
        return this.hc;
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Name) {
            Name that = (Name)obj;
            if (!this.getLocalName().equals(that.getLocalName())) return false;
            if (!this.getNamespaceUri().equals(that.getNamespaceUri())) return false;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "{" + this.namespaceUri + "}" + this.localName;
    }
}
