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

import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.StringUtil;

/**
 * An attribute of an XMI element.
 */
public class XmiAttribute extends XmiBasePart implements XmiDescendent {

    private XmiElement parent;

    /**
     * @param name the attribute name (cannot be <code>null</code> or empty)
     */
    public XmiAttribute( String name ) {
        super(name);
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiBasePart#equals(java.lang.Object)
     */
    @Override
    public boolean equals( final Object obj ) {
        if (!super.equals(obj)) {
            return false;
        }

        if (!(obj instanceof XmiAttribute)) {
            return false;
        }

        final XmiAttribute that = (XmiAttribute)obj;

        // compare parent
        return (this.parent == that.parent);
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiBasePart#getNamespacePrefix()
     */
    @Override
    public String getNamespacePrefix() {
        final String prefix = super.getNamespacePrefix();

        if (StringUtil.isBlank(prefix)) {
            final XmiElement parent = getParent();

            if (parent != null) {
                return parent.getNamespacePrefix();
            }
        }

        return prefix;
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiBasePart#getNamespaceUri()
     */
    @Override
    public String getNamespaceUri() {
        final String uri = super.getNamespaceUri();

        if (StringUtil.isBlank(uri)) {
            final XmiElement parent = getParent();

            if (parent != null) {
                return parent.getNamespaceUri();
            }
        }

        return uri;
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiDescendent#getParent()
     */
    @Override
    public XmiElement getParent() {
        return this.parent;
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiBasePart#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(super.hashCode(), this.parent);
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiDescendent#setParent(org.modeshape.sequencer.teiid.xmi.XmiElement)
     */
    @Override
    public void setParent( final XmiElement parent ) {
        this.parent = parent;
    }
}
