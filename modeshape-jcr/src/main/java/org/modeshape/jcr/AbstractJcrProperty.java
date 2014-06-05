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

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.VersionException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.RepositoryNodeTypeManager.NodeTypes;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.basic.NodeKeyReference;
import org.modeshape.jcr.value.basic.UuidReference;

/**
 * An abstract {@link Property JCR Property} implementation.
 */
@NotThreadSafe
abstract class AbstractJcrProperty extends AbstractJcrItem implements org.modeshape.jcr.api.Property, Comparable<org.modeshape.jcr.api.Property> {

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

    final void setPropertyDefinitionId( PropertyDefinitionId propDefnId,
                                        int nodeTypesVersion ) {
        this.cachedDefn = new CachedDefinition(propDefnId, nodeTypesVersion);
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
        NodeTypes nodeTypes = session.nodeTypes();
        if (defn == null || nodeTypes.getVersion() > defn.nodeTypesVersion) {
            Name primaryType = node.getPrimaryTypeName();
            Set<Name> mixinTypes = node.getMixinTypeNames();
            PropertyDefinitionId id = node.propertyDefinitionFor(property(), primaryType, mixinTypes, nodeTypes).getId();
            setPropertyDefinitionId(id, nodeTypes.getVersion());
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
        NodeTypes nodeTypes = session.nodeTypes();
        if (defn == null || nodeTypes.getVersion() > defn.nodeTypesVersion) {
            Name primaryType = node.getPrimaryTypeName();
            Set<Name> mixinTypes = node.getMixinTypeNames();
            JcrPropertyDefinition propDefn = node.propertyDefinitionFor(property(), primaryType, mixinTypes, nodeTypes);
            PropertyDefinitionId id = propDefn.getId();
            setPropertyDefinitionId(id, nodeTypes.getVersion());
            return propDefn;
        }
        return nodeTypes.getPropertyDefinition(defn.propDefnId);
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
                                int propertyType ) throws ValueFormatException {
        try {
            return new JcrValue(session().context().getValueFactories(), propertyType, value);
        } catch (org.modeshape.jcr.value.ValueFormatException e) {
            throw new ValueFormatException(e);
        }
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
            // Node is not checked out, so changing property is only allowed if OPV of property is 'ignore' ...
            JcrPropertyDefinition defn = getDefinition();
            if (defn.getOnParentVersion() != OnParentVersionAction.IGNORE) {
                // Can't change this property ...
                String path = getParent().getPath();
                throw new VersionException(JcrI18n.nodeIsCheckedIn.text(path));
            }
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
    public String getLocalName() {
        return name.getLocalName();
    }

    @Override
    public String getNamespaceURI() {
        return name.getNamespaceUri();
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
        checkNotProtected();
        checkForLock();
        checkForCheckedOut();
        session.checkPermission(this, ModeShapePermissions.REMOVE);
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

    private void checkNotProtected() throws RepositoryException {
        if (this.getDefinition().isProtected()) {
            throw new ConstraintViolationException(JcrI18n.propertyIsProtected.text(getPath()));
        }
    }

    @Override
    public abstract JcrValue[] getValues() throws ValueFormatException, RepositoryException;

    @Override
    public abstract JcrValue getValue() throws ValueFormatException, RepositoryException;

    @SuppressWarnings( "deprecation" )
    @Override
    public void save() throws RepositoryException {
        checkSession();
        // This is not a correct implementation, but it's good enough to work around some TCK requirements for version tests
        // Plus, Item.save() has been removed from the JCR 2.0 spec (and deprecated in JCR 2.0's Java API).
        getParent().save();
    }

    @Override
    public int compareTo( org.modeshape.jcr.api.Property that ) {
        if (that == this) return 0;
        try {
            return this.getName().compareTo(that.getName());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    protected Node valueToNode( Object value ) throws RepositoryException {
        ValueFactories factories = context().getValueFactories();
        try {
            if (value instanceof Reference) {
                NodeKey key = null;
                if (value instanceof NodeKeyReference) {
                    // REFERENCE and WEAKREFERENCE values are node keys ...
                    key = ((NodeKeyReference)value).getNodeKey();
                } else if (value instanceof UuidReference) {
                    // REFERENCE and WEAKREFERENCE values should be node keys, so create a key from the
                    // supplied UUID and this node's key ...
                    UUID uuid = ((UuidReference)value).getUuid();
                    key = getParent().key().withId(uuid.toString());
                } else {
                    assert false : "Unknown type of Reference value";
                }
                return session().node(key, null);
            }
            // STRING, PATH and NAME values will be convertable to a Path object ...
            Path path = factories.getPathFactory().create(value);
            return path.isAbsolute() ? session().node(path) : session().node(getParent().node(), path);
        } catch (org.modeshape.jcr.value.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
        catch (PathNotFoundException pathNotFound) {
            //expected by the TCK
            throw new ItemNotFoundException(pathNotFound.getMessage(), pathNotFound);
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
                while (iter.hasNext()) {
                    Object value = iter.next();
                    appendValueToString(stringFactory, sb, value);
                    if (iter.hasNext()) sb.append(',');
                }
                sb.append(']');
            } else {
                Object value = property.getFirstValue();
                appendValueToString(stringFactory, sb, value);
            }
        } catch (RepositoryException e) {
            // The node likely does not exist ...
            sb.append(" on deleted node ").append(node.key());
        }
        return sb.toString();
    }

    private void appendValueToString( ValueFactory<String> stringFactory,
                                      StringBuilder sb,
                                      Object value ) {
        if (value instanceof javax.jcr.Binary) {
            sb.append("**binary-value-not-shown**");
        } else {
            sb.append(stringFactory.create(value));
        }
    }

    @Override
    public <T> T getAs( Class<T> type,
                        int index ) throws IndexOutOfBoundsException, ValueFormatException, RepositoryException {
        checkSession();
        Object value = property().getValue(index);
        Object convertedValue = null;
        try {
            if (String.class.equals(type)) {
                convertedValue = context().getValueFactories().getStringFactory().create(value);
            } else if (Long.class.equals(type)) {
                convertedValue = context().getValueFactories().getLongFactory().create(value);
            } else if (Boolean.class.equals(type)) {
                convertedValue = context().getValueFactories().getBooleanFactory().create(value);
            } else if (Date.class.equals(type)) {
                Calendar calendar = context().getValueFactories().getDateFactory().create(value).toCalendar();
                convertedValue = calendar.getTime();
            } else if (Calendar.class.equals(type)) {
                convertedValue = context().getValueFactories().getDateFactory().create(value).toCalendar();
            } else if (DateTime.class.equals(type)) {
                convertedValue = context().getValueFactories().getDateFactory().create(value);
            } else if (Double.class.equals(type)) {
                convertedValue = context().getValueFactories().getDoubleFactory().create(value);
            } else if (BigDecimal.class.equals(type)) {
                convertedValue = context().getValueFactories().getDecimalFactory().create(value);
            } else if (java.io.InputStream.class.equals(type)) {
                BinaryValue binary = context().getValueFactories().getBinaryFactory().create(value);
                convertedValue = binary.getStream();
            } else if (javax.jcr.Binary.class.isAssignableFrom(type)) {
                convertedValue = context().getValueFactories().getBinaryFactory().create(value);
            } else if (Node.class.equals(type)) {
                convertedValue = valueToNode(value);
            } else if (NodeIterator.class.equals(type)) {
                convertedValue = new JcrSingleNodeIterator((AbstractJcrNode)getNode());
            } else {
                throw new ValueFormatException(JcrI18n.unableToConvertPropertyValueAtIndexToType.text(getPath(), index, type));
            }
        } catch (org.modeshape.jcr.value.ValueFormatException e) {
            throw new ValueFormatException(e);
        }
        return type.cast(convertedValue);
    }
}
