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

import java.io.InputStream;
import java.math.BigDecimal;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.ActivityViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.RepositoryNodeTypeManager.NodeTypes;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.CachedNode.ReferenceType;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.PropertyTypeUtil;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;

/**
 * The abstract base class for all {@link Node} implementations.
 */
@ThreadSafe
abstract class AbstractJcrNode extends AbstractJcrItem implements Node {

    enum Type {
        ROOT,
        NODE,
        SYSTEM,
        SHARED,
        VERSION,
        VERSION_HISTORY;

        private static final Map<Name, Type> DEFAULT_TYPE_BY_NAME;

        static {
            Map<Name, Type> byName = new HashMap<Name, Type>();
            // Root node ...
            byName.put(ModeShapeLexicon.ROOT, Type.ROOT);
            // System content ...
            byName.put(ModeShapeLexicon.SYSTEM, Type.SYSTEM);
            // Versioning
            byName.put(JcrNtLexicon.VERSION_HISTORY, Type.VERSION_HISTORY);
            byName.put(JcrNtLexicon.VERSION_LABELS, Type.SYSTEM);
            byName.put(JcrNtLexicon.VERSION, Type.VERSION);
            // Node types ...
            byName.put(ModeShapeLexicon.NODE_TYPES, Type.SYSTEM);
            byName.put(JcrNtLexicon.NODE_TYPE, Type.SYSTEM);
            byName.put(JcrNtLexicon.PROPERTY_DEFINITION, Type.SYSTEM);
            byName.put(JcrNtLexicon.CHILD_NODE_DEFINITION, Type.SYSTEM);
            // Namespaces ...
            byName.put(ModeShapeLexicon.NAMESPACES, Type.SYSTEM);
            byName.put(ModeShapeLexicon.NAMESPACE, Type.SYSTEM);
            // Locks ...
            byName.put(ModeShapeLexicon.LOCKS, Type.SYSTEM);
            byName.put(ModeShapeLexicon.LOCK, Type.SYSTEM);

            // Shared nodes ...
            // TODO: Shared nodes
            // byName.put(ModeShapeLexicon.SHARE,Type.SHARED);

            DEFAULT_TYPE_BY_NAME = Collections.unmodifiableMap(byName);
        }

        /**
         * Determine the type given the supplied primary type.
         * 
         * @param primaryType the primary type
         * @return the type, or null if the node type could not be determined by the suppplied primary type
         */
        public static Type typeForPrimaryType( Name primaryType ) {
            return DEFAULT_TYPE_BY_NAME.get(primaryType);
        }
    }

    @Immutable
    private final static class CachedDefinition {
        protected final NodeDefinitionId nodeDefnId;
        protected final int nodeTypesVersion;

        protected CachedDefinition( NodeDefinitionId nodeDefnId,
                                    int nodeTypesVersion ) {
            this.nodeDefnId = nodeDefnId;
            this.nodeTypesVersion = nodeTypesVersion;
        }
    }

    protected static final Pattern WILDCARD_PATTERN = Pattern.compile(".*");
    private static final Set<Name> INTERNAL_NODE_TYPE_NAMES = Collections.singleton(ModeShapeLexicon.SHARE);

    protected final NodeKey key;
    private final ConcurrentMap<Name, AbstractJcrProperty> jcrProperties = new ConcurrentHashMap<Name, AbstractJcrProperty>();
    private volatile CachedDefinition cachedDefn;

    protected AbstractJcrNode( JcrSession session,
                               NodeKey key ) {
        super(session);
        this.key = key;
    }

    abstract boolean isRoot();

    abstract Type type();

    /**
     * Check that this type of node can be modified
     * 
     * @throws RepositoryException
     */
    protected void checkNodeTypeCanBeModified() throws RepositoryException {
    }

    protected SessionCache sessionCache() {
        return session.cache();
    }

    protected final NodeKey key() {
        return this.key;
    }

    /**
     * Get the cached node.
     * 
     * @return the cached node
     * @throws InvalidItemStateException if the node has been removed in this session's transient state
     * @throws ItemNotFoundException if the node does not exist
     */
    protected final CachedNode node() throws ItemNotFoundException, InvalidItemStateException {
        CachedNode node = sessionCache().getNode(key);
        if (node == null) {
            if (sessionCache().isDestroyed(key)) {
                throw new InvalidItemStateException("The node with key " + key + " has been removed in this session.");
            }
            throw new ItemNotFoundException("The node with key " + key + " no longer exists.");
        }
        return node;
    }

    protected final MutableCachedNode mutable() {
        return sessionCache().mutable(key);
    }

    protected final MutableCachedNode mutableParent() throws RepositoryException {
        SessionCache cache = sessionCache();
        return cache.mutable(node().getParentKey(cache));
    }

    @Override
    Path path() throws ItemNotFoundException, InvalidItemStateException {
        return node().getPath(sessionCache());
    }

    /**
     * Obtain a string identifying this node, usually for error or logging purposes. This method never throws an exception.
     * 
     * @return the location string; never null
     */
    protected final String location() {
        try {
            return getPath();
        } catch (Throwable t) {
            return key.toString();
        }
    }

    protected final Name name() throws RepositoryException {
        return node().getName(sessionCache());
    }

    protected final Segment segment() throws RepositoryException {
        return node().getSegment(sessionCache());
    }

    @Override
    public final String getIdentifier() {
        return key.getIdentifier();
    }

    /**
     * Get the absolute and normalized identifier path for this node, regardless of whether this node is referenceable.
     * 
     * @return the node's identifier path; never null
     * @throws RepositoryException if there is an error accessing the identifier of this node
     */
    final String identifierPath() throws RepositoryException {
        return "[" + getIdentifier() + "]";
    }

    @Override
    public final JcrSession getSession() {
        return session();
    }

    @Override
    public AbstractJcrProperty getProperty( String relativePath ) throws PathNotFoundException, RepositoryException {
        CheckArg.isNotEmpty(relativePath, "relativePath");
        checkSession();
        int indexOfFirstSlash = relativePath.indexOf('/');
        if (indexOfFirstSlash == 0 || relativePath.startsWith("[")) {
            // Not a relative path ...
            throw new IllegalArgumentException(JcrI18n.invalidPathParameter.text(relativePath, "relativePath"));
        }

        Name propertyName = null;
        if (indexOfFirstSlash != -1) {
            // We know it's a relative path with more than one segment ...
            Path path = pathFrom(relativePath).getNormalizedPath();
            assert !path.isIdentifier();
            if (path.size() > 1) {
                try {
                    AbstractJcrItem item = session.findItem(key, path);
                    if (item instanceof AbstractJcrProperty) {
                        return (AbstractJcrProperty)item;
                    }
                } catch (ItemNotFoundException e) {
                    I18n msg = JcrI18n.propertyNotFoundAtPathRelativeToReferenceNode;
                    throw new PathNotFoundException(msg.text(relativePath, location(), workspaceName()));
                }
                I18n msg = JcrI18n.propertyNotFoundAtPathRelativeToReferenceNode;
                throw new PathNotFoundException(msg.text(relativePath, location(), workspaceName()));
            }
            propertyName = path.getLastSegment().getName();
        } else {
            propertyName = nameFrom(relativePath);
        }
        // It's just a name, so look for it directly ...
        AbstractJcrProperty result = getProperty(propertyName);
        if (result != null) return result;
        I18n msg = JcrI18n.pathNotFoundRelativeTo;
        throw new PathNotFoundException(msg.text(relativePath, location(), workspaceName()));
    }

    /**
     * Get the {@link AbstractJcrProperty JCR Property} object for the existing property with the supplied name.
     * 
     * @param propertyName the property name; may not be null
     * @return the JCR Property object, or null if there is no property with the specified name
     * @throws RepositoryException if there is a problem accessing the repository
     */
    final AbstractJcrProperty getProperty( Name propertyName ) throws RepositoryException {
        AbstractJcrProperty prop = jcrProperties.get(propertyName);
        if (prop == null) {
            // See if there's a property on the node ...
            CachedNode node = node();
            SessionCache cache = sessionCache();
            org.modeshape.jcr.value.Property p = node.getProperty(propertyName, cache);
            if (p != null) {
                Name primaryType = node.getPrimaryType(cache);
                Set<Name> mixinTypes = node.getMixinTypes(cache);
                prop = createJcrProperty(p, primaryType, mixinTypes);
                if (prop != null) {
                    AbstractJcrProperty newJcrProperty = jcrProperties.putIfAbsent(propertyName, prop);
                    if (newJcrProperty != null) {
                        // Some other thread snuck in and created it, so use that one ...
                        prop = newJcrProperty;
                    }
                }
            }
        } else {
            // Make sure the property hasn't been removed by another session ...
            CachedNode node = node();
            SessionCache cache = sessionCache();
            if (!node.hasProperty(propertyName, cache)) {
                jcrProperties.remove(propertyName);
                prop = null;
            }
        }
        return prop;
    }

    /**
     * Create a new JCR Property instance given the supplied information. Note that this does not alter the node in any way, since
     * it does not store a reference to this property (the caller must do that if needed).
     * 
     * @param property the cached node property; may not be null
     * @param primaryTypeName the name of the node's primary type; may not be null
     * @param mixinTypeNames the names of the node's mixin types; may be null or empty
     * @return the JCR Property instance, or null if the property could not be represented with a valid property definition given
     *         the primary type and mixin types
     * @throws ConstraintViolationException if the property has no valid property definition
     */
    private final AbstractJcrProperty createJcrProperty( Property property,
                                                         Name primaryTypeName,
                                                         Set<Name> mixinTypeNames ) throws ConstraintViolationException {
        NodeTypes nodeTypes = session.nodeTypes();
        JcrPropertyDefinition defn = propertyDefinitionFor(property, primaryTypeName, mixinTypeNames, nodeTypes);
        int jcrPropertyType = defn.getRequiredType();
        jcrPropertyType = determineBestPropertyTypeIfUndefined(jcrPropertyType, property);
        AbstractJcrProperty prop = null;
        if (property.isSingle()) {
            prop = new JcrSingleValueProperty(this, property.getName(), jcrPropertyType);
        } else {
            prop = new JcrMultiValueProperty(this, property.getName(), jcrPropertyType);
        }
        prop.setPropertyDefinitionId(defn.getId(), nodeTypes.getVersion());
        return prop;
    }

    private final int determineBestPropertyTypeIfUndefined( int actualPropertyType,
                                                            Property property ) {
        if (actualPropertyType == PropertyType.UNDEFINED) {
            return PropertyTypeUtil.jcrPropertyTypeFor(property);
        }
        return actualPropertyType;
    }

    final ValueFactories factories() {
        return context().getValueFactories();
    }

    final String readable( Object obj ) {
        return session.stringFactory().create(obj);
    }

    final String readable( Collection<?> obj ) {
        ValueFactory<String> stringFactory = session.stringFactory();
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        Iterator<?> iter = obj.iterator();
        if (iter.hasNext()) {
            sb.append(stringFactory.create(iter.next()));
            while (iter.hasNext()) {
                sb.append(',');
                sb.append(stringFactory.create(iter.next()));
            }
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Find the property definition for the property, given this node's primary type and mixin types.
     * 
     * @param property the property owned by this node; may not be null
     * @param primaryType the name of the node's primary type; may not be null
     * @param mixinTypes the names of the node's mixin types; may be null or empty
     * @param nodeTypes the node types cache to use; may not be null
     * @return the property definition; never null
     * @throws ConstraintViolationException if the property has no valid property definition
     */
    final JcrPropertyDefinition propertyDefinitionFor( org.modeshape.jcr.value.Property property,
                                                       Name primaryType,
                                                       Set<Name> mixinTypes,
                                                       NodeTypes nodeTypes ) throws ConstraintViolationException {

        // Figure out the JCR property type ...
        boolean single = property.isSingle();
        boolean skipProtected = false;
        JcrPropertyDefinition defn = findBestPropertyDefintion(primaryType,
                                                               mixinTypes,
                                                               property,
                                                               single,
                                                               skipProtected,
                                                               false,
                                                               nodeTypes);
        if (defn != null) return defn;

        // See if there is a definition that has constraints that were violated ...
        defn = findBestPropertyDefintion(primaryType, mixinTypes, property, single, skipProtected, true, nodeTypes);
        String pName = readable(property.getName());
        String loc = location();
        if (defn != null) {
            I18n msg = JcrI18n.propertyNoLongerSatisfiesConstraints;
            throw new ConstraintViolationException(msg.text(pName, loc, defn.getName(), defn.getDeclaringNodeType().getName()));
        }
        CachedNode node = sessionCache().getNode(key);
        String ptype = readable(node.getPrimaryType(sessionCache()));
        String mixins = readable(node.getMixinTypes(sessionCache()));
        String pstr = property.getString(session.namespaces());
        throw new ConstraintViolationException(JcrI18n.propertyNoLongerHasValidDefinition.text(pstr, loc, ptype, mixins));
    }

    /**
     * Find the best property definition in this node's primary type and mixin types.
     * 
     * @param primaryTypeNameOfParent the name of the primary type for the parent node; may not be null
     * @param mixinTypeNamesOfParent the names of the mixin types for the parent node; may be null or empty if there are no mixins
     *        to include in the search
     * @param property the property
     * @param isSingle true if the property definition should be single-valued, or false if the property definition should allow
     *        multiple values
     * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
     *        this operation is being done from within internal implementations
     * @param skipConstraints true if any constraints on the potential property definitions should be skipped; usually this is
     *        true for the first attempt but then 'false' for a subsequent attempt when figuring out an appropriate error message
     * @param nodeTypes the node types cache to use; may not be null
     * @return the property definition that allows setting this property, or null if there is no such definition
     */
    final JcrPropertyDefinition findBestPropertyDefintion( Name primaryTypeNameOfParent,
                                                           Collection<Name> mixinTypeNamesOfParent,
                                                           org.modeshape.jcr.value.Property property,
                                                           boolean isSingle,
                                                           boolean skipProtected,
                                                           boolean skipConstraints,
                                                           NodeTypes nodeTypes ) {
        JcrPropertyDefinition definition = null;
        int propertyType = PropertyTypeUtil.jcrPropertyTypeFor(property);

        // If single-valued ...
        ValueFactories factories = context().getValueFactories();
        if (isSingle) {
            // Create a value for the ModeShape property value ...
            Object value = property.getFirstValue();
            Value jcrValue = new JcrValue(factories, propertyType, value);
            definition = nodeTypes.findPropertyDefinition(session,
                                                          primaryTypeNameOfParent,
                                                          mixinTypeNamesOfParent,
                                                          property.getName(),
                                                          jcrValue,
                                                          true,
                                                          skipProtected);
        } else {
            // Create values for the ModeShape property value ...
            Value[] jcrValues = new Value[property.size()];
            int index = 0;
            for (Object value : property) {
                jcrValues[index++] = new JcrValue(factories, propertyType, value);
            }
            definition = nodeTypes.findPropertyDefinition(session,
                                                          primaryTypeNameOfParent,
                                                          mixinTypeNamesOfParent,
                                                          property.getName(),
                                                          jcrValues,
                                                          skipProtected);
        }

        if (definition != null) return definition;

        // No definition that allowed the values ...
        return null;
    }

    final boolean hasProperty( Name name ) throws RepositoryException {
        if (jcrProperties.containsKey(name)) return true;
        return node().hasProperty(name, sessionCache());
    }

    boolean removeProperty( AbstractJcrProperty property ) {
        if (jcrProperties.remove(property.name(), property)) {
            mutable().removeProperty(sessionCache(), property.name());
            return true;
        }
        return false;
    }

    boolean isReferenceable() throws RepositoryException {
        return isNodeType(JcrMixLexicon.REFERENCEABLE);
    }

    boolean isLockable() throws RepositoryException {
        return isNodeType(JcrMixLexicon.LOCKABLE);
    }

    boolean isShareable() throws RepositoryException {
        return isNodeType(JcrMixLexicon.SHAREABLE);
    }

    boolean isShared() {
        return false;
    }

    final JcrValue valueFrom( int propertyType,
                              Object value ) {
        return new JcrValue(context().getValueFactories(), propertyType, value);
    }

    final JcrValue valueFrom( String value ) {
        return session.valueFactory().createValue(value);
    }

    final JcrValue valueFrom( UUID value ) {
        Reference ref = context().getValueFactories().getReferenceFactory().create(value);
        return valueFrom(PropertyType.REFERENCE, ref);
    }

    final JcrValue valueFrom( Calendar value ) {
        DateTime dateTime = context().getValueFactories().getDateFactory().create(value);
        return valueFrom(PropertyType.DATE, dateTime);
    }

    final JcrValue valueFrom( InputStream value ) {
        org.modeshape.jcr.value.Binary binary = context().getValueFactories().getBinaryFactory().create(value);
        return valueFrom(PropertyType.BINARY, binary);
    }

    final JcrValue valueFrom( Binary value ) {
        return valueFrom(PropertyType.BINARY, value);
    }

    final JcrValue valueFrom( javax.jcr.Node value ) {
        NodeKey key = ((AbstractJcrNode)value).key();
        Reference ref = session.context().getValueFactories().getReferenceFactory().create(key);
        return valueFrom(PropertyType.REFERENCE, ref);
    }

    final JcrValue[] valuesFrom( int propertyType,
                                 Object[] values ) {
        /*
         * Null values in the array are "compacted" (read: ignored) as per section 7.1.6 in the JCR 1.0.1 specification. 
         */
        int len = values.length;
        ValueFactories factories = context().getValueFactories();
        List<JcrValue> results = new ArrayList<JcrValue>(len);
        for (int i = 0; i != len; ++i) {
            if (values[i] != null) results.add(new JcrValue(factories, propertyType, values[i]));
        }
        return results.toArray(new JcrValue[results.size()]);
    }

    final JcrVersionManager versionManager() {
        return session.workspace().versionManager();
    }

    /**
     * Checks that this node is not already locked by another session. If the node is not locked or the node is locked but the
     * lock is owned by this {@code Session}, this method completes silently. If the node is locked (either directly or as part of
     * a deep lock from an ancestor), this method throws a {@code LockException}.
     * 
     * @throws LockException if this node is locked (that is, if {@code isLocked() == true && getLock().getLockToken() == null}).
     * @throws RepositoryException if any other error occurs
     * @see Node#isLocked()
     * @see Lock#getLockToken()
     */
    protected final void checkForLock() throws LockException, RepositoryException {
        Lock lock = getLockIfExists();
        if (lock != null && !lock.isLockOwningSession() && lock.getLockToken() == null) {
            throw new LockException(JcrI18n.lockTokenNotHeld.text(location()));
        }
    }

    /**
     * Verifies that this node is either not versionable or that it is versionable but checked out.
     * 
     * @throws VersionException if the node is versionable but is checked in and cannot be modified
     * @throws RepositoryException if there is an error accessing the repository
     */
    protected final void checkForCheckedOut() throws VersionException, RepositoryException {
        if (!isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(location()));
        }
    }

    /**
     * Get the total number of children.
     * 
     * @return the total number of children
     * @throws RepositoryException
     */
    protected final long childCount() throws RepositoryException {
        return node().getChildReferences(sessionCache()).size();
    }

    /**
     * Get the number of children that have the supplied name.
     * 
     * @param name the child name
     * @return the number of children with names that match the supplied name
     * @throws RepositoryException
     */
    protected final long childCount( Name name ) throws RepositoryException {
        return node().getChildReferences(sessionCache()).getChildCount(name);
    }

    /**
     * Get the JCR node for the named child.
     * 
     * @param name the child name; may not be null
     * @param expectedType the expected implementation type for the node, or null if it is not known
     * @return the JCR node; never null
     * @throws PathNotFoundException if there is no child with the supplied name
     * @throws ItemNotFoundException if this node or the referenced child no longer exist or cannot be found
     * @throws InvalidItemStateException if this node has been removed in this session's transient state
     */
    protected final AbstractJcrNode childNode( Name name,
                                               Type expectedType )
        throws PathNotFoundException, ItemNotFoundException, InvalidItemStateException {
        ChildReference ref = node().getChildReferences(sessionCache()).getChild(name);
        if (ref == null) {
            String msg = JcrI18n.childNotFoundUnderNode.text(readable(name), location(), session.workspaceName());
            throw new PathNotFoundException(msg);
        }
        return session().node(ref.getKey(), expectedType);
    }

    /**
     * Get the JCR node for the named child.
     * 
     * @param segment the child name and SNS index; may not be null
     * @param expectedType the expected implementation type for the node, or null if it is not known
     * @return the JCR node; never null
     * @throws PathNotFoundException if there is no child with the supplied name
     * @throws ItemNotFoundException if this node or the referenced child cannot be found
     * @throws InvalidItemStateException if this node has been removed in this session's transient state
     */
    protected final AbstractJcrNode childNode( Segment segment,
                                               Type expectedType )
        throws PathNotFoundException, ItemNotFoundException, InvalidItemStateException {
        ChildReference ref = node().getChildReferences(sessionCache()).getChild(segment);
        if (ref == null) {
            String msg = JcrI18n.childNotFoundUnderNode.text(readable(segment), location(), session.workspaceName());
            throw new PathNotFoundException(msg);
        }
        return session().node(ref.getKey(), expectedType);
    }

    @Override
    public boolean hasNode( String relativePath ) throws RepositoryException {
        CheckArg.isNotEmpty(relativePath, "relativePath");
        checkSession();
        if (relativePath.equals(".")) return true;
        if (relativePath.equals("..")) return isRoot() ? false : true;
        int indexOfFirstSlash = relativePath.indexOf('/');
        if (indexOfFirstSlash == 0 || relativePath.startsWith("[")) {
            // Not a relative path ...
            throw new IllegalArgumentException(JcrI18n.invalidPathParameter.text(relativePath, "relativePath"));
        }
        Path.Segment segment = null;
        if (indexOfFirstSlash != -1) {
            // We know it's a relative path with more than one segment ...
            Path path = pathFrom(relativePath).getNormalizedPath();
            if (path.size() == 1) {
                if (path.getLastSegment().isSelfReference()) return true;
                if (path.getLastSegment().isParentReference()) return isRoot() ? false : true;
            }
            // We know it's a resolved relative path with more than one segment ...
            if (path.size() > 1) {
                try {
                    return session().node(node(), path) != null;
                } catch (PathNotFoundException e) {
                    return false;
                }
            }
            segment = path.getLastSegment();
        } else {
            segment = segmentFrom(relativePath);
        }
        assert !segment.isIdentifier();

        // It's just a name, so look for a child ...
        ChildReference ref = node().getChildReferences(sessionCache()).getChild(segment);
        return ref != null;
    }

    @Override
    public AbstractJcrNode getNode( String relativePath ) throws PathNotFoundException, RepositoryException {
        CheckArg.isNotEmpty(relativePath, "relativePath");
        checkSession();
        if (relativePath.equals(".")) return this;
        if (relativePath.equals("..")) return this.getParent();
        int indexOfFirstSlash = relativePath.indexOf('/');
        if (indexOfFirstSlash == 0 || relativePath.startsWith("[")) {
            // Not a relative path ...
            throw new IllegalArgumentException(JcrI18n.invalidPathParameter.text(relativePath, "relativePath"));
        }
        Path.Segment segment = null;
        if (indexOfFirstSlash != -1) {
            // We know it's a relative path with more than one segment ...
            Path path = pathFrom(relativePath).getNormalizedPath();
            if (path.size() == 1) {
                if (path.getLastSegment().isSelfReference()) return this;
                if (path.getLastSegment().isParentReference()) return this.getParent();
            }
            // We know it's a resolved relative path with more than one segment ...
            if (path.size() > 1) {
                return session().node(node(), path);
            }
            segment = path.getLastSegment();
        } else {
            segment = segmentFrom(relativePath);
        }
        assert !segment.isIdentifier();

        // It's just a name, so look for a child ...
        ChildReference ref = node().getChildReferences(sessionCache()).getChild(segment);
        if (ref == null) {
            String msg = JcrI18n.childNotFoundUnderNode.text(readable(segment), location(), session.workspaceName());
            throw new PathNotFoundException(msg);
        }
        return session().node(ref.getKey(), null);
    }

    AbstractJcrNode getNode( Name childName ) throws PathNotFoundException, RepositoryException {
        // It's just a name, so look for a child ...
        ChildReference ref = node().getChildReferences(sessionCache()).getChild(childName);
        if (ref == null) {
            String msg = JcrI18n.childNotFoundUnderNode.text(readable(childName), location(), session.workspaceName());
            throw new PathNotFoundException(msg);
        }
        return session().node(ref.getKey(), null);
    }

    AbstractJcrNode getNodeIfExists( Name childName ) throws RepositoryException {
        // It's just a name, so look for a child ...
        ChildReference ref = node().getChildReferences(sessionCache()).getChild(childName);
        return ref != null ? session().node(ref.getKey(), null) : null;
    }

    @Override
    public NodeIterator getNodes() throws RepositoryException {
        ChildReferences childReferences = node().getChildReferences(sessionCache());
        if (childReferences.isEmpty()) return JcrEmptyNodeIterator.INSTANCE;
        return new JcrChildNodeIterator(new ChildNodeResolver(session), childReferences);
    }

    @Override
    public NodeIterator getNodes( String namePattern ) throws RepositoryException {
        CheckArg.isNotNull(namePattern, "namePattern");
        checkSession();
        namePattern = namePattern.trim();
        if (namePattern.length() == 0) return JcrEmptyNodeIterator.INSTANCE;
        if ("*".equals(namePattern)) return getNodes();
        return getNodes(namePattern.split("[|]"));
    }

    @Override
    public NodeIterator getNodes( String[] nameGlobs ) throws RepositoryException {
        CheckArg.isNotNull(nameGlobs, "nameGlobs");
        if (nameGlobs.length == 0) return JcrEmptyNodeIterator.INSTANCE;

        List<?> patterns = createPatternsFor(nameGlobs);
        Iterator<ChildReference> iter = null;
        if (patterns.size() == 1 && patterns.get(0) instanceof String) {
            // This is a literal, so just look up by name ...
            Name literal = nameFrom((String)patterns.get(0));
            iter = node().getChildReferences(sessionCache()).iterator(literal);
        } else {
            NamespaceRegistry registry = session.namespaces();
            iter = node().getChildReferences(sessionCache()).iterator(patterns, registry);
        }
        return new JcrChildNodeIterator(new ChildNodeResolver(session), iter);
    }

    protected static List<?> createPatternsFor( String[] namePatterns ) throws RepositoryException {
        List<Object> patterns = new LinkedList<Object>();
        for (String stringPattern : namePatterns) {
            stringPattern = stringPattern.trim();
            int length = stringPattern.length();
            if (length == 0) continue;
            if (stringPattern.indexOf("*") == -1) {
                // Doesn't use wildcard, so use String not Pattern
                patterns.add(stringPattern);
            } else {
                // We need to escape the regular expression characters ...
                StringBuilder sb = new StringBuilder(length);
                for (int i = 0; i != length; i++) {
                    char c = stringPattern.charAt(i);
                    switch (c) {
                        // Per the spec, the the following characters are not allowed in patterns:
                        case '/':
                        case '[':
                        case ']':
                        case '\'':
                        case '"':
                        case '|':
                        case '\t':
                        case '\n':
                        case '\r':
                            String msg = JcrI18n.invalidNamePattern.text(c, stringPattern);
                            throw new RepositoryException(msg);
                            // The following characters must be escaped when used in regular expressions ...
                        case '?':
                        case '(':
                        case ')':
                        case '$':
                        case '^':
                        case '.':
                        case '{':
                        case '}':
                        case '\\':
                            sb.append("\\");
                            sb.append(c);
                            break;
                        case '*':
                            // replace with the regular expression wildcard
                            sb.append(".*");
                            break;
                        default:
                            sb.append(c);
                            break;
                    }
                }
                String escapedString = sb.toString();
                Pattern pattern = Pattern.compile(escapedString);
                patterns.add(pattern);
            }
        }
        return patterns;
    }

    @Override
    public AbstractJcrNode addNode( String relPath )
        throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException,
        RepositoryException {
        checkSession();
        return addNode(relPath, "nt:unstructured", null);
    }

    @Override
    public AbstractJcrNode addNode( String relPath,
                                    String primaryNodeTypeName )
        throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException,
        ConstraintViolationException, RepositoryException {
        checkSession();
        return addNode(relPath, primaryNodeTypeName, null);
    }

    /**
     * Adds the a new node with the given primary type (if specified) at the given relative path with the given UUID (if
     * specified).
     * 
     * @param relPath the at which the new node should be created
     * @param primaryNodeTypeName the desired primary type for the new node; null value indicates that the default primary type
     *        from the appropriate definition for this node should be used
     * @param desiredKey the key for the new node; may be null if the key is to be generated
     * @return the newly created node
     * @throws ItemExistsException if an item at the specified path already exists and same-name siblings are not allowed.
     * @throws PathNotFoundException if the specified path implies intermediary nodes that do not exist.
     * @throws VersionException not thrown at this time, but included for compatibility with the specification
     * @throws ConstraintViolationException if the change would violate a node type or implementation-specific constraint.
     * @throws LockException not thrown at this time, but included for compatibility with the specification
     * @throws RepositoryException if another error occurs
     * @see #addNode(String, String)
     */
    final AbstractJcrNode addNode( String relPath,
                                   String primaryNodeTypeName,
                                   NodeKey desiredKey )
        throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException,
        RepositoryException {

        // Parse the primary type name ...
        Name childPrimaryTypeName = null;
        try {
            childPrimaryTypeName = session.nameFactory().create(primaryNodeTypeName);
        } catch (org.modeshape.jcr.value.ValueFormatException e) {
            throw new RepositoryException(JcrI18n.invalidNodeTypeNameParameter.text(primaryNodeTypeName, "primaryNodeTypeName"));
        }

        // Resolve the relative path ...
        Path path = null;
        try {
            path = session.pathFactory().create(relPath);
        } catch (org.modeshape.jcr.value.ValueFormatException e) {
            throw new RepositoryException(JcrI18n.invalidPathParameter.text(relPath, "relPath"));
        }
        if (path.size() == 0 || path.isIdentifier() || path.getLastSegment().getIndex() > 1 || relPath.endsWith("]")) {
            throw new RepositoryException(JcrI18n.invalidPathParameter.text(relPath, "relPath"));
        }
        if (path.size() > 1) {
            // The relative path points to another node, so look for it ...
            Path parentPath = path.getParent();
            try {
                // Find the parent node ...
                AbstractJcrItem parent = session.findItem(this, parentPath);
                if (parent instanceof AbstractJcrNode) {
                    // delegate to the parent node ...
                    Name childName = path.getLastSegment().getName();
                    session.checkPermission(path, ModeShapePermissions.ADD_NODE);
                    return ((AbstractJcrNode)parent).addChildNode(childName, childPrimaryTypeName, desiredKey);
                } else if (parent instanceof AbstractJcrProperty) {
                    // Per the TCK, if relPath references a property, then we have to throw a ConstraintViolationException.
                    throw new ConstraintViolationException(JcrI18n.invalidPathParameter.text(relPath, "relPath"));
                }
            } catch (ItemNotFoundException e) {
                // We have to convert to a path not found ...
                throw new PathNotFoundException(e.getMessage(), e.getCause());
            } catch (RepositoryException e) {
                throw e;
            }
        }

        // Otherwise, the path has size == 1 and it specifies the child ...
        session.checkPermission(path, ModeShapePermissions.ADD_NODE);
        Name childName = path.getLastSegment().getName();
        return addChildNode(childName, childPrimaryTypeName, desiredKey);
    }

    /**
     * Adds the a new node with the given primary type (if specified) at the given relative path with the given UUID (if
     * specified).
     * 
     * @param childName the name for the new node; may not be null
     * @param childPrimaryNodeTypeName the desired primary type for the new node; null value indicates that the default primary
     *        type from the appropriate definition for this node should be used
     * @param desiredKey the key for the new node; may be null if the key is to be generated
     * @return the newly created node
     * @throws ItemExistsException if an item at the specified path already exists and same-name siblings are not allowed.
     * @throws PathNotFoundException if the specified path implies intermediary nodes that do not exist.
     * @throws VersionException not thrown at this time, but included for compatibility with the specification
     * @throws ConstraintViolationException if the change would violate a node type or implementation-specific constraint.
     * @throws LockException not thrown at this time, but included for compatibility with the specification
     * @throws RepositoryException if another error occurs
     * @see #addNode(String, String)
     */
    final AbstractJcrNode addChildNode( Name childName,
                                        Name childPrimaryNodeTypeName,
                                        NodeKey desiredKey )
        throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException,
        RepositoryException {
        checkNodeTypeCanBeModified();

        if (isLocked() && !getLock().isLockOwningSession()) {
            throw new LockException(JcrI18n.lockTokenNotHeld.text(location()));
        }

        // Determine the node type based upon this node's type information ...
        SessionCache cache = sessionCache();
        CachedNode node = node();
        Name primaryTypeName = node.getPrimaryType(cache);
        Set<Name> mixins = node.getMixinTypes(cache);
        int numExistingSns = node.getChildReferences(cache).getChildCount(childName);

        // Determine the name for the primary node type
        NodeTypes nodeTypes = session.nodeTypes();
        JcrNodeDefinition childDefn = null;
        if (childPrimaryNodeTypeName != null) {
            if (INTERNAL_NODE_TYPE_NAMES.contains(childPrimaryNodeTypeName)) {
                String workspaceName = workspaceName();
                String childPath = readable(session.pathFactory().create(path(), childName, numExistingSns + 1));
                String msg = JcrI18n.unableToCreateNodeWithInternalPrimaryType.text(childPrimaryNodeTypeName,
                                                                                    childPath,
                                                                                    workspaceName);
                throw new ConstraintViolationException(msg);
            }
            JcrNodeType primaryType = nodeTypes.getNodeType(childPrimaryNodeTypeName);
            if (primaryType == null) {
                Path pathForChild = session.pathFactory().create(path(), childName, numExistingSns + 1);
                I18n msg = JcrI18n.unableToCreateNodeWithPrimaryTypeThatDoesNotExist;
                throw new NoSuchNodeTypeException(msg.text(childPrimaryNodeTypeName, pathForChild, workspaceName()));
            }

            if (primaryType.isMixin()) {
                I18n msg = JcrI18n.cannotUseMixinTypeAsPrimaryType;
                throw new ConstraintViolationException(msg.text(primaryType.getName()));
            }

            if (primaryType.isAbstract()) {
                I18n msg = JcrI18n.primaryTypeCannotBeAbstract;
                throw new ConstraintViolationException(msg.text(primaryType.getName()));
            }
        }

        // Determine the node type based upon this node's type information ...
        boolean skipProtected = true;
        childDefn = nodeTypes.findChildNodeDefinition(primaryTypeName,
                                                      mixins,
                                                      childName,
                                                      childPrimaryNodeTypeName,
                                                      numExistingSns,
                                                      skipProtected);
        if (childDefn == null) {
            // Failed to find an appropriate child node definition. But we need more information to throw the correct error.
            int sns = numExistingSns + 1;
            String childPath = readable(session.pathFactory().create(path(), childName, sns));
            if (numExistingSns > 0) {
                // There was already at least one existing node with the same name, so see if there is a child node definition
                // that does not allow same-name-siblings ...
                childDefn = nodeTypes.findChildNodeDefinition(primaryTypeName,
                                                              mixins,
                                                              childName,
                                                              childPrimaryNodeTypeName,
                                                              0,
                                                              skipProtected);

                // This failed, so start getting the info required to throw an exception ...
                String workspaceName = workspaceName();

                if (childDefn != null) {
                    // So this failed only because the child definition did not allow same-name-siblings,
                    // so throw ItemExistsException per the JavaDoc of Node.addNode(String) and
                    // per the JCR 1.0.1 specification, section 7.1.4. (The JCR 2.0 specification is less
                    // clear about the exact signatures and exception, relying upon the JavaDoc for these.)
                    // Only failed because there was no SNS definition - throw ItemExistsException per 7.1.4 of 1.0.1 spec
                    String msg = JcrI18n.noSnsDefinitionForNode.text(childPath, workspaceName);
                    throw new ItemExistsException(msg);
                }
            }
            // Didn't work for other reasons - throw ConstraintViolationException
            String repoName = session.repository().repositoryName();
            String msg = JcrI18n.nodeDefinitionCouldNotBeDeterminedForNode.text(childPath, workspaceName(), repoName);
            throw new ConstraintViolationException(msg);
        }
        assert childDefn != null;

        if (childPrimaryNodeTypeName == null) {
            JcrNodeType defaultPrimaryType = childDefn.getDefaultPrimaryType();
            if (defaultPrimaryType == null) {
                // There is no default primary type ...
                int sns = numExistingSns + 1;
                String childPath = readable(session.pathFactory().create(path(), childName, sns));
                I18n msg = JcrI18n.unableToCreateNodeWithNoDefaultPrimaryTypeOnChildNodeDefinition;
                String nodeTypeName = childDefn.getDeclaringNodeType().getName();
                throw new NoSuchNodeTypeException(msg.text(childDefn.getName(), nodeTypeName, childPath, workspaceName()));

            }
            childPrimaryNodeTypeName = defaultPrimaryType.getInternalName();
        }

        // We can create the child, so start by building the required properties ...
        PropertyFactory propFactory = session.propertyFactory();
        Property ptProp = propFactory.create(JcrLexicon.PRIMARY_TYPE, childPrimaryNodeTypeName);

        if (JcrNtLexicon.UNSTRUCTURED.equals(childPrimaryNodeTypeName)) {
            // This is very common, and we know they don't have auto-created properties or children ...
            MutableCachedNode newChild = mutable().createChild(cache, desiredKey, childName, ptProp);

            // And get or create the JCR node ...
            AbstractJcrNode jcrNode = session.node(newChild.getKey(), null);

            // Set the child node definition ...
            jcrNode.setNodeDefinitionId(childDefn.getId(), nodeTypes.getVersion());
            return jcrNode;
        }

        // Auto-create the properties ...
        NodeTypes capabilities = session.repository().nodeTypeManager().getNodeTypes();
        int sns = numExistingSns + 1;
        LinkedList<Property> props = autoCreatePropertiesFor(childName, sns, childPrimaryNodeTypeName, propFactory, capabilities);

        // Then create the node ...
        MutableCachedNode newChild = null;
        if (props != null) {
            props.addFirst(ptProp);
            newChild = mutable().createChild(cache, desiredKey, childName, props);
        } else {
            newChild = mutable().createChild(cache, desiredKey, childName, ptProp);
        }

        // And get or create the JCR node ...
        AbstractJcrNode jcrNode = session.node(newChild.getKey(), null);

        // Set the child node definition ...
        jcrNode.setNodeDefinitionId(childDefn.getId(), nodeTypes.getVersion());

        // Create any mandatory properties or child nodes ...
        jcrNode.autoCreateChildren(childPrimaryNodeTypeName, capabilities);

        return jcrNode;
    }

    /**
     * If there are any auto-created properties, create them and return them in a list.
     * 
     * @param nodeName the name of the node; may not be null
     * @param snsIndex the same-name-sibling index for this node
     * @param primaryType the name of the primary type; may not be null
     * @param propertyFactory the factory for properties; may not be null
     * @param capabilities the node type capabilities cache; may not be null
     * @return the list of auto-created properties, or null if there are none
     */
    protected LinkedList<Property> autoCreatePropertiesFor( Name nodeName,
                                                            int snsIndex,
                                                            Name primaryType,
                                                            PropertyFactory propertyFactory,
                                                            NodeTypes capabilities ) {
        Collection<JcrPropertyDefinition> autoPropDefns = capabilities.getAutoCreatedPropertyDefinitions(primaryType);
        if (autoPropDefns.isEmpty()) {
            return null;
        }
        // There is at least one auto-created property on this node ...
        LinkedList<Property> props = new LinkedList<Property>();
        for (JcrPropertyDefinition defn : autoPropDefns) {
            Name propName = defn.getInternalName();
            if (defn.hasDefaultValues()) {
                // This may or may not be auto-created; we don't care ...
                Object[] defaultValues = defn.getRawDefaultValues();
                Property prop = null;
                if (defn.isMultiple()) {
                    prop = propertyFactory.create(propName, defaultValues);
                } else {
                    prop = propertyFactory.create(propName, defaultValues[0]);
                }
                props.add(prop);
            }
        }
        return props;
    }

    /**
     * Create in this node any auto-created child nodes.
     * 
     * @param primaryType the desired primary type for the new node; null value indicates that the default primary type from the
     *        appropriate definition for this node should be used
     * @param capabilities the node type capabilities cache; may not be null
     * @throws ItemExistsException if an item at the specified path already exists and same-name siblings are not allowed.
     * @throws PathNotFoundException if the specified path implies intermediary nodes that do not exist.
     * @throws VersionException not thrown at this time, but included for compatibility with the specification
     * @throws ConstraintViolationException if the change would violate a node type or implementation-specific constraint.
     * @throws LockException not thrown at this time, but included for compatibility with the specification
     * @throws RepositoryException if another error occurs
     */
    protected void autoCreateChildren( Name primaryType,
                                       NodeTypes capabilities )
        throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException,
        RepositoryException {
        Collection<JcrNodeDefinition> autoChildDefns = capabilities.getAutoCreatedChildNodeDefinitions(primaryType);
        if (!autoChildDefns.isEmpty()) {
            // There is at least one auto-created child under this node ...
            Set<Name> childNames = new HashSet<Name>();
            for (JcrNodeDefinition defn : autoChildDefns) {
                // Residual definitions cannot be both auto-created and residual;
                // see Section 3.7.2.3.4 of the JCR 2.0 specfication"
                assert !defn.isResidual();
                if (defn.isProtected()) {
                    // Protected items are created by the implementation, so we'll not do these ...
                    continue;
                }
                Name childName = defn.getInternalName();
                if (!childNames.contains(childName)) {
                    // We've not already created a child with this name ...
                    JcrNodeType childPrimaryType = defn.getDefaultPrimaryType();
                    addChildNode(childName, childPrimaryType.getInternalName(), null);
                }
            }
        }
    }

    @Override
    public void orderBefore( String srcChildRelPath,
                             String destChildRelPath )
        throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException, ItemNotFoundException,
        LockException, RepositoryException {
        checkSession();

        // Make sure ordering is supported on this node ...
        if (!getPrimaryNodeType().hasOrderableChildNodes()) {
            String msg = JcrI18n.notOrderable.text(getPrimaryNodeType().getName(), location());
            throw new UnsupportedRepositoryOperationException(msg);
        }

        Path srcPath = session.pathFactory().create(srcChildRelPath);
        if (srcPath.isAbsolute() || srcPath.size() != 1) {
            throw new ItemNotFoundException(JcrI18n.invalidPathParameter.text(srcChildRelPath, "destChildRelPath"));
        }

        session.checkPermission(srcPath.getParent(), ModeShapePermissions.ADD_NODE);

        SessionCache cache = session.cache();
        ChildReferences childRefs = node().getChildReferences(cache);
        ChildReference srcRef = childRefs.getChild(srcPath.getLastSegment());
        if (srcRef == null) {
            String workspaceName = workspaceName();
            throw new ItemNotFoundException(JcrI18n.pathNotFound.text(srcChildRelPath, workspaceName));
        }

        NodeKey destKey = null;
        if (destChildRelPath != null) {
            Path destPath = session.pathFactory().create(destChildRelPath);
            if (destPath.isAbsolute() || destPath.size() != 1) {
                throw new ItemNotFoundException(JcrI18n.invalidPathParameter.text(destChildRelPath, "destChildRelPath"));
            }

            ChildReference destRef = childRefs.getChild(destPath.getLastSegment());
            if (destRef == null) {
                String workspaceName = workspaceName();
                throw new ItemNotFoundException(JcrI18n.pathNotFound.text(destChildRelPath, workspaceName));
            }
            destKey = destRef.getKey();
        }

        // Now that we've verified the parameters, perform the move ...
        mutableParent().reorderChild(cache, srcRef.getKey(), destKey);
    }

    @Override
    public AbstractJcrProperty setProperty( String name,
                                            Value value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        CheckArg.isNotNull(name, "name");
        checkSession();
        if (value == null) return removeExistingProperty(nameFrom(name));
        JcrValue jcrValue = (JcrValue)value;
        if (jcrValue.value() == null) {
            throw new ValueFormatException(JcrI18n.valueMayNotContainNull.text(name));
        }
        return setProperty(nameFrom(name), jcrValue, false);
    }

    @Override
    public AbstractJcrProperty setProperty( String name,
                                            Value value,
                                            int type )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        CheckArg.isNotNull(name, "name");
        checkSession();
        if (value == null) return removeExistingProperty(nameFrom(name));
        JcrValue jcrValue = (JcrValue)value;
        if (jcrValue.value() == null) {
            throw new ValueFormatException(JcrI18n.valueMayNotContainNull.text(name));
        }
        return setProperty(nameFrom(name), jcrValue.asType(type), false);
    }

