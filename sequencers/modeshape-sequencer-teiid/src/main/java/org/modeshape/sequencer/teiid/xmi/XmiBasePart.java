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
    protected XmiBasePart(String name) {
        CheckArg.isNotEmpty(name, "name");
        this.name = name;
    }
    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ((obj == null) || !(obj instanceof XmiBasePart)) {
            return false;
        }

        XmiBasePart that = (XmiBasePart)obj;

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
     * {@inheritDoc}
     *
     * @see org.modeshape.sequencer.teiid.xmi.XmiPart#getName()
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.modeshape.sequencer.teiid.xmi.XmiPart#getNamespacePrefix()
     */
    @Override
    public String getNamespacePrefix() {
        return this.namespacePrefix;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.modeshape.sequencer.teiid.xmi.XmiPart#getNamespaceUri()
     */
    @Override
    public String getNamespaceUri() {
        return this.namespaceUri;
    }

    /**
     * {@inheritDoc}
     *
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
     * {@inheritDoc}
     *
     * @see org.modeshape.sequencer.teiid.xmi.XmiPart#getValue()
     */
    @Override
    public String getValue() {
        return this.value;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(this.name, this.namespacePrefix, this.namespaceUri, this.value);
    }

    public void setNamespacePrefix( final String newNamespacePrefix ) {
        this.namespacePrefix = newNamespacePrefix;
    }

    public void setNamespaceUri( final String newNamespaceUri ) {
        this.namespaceUri = newNamespaceUri;
    }

    public void setValue( final String newValue ) {
        this.value = newValue;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getQName() + '=' + getValue();
    }
}