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
     * {@inheritDoc}
     * 
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
     * {@inheritDoc}
     * 
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
     * {@inheritDoc}
     * 
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
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.teiid.xmi.XmiDescendent#getParent()
     */
    @Override
    public XmiElement getParent() {
        return this.parent;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.teiid.xmi.XmiBasePart#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(super.hashCode(), this.parent);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.teiid.xmi.XmiDescendent#setParent(org.modeshape.sequencer.teiid.xmi.XmiElement)
     */
    @Override
    public void setParent( final XmiElement parent ) {
        this.parent = parent;
    }
}
