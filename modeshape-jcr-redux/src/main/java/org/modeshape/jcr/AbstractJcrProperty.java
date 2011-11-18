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
import java.util.Set;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.ValueFactory;

/**
 * An abstract {@link Property JCR Property} implementation.
 */
@NotThreadSafe
abstract class AbstractJcrProperty extends AbstractJcrItem implements Property, Comparable<Property> {

    @Immutable
    private final static class CachedDefinition {
        protected final PropertyDefinitionId propDefnId;
        protected final int nodeTypesVersion;

        protected CachedDefinition( PropertyDefinitionId propDefnId,
                                    int nodeTypesVersion ) {
            this.propDefnId = propDefnId;
            this.nodeTypesVersion = nodeTypesVersion;
        }
    }

    private final AbstractJcrNode node;
    private final Name name;
    private int propertyType;
    private volatile CachedDefinition cachedDefn;

    AbstractJcrProperty( AbstractJcrNode node,
                         Name name,
                         int propertyType ) {
        super(node.session());
        assert node != null;
        assert name != null;
        this.node = node;
        this.name = name;
        this.propertyType = propertyType;
    }

    final void setPropertyDefinitionId( PropertyDefinitionId propDefnId ) {
        this.cachedDefn = new CachedDefinition(propDefnId, node.session().nodeTypesVersion());
    }

    final void releasePropertyDefinitionId() {
        this.cachedDefn = null;
    }

    /**
     * Get the property definition ID.
     * 
     * @return the cached property definition ID; never null
     * @throws ItemNotFoundException if the node that contains this property doesn't exist anymore
     * @throws ConstraintViolationException if no valid property definition could be found
     * @throws InvalidItemStateException if the node has been removed in this session's transient state
     */
    final PropertyDefinitionId propertyDefinitionId()
        throws ItemNotFoundException, ConstraintViolationException, InvalidItemStateException {
        CachedDefinition defn = cachedDefn;
        if (defn == null || node.session().nodeTypesVersion() > defn.nodeTypesVersion) {
            Name primaryType = node.getPrimaryTypeName();
            Set<Name> mixinTypes = node.getMixinTypeNames();
            PropertyDefinitionId id = node.propertyDefinitionFor(property(), primaryType, mixinTypes).getId();
            setPropertyDefinitionId(id);
            return id;
        }
        return defn.propDefnId;
    }

    /**
     * Get the definition for this property.
     * 
     * @return the cached property definition ID; never null
     * @throws ItemNotFoundException if the node that contains this property doesn't exist anymore
     * @throws ConstraintViolationException if no valid property definition could be found
     * @throws InvalidItemStateException if the node has been removed in this session's transient state
     */
    final JcrPropertyDefinition propertyDefinition()
        throws ItemNotFoundException, ConstraintViolationException, InvalidItemStateException {
        CachedDefinition defn = cachedDefn;
        if (defn == null || node.session().nodeTypesVersion() > defn.nodeTypesVersion) {
            Name primaryType = node.getPrimaryTypeName();
            Set<Name> mixinTypes = node.getMixinTypeNames();
            JcrPropertyDefinition propDefn = node.propertyDefinitionFor(property(), primaryType, mixinTypes);
            PropertyDefinitionId id = propDefn.getId();
            setPropertyDefinitionId(id);
            return propDefn;
        }
        return session.repository().nodeTypeManager().getPropertyDefinition(defn.propDefnId);
    }

    final CachedNode cachedNode() throws ItemNotFoundException, InvalidItemStateException {
        return node.node();
    }

    final MutableCachedNode mutable() {
        return node.mutable();
    }

    final SessionCache sessionCache() {
        return node.sessionCache();
    }

    final PropertyFactory propertyFactory() {
        return node.session().propertyFactory();
    }

    final org.modeshape.jcr.value.Property property() throws ItemNotFoundException, InvalidItemStateException {
        return cachedNode().getProperty(name, sessionCache());
    }

