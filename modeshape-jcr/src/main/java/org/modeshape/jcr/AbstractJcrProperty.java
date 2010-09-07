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

import java.util.Iterator;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.session.GraphSession.PropertyInfo;
import org.modeshape.jcr.SessionCache.JcrPropertyPayload;
import org.modeshape.jcr.SessionCache.NodeEditor;

/**
 * An abstract {@link Property JCR Property} implementation.
 */
@NotThreadSafe
abstract class AbstractJcrProperty extends AbstractJcrItem implements Property, Comparable<Property> {

    protected final AbstractJcrNode node;
    protected final Name name;

    AbstractJcrProperty( SessionCache cache,
                         AbstractJcrNode node,
                         Name name ) {
        super(cache);
        assert node != null;
        assert name != null;
        this.node = node;
        this.name = name;
    }

    final NodeEditor editor() throws ItemNotFoundException, InvalidItemStateException, RepositoryException {
        return node.editor();
    }

    public abstract boolean isMultiple();

    /**
     * Checks that this property's parent node is not already locked by another session. If the parent node is not locked or the
     * parent node is locked but the lock is owned by this {@code Session}, this method completes silently. If the parent node is
     * locked (either directly or as part of a deep lock from an ancestor), this method throws a {@code LockException}.
     * 
     * @throws LockException if the parent node of this property is locked (that is, if {@code getParent().isLocked() == true &&
     *         getParent().getLock().getLockToken() == null}.
     * @throws RepositoryException if any other error occurs
     * @see Node#isLocked()
     * @see Lock#getLockToken()
     */
    protected final void checkForLock() throws LockException, RepositoryException {

        if (this.getParent().isLocked() && !getParent().getLock().isLockOwningSession()) {
            Lock parentLock = this.getParent().getLock();
            if (parentLock != null && parentLock.getLockToken() == null) {
                throw new LockException(JcrI18n.lockTokenNotHeld.text(this.getParent().location()));
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>visitor</code> is <code>null</code>.
     * @see javax.jcr.Item#accept(javax.jcr.ItemVisitor)
     */
    public final void accept( ItemVisitor visitor ) throws RepositoryException {
        CheckArg.isNotNull(visitor, "visitor");
        checkSession();
        visitor.visit(this);
    }

    final PropertyInfo<JcrPropertyPayload> propertyInfo() throws PathNotFoundException, RepositoryException {
        return node.nodeInfo().getProperty(name);
    }

    final Name name() {
        return name;
    }

    final JcrPropertyPayload payload() throws RepositoryException {
        return propertyInfo().getPayload();
    }

    final org.modeshape.graph.property.Property property() throws RepositoryException {
        return propertyInfo().getProperty();
    }

    JcrValue createValue( Object value ) throws RepositoryException {
        return new JcrValue(context().getValueFactories(), this.cache, payload().getPropertyType(), value);
    }

    final JcrValue createValue( Object value,
                                int propertyType ) {
        return new JcrValue(context().getValueFactories(), this.cache, propertyType, value);
    }

    @Override
    Path path() throws RepositoryException {
        return context().getValueFactories().getPathFactory().create(node.path(), name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getType()
     */
    public int getType() throws RepositoryException {
        checkSession();
        return payload().getPropertyType();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getDefinition()
     */
    public final JcrPropertyDefinition getDefinition() throws RepositoryException {
        checkSession();
        return cache.session().nodeTypeManager().getPropertyDefinition(payload().getPropertyDefinitionId());
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method returns the string form of the {@link org.modeshape.graph.property.Property#getName()}, computed dynamically
     * each time this method is called to ensure that the property namespace prefix is used.
     * </p>
     * 
     * @see javax.jcr.Item#getName()
     */
    public final String getName() {
        return name.getString(namespaces());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getParent()
     */
    public final AbstractJcrNode getParent() {
        return node;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getPath()
     */
    public final String getPath() throws RepositoryException {
        return path().getString(namespaces());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#isModified()
     */
    public final boolean isModified() {
        try {
            checkSession();
            return propertyInfo().isModified();
        } catch (RepositoryException re) {
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
            checkSession();
            return propertyInfo().isNew();
        } catch (RepositoryException re) {
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
        checkSession();
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
        checkSession();
        AbstractJcrNode parentNode = getParent();
        if (parentNode.isLocked()) {
            Lock parentLock = parentNode.lockManager().getLock(parentNode);
            if (parentLock != null && !parentLock.isLockOwningSession()) {
                throw new LockException(JcrI18n.lockTokenNotHeld.text(getPath()));
            }
        }

        if (!parentNode.isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(getPath()));
        }

        editor().removeProperty(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#save()
     */
    public void save() throws RepositoryException {
        checkSession();
        // This is not a correct implementation, but it's good enough to work around some TCK requirements for version tests
        // Plus, Item.save() has been removed from the JCR 2.0 spec (and deprecated in JCR 2.0's Java API).
        getParent().save();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Property that ) {
        if (that == this) return 0;
        try {
            return this.getName().compareTo(that.getName());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        try {
            ValueFactory<String> stringFactory = session().getExecutionContext().getValueFactories().getStringFactory();
            StringBuilder sb = new StringBuilder();
            sb.append(getName()).append('=');
            org.modeshape.graph.property.Property property = propertyInfo().getProperty();
            if (isMultiple()) {
                sb.append('[');
                Iterator<?> iter = property.iterator();
                if (iter.hasNext()) {
                    sb.append(stringFactory.create(iter.next()));
                    if (iter.hasNext()) sb.append(',');
                }
                sb.append(']');
            } else {
                sb.append(stringFactory.create(property.getFirstValue()));
            }
            return sb.toString();
        } catch (RepositoryException e) {
            return super.toString();
        }
    }
}