    @Override
    public AbstractJcrProperty setProperty( String name,
                                            Value[] values )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        CheckArg.isNotNull(name, "name");
        checkSession();
        if (values == null) return removeExistingProperty(nameFrom(name));

        if (values.length == 0) {
            values = new JcrValue[] {};
        } else {
            // Check for a non-null Value that contains a null reference ...
            for (Value value : values) {
                JcrValue jcrValue = (JcrValue)value;
                if (jcrValue != null && jcrValue.value() == null) {
                    throw new ValueFormatException(JcrI18n.valueMayNotContainNull.text(name));
                }
            }
        }

        return setProperty(nameFrom(name), values, PropertyType.UNDEFINED, false);
    }

    @Override
    public AbstractJcrProperty setProperty( String name,
                                            Value[] values,
                                            int type )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        CheckArg.isNotNull(name, "name");
        checkSession();
        if (values == null) return removeExistingProperty(nameFrom(name));

        if (values.length == 0) {
            values = new JcrValue[] {};
        } else {
            // Check for a non-null Value that contains a null reference ...
            for (Value value : values) {
                JcrValue jcrValue = (JcrValue)value;
                if (jcrValue != null && jcrValue.value() == null) {
                    throw new ValueFormatException(JcrI18n.valueMayNotContainNull.text(name));
                }
            }
        }

        // Set the value, perhaps to an empty array ...
        return setProperty(nameFrom(name), values, type, false);
    }

    @Override
    public AbstractJcrProperty setProperty( String name,
                                            String[] values )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        CheckArg.isNotNull(name, "name");
        checkSession();
        if (values == null) return removeExistingProperty(nameFrom(name));
        return setProperty(nameFrom(name), valuesFrom(PropertyType.STRING, values), PropertyType.UNDEFINED, false);
    }

    @Override
    public AbstractJcrProperty setProperty( String name,
                                            String[] values,
                                            int type )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        CheckArg.isNotNull(name, "name");
        checkSession();
        if (values == null) return removeExistingProperty(nameFrom(name));
        return setProperty(nameFrom(name), valuesFrom(type, values), PropertyType.UNDEFINED, false);
    }

    @Override
    public AbstractJcrProperty setProperty( String name,
                                            String value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        CheckArg.isNotNull(name, "name");
        checkSession();
        if (value == null) return removeExistingProperty(nameFrom(name));
        return setProperty(nameFrom(name), valueFrom(PropertyType.STRING, value), false);
    }

    @Override
    public AbstractJcrProperty setProperty( String name,
                                            String value,
                                            int type )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        CheckArg.isNotNull(name, "name");
        checkSession();
        if (value == null) return removeExistingProperty(nameFrom(name));
        return setProperty(nameFrom(name), valueFrom(type, value), false);
    }

    @Override
    public AbstractJcrProperty setProperty( String name,
                                            InputStream value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        CheckArg.isNotNull(name, "name");
        checkSession();
        if (value == null) return removeExistingProperty(nameFrom(name));
        return setProperty(nameFrom(name), valueFrom(value), false);
    }

    @Override
    public AbstractJcrProperty setProperty( String name,
                                            Binary value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        CheckArg.isNotNull(name, "name");
        checkSession();
        return setProperty(nameFrom(name), valueFrom(PropertyType.BINARY, value), false);
    }

    @Override
    public AbstractJcrProperty setProperty( String name,
                                            boolean value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        CheckArg.isNotNull(name, "name");
        checkSession();
        return setProperty(nameFrom(name), valueFrom(PropertyType.BOOLEAN, value), false);
    }

    @Override
    public AbstractJcrProperty setProperty( String name,
                                            double value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        CheckArg.isNotNull(name, "name");
        checkSession();
        return setProperty(nameFrom(name), valueFrom(PropertyType.DOUBLE, value), false);
    }

    @Override
    public AbstractJcrProperty setProperty( String name,
                                            BigDecimal value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        CheckArg.isNotNull(name, "name");
        checkSession();
        return setProperty(nameFrom(name), valueFrom(PropertyType.DECIMAL, value), false);
    }

    @Override
    public AbstractJcrProperty setProperty( String name,
                                            long value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        CheckArg.isNotNull(name, "name");
        checkSession();
        return setProperty(nameFrom(name), valueFrom(PropertyType.LONG, value), false);
    }

    @Override
    public AbstractJcrProperty setProperty( String name,
                                            Calendar value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        CheckArg.isNotNull(name, "name");
        checkSession();
        if (value == null) return removeExistingProperty(nameFrom(name));
        return setProperty(nameFrom(name), valueFrom(value), false);
    }

    @Override
    public AbstractJcrProperty setProperty( String name,
                                            Node value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        CheckArg.isNotNull(name, "name");
        checkSession();
        if (value == null) return removeExistingProperty(nameFrom(name));
        return setProperty(nameFrom(name), valueFrom(value), false);
    }

    /**
     * Removes an existing property with the supplied name. Note that if a propert with the given name does not exist, then this
     * method returns null and does not throw an exception.
     * 
     * @param name the name of the property; may not be null
     * @return the property that was removed
     * @throws VersionException if the node is checked out
     * @throws LockException if the node is locked
     * @throws RepositoryException if some other error occurred
     */
    final AbstractJcrProperty removeExistingProperty( Name name ) throws VersionException, LockException, RepositoryException {
        AbstractJcrProperty existing = getProperty(name);
        if (existing != null) {
            existing.remove();
            return existing;
        }
        // Return without throwing an exception to match behavior of the reference implementation.
        // This is also in conformance with the spec. See MODE-956 for details.
        return null;
    }

    /**
     * @param name the name of the property; may not be null
     * @param value the value of the property; may not be null
     * @param skipReferenceValidation indicates whether constraints on REFERENCE properties should be enforced
     * @return the new JCR property object
     * @throws VersionException if the node is checked out
     * @throws LockException if the node is locked
     * @throws ConstraintViolationException if the new value would violate the constraints on the property definition
     * @throws RepositoryException if the named property does not exist, or if some other error occurred
     */
    final AbstractJcrProperty setProperty( Name name,
                                           JcrValue value,
                                           boolean skipReferenceValidation )
        throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        assert value != null;
        assert value.value() != null;
        checkForLock();
        checkForCheckedOut();
        checkNodeTypeCanBeModified();
        session.checkPermission(path(), ModeShapePermissions.SET_PROPERTY);

        // Check for an existing JCR property object; note that this will load the internal property if necessary ...
        AbstractJcrProperty existing = getProperty(name);
        if (existing != null) {
            // Found an existing property ...
            if (existing.isMultiple()) {
                // The cardinality of the new values does not match the cardinality of the existing property ...
                I18n msg = JcrI18n.unableToSetMultiValuedPropertyUsingSingleValue;
                throw new javax.jcr.ValueFormatException(msg.text(readable(name), location(), workspaceName()));
            }
            // Delegate to the existing JCR property ...
            existing.setValue(value);
            return existing;
        }

        // Otherwise, we have to create the property, so first find a valid property definition ...
        SessionCache cache = sessionCache();
        MutableCachedNode node = mutable();
        Name primaryType = node.getPrimaryType(cache);
        Set<Name> mixinTypes = node.getMixinTypes(cache);
        NodeTypes nodeTypes = session.nodeTypes();
        JcrPropertyDefinition defn = null;
        defn = nodeTypes.findPropertyDefinition(session, primaryType, mixinTypes, name, value, true, true, true);

        if (defn == null) {
            // Failed to find a valid property definition,
            // so figure out if there's a definition that would work if it had no constraints ...
            defn = nodeTypes.findPropertyDefinition(session, primaryType, mixinTypes, name, value, true, true, false);

            String propName = readable(name);
            if (defn != null) {
                String defnName = defn.getName();
                String nodeTypeName = defn.getDeclaringNodeType().getName();
                I18n msg = JcrI18n.valueViolatesConstraintsOnDefinition;
                throw new ConstraintViolationException(msg.text(propName, value.getString(), location(), defnName, nodeTypeName));
            }
            I18n msg = JcrI18n.noPropertyDefinition;
            throw new ConstraintViolationException(msg.text(propName, location(), readable(primaryType), readable(mixinTypes)));
        }

        // The 'findBestPropertyDefintion' method checks constraints for all definitions exception those with a
        // require type of REFERENCE. This is because checking such constraints may cause unnecessary loading of nodes.
        // Therefore, see if this is the case ...
        int requiredType = defn.getRequiredType();
        if (requiredType == PropertyType.REFERENCE || requiredType == PropertyType.WEAKREFERENCE) {
            // Check that the REFERENCE value satisfies the constraints ...
            if (!defn.canCastToTypeAndSatisfyConstraints(value, session)) {
                // The REFERENCE value did not satisfy the constraints ...
                String propName = readable(name);
                String defnName = defn.getName();
                String nodeTypeName = defn.getDeclaringNodeType().getName();
                I18n i18n = JcrI18n.weakReferenceValueViolatesConstraintsOnDefinition;
                if (requiredType == PropertyType.REFERENCE) i18n = JcrI18n.referenceValueViolatesConstraintsOnDefinition;
                throw new ConstraintViolationException(i18n.text(propName, value.getString(), location(), defnName, nodeTypeName));
            }
        }

        // Create the JCR Property object ...
        if (requiredType == PropertyType.UNDEFINED) requiredType = value.getType();
        // Convert the value to the required type ...
        value = value.asType(requiredType);
        AbstractJcrProperty jcrProp = new JcrSingleValueProperty(this, name, requiredType);
        AbstractJcrProperty otherProp = this.jcrProperties.putIfAbsent(name, jcrProp);
        if (otherProp != null) {
            // Someone snuck in and created this property while we created ours, so use that instance instead ...
            jcrProp = otherProp;
        }
        // Set the property on the cached node (even if there was an 'otherProp' instance) ...
        Property newProperty = session.propertyFactory().create(name, value.value());
        node.setProperty(cache, newProperty);
        return jcrProp;
    }

    /**
     * @param name the name of the property; may not be null
     * @param values the values of the property; may not be null
     * @param jcrPropertyType the expected property type; may be {@link PropertyType#UNDEFINED} if the values should not be
     *        converted
     * @param skipReferenceValidation indicates whether constraints on REFERENCE properties should be enforced
     * @return the new JCR property object
     * @throws VersionException if the node is checked out
     * @throws LockException if the node is locked
     * @throws ConstraintViolationException if the new value would violate the constraints on the property definition
     * @throws RepositoryException if the named property does not exist, or if some other error occurred
     */
    final AbstractJcrProperty setProperty( Name name,
                                           Value[] values,
                                           int jcrPropertyType,
                                           boolean skipReferenceValidation )
        throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        assert values != null;
        checkForLock();
        checkForCheckedOut();
        checkNodeTypeCanBeModified();
        session.checkPermission(path(), ModeShapePermissions.SET_PROPERTY);

        // Force a conversion to the specified property type (if required) ...
        if (jcrPropertyType != PropertyType.UNDEFINED) {
            int len = values.length;
            JcrValue[] newValues = null;
            if (len == 0) {
                newValues = JcrMultiValueProperty.EMPTY_VALUES;
            } else {
                List<JcrValue> valuesWithDesiredType = new ArrayList<JcrValue>(len);
                for (int i = 0; i != len; ++i) {
                    JcrValue value = (JcrValue)values[i];
                    if (value == null) continue; // null values are removed
                    value = value.asType(jcrPropertyType);
                    valuesWithDesiredType.add(value);
                }
                if (valuesWithDesiredType.isEmpty()) {
                    newValues = JcrMultiValueProperty.EMPTY_VALUES;
                } else {
                    newValues = valuesWithDesiredType.toArray(new JcrValue[valuesWithDesiredType.size()]);
                }
            }
            values = newValues;
        }

        // Check for an existing JCR property object; note that this will load the internal property if necessary ...
        AbstractJcrProperty existing = getProperty(name);
        if (existing != null) {
            // Found an existing property, and per the JavaDoc for the multi-valued javax.jcr.Node#setProperty(...),
            // this method is to throw an exception if there is an existing property and it is not already multi-valued ...
            if (!existing.isMultiple()) {
                // The cardinality of the new values does not match the cardinality of the existing property ...
                I18n msg = JcrI18n.unableToSetSingleValuedPropertyUsingMultipleValues;
                throw new javax.jcr.ValueFormatException(msg.text(readable(name), location(), workspaceName()));
            }
            // Delegate to the existing JCR property ...
            existing.setValue(values);
            return existing;
        }

        // Otherwise, we have to create the property, so first find a valid property definition ...
        SessionCache cache = sessionCache();
        MutableCachedNode node = mutable();
        Name primaryType = node.getPrimaryType(cache);
        Set<Name> mixinTypes = node.getMixinTypes(cache);
        NodeTypes nodeTypes = session.nodeTypes();
        JcrPropertyDefinition defn = null;
        defn = nodeTypes.findPropertyDefinition(session, primaryType, mixinTypes, name, values, true, skipReferenceValidation);

        if (defn == null) {
            // Failed to find a valid property definition,
            // so figure out if there's a definition that would work if it had no constraints ...
            defn = nodeTypes.findPropertyDefinition(session, primaryType, mixinTypes, name, values, true, false);

            String propName = readable(name);
            if (defn != null) {
                String defnName = defn.getName();
                String nodeTypeName = defn.getDeclaringNodeType().getName();
                I18n msg = JcrI18n.valueViolatesConstraintsOnDefinition;
                throw new ConstraintViolationException(msg.text(propName, readable(values), location(), defnName, nodeTypeName));
            }
            // See if we can find a single-valued property definition ...
            defn = nodeTypes.findPropertyDefinition(session, primaryType, mixinTypes, name, values[0], true, false);
            if (defn == null) {
                // The cardinality of the new values does not match the available property definition ...
                I18n msg = JcrI18n.unableToSetSingleValuedPropertyUsingMultipleValues;
                throw new javax.jcr.ValueFormatException(msg.text(readable(name), location(), workspaceName()));
            }
            I18n msg = JcrI18n.noPropertyDefinition;
            throw new ConstraintViolationException(msg.text(propName, location(), readable(primaryType), readable(mixinTypes)));
        }

        // The 'findBestPropertyDefintion' method checks constraints for all definitions exception those with a
        // require type of REFERENCE. This is because checking such constraints may cause unnecessary loading of nodes.
        // Therefore, see if this is the case ...
        int requiredType = defn.getRequiredType();
        if (!skipReferenceValidation && (requiredType == PropertyType.REFERENCE || requiredType == PropertyType.WEAKREFERENCE)) {
            // Check that the REFERENCE value satisfies the constraints ...
            if (!defn.canCastToTypeAndSatisfyConstraints(values, session)) {
                // The REFERENCE value did not satisfy the constraints ...
                String propName = readable(name);
                String defnName = defn.getName();
                String nodeTypeName = defn.getDeclaringNodeType().getName();
                I18n i18n = JcrI18n.weakReferenceValueViolatesConstraintsOnDefinition;
                if (requiredType == PropertyType.REFERENCE) i18n = JcrI18n.referenceValueViolatesConstraintsOnDefinition;
                throw new ConstraintViolationException(i18n.text(propName, readable(values), location(), defnName, nodeTypeName));
            }
        }

        // Create the JCR Property object ...
        if (requiredType == PropertyType.UNDEFINED) {
            for (Value value : values) {
                if (value == null) continue;
                requiredType = value.getType();
                break;
            }
        }
        AbstractJcrProperty jcrProp = new JcrMultiValueProperty(this, name, requiredType);
        jcrProp.setPropertyDefinitionId(defn.getId(), nodeTypes.getVersion());
        AbstractJcrProperty otherProp = this.jcrProperties.putIfAbsent(name, jcrProp);
        if (otherProp != null) {
            // Someone snuck in and created this property while we created ours, so use that instance instead ...
            // Check that the cardinality is correct ...
            if (!jcrProp.isMultiple()) {
                // Overwrite the value anyway ...
                this.jcrProperties.put(name, jcrProp);
            }
            jcrProp = otherProp;
        }

        // The values may need to be converted to the definition's required type ...
        int numValues = values.length;
        Object[] objValues = new Object[numValues];
        int propertyType = defn.getRequiredType();
        if (propertyType == PropertyType.UNDEFINED || propertyType == jcrPropertyType) {
            // Can use the values as is ...
            for (int i = 0; i != numValues; ++i) {
                objValues[i] = ((JcrValue)values[i]).value();
            }
        } else {
            // A conversion is required ...
            try {
                org.modeshape.jcr.value.PropertyType msType = PropertyTypeUtil.modePropertyTypeFor(propertyType);
                org.modeshape.jcr.value.ValueFactory<?> factory = context().getValueFactories().getValueFactory(msType);
                for (int i = 0; i != numValues; ++i) {
                    objValues[i] = factory.create(((JcrValue)values[i]).value());
                }
            } catch (org.modeshape.jcr.value.ValueFormatException e) {
                throw new ValueFormatException(e.getMessage());
            }
        }

        // Set the property on the cached node (even if there was an 'otherProp' instance) ...
        Property newProperty = session.propertyFactory().create(name, objValues);
        node.setProperty(cache, newProperty);
        return jcrProp;
    }

    final Collection<AbstractJcrProperty> findJcrProperties( Iterator<Property> propertyIterator )
        throws AccessDeniedException, RepositoryException {
        try {
            Collection<AbstractJcrProperty> result = new LinkedList<AbstractJcrProperty>();
            while (propertyIterator.hasNext()) {
                Property property = propertyIterator.next();
                Name propertyName = property.getName();
                AbstractJcrProperty jcrProp = getProperty(propertyName);
                if (jcrProp != null) result.add(jcrProp);
            }
            return result;
        } catch (AccessControlException e) {
            throw new AccessDeniedException(e.getMessage(), e);
        } catch (Throwable e) {
            throw new RepositoryException(e.getMessage(), e);
        }
    }

    @Override
    public PropertyIterator getProperties() throws RepositoryException {
        checkSession();
        Iterator<Property> iter = node().getProperties(sessionCache());
        return new JcrPropertyIterator(findJcrProperties(iter));
    }

    @Override
    public PropertyIterator getProperties( String namePattern ) throws RepositoryException {
        namePattern = namePattern.trim();
        if (namePattern.length() == 0) return JcrEmptyPropertyIterator.INSTANCE;
        if ("*".equals(namePattern)) return getProperties();
        return getProperties(namePattern.split("[|]"));
    }

    @Override
    public PropertyIterator getProperties( String[] nameGlobs ) throws RepositoryException {
        CheckArg.isNotNull(nameGlobs, "nameGlobs");
        if (nameGlobs.length == 0) return JcrEmptyPropertyIterator.INSTANCE;

        List<?> patterns = createPatternsFor(nameGlobs);
        if (patterns.size() == 1 && patterns.get(0) instanceof String) {
            // This is a literal, so just look up by name ...
            Name literal = nameFrom((String)patterns.get(0));
            AbstractJcrProperty prop = getProperty(literal);
            if (prop == null) return JcrEmptyPropertyIterator.INSTANCE;
            return new JcrPropertyIterator(Collections.singletonList((javax.jcr.Property)prop));
        }
        Iterator<Property> propIter = node().getProperties(patterns, sessionCache());
        return new JcrPropertyIterator(findJcrProperties(propIter));
    }

    @Override
    public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
        checkSession();
        // Get the primary item name from this node's type ...
        NodeType primaryType = getPrimaryNodeType();
        String primaryItemNameString = primaryType.getPrimaryItemName();
        if (primaryItemNameString == null) {
            I18n msg = JcrI18n.noPrimaryItemNameDefinedOnPrimaryType;
            throw new ItemNotFoundException(msg.text(primaryType.getName(), location(), workspaceName()));
        }
        try {
            Path primaryItemPath = context().getValueFactories().getPathFactory().create(primaryItemNameString);
            if (primaryItemPath.size() == 1 && !primaryItemPath.isAbsolute()) {
                try {
                    return session.node(node(), primaryItemPath);
                } catch (PathNotFoundException e) {
                    // Must not be any child by that name, so now look for a property on the parent node ...
                    return getProperty(primaryItemPath.getLastSegment().getName());
                }
            }
            I18n msg = JcrI18n.primaryItemNameForPrimaryTypeIsNotValid;
            throw new ItemNotFoundException(msg.text(primaryType.getName(), primaryItemNameString, location(), workspaceName()));
        } catch (ValueFormatException error) {
            I18n msg = JcrI18n.primaryItemNameForPrimaryTypeIsNotValid;
            throw new ItemNotFoundException(msg.text(primaryType.getName(), primaryItemNameString, location(), workspaceName()));
        } catch (PathNotFoundException error) {
            I18n msg = JcrI18n.primaryItemDoesNotExist;
            throw new ItemNotFoundException(msg.text(primaryType.getName(), primaryItemNameString, location(), workspaceName()));
        }
    }

    @Deprecated
    @Override
    public final String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        if (!isReferenceable()) {
            throw new UnsupportedRepositoryOperationException();
        }
        return getIdentifier();
    }

    @Override
    public int getIndex() throws RepositoryException {
        return node().getSegment(sessionCache()).getIndex();
    }

    @Override
    public final PropertyIterator getReferences() throws RepositoryException {
        return getReferences(null);
    }

    @Override
    public final PropertyIterator getReferences( String propertyName ) throws RepositoryException {
        checkSession();
        return propertiesOnOtherNodesReferencingThis(propertyName, PropertyType.REFERENCE);
    }

    @Override
    public PropertyIterator getWeakReferences() throws RepositoryException {
        return getWeakReferences(null);
    }

    @Override
    public PropertyIterator getWeakReferences( String propertyName ) throws RepositoryException {
        checkSession();
        return propertiesOnOtherNodesReferencingThis(propertyName, PropertyType.WEAKREFERENCE);
    }

    /**
     * Find the properties on other nodes that are REFERENCE or WEAKREFERENCE properties (as dictated by the
     * <code>referenceType</code> parameter) to this node.
     * 
     * @param propertyName the name of the referring REFERENCE or WEAKREFERENCE properties on the other nodes, or null if all
     *        referring REFERENCE or WEAKREFERENCE properties should be returned
     * @param referenceType either {@link PropertyType#REFERENCE} or {@link PropertyType#WEAKREFERENCE}
     * @return the property iterator; never null by may be {@link JcrEmptyPropertyIterator empty} if there are no references or if
     *         this node is not referenceable
     * @throws RepositoryException if there is an error finding the referencing properties
     */
    protected PropertyIterator propertiesOnOtherNodesReferencingThis( String propertyName,
                                                                      int referenceType ) throws RepositoryException {
        assert referenceType == PropertyType.REFERENCE || referenceType == PropertyType.WEAKREFERENCE;
        if (!this.isReferenceable()) {
            // This node is not referenceable, so it cannot have any references to it ...
            return JcrEmptyPropertyIterator.INSTANCE;
        }
        ReferenceType refType = referenceType == PropertyType.REFERENCE ? ReferenceType.STRONG : ReferenceType.WEAK;
        NodeIterator iter = referringNodes(refType);
        if (!iter.hasNext()) {
            return JcrEmptyPropertyIterator.INSTANCE;
        }
        // Use the identifier, not the UUID (since the getUUID() method just calls the getIdentifier() method) ...
        String id = getIdentifier();
        List<javax.jcr.Property> references = new LinkedList<javax.jcr.Property>();
        while (iter.hasNext()) {
            javax.jcr.Node node = iter.nextNode();

            // Go through the properties and look for reference properties that have a value of this node's UUID ...
            PropertyIterator propIter = node.getProperties();
            while (propIter.hasNext()) {
                javax.jcr.Property prop = propIter.nextProperty();
                // Look at the definition's required type ...
                int propType = prop.getDefinition().getRequiredType();
                if (propType == referenceType || propType == PropertyType.UNDEFINED || propType == PropertyType.STRING) {
                    if (propertyName != null && !propertyName.equals(prop.getName())) continue;
                    if (prop.getDefinition().isMultiple()) {
                        for (Value value : prop.getValues()) {
                            if (id.equals(value.getString())) {
                                references.add(prop);
                                break;
                            }
                        }
                    } else {
                        Value value = prop.getValue();
                        if (id.equals(value.getString())) {
                            references.add(prop);
                        }
                    }
                }
            }
        }

        if (references.isEmpty()) return JcrEmptyPropertyIterator.INSTANCE;
        return new JcrPropertyIterator(references);
    }

    /**
     * Obtain an iterator over the nodes that reference this node.
     * 
     * @param referenceType specification of the type of references to include; may not be null
     * @return the iterator over the referencing nodes; never null
     * @throws RepositoryException if an error occurs while obtaining the information
     */
    protected final NodeIterator referringNodes( ReferenceType referenceType ) throws RepositoryException {
        if (!this.isReferenceable()) {
            return JcrEmptyNodeIterator.INSTANCE;
        }

        // Get all of the nodes that are referring to this node ...
        Set<NodeKey> keys = node().getReferrers(sessionCache(), referenceType);
        if (keys.isEmpty()) return JcrEmptyNodeIterator.INSTANCE;
        return new JcrNodeIterator(session(), keys.iterator(), keys.size(), null);
    }

    @Override
    public boolean hasProperty( String relPath ) throws RepositoryException {
        CheckArg.isNotEmpty(relPath, "relPath");
        checkSession();
        if (relPath.indexOf('/') >= 0 || relPath.startsWith("[")) {
            try {
                getProperty(relPath);
                return true;
            } catch (PathNotFoundException e) {
                return false;
            }
        }
        if (relPath.startsWith(".")) {
            if (relPath.length() == 1) return false;
            if (relPath.equals("..")) return false;
        }
        // Otherwise it should be a property on this node ...
        return node().hasProperty(nameFrom(relPath), sessionCache());
    }

    @Override
    public boolean hasNodes() throws RepositoryException {
        return !node().getChildReferences(sessionCache()).isEmpty();
    }

    @Override
    public boolean hasProperties() throws RepositoryException {
        return node().hasProperties(sessionCache());
    }

    @Override
    public JcrNodeType getPrimaryNodeType() throws RepositoryException {
        checkSession();
        return session().nodeTypeManager().getNodeType(node().getPrimaryType(sessionCache()));
    }

    /**
     * Get the name of this node's primary type.
     * 
     * @return the primary type name
     * @throws ItemNotFoundException if this node no longer exists in the repository
     * @throws InvalidItemStateException if the node has been removed in this session's transient state
     */
    Name getPrimaryTypeName() throws ItemNotFoundException, InvalidItemStateException {
        return node().getPrimaryType(sessionCache());
    }

    @Override
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        checkSession();
        JcrNodeTypeManager nodeTypeManager = session().nodeTypeManager();
        List<NodeType> mixinNodeTypes = new LinkedList<NodeType>();
        for (Name mixinTypeName : node().getMixinTypes(sessionCache())) {
            NodeType nodeType = nodeTypeManager.getNodeType(mixinTypeName);
            if (nodeType != null) mixinNodeTypes.add(nodeType);
        }
        return mixinNodeTypes.toArray(new NodeType[mixinNodeTypes.size()]);
    }

    /**
     * Get the names of this node's mixin types.
     * 
     * @return the mixin type names; never null but possibly empty
     * @throws ItemNotFoundException if this node no longer exists in the repository
     * @throws InvalidItemStateException if the node has been removed in this session's transient state
     */
    Set<Name> getMixinTypeNames() throws ItemNotFoundException, InvalidItemStateException {
        return node().getMixinTypes(sessionCache());
    }

    @Override
    public boolean isNodeType( String nodeTypeName ) throws RepositoryException {
        return isNodeType(nameFrom(nodeTypeName));
    }

    /**
     * Determine whether this node's primary type or any of the mixins are or extend the node type with the supplied name. This
     * method is semantically equivalent to but slightly more efficient than the {@link #isNodeType(String) equivalent in the JCR
     * API}, especially when the node type name is already a {@link Name} object.
     * 
     * @param nodeTypeName the name of the node type
     * @return true if this node is of the node type given by the supplied name, or false otherwise
     * @throws RepositoryException if there is an exception
     */
    public final boolean isNodeType( Name nodeTypeName ) throws RepositoryException {
        checkSession();
        SessionCache cache = sessionCache();
        NodeTypes nodeTypes = session().nodeTypes();
        try {
            CachedNode node = node();
            // Check the primary type ...
            Name primaryTypeName = node.getPrimaryType(cache);
            JcrNodeType primaryType = nodeTypes.getNodeType(primaryTypeName);
            if (primaryType.isNodeType(nodeTypeName)) {
                return true;
            }
            Set<Name> mixinTypes = node.getMixinTypes(cache);
            for (Name mixinTypeName : mixinTypes) {
                JcrNodeType mixinType = nodeTypes.getNodeType(mixinTypeName);
                if (mixinType != null && mixinType.isNodeType(nodeTypeName)) {
                    return true;
                }
            }
        } catch (ItemNotFoundException e) {
            // The node has been removed, so do nothing
        }
        return false;
    }

    private void autoCreateItemsFor( JcrNodeType nodeType )
        throws InvalidItemStateException, ConstraintViolationException, AccessDeniedException, RepositoryException {

        MutableCachedNode node = mutable();
        SessionCache cache = sessionCache();
        for (JcrPropertyDefinition propDefn : nodeType.allPropertyDefinitions()) {
            if (propDefn.isAutoCreated() && !propDefn.isProtected()) {
                Name propName = propDefn.getInternalName();
                Property autoCreatedProp = node.getProperty(propName, cache);
                if (autoCreatedProp == null) {
                    // We have to 'auto-create' the property ...
                    JcrValue[] defaultValues = propDefn.getDefaultValues();
                    if (defaultValues != null) { // may be empty
                        if (propDefn.isMultiple()) {
                            setProperty(propDefn.getInternalName(), defaultValues, propDefn.getRequiredType(), true);
                        } else {
                            assert propDefn.getDefaultValues().length == 1;
                            setProperty(propDefn.getInternalName(), defaultValues[0], false); // don't skip constraint checks
                        }
                    }
                    // otherwise, we don't care
                }
            }
        }

        for (JcrNodeDefinition nodeDefn : nodeType.allChildNodeDefinitions()) {
            if (nodeDefn.isAutoCreated() && !nodeDefn.isProtected()) {
                Name nodeName = nodeDefn.getInternalName();
                ChildReferences refs = node.getChildReferences(cache);
                if (refs.getChildCount(nodeName) == 0) {
                    JcrNodeType defaultPrimaryType = nodeDefn.getDefaultPrimaryType();
                    assert defaultPrimaryType != null;
                    Name primaryType = defaultPrimaryType.getInternalName();
                    addChildNode(nodeName, primaryType, null);
                }
            }
        }
    }

    @Override
    public void setPrimaryType( String nodeTypeName )
        throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        CheckArg.isNotZeroLength(nodeTypeName, "nodeTypeName");

        checkSession();
        checkForLock();
        checkForCheckedOut();
        session.checkPermission(path(), ModeShapePermissions.SET_PROPERTY);

        if (isRoot()) {
            throw new ConstraintViolationException(JcrI18n.setPrimaryTypeOnRootNodeIsNotSupported.text());
        }

        Name newPrimaryTypeName = nameFrom(nodeTypeName);
        NodeTypes nodeTypes = session.nodeTypes();
        if (newPrimaryTypeName.equals(getPrimaryTypeName())) return;
        final JcrNodeType newPrimaryType = nodeTypes.getNodeType(newPrimaryTypeName);
        if (newPrimaryType.isMixin()) {
            throw new ConstraintViolationException(JcrI18n.cannotUseMixinTypeAsPrimaryType.text(nodeTypeName));
        }

        // Make sure that all existing properties will have a valid property definition with the new primary type ...
        SessionCache cache = sessionCache();
        CachedNode node = node();
        Name oldPrimaryType = node.getPrimaryType(cache);
        Set<Name> mixinTypeNames = node.getMixinTypes(cache);
        Iterator<Property> iter = node.getProperties(cache);
        while (iter.hasNext()) {
            Property prop = iter.next();
            try {
                createJcrProperty(prop, newPrimaryTypeName, mixinTypeNames);
            } catch (ConstraintViolationException e) {
                // Change the message ...
                String propName = readable(prop.getName());
                I18n msg = JcrI18n.unableToChangePrimaryTypeDueToPropertyDefinition;
                throw new ConstraintViolationException(msg.text(location(), oldPrimaryType, newPrimaryTypeName, propName), e);
            }
        }

        // Check that this would not violate the parent's child node type definitions ...
        Name nodeName = node.getName(cache);
        CachedNode parent = getParent().node();
        Name primaryType = parent.getPrimaryType(cache);
        Set<Name> mixins = parent.getMixinTypes(cache);
        int numExistingSns = parent.getChildReferences(cache).getChildCount(nodeName);
        boolean skipProtected = true;
        JcrNodeDefinition childDefn = nodeTypes.findChildNodeDefinition(primaryType,
                                                                        mixins,
                                                                        nodeName,
                                                                        newPrimaryTypeName,
                                                                        numExistingSns,
                                                                        skipProtected);
        if (childDefn == null) {
            String ptype = readable(primaryType);
            String mtypes = readable(parent.getMixinTypes(cache));
            I18n msg = JcrI18n.unableToChangePrimaryTypeDueToParentsChildDefinition;
            throw new ConstraintViolationException(msg.text(location(), oldPrimaryType, newPrimaryTypeName, ptype, mtypes));
        }
        setNodeDefinitionId(childDefn.getId(), nodeTypes.getVersion());

        // Change the primary type property ...
        boolean wasReferenceable = isReferenceable();
        MutableCachedNode mutable = mutable();
        mutable.setProperty(cache, session.propertyFactory().create(JcrLexicon.PRIMARY_TYPE, newPrimaryTypeName));

        if (wasReferenceable && !isReferenceable()) {
            // Need to remove the 'jcr:uuid' reference ...
            mutable.removeProperty(cache, JcrLexicon.UUID);
        }

        // And auto-create any properties that are defined by the new primary type ...
        autoCreateItemsFor(newPrimaryType);

        // Since we've changed the primary type, release the cached property definition IDs for the node's properties ...
        for (AbstractJcrProperty prop : this.jcrProperties.values()) {
            prop.releasePropertyDefinitionId();
        }
    }

    @Override
    public void addMixin( String mixinName )
        throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        CheckArg.isNotZeroLength(mixinName, "mixinName");

        checkSession();
        checkForLock();
        checkForCheckedOut();
        Path path = path();
        session.checkPermission(path, ModeShapePermissions.SET_PROPERTY);

        if (!canAddMixin(mixinName)) {
            throw new ConstraintViolationException(JcrI18n.cannotAddMixin.text(mixinName));
        }
        if (isNodeType(mixinName)) return;
        boolean wasReferenceable = isReferenceable();
        Name mixinTypeName = nameFrom(mixinName);

        // Change the mixin types property (atomically, even if some other operation snuck in and added the mixin) ...
        SessionCache cache = sessionCache();
        MutableCachedNode mutable = mutable();
        mutable.addMixin(cache, mixinTypeName);

        NodeTypes nodeTypes = session.nodeTypes();
        JcrNodeType mixinType = nodeTypes.getNodeType(mixinTypeName);
        if (!wasReferenceable && mixinType.isNodeType(JcrMixLexicon.REFERENCEABLE)) {
            // Need to add the 'jcr:uuid' reference ...
            Property uuidProp = session.propertyFactory().create(JcrLexicon.UUID, getIdentifier());
            mutable.setProperty(cache, uuidProp);
        }

        // And auto-create any properties that are defined by the new primary type ...
        autoCreateItemsFor(mixinType);

        // Since we've changed the mixins, release the cached property definition IDs for the node's properties ...
        for (AbstractJcrProperty prop : this.jcrProperties.values()) {
            prop.releasePropertyDefinitionId();
        }
    }

    @Override
    public void removeMixin( String mixinName )
        throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        CheckArg.isNotZeroLength(mixinName, "mixinName");

        checkSession();
        checkForLock();
        checkForCheckedOut();
        session.checkPermission(path(), ModeShapePermissions.SET_PROPERTY);

        if (getDefinition().isProtected()) {
            throw new ConstraintViolationException(JcrI18n.cannotRemoveFromProtectedNode.text(getPath()));
        }

        // TODO: When removing the 'mix:versionable' mixin, should we automatically remove the 'mix:versionable' properties?

        NodeTypes nodeTypes = session.nodeTypes();
        Name removedMixinName = nameFrom(mixinName);
        if (!isNodeType(mixinName)) return;

        // Get the information from the node ...
        SessionCache cache = sessionCache();
        CachedNode cachedNode = node();
        Name primaryTypeName = cachedNode.getPrimaryType(cache);

        // Build up the list of new mixin types ...
        Set<Name> newMixinNames = new HashSet<Name>(cachedNode.getMixinTypes(cache));
        if (!newMixinNames.remove(removedMixinName)) {
            // Nothing to remove ...
            return;
        }

        // ------------------------------------------------------------------------------
        // Check that any remaining properties that use the mixin type to be removed
        // match the residual definition for the node.
        // ------------------------------------------------------------------------------
        for (PropertyIterator iter = getProperties(); iter.hasNext();) {
            javax.jcr.Property property = iter.nextProperty();
            if (mixinName.equals(property.getDefinition().getDeclaringNodeType().getName())) {
                JcrPropertyDefinition match;

                // Only the residual definition would work - if there were any other definition for this name,
                // the mixin type would not have been added due to the conflict
                if (property.getDefinition().isMultiple()) {
                    match = nodeTypes.findPropertyDefinition(session,
                                                             primaryTypeName,
                                                             newMixinNames,
                                                             JcrNodeType.RESIDUAL_NAME,
                                                             property.getValues(),
                                                             true);
                } else {
                    match = nodeTypes.findPropertyDefinition(session,
                                                             primaryTypeName,
                                                             newMixinNames,
                                                             JcrNodeType.RESIDUAL_NAME,
                                                             property.getValue(),
                                                             true,
                                                             true);
                }

                if (match == null) {
                    throw new ConstraintViolationException(JcrI18n.noPropertyDefinition.text(property.getName(),
                                                                                             location(),
                                                                                             readable(primaryTypeName),
                                                                                             readable(newMixinNames)));
                }
            }
        }

        // ------------------------------------------------------------------------------
        // Check that any remaining child nodes that use the mixin type to be removed
        // match the residual definition for the node.
        // ------------------------------------------------------------------------------
        // TODO: Incorrect logic????
        for (NodeIterator iter = getNodes(); iter.hasNext();) {
            AbstractJcrNode child = (AbstractJcrNode)iter.nextNode();
            int snsCount = (int)childCount(child.name());
            if (mixinName.equals(child.getDefinition().getDeclaringNodeType().getName())) {
                // Only the residual definition would work - if there were any other definition for this name,
                // the mixin type would not have been added due to the conflict
                JcrNodeDefinition match = nodeTypes.findChildNodeDefinition(primaryTypeName,
                                                                            newMixinNames,
                                                                            JcrNodeType.RESIDUAL_NAME,
                                                                            child.getPrimaryNodeType().getInternalName(),
                                                                            snsCount,
                                                                            true);

                if (match == null) {
                    throw new ConstraintViolationException(JcrI18n.noChildNodeDefinition.text(child.getName(),
                                                                                              location(),
                                                                                              readable(primaryTypeName),
                                                                                              readable(newMixinNames)));
                }
            }
        }

        boolean wasReferenceable = isReferenceable();

        // Change the mixin types property (atomically, even if some other operation snuck in and added the mixin) ...
        MutableCachedNode mutable = mutable();
        mutable.removeMixin(cache, removedMixinName);

        if (wasReferenceable && !isReferenceable()) {
            // Need to remove the 'jcr:uuid' reference ...
            mutable.removeProperty(cache, JcrLexicon.UUID);
        }

        // Since we've changed the mixins, release the cached property definition IDs for the node's properties ...
        for (AbstractJcrProperty prop : this.jcrProperties.values()) {
            prop.releasePropertyDefinitionId();
        }
        for (NodeIterator iter = getNodes(); iter.hasNext();) {
            AbstractJcrNode child = (AbstractJcrNode)iter.nextNode();
            child.releaseNodeDefinitionId();
        }
    }

    @Override
    public boolean canAddMixin( String mixinName ) throws NoSuchNodeTypeException, RepositoryException {
        CheckArg.isNotEmpty(mixinName, "mixinName");

        JcrNodeType mixinType = session().nodeTypeManager().getNodeType(mixinName);
        if (!mixinType.isMixin()) return false;
        if (isLocked()) return false;
        if (!isCheckedOut()) return false;
        if (getDefinition().isProtected()) return false;
        if (mixinType.isAbstract()) return false;
        if (!mixinType.isMixin()) return false;
        if (isNodeType(mixinType.getInternalName())) return true;

        // ------------------------------------------------------------------------------
        // Check for any existing properties based on residual definitions that conflict
        // ------------------------------------------------------------------------------
        for (JcrPropertyDefinition propDefn : mixinType.propertyDefinitions()) {
            Name propName = propDefn.getInternalName();
            AbstractJcrProperty existingProp = getProperty(propName);
            if (existingProp == null) continue;
            if (propDefn.isMultiple()) {
                if (!propDefn.canCastToTypeAndSatisfyConstraints(existingProp.getValues(), session())) {
                    return false;
                }
            } else {
                if (!propDefn.canCastToTypeAndSatisfyConstraints(existingProp.getValue(), session())) {
                    return false;
                }
            }
        }

        // ------------------------------------------------------------------------------
        // Check for any existing child nodes based on residual definitions that conflict
        // ------------------------------------------------------------------------------
        Set<Name> mixinChildNodeNames = new HashSet<Name>();
        for (JcrNodeDefinition nodeDefinition : mixinType.childNodeDefinitions()) {
            mixinChildNodeNames.add(nodeDefinition.getInternalName());
        }

        CachedNode node = node();
        NodeCache cache = cache();
        NodeTypes nodeTypes = session.nodeTypes();
        for (Name nodeName : mixinChildNodeNames) {
            // Need to figure out if the child node requires an SNS definition
            ChildReferences refs = node.getChildReferences(cache());
            int snsCount = refs.getChildCount(nodeName);
            if (snsCount == 0) continue;

            // TODO: Incorrect logic????
            Iterator<ChildReference> iter = refs.iterator(nodeName);
            while (iter.hasNext()) {
                ChildReference ref = iter.next();
                CachedNode child = cache.getNode(ref);
                Name childPrimaryType = child.getPrimaryType(cache);
                boolean skipProtected = true;
                JcrNodeDefinition childDefn = nodeTypes.findChildNodeDefinition(mixinType.getInternalName(),
                                                                                null,
                                                                                nodeName,
                                                                                childPrimaryType,
                                                                                snsCount,
                                                                                skipProtected);
                if (childDefn == null) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public NodeDefinition getDefinition() throws RepositoryException {
        checkSession();
        return nodeDefinition();
    }

    final void setNodeDefinitionId( NodeDefinitionId nodeDefnId,
                                    int nodeTypeVersion ) {
        this.cachedDefn = new CachedDefinition(nodeDefnId, nodeTypeVersion);
    }

    final void releaseNodeDefinitionId() {
        this.cachedDefn = null;
    }

    /**
     * Get the property definition ID.
     * 
     * @return the cached property definition ID; never null
     * @throws ItemNotFoundException if the node that contains this property doesn't exist anymore
     * @throws ConstraintViolationException if no valid property definition could be found
     * @throws RepositoryException if there is a problem with this repository
     */
    NodeDefinitionId nodeDefinitionId() throws ItemNotFoundException, ConstraintViolationException, RepositoryException {
        CachedDefinition defn = cachedDefn;
        NodeTypes nodeTypes = session().nodeTypes();
        if (defn == null || nodeTypes.getVersion() > defn.nodeTypesVersion) {
            assert !this.isRoot();
            // Determine the node type based upon this node's type information ...
            CachedNode parent = getParent().node();
            SessionCache cache = sessionCache();
            Name nodeName = name();
            Name primaryType = node().getPrimaryType(cache);
            Name parentPrimaryType = parent.getPrimaryType(cache);
            Set<Name> parentMixins = parent.getMixinTypes(cache);
            int numExistingSnsInParent = parent.getChildReferences(cache).getChildCount(nodeName);
            boolean skipProtected = true;
            JcrNodeDefinition childDefn = nodeTypes.findChildNodeDefinition(parentPrimaryType,
                                                                            parentMixins,
                                                                            nodeName,
                                                                            primaryType,
                                                                            numExistingSnsInParent,
                                                                            skipProtected);
            if (childDefn == null) {
                throw new ConstraintViolationException(JcrI18n.noChildNodeDefinition.text(nodeName,
                                                                                          getParent().location(),
                                                                                          readable(parentPrimaryType),
                                                                                          readable(parentMixins)));
            }
            NodeDefinitionId id = childDefn.getId();
            setNodeDefinitionId(id, nodeTypes.getVersion());
            return id;
        }
        return defn.nodeDefnId;
    }

    /**
     * Get the definition for this property.
     * 
     * @return the cached property definition ID; never null
     * @throws ItemNotFoundException if the node that contains this property doesn't exist anymore
     * @throws ConstraintViolationException if no valid property definition could be found
     * @throws RepositoryException if there is a problem with this repository
     */
    final NodeDefinition nodeDefinition() throws ItemNotFoundException, ConstraintViolationException, RepositoryException {
        CachedDefinition defn = cachedDefn;
        NodeTypes nodeTypes = session().nodeTypes();
        if (defn == null || nodeTypes.getVersion() > defn.nodeTypesVersion) {
            assert !this.isRoot();
            // Determine the node type based upon this node's type information ...
            CachedNode parent = getParent().node();
            SessionCache cache = sessionCache();
            Name nodeName = name();
            Name primaryType = node().getPrimaryType(cache);
            Name parentPrimaryType = parent.getPrimaryType(cache);
            Set<Name> parentMixins = parent.getMixinTypes(cache);
            int numExistingSnsInParent = parent.getChildReferences(cache).getChildCount(nodeName);
            boolean skipProtected = false;
            JcrNodeDefinition childDefn = nodeTypes.findChildNodeDefinition(parentPrimaryType,
                                                                            parentMixins,
                                                                            nodeName,
                                                                            primaryType,
                                                                            numExistingSnsInParent,
                                                                            skipProtected);
            if (childDefn == null) {
                throw new ConstraintViolationException(JcrI18n.noChildNodeDefinition.text(nodeName,
                                                                                          getParent().location(),
                                                                                          readable(parentPrimaryType),
                                                                                          readable(parentMixins)));
            }
            NodeDefinitionId id = childDefn.getId();
            setNodeDefinitionId(id, nodeTypes.getVersion());
            return childDefn;
        }
        return nodeTypes.getChildNodeDefinition(defn.nodeDefnId);
    }

    /**
     * Throw a {@link ConstraintViolationException} if this node is protected (based on the its node definition).
     * 
     * @throws ConstraintViolationException if this node's definition indicates that the node is protected
     * @throws RepositoryException if an error occurs retrieving the definition for this node
     */
    private void checkNotProtected() throws ConstraintViolationException, RepositoryException {
        if (getDefinition().isProtected()) {
            throw new ConstraintViolationException(JcrI18n.cannotRemoveItemWithProtectedDefinition.text(getPath()));
        }
    }

    @Override
    public Version checkin()
        throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException,
        RepositoryException {
        return versionManager().checkin(this);
    }

    @Override
    public void checkout()
        throws UnsupportedRepositoryOperationException, LockException, ActivityViolationException, RepositoryException {
        versionManager().checkout(this);
    }

    @Override
    public void doneMerge( Version version )
        throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        versionManager().doneMerge(this, version);
    }

    @Override
    public void cancelMerge( Version version )
        throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        versionManager().cancelMerge(this, version);
    }

    @Override
    public void update( String srcWorkspace )
        throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        CheckArg.isNotNull(srcWorkspace, "srcWorkspace");
        checkSession();

        if (session().hasPendingChanges()) {
            throw new InvalidItemStateException(JcrI18n.noPendingChangesAllowed.text());
        }

        checkNotProtected();

        Path correspondingPath = null;
        try {
            correspondingPath = correspondingNodePath(srcWorkspace);
        } catch (ItemNotFoundException infe) {
            return;
        }

        // Need to force remove in case this node is not referenceable
        // TODO: Clone
        // cache.graphSession().immediateClone(correspondingPath, srcWorkspace, path(), true, true);

        session().refresh(false);
    }

    @Override
    public NodeIterator merge( String srcWorkspace,
                               boolean bestEffort )
        throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException,
        RepositoryException {

        CheckArg.isNotNull(srcWorkspace, "srcWorkspace");
        checkNotProtected();
        return versionManager().merge(this, srcWorkspace, bestEffort, false);
    }

    @Override
    public String getCorrespondingNodePath( String workspaceName )
        throws ItemNotFoundException, NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        checkSession();
        NamespaceRegistry namespaces = this.context().getNamespaceRegistry();
        return correspondingNodePath(workspaceName).getString(namespaces);
    }

    protected final Path correspondingNodePath( String workspaceName )
        throws NoSuchWorkspaceException, ItemNotFoundException, RepositoryException {
        assert workspaceName != null;
        NamespaceRegistry namespaces = this.context().getNamespaceRegistry();

        // Find the closest ancestor (including this node) that is referenceable ...
        AbstractJcrNode referenceableRoot = this;
        while (!referenceableRoot.isNodeType(JcrMixLexicon.REFERENCEABLE.getString(namespaces))) {
            referenceableRoot = referenceableRoot.getParent();
        }

        // Find the relative path from the nearest referenceable node to this node (or null if this node is referenceable) ...
        Path relativePath = path().equals(referenceableRoot.path()) ? null : path().relativeTo(referenceableRoot.path());
        String identifier = referenceableRoot.getIdentifier();
        NodeKey nodeKey = new NodeKey(identifier).withWorkspaceKey(NodeKey.keyForWorkspaceName(workspaceName));
        return session.getPathForCorrespondingNode(workspaceName, nodeKey, relativePath);
    }

    @Override
    public NodeIterator getSharedSet() throws RepositoryException {
        if (isShareable()) {
            // Find the nodes that make up this shared set ...
            return sharedSet();
        }
        // Otherwise, the shared set is just this node ...
        return new JcrSingleNodeIterator(this);
    }

    /**
     * Find all of the {@link javax.jcr.Node}s that make up the shared set.
     * 
     * @return the query result over the nodes in the node set; never null, but possibly empty if the node given by the identifier
     *         does not exist or is not a shareable node, or possibly of size 1 if the node given by the identifier does exist and
     *         is shareable but has no other nodes in the shared set
     * @throws RepositoryException if there is a problem executing the query or finding the shared set
     */
    NodeIterator sharedSet() throws RepositoryException {
        // TODO: Query
        // TODO: Shared nodes
        return JcrEmptyNodeIterator.INSTANCE;
        // AbstractJcrNode original = this;
        // String identifierOfSharedNode = getIdentifier();
        // if (this instanceof JcrSharedNode) {
        // original = ((JcrSharedNode)this).originalNode();
        // }
        // // Execute a query that will report all proxy nodes ...
        // QueryBuilder builder = new QueryBuilder(context().getValueFactories().getTypeSystem());
        // QueryCommand query = builder.select("jcr:primaryType")
        // .from("mode:share")
        // .where()
        // .referenceValue("mode:share", "mode:sharedUuid")
        // .isEqualTo(identifierOfSharedNode)
        // .end()
        // .query();
        // Query jcrQuery = session().workspace().queryManager().createQuery(query);
        // QueryResult result = jcrQuery.execute();
        // // And combine the results ...
        // return new JcrNodeIterator(original, result.getNodes());
    }

    @Override
    public void removeSharedSet() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
    }

    @Override
    public void removeShare() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        checkForCheckedOut();
        // A node that is locked by one session can be removed by another session as long as there is no lock
        // on the parent node. See Section 17.7 of the JCR 2.0 specification:
        //
        // "Removing a node is considered an alteration of its parent. This means that a node within the scope of
        // a lock may be removed by a session that is not an owner of that lock, assuming no other restriction
        // prevents the removal."
        getParent().checkForLock();
        Path path = path();
        session.checkPermission(path, ModeShapePermissions.REMOVE);

        if (isShareable()) {
            // TODO: Shared nodes
            // // Get the nodes in the shared set ...
            // NodeIterator sharedSetNodes = sharedSet();
            // long sharedSetSize = sharedSetNodes.getSize(); // computed w/o respect for privileges
            // if (sharedSetSize <= 1) {
            // // There aren't any other nodes in the shared set, so simply remove this node ...
            // doRemove();
            // return;
            // }
            // // Find the second node in the shared set that is not this object ...
            // AbstractJcrNode originalNode = (AbstractJcrNode)sharedSetNodes.nextNode();
            // if (originalNode == this) {
            // // We need to move this node into the first proxy ...
            // JcrSharedNode firstProxy = (JcrSharedNode)sharedSetNodes.nextNode();
            // assert !this.isRoot();
            // assert !firstProxy.isRoot();
            // boolean sameParent = firstProxy.getParent().equals(this.getParent());
            // NodeEditor parentEditor = firstProxy.editorForParent();
            // if (sameParent) {
            // // Move this node to just before the other shareable node ...
            // parentEditor.orderChildBefore(this.segment(), firstProxy.segment());
            // // And finally remove the first proxy ...
            // firstProxy.doRemove();
            // } else {
            // // Find the node immediately following the proxy ...
            // Node<JcrNodePayload, JcrPropertyPayload> proxyNode = firstProxy.proxyInfo();
            // Node<JcrNodePayload, JcrPropertyPayload> nextChild = parentEditor.node().getChildAfter(proxyNode);
            // Name newName = proxyNode.getName();
            // // Remove the first proxy ...
            // firstProxy.doRemove();
            // // Move this node to the new parent ...
            // Node<JcrNodePayload, JcrPropertyPayload> newNode = parentEditor.moveToBeChild(this, newName);
            // if (nextChild != null) {
            // // And place this node where the first proxy was (just before the 'nextChild') ...
            // parentEditor.orderChildBefore(newNode.getSegment(), nextChild.getSegment());
            // }
            // }
            // } else {
            // // We can just remove this proxy ...
            // doRemove();
            // }
            // return;
        }
        // If we get to here, either there are no other nodes in the shared set or this node is a non-shareable node,
        // so simply remove this node (per section 14.2 of the JCR 2.0 specification) ...
        doRemove(path);
    }

    /**
     * Perform a real remove of this node.
     * 
     * @param path the path of this node; never null
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    protected abstract void doRemove( Path path )
        throws VersionException, LockException, ConstraintViolationException, AccessDeniedException, RepositoryException;

    @Override
    public boolean isCheckedOut() throws RepositoryException {
        AbstractJcrNode node = this;
        SessionCache cache = sessionCache();
        ValueFactory<Boolean> booleanFactory = session.context().getValueFactories().getBooleanFactory();
        while (node != null) {
            if (node.isNodeType(JcrMixLexicon.VERSIONABLE)) {
                Property prop = node.node().getProperty(JcrLexicon.IS_CHECKED_OUT, cache);
                // This prop can only be null if the node has not been saved since it was made versionable.
                return prop == null || booleanFactory.create(prop.getFirstValue());
            }
            if (node.isRoot()) break;
            node = node.getParent();
        }
        return true;
    }

    @Override
    public void restore( String versionName,
                         boolean removeExisting )
        throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException,
        InvalidItemStateException, RepositoryException {
        restore(getVersionHistory().getVersion(versionName), removeExisting);
    }

    @Override
    public void restore( Version version,
                         boolean removeExisting )
        throws VersionException, ItemExistsException, InvalidItemStateException, UnsupportedRepositoryOperationException,
        LockException, RepositoryException {
        try {
            checkNotProtected();
        } catch (ConstraintViolationException cve) {
            throw new UnsupportedRepositoryOperationException(cve);
        }
        versionManager().restore(getPath(), version, removeExisting);
    }

    @Override
    public void restore( Version version,
                         String relPath,
                         boolean removeExisting )
        throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException,
        UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        checkNotProtected();

        PathFactory pathFactory = session.pathFactory();
        Path relPathAsPath = pathFactory.create(relPath);
        if (relPathAsPath.isAbsolute()) throw new RepositoryException(JcrI18n.invalidRelativePath.text(relPath));
        Path actualPath = pathFactory.create(path(), relPathAsPath).getCanonicalPath();

        versionManager().restore(session.stringFactory().create(actualPath), version, removeExisting);
    }

    @Override
    public void restoreByLabel( String versionLabel,
                                boolean removeExisting )
        throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException,
        InvalidItemStateException, RepositoryException {
        restore(getVersionHistory().getVersionByLabel(versionLabel), removeExisting);
    }

    @Override
    public JcrVersionHistoryNode getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException {
        return versionManager().getVersionHistory(this);
    }

    @Override
    public JcrVersionNode getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
        checkSession();
        if (!hasProperty(JcrLexicon.BASE_VERSION)) {
            throw new UnsupportedRepositoryOperationException(JcrI18n.requiresVersionable.text());
        }
        return (JcrVersionNode)session().getNodeByIdentifier(getProperty(JcrLexicon.BASE_VERSION).getString());
    }

    @Override
    public Lock lock( boolean isDeep,
                      boolean isSessionScoped )
        throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException,
        RepositoryException {
        // Session's liveness will be checked in 'lockManager()' ...
        return session.lockManager().lock(this, isDeep, isSessionScoped, Long.MAX_VALUE, null);
    }

    @Override
    public final Lock getLock()
        throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        // Session's liveness will be checked in 'lockManager()' ...
        return session.lockManager().getLock(this);
    }

    protected final Lock getLockIfExists()
        throws UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        return session.lockManager().getLockIfExists(this);
    }

    @Override
    public void unlock()
        throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException,
        RepositoryException {
        // Session's liveness will be checked in 'lockManager()' ...
        session.lockManager().unlock(this);
    }

    @Override
    public boolean holdsLock() {
        // Session's liveness will be checked in 'lockManager()' ...
        return session.lockManager().holdsLock(this);
    }

    @Override
    public boolean isLocked() throws RepositoryException {
        // Session's liveness will be checked in 'lockManager()' ...
        return session.lockManager().isLocked(this);
    }

    @Override
    public void followLifecycleTransition( String transition ) throws UnsupportedRepositoryOperationException /*, InvalidLifecycleTransitionException, RepositoryException*/{
        throw new UnsupportedRepositoryOperationException();
    }

    @Override
    public String[] getAllowedLifecycleTransistions() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @Override
    public boolean isNode() {
        return true;
    }

    @Override
    public boolean isNew() {
        try {
            CachedNode node = node();
            return node instanceof MutableCachedNode && ((MutableCachedNode)node).isNew();
        } catch (RepositoryException e) {
            // continue by returning false, since the node probably doesn't exist anymore
            return false;
        }
    }

    @Override
    public boolean isModified() {
        try {
            CachedNode node = node();
            if (node instanceof MutableCachedNode) {
                MutableCachedNode mutable = (MutableCachedNode)node;
                return !mutable.isNew() && mutable.hasChanges();
            }
        } catch (RepositoryException e) {
            // continue by returning false, since the node probably doesn't exist anymore
        }
        return false;
    }

    @Override
    public boolean isSame( Item otherItem ) throws RepositoryException {
        if (otherItem == this) return true;
        if (otherItem instanceof AbstractJcrNode) {
            NodeKey thatKey = ((AbstractJcrNode)otherItem).key();
            if (!this.key.equals(thatKey)) return false;
            // Make sure they are the same repository ...
            return super.isSameRepository(otherItem);
        }
        return false;
    }

    @Override
    public void accept( ItemVisitor visitor ) throws RepositoryException {
        CheckArg.isNotNull(visitor, "visitor");
        checkSession();
        visitor.visit(this);
    }

    @Deprecated
    @Override
    public void save()
        throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException,
        ReferentialIntegrityException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
        session.save(this);
    }

    @Override
    public void refresh( boolean keepChanges ) throws RepositoryException {
        if (!keepChanges) {
            session.cache().clear(node());
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * According to Section 14.3 of the JCR 2.0 specification, an implementation may choose whether the {@link Item#remove()}
     * method (and the {@link Session#removeItem(String)} method) behaves as a {@link javax.jcr.Node#removeShare()} or
     * {@link javax.jcr.Node#removeSharedSet()}. {@link javax.jcr.Node#removeShare()} just removes this node from the shared set,
     * whereas {@link javax.jcr.Node#removeSharedSet()} removes all nodes in the shared set, including the original shared node.
     * </p>
     * <p>
     * ModeShape implements {@link Item#remove()} of a shared node as simply removing this node from the shared set. In other
     * words, this method is equivalent to calling {@link #removeShare()}.
     * </p>
     * 
     * @see javax.jcr.Item#remove()
     */
    @Override
    public void remove()
        throws VersionException, LockException, ConstraintViolationException, AccessDeniedException, RepositoryException {
        // Since this node might be shareable, we want to implement 'remove()' by calling 'removeShare()',
        // which will behave correctly even if it is not shareable ...
        removeShare();
    }

    /**
     * Performs a "best effort" check on whether a node can be added at the given relative path from this node with the given
     * primary node type (if one is specified).
     * <p>
     * Note that a result of {@code true} from this method does not guarantee that a call to {@code #addNode(String, String)} with
     * the same arguments will succeed, but a result of {@code false} guarantees that it would fail (assuming that the current
     * repository state does not change).
     * </p>
     * 
     * @param relPath the relative path at which the node would be added; may not be null
     * @param primaryNodeTypeName the primary type that would be used for the node; null indicates that a default primary type
     *        should be used if possible
     * @return false if the node could not be added for any reason; true if the node <i>might</i> be able to be added
     * @throws RepositoryException if an error occurs accessing the repository
     */
    final boolean canAddNode( String relPath,
                              String primaryNodeTypeName ) throws RepositoryException {
        CheckArg.isNotEmpty(relPath, relPath);
        checkSession();

        // Determine the path ...
        Path path = null;
        try {
            path = session().pathFactory().create(relPath);
        } catch (org.modeshape.jcr.value.ValueFormatException e) {
            return false;
        }
        if (path.size() == 0) {
            return false;
        }
        if (path.isIdentifier()) {
            return false;
        }
        if (path.getLastSegment().getIndex() > 1 || relPath.endsWith("]")) {
            return false;
        }
        if (path.size() > 1) {
            // The only segment in the path is the child name ...
            Path parentPath = path.getParent();
            try {
                // Find the parent node ...
                AbstractJcrNode other = session.node(node(), parentPath);
                return other.canAddNode(primaryNodeTypeName);
            } catch (RepositoryException e) {
                return false;
            }
        }
        return this.canAddNode(primaryNodeTypeName);
    }

    private final boolean canAddNode( String primaryNodeTypeName ) throws RepositoryException {
        if (isLocked() && !getLock().isLockOwningSession()) {
            return false;
        }

        // Determine the name for the primary node type
        if (primaryNodeTypeName != null) {
            if (!session().nodeTypeManager().hasNodeType(primaryNodeTypeName)) return false;

            JcrNodeType nodeType = session().nodeTypeManager().getNodeType(primaryNodeTypeName);
            if (nodeType.isAbstract()) return false;
            if (nodeType.isMixin()) return false;
            if (INTERNAL_NODE_TYPE_NAMES.contains(nodeType.getInternalName())) return false;
        }

        return true;
    }

    @Override
    public String toString() {
        try {
            PropertyIterator iter = this.getProperties();
            StringBuffer propertyBuff = new StringBuffer();
            while (iter.hasNext()) {
                AbstractJcrProperty prop = (AbstractJcrProperty)iter.nextProperty();
                propertyBuff.append(prop).append(", ");
            }
            return this.getPath() + " {" + propertyBuff.toString() + "}";
        } catch (RepositoryException re) {
            return re.getMessage();
        }
    }

    protected static final class ChildNodeResolver implements JcrChildNodeIterator.NodeResolver {
        private final JcrSession session;

        protected ChildNodeResolver( JcrSession session ) {
            this.session = session;
        }

        @Override
        public Node nodeFrom( ChildReference ref ) {
            try {
                return session.node(ref.getKey(), null);
            } catch (RepositoryException e) {
                return null;
            }
        }
    }
}
