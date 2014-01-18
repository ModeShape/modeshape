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
package org.modeshape.sequencer.teiid.xmi;

import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.StringUtil;

/**
 * A base implementation of a XMI part.
 */
public abstract class XmiBasePart implements XmiPart {

    private final String name; // never null
    private String namespacePrefix;
    private String namespaceUri;
    private String value;

    /**
     * @param name the part name (cannot be <code>null</code> or empty)
     */
    protected XmiBasePart( final String name ) {
        CheckArg.isNotEmpty(name, "name");
        this.name = name;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( final Object obj ) {
        if ((obj == null) || !(obj instanceof XmiBasePart)) {
            return false;
        }

        final XmiBasePart that = (XmiBasePart)obj;

        if (this == that) {
            return true;
        }

        // compare name
        if (StringUtil.isBlank(this.name)) {
            if (!StringUtil.isBlank(that.name)) {
                return false;
            }
        } else {
            if (!this.name.equals(that.name)) {
                return false;
            }
        }

        // compare namespace URI
        if (StringUtil.isBlank(this.namespaceUri)) {
            if (!StringUtil.isBlank(that.namespaceUri)) {
                return false;
            }
        } else {
            if (!this.namespaceUri.equals(that.namespaceUri)) {
                return false;
            }
        }

        // compare namespace prefix
        if (StringUtil.isBlank(this.namespacePrefix)) {
            if (!StringUtil.isBlank(that.namespacePrefix)) {
                return false;
            }
        } else {
            if (!this.namespacePrefix.equals(that.namespacePrefix)) {
                return false;
            }
        }

        // compare value
        if (StringUtil.isBlank(this.value)) {
            if (!StringUtil.isBlank(that.value)) {
                return false;
            }
        } else {
            if (!this.value.equals(that.value)) {
                return false;
            }
        }

        return true;
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiPart#getName()
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiPart#getNamespacePrefix()
     */
    @Override
    public String getNamespacePrefix() {
        return this.namespacePrefix;
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiPart#getNamespaceUri()
     */
    @Override
    public String getNamespaceUri() {
        return this.namespaceUri;
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiPart#getQName()
     */
    @Override
    public String getQName() {
        if (StringUtil.isBlank(getNamespacePrefix())) {
            return getName();
        }

        return getNamespacePrefix() + ':' + getName();
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiPart#getValue()
     */
    @Override
    public String getValue() {
        return this.value;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(this.name, this.namespacePrefix, this.namespaceUri, this.value);
    }

    /**
     * @param newNamespacePrefix the new namespace prefix (can be <code>null</code> or empty)
     */
    public void setNamespacePrefix( final String newNamespacePrefix ) {
        this.namespacePrefix = newNamespacePrefix;
    }

    /**
     * @param newNamespaceUri the new namespace URI (can be <code>null</code> or empty)
     */
    public void setNamespaceUri( final String newNamespaceUri ) {
        this.namespaceUri = newNamespaceUri;
    }

    /**
     * @param newValue the new part value (can be <code>null</code> or empty)
     */
    public void setValue( final String newValue ) {
        this.value = newValue;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getQName() + '=' + getValue();
    }
}
