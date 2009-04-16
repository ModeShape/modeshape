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

import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.jcr.SessionCache.NodeEditor;
import org.jboss.dna.jcr.cache.PropertyInfo;

/**
 * @author jverhaeg
 */
@NotThreadSafe
abstract class AbstractJcrProperty extends AbstractJcrItem implements Property {

    protected final PropertyId propertyId;

    AbstractJcrProperty( SessionCache cache,
                         PropertyId propertyId ) {
        super(cache);
        assert propertyId != null;
        this.propertyId = propertyId;
    }

    final NodeEditor editor() throws ItemNotFoundException, InvalidItemStateException, RepositoryException {
        return cache.getEditorFor(propertyId.getNodeId());
    }

    abstract boolean isMultiple();

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>visitor</code> is <code>null</code>.
     * @see javax.jcr.Item#accept(javax.jcr.ItemVisitor)
     */
    public final void accept( ItemVisitor visitor ) throws RepositoryException {
        CheckArg.isNotNull(visitor, "visitor");
        visitor.visit(this);
    }

    final PropertyInfo propertyInfo() throws PathNotFoundException, RepositoryException {
        return cache.findPropertyInfo(propertyId);
    }

    final Name name() throws RepositoryException {
        return propertyInfo().getPropertyName();
    }

    final org.jboss.dna.graph.property.Property property() throws RepositoryException {
        return propertyInfo().getProperty();
    }

    JcrValue createValue( Object value ) throws RepositoryException {
        return new JcrValue(context().getValueFactories(), this.cache, propertyInfo().getPropertyType(), value);
    }

    final JcrValue createValue( Object value,
                                int propertyType ) {
        return new JcrValue(context().getValueFactories(), this.cache, propertyType, value);
    }

    @Override
    Path path() throws RepositoryException {
        return cache.getPathFor(propertyInfo());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getType()
     */
    public int getType() throws RepositoryException {
        return propertyInfo().getPropertyType();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getDefinition()
     */
    public final PropertyDefinition getDefinition() throws RepositoryException {
        PropertyInfo info = propertyInfo();
        PropertyDefinitionId definitionId = info.getDefinitionId();
        return cache.session().nodeTypeManager().getPropertyDefinition(definitionId);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method returns the string form of the {@link org.jboss.dna.graph.property.Property#getName()}, computed dynamically
     * each time this method is called to ensure that the property namespace prefix is used.
     * </p>
     * 
     * @see javax.jcr.Item#getName()
     */
    public final String getName() throws RepositoryException {
        return propertyInfo().getPropertyName().getString(namespaces());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getParent()
     */
    public final Node getParent() throws RepositoryException {
        return cache.findJcrNode(propertyId.getNodeId());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getPath()
     */
    public final String getPath() throws RepositoryException {
        return cache.getPathFor(propertyInfo()).getString(namespaces());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#isModified()
     */
    public final boolean isModified() {
        try {
            return propertyInfo().isModified();
        }
        catch (RepositoryException re) {
            throw new IllegalStateException(re);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#isNew()
     */
    public final boolean isNew() {
        try {
            return propertyInfo().isNew();
        }
        catch (RepositoryException re) {
            throw new IllegalStateException(re);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @return false
     * @see javax.jcr.Item#isNode()
     */
    public final boolean isNode() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#isSame(javax.jcr.Item)
     */
    @Override
    public final boolean isSame( Item otherItem ) throws RepositoryException {
        if (otherItem instanceof Property) {
            Property otherProperty = (Property)otherItem;
            // The nodes that own the properties must be the same ...
            if (!getParent().isSame(otherProperty.getParent())) return false;
            // The properties must have the same name ...
            return getName().equals(otherProperty.getName());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Item#refresh(boolean)
     */
    public void refresh( boolean keepChanges ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#remove()
     */
    public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        editor().removeProperty(propertyId.getPropertyName());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Item#save()
     */
    public void save() {
        throw new UnsupportedOperationException();
    }
}