    final JcrValue createValue( Object value ) {
        return new JcrValue(session().context().getValueFactories(), this.propertyType, value);
    }

    final JcrValue createValue( Object value,
                                int propertyType ) {
        return new JcrValue(session().context().getValueFactories(), this.propertyType, value);
    }

    @Override
    public JcrSession getSession() {
        return node.getSession();
    }

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
                throw new LockException(JcrI18n.lockTokenNotHeld.text(node.location()));
            }
        }
    }

    /**
     * Verifies that this node is either not versionable or that it is versionable but checked out.
     * 
     * @throws VersionException if the node is versionable but is checked in and cannot be modified
     * @throws RepositoryException if there is an error accessing the repository
     */
    protected final void checkForCheckedOut() throws VersionException, RepositoryException {
        if (!node.isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(getPath()));
        }
    }

    @Override
    public final void accept( ItemVisitor visitor ) throws RepositoryException {
        CheckArg.isNotNull(visitor, "visitor");
        checkSession();
        visitor.visit(this);
    }

    final Name name() {
        return name;
    }

    @Override
    Path path() throws RepositoryException {
        return session().pathFactory().create(node.path(), name);
    }

    @Override
    public int getType() throws RepositoryException {
        checkSession();
        return propertyType;
    }

    @Override
    public final JcrPropertyDefinition getDefinition() throws RepositoryException {
        checkSession();
        return propertyDefinition();
    }

    @Override
    public final String getName() {
        return name.getString(namespaces());
    }

    @Override
    public final AbstractJcrNode getParent() {
        return node;
    }

    @Override
    public final String getPath() throws RepositoryException {
        return path().getString(namespaces());
    }

    @Override
    public final boolean isModified() {
        try {
            checkSession();
            CachedNode node = cachedNode();
            return node instanceof MutableCachedNode && ((MutableCachedNode)node).isPropertyModified(sessionCache(), name);
        } catch (RepositoryException re) {
            throw new IllegalStateException(re);
        }
    }

    @Override
    public final boolean isNew() {
        try {
            checkSession();
            CachedNode node = cachedNode();
            return node instanceof MutableCachedNode && ((MutableCachedNode)node).isPropertyNew(sessionCache(), name);
        } catch (RepositoryException re) {
            throw new IllegalStateException(re);
        }
    }

    @Override
    public final boolean isNode() {
        return false;
    }

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

    @Override
    public void refresh( boolean keepChanges ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        checkForLock();
        checkForCheckedOut();
        session.checkPermission(path(), ModeShapePermissions.REMOVE);
        AbstractJcrNode parentNode = getParent();
        if (parentNode.isLocked()) {
            Lock parentLock = parentNode.getLock();
            if (parentLock != null && !parentLock.isLockOwningSession()) {
                throw new LockException(JcrI18n.lockTokenNotHeld.text(getPath()));
            }
        }

        if (!parentNode.isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(getPath()));
        }

        node.removeProperty(this);
    }

    @Override
    public abstract JcrValue[] getValues() throws ValueFormatException, RepositoryException;

    @Override
    public abstract JcrValue getValue() throws ValueFormatException, RepositoryException;

    @Override
    public void save() throws RepositoryException {
        checkSession();
        // This is not a correct implementation, but it's good enough to work around some TCK requirements for version tests
        // Plus, Item.save() has been removed from the JCR 2.0 spec (and deprecated in JCR 2.0's Java API).
        getParent().save();
    }

    @Override
    public int compareTo( Property that ) {
        if (that == this) return 0;
        try {
            return this.getName().compareTo(that.getName());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        ValueFactory<String> stringFactory = session().context().getValueFactories().getStringFactory();
        StringBuilder sb = new StringBuilder();
        try {
            org.modeshape.jcr.value.Property property = cachedNode().getProperty(name, sessionCache());
            sb.append(getName()).append('=');
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
        } catch (RepositoryException e) {
            // The node likely does not exist ...
            sb.append(" on deleted node " + node.key());
        }
        return sb.toString();
    }
}
