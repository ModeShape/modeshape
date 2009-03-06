/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.property.Name;

/**
 * DNA implementation of the {@link ItemDefinition} interface. This implementation is immutable and has all fields initialized
 * through its constructor.
 */
@Immutable
class JcrItemDefinition implements ItemDefinition {

    protected final JcrSession session;

    private final NodeType declaringNodeType;
    protected final Name name;
    private final int onParentVersion;
    private final boolean autoCreated;
    private final boolean mandatory;
    private final boolean protectedItem;

    JcrItemDefinition( JcrSession session,
                       NodeType declaringNodeType,
                       Name name,
                       int onParentVersion,
                       boolean autoCreated,
                       boolean mandatory,
                       boolean protectedItem ) {
        super();
        this.session = session;
        this.declaringNodeType = declaringNodeType;
        this.name = name;
        this.onParentVersion = onParentVersion;
        this.autoCreated = autoCreated;
        this.mandatory = mandatory;
        this.protectedItem = protectedItem;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#getDeclaringNodeType()
     */
    public NodeType getDeclaringNodeType() {
        return declaringNodeType;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#getName()
     */
    public String getName() {
        if (name == null) {
            return JcrNodeType.RESIDUAL_ITEM_NAME;
        }

        return name.getString(session.getExecutionContext().getNamespaceRegistry());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#getOnParentVersion()
     */
    public int getOnParentVersion() {
        return onParentVersion;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#isAutoCreated()
     */
    public boolean isAutoCreated() {
        return autoCreated;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#isMandatory()
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#isProtected()
     */
    public boolean isProtected() {
        return protectedItem;
    }

}
