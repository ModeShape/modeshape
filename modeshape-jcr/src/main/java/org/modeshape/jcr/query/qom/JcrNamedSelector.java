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
package org.modeshape.jcr.query.qom;

import javax.jcr.query.qom.Selector;
import org.modeshape.common.util.ObjectUtil;
import org.modeshape.graph.query.model.NamedSelector;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.Visitor;

/**
 * Implementation of the (named) selector for the JCR Query Object Model and the Graph API.
 */
public class JcrNamedSelector extends NamedSelector implements Selector, JcrSource {

    private static final long serialVersionUID = 1L;

    /**
     * Create a selector with the supplied selector.
     * 
     * @param selector the selector; may not be null
     */
    public JcrNamedSelector( NamedSelector selector ) {
        super(selector.name(), selector.alias());
    }

    /**
     * Create a selector with the supplied node type name and alias.
     * 
     * @param nodeTypeName the name of the required node type; may not be null
     * @param selectorName the selector name; may not be null
     */
    public JcrNamedSelector( SelectorName nodeTypeName,
                             SelectorName selectorName ) {
        super(nodeTypeName, selectorName);

    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.Selector#getNodeTypeName()
     */
    @Override
    public String getNodeTypeName() {
        return name().name();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.Selector#getSelectorName()
     */
    @Override
    public String getSelectorName() {
        return alias().name();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return name().hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Selector) {
            Selector that = (Selector)obj;
            if (!this.getNodeTypeName().equals(that.getNodeTypeName())) return false;
            if (!ObjectUtil.isEqualWithNulls(this.getSelectorName(), that.getSelectorName())) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitable#accept(org.modeshape.graph.query.model.Visitor)
     */
    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}
