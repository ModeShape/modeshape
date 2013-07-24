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
package org.modeshape.jcr;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ItemDefinition;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.api.Namespaced;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;

/**
 * ModeShape implementation of the {@link ItemDefinition} interface. This implementation is immutable and has all fields
 * initialized through its constructor.
 */
@Immutable
abstract class JcrItemDefinition implements ItemDefinition, Namespaced {

    protected final ExecutionContext context;

    protected final JcrNodeType declaringNodeType;
    protected final Name name;
    private final int onParentVersion;
    private final boolean autoCreated;
    private final boolean mandatory;
    private final boolean protectedItem;

    JcrItemDefinition( ExecutionContext context,
                       JcrNodeType declaringNodeType,
                       Name name,
                       int onParentVersion,
                       boolean autoCreated,
                       boolean mandatory,
                       boolean protectedItem ) {
        super();
        this.context = context;
        this.declaringNodeType = declaringNodeType;
        this.name = name != null ? name : context.getValueFactories().getNameFactory().create(JcrNodeType.RESIDUAL_ITEM_NAME);
        this.onParentVersion = onParentVersion;
        this.autoCreated = autoCreated;
        this.mandatory = mandatory;
        this.protectedItem = protectedItem;
    }

    abstract NodeKey key();

    final Name getInternalName() {
        return name;
    }

    @Override
    public String getLocalName() throws RepositoryException {
        return name.getLocalName();
    }

    @Override
    public String getNamespaceURI() throws RepositoryException {
        return name.getNamespaceUri();
    }

    /**
     * Determine whether this is a residual item. Section 6.7.15 in the JSR 1.0 specification defines a residual item as one
     * having a name equal to "*".
     * 
     * @return true if this item is residual, or false otherwise
     */
    public boolean isResidual() {
        return name.getLocalName().equals("*");
    }

    @Override
    public JcrNodeType getDeclaringNodeType() {
        return declaringNodeType;
    }

    @Override
    public String getName() {
        if (name == null) {
            return JcrNodeType.RESIDUAL_ITEM_NAME;
        }

        return name.getString(context.getNamespaceRegistry());
    }

    @Override
    public int getOnParentVersion() {
        return onParentVersion;
    }

    @Override
    public boolean isAutoCreated() {
        return autoCreated;
    }

    @Override
    public boolean isMandatory() {
        return mandatory;
    }

    @Override
    public boolean isProtected() {
        return protectedItem;
    }

}
