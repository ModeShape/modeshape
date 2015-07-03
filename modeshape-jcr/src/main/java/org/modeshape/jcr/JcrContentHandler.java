/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.Binary;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.collection.Collections;
import org.modeshape.common.collection.LinkedHashMultimap;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.XmlNameEncoder;
import org.modeshape.common.util.Base64;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.CachedNode.ReferenceType;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.ReferrerCounts;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.basic.NodeKeyReference;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Content handler that provides SAX-based event handling that maps incoming documents to the repository based on the
 * functionality described in section 7.3 of the JCR 1.0.1 specification.
 * <p>
 * Each content handler is only intended to be used once and discarded. This class is <b>NOT</b> thread-safe.
 * </p>
 * 
 * @see JcrSession#getImportContentHandler(String, int)
 * @see JcrWorkspace#getImportContentHandler(String, int)
 */
@NotThreadSafe
class JcrContentHandler extends DefaultHandler {

    /**
     * Encoder to properly escape XML names.
     * 
     * @see XmlNameEncoder
     */
    protected static final TextDecoder SYSTEM_VIEW_NAME_DECODER = new XmlNameEncoder();
    protected static final TextDecoder DOCUMENT_VIEW_NAME_DECODER = JcrDocumentViewExporter.NAME_DECODER;
    protected static final TextDecoder DOCUMENT_VIEW_VALUE_DECODER = JcrDocumentViewExporter.VALUE_DECODER;
    protected static final List<String> INTERNAL_MIXINS = Arrays.asList(JcrMixLexicon.VERSIONABLE.getString().toLowerCase());

    private static final String ALT_XML_SCHEMA_NAMESPACE_PREFIX = "xsd";

    protected final NamespaceRegistry namespaces;
    protected final int uuidBehavior;
    protected final boolean retentionInfoRetained;
    protected final boolean lifecycleInfoRetained;
    protected final List<AbstractJcrNode> nodesForPostProcessing = new LinkedList<>();
    protected final Map<String, NodeKey> uuidToNodeKeyMapping = new HashMap<>();
    protected final Set<NodeKey> importedNodeKeys = new HashSet<>();
    protected final Map<NodeKey, String> shareIdsToUUIDMap = new HashMap<>();
    protected final Map<NodeKey, ReferrerCounts> referrersByNodeKey = new HashMap<>();
    protected final LinkedHashMultimap<NodeKey, ReferenceProperty> allReferenceProperties = LinkedHashMultimap.create();
    protected SessionCache cache;
    protected final String primaryTypeName;
    protected final String mixinTypesName;
    protected final String uuidName;

    private final JcrSession session;
    private final ExecutionContext context;
    private final NameFactory nameFactory;
    private final PathFactory pathFactory;
    private final org.modeshape.jcr.value.ValueFactory<String> stringFactory;
    private final ValueFactory jcrValueFactory;
    private final JcrNodeTypeManager nodeTypes;
    private final org.modeshape.jcr.api.NamespaceRegistry jcrNamespaceRegistry;
    private final boolean saveWhenCompleted;
    private final String systemWorkspaceKey;

    private AbstractJcrNode currentNode;
    private ContentHandler delegate;


    JcrContentHandler( JcrSession session,
                       AbstractJcrNode parent,
                       int uuidBehavior,
                       boolean saveWhenCompleted,
                       boolean retentionInfoRetained,
                       boolean lifecycleInfoRetained ) throws PathNotFoundException, RepositoryException {
        assert session != null;
        assert uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW
               || uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING
               || uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING
               || uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW;

        this.session = session;
        this.session.initBaseVersionKeys();
        this.context = this.session.context();
        this.namespaces = context.getNamespaceRegistry();
        this.nameFactory = context.getValueFactories().getNameFactory();
        this.pathFactory = context.getValueFactories().getPathFactory();
        this.stringFactory = context.getValueFactories().getStringFactory();
        this.uuidBehavior = uuidBehavior;
        this.retentionInfoRetained = retentionInfoRetained;
        this.lifecycleInfoRetained = lifecycleInfoRetained;
        this.saveWhenCompleted = saveWhenCompleted;

        this.cache = session.cache();

        this.currentNode = parent;

        this.jcrValueFactory = session.getValueFactory();
        this.nodeTypes = session.nodeTypeManager();
        this.jcrNamespaceRegistry = (org.modeshape.jcr.api.NamespaceRegistry)session.workspace().getNamespaceRegistry();

        this.primaryTypeName = JcrLexicon.PRIMARY_TYPE.getString(this.namespaces);
        this.mixinTypesName = JcrLexicon.MIXIN_TYPES.getString(this.namespaces);
        this.uuidName = JcrLexicon.UUID.getString(this.namespaces);
        this.systemWorkspaceKey = session.repository().systemWorkspaceKey();
    }

    protected final JcrSession session() {
        return session;
    }

    protected final NamespaceRegistry namespaces() {
        return namespaces;
    }

    protected final JcrNodeTypeManager nodeTypes() {
        return nodeTypes;
    }

    protected final JcrNodeType nodeTypeFor( String name ) {
        return nodeTypes.getNodeType(nameFor(name));
    }

    protected final Map<Name, Integer> propertyTypesFor( String primaryTypeName ) {
        Map<Name, Integer> propertyTypesMap = new HashMap<>();
        JcrNodeType nodeType = nodeTypeFor(primaryTypeName);
        if (nodeType == null) {
            // nt:share falls in this category
            return propertyTypesMap;
        }
        for (JcrPropertyDefinition propertyDefinition : nodeType.getPropertyDefinitions()) {
            propertyTypesMap.put(propertyDefinition.getInternalName(), propertyDefinition.getRequiredType());
        }
        return propertyTypesMap;
    }

    protected final String stringFor( Object name ) {
        return stringFactory.create(name);
    }

    protected final Name nameFor( String name ) {
        return nameFactory.create(name);
    }

    protected final Name nameFor( String namespaceUri,
                                  String localName ) {
        return nameFactory.create(namespaceUri, localName);
    }

    protected final Path pathFor( Path parentPath,
                                  Name... names ) {
        return pathFactory.create(parentPath, names);
    }

    protected final Value valueFor( String value,
                                    int type ) throws ValueFormatException {
        return jcrValueFactory.createValue(value, type);
    }

    protected final Value valueFor( InputStream stream ) throws RepositoryException {
        Binary binary = jcrValueFactory.createBinary(stream);
        return jcrValueFactory.createValue(binary);
    }

    protected final SessionCache cache() {
        return cache;
    }

    protected final boolean isInternal(Name propertyName) {
        return
                propertyName.getNamespaceUri().equals(JcrLexicon.Namespace.URI) ||
                propertyName.getNamespaceUri().equals(JcrNtLexicon.Namespace.URI) ||
                propertyName.getNamespaceUri().equals(ModeShapeLexicon.NAMESPACE.getNamespaceUri());
    }

    protected void postProcessNodes() throws SAXException {
        try {
            // first make sure all the necessary reference properties have been set
            processReferences();
            
            for (AbstractJcrNode node : nodesForPostProcessing) {
                MutableCachedNode mutable = node.mutable();

                // ---------------
                // mix:versionable
                // ---------------
                if (node.isNodeType(JcrMixLexicon.VERSIONABLE)) {

                    // Does the versionable node already have a reference to the version history?
                    // If so, then we ignore it because we'll use our own key ...

                    // Does the versionable node already have a base version?
                    AbstractJcrProperty baseVersionProp = node.getProperty(JcrLexicon.BASE_VERSION);
                    if (baseVersionProp != null) {
                        // we rely on the fact that the base version ref is exported with full key
                        NodeKeyReference baseVersionRef = (NodeKeyReference)baseVersionProp.getValue().value();
                        String workspaceKey = baseVersionRef.getNodeKey().getWorkspaceKey();
                        //we only register the base version if it comes from the system workspace (if it doesn't come from the
                        //system workspace, it's not valid - e.g. could be coming from an older version of ModeShape)
                        if (systemWorkspaceKey.equals(workspaceKey)) {
                            session.setDesiredBaseVersionKey(node.key(), baseVersionRef.getNodeKey());
                        }
                    }
                }

                // ---------------
                // mix:lockable
                // ---------------
                if (node.isNodeType(JcrMixLexicon.LOCKABLE) && node.isLocked()) {
                    // Nodes should not be locked upon import ...
                    node.unlock();
                }

                // ---------------
                // mix:lifecycle
                // ---------------
                if (node.isNodeType(JcrMixLexicon.LIFECYCLE)) {
                    if (lifecycleInfoRetained && !isValidReference(node, JcrLexicon.LIFECYCLE_POLICY, false)) {
                        // The 'jcr:lifecyclePolicy' REFERENCE values is not valid or does not reference an existing node,
                        // so the 'jcr:lifecyclePolicy' and 'jcr:currentLifecycleState' properties should be removed...
                        mutable.removeProperty(cache, JcrLexicon.LIFECYCLE_POLICY);
                        mutable.removeProperty(cache, JcrLexicon.CURRENT_LIFECYCLE_STATE);
                    }
                }

                // --------------------
                // mix:managedRetention
                // --------------------
                if (node.isNodeType(JcrMixLexicon.MANAGED_RETENTION)) {
                    if (retentionInfoRetained && !isValidReference(node, JcrLexicon.RETENTION_POLICY, false)) {
                        // The 'jcr:retentionPolicy' REFERENCE values is not valid or does not reference an existing node,
                        // so the 'jcr:retentionPolicy', 'jcr:hold' and 'jcr:isDeep' properties should be removed ...
                        mutable.removeProperty(cache, JcrLexicon.HOLD);
                        mutable.removeProperty(cache, JcrLexicon.IS_DEEP);
                        mutable.removeProperty(cache, JcrLexicon.RETENTION_POLICY);
                    }

                }

                // --------------------
                // mix:share
                // --------------------
                if (node.isNodeType(ModeShapeLexicon.SHARE)) {
                    // get the actual key of the shareable node
                    String shareableNodeUUID = shareIdsToUUIDMap.get(node.key());
                    assert shareableNodeUUID != null;
                    NodeKey shareableNodeKey = uuidToNodeKeyMapping.get(shareableNodeUUID);
                    assert shareableNodeKey != null;

                    // unlink the current key from its parent references
                    NodeKey parentKey = mutable.getParentKey(cache);
                    MutableCachedNode parent = cache.mutable(parentKey);
                    parent.removeChild(cache, node.key());

                    // re-link it with the correct key - that of the shareable node
                    parent.linkChild(cache, shareableNodeKey, node.name());
                }
            }
        } catch (RepositoryException e) {
            throw new EnclosingSAXException(e);
        }
    }

    private void processReferences() throws RepositoryException {
        // if there were any reference properties imported, they can only be set on the corresponding nodes *after* all
        // the graph has been imported
        for (Map.Entry<NodeKey, ReferenceProperty> entry : this.allReferenceProperties.entries()) {
            AbstractJcrNode node = session.node(entry.getKey(), null);
            ReferenceProperty referenceProperty = entry.getValue();
            AbstractJcrProperty property = null;
            // set the reference property without validating it first
            if (!referenceProperty.isMultiple()) {
                property = node.setProperty(referenceProperty.name(), referenceProperty.value(), true, true, true,
                                            false);
            } else {
                property = node.setProperty(referenceProperty.name(), referenceProperty.values(),
                                            referenceProperty.type(), true, true, false, true);
            }
            // check if there are any public hard references with constraints in which case we should validate them
            // this should not look at any internal (protected) references as they may be changed later on
            if (property.getType() == PropertyType.REFERENCE &&
                property.getDefinition().getValueConstraints().length != 0 &&
                !property.getDefinition().isProtected()) {   
                if (!isValidReference(property)) {
                    JcrPropertyDefinition defn = property.getDefinition();
                    String name = stringFor(property.name());
                    String path = property.getParent().getPath();
                    throw new ConstraintViolationException(JcrI18n.constraintViolatedOnReference.text(name, path, defn));
                }
            }
        }
        
        // Restore the back references on the nodes which have been removed/replaced by the import and which have referrers
        // outside the graph of nodes that was imported
        for (Map.Entry<NodeKey, ReferrerCounts> entry : referrersByNodeKey.entrySet()) {
            PropertyFactory propFactory = context.getPropertyFactory();
            MutableCachedNode referred = cache.mutable(entry.getKey());
            ReferrerCounts counts = entry.getValue();
            if (referred != null && counts != null) {
                // Add in the strong and weak referrers (that are outside the import scope) that used to be in the node
                // before it was replaced ...
                for (NodeKey key : counts.getStrongReferrers()) {
                    int count = counts.countStrongReferencesFrom(key);
                    for (int i = 0; i != count; ++i) {
                        Property prop = propFactory.create(nameFor(key.toString() + i));
                        referred.addReferrer(cache, prop, key, ReferenceType.STRONG);
                    }
                }
                for (NodeKey key : counts.getWeakReferrers()) {
                    int count = counts.countWeakReferencesFrom(key);
                    for (int i = 0; i != count; ++i) {
                        Property prop = propFactory.create(nameFor(key.toString() + i));
                        referred.addReferrer(cache, prop, key, ReferenceType.WEAK);
                    }
                }
            }
        }
    }

    protected boolean isValidReference( AbstractJcrNode node,
                                        Name propertyName,
                                        boolean returnValueIfNoProperty ) throws RepositoryException {
        AbstractJcrProperty property = node.getProperty(propertyName);
        return property == null ? returnValueIfNoProperty : isValidReference(property);
    }

    protected boolean isValidReference( AbstractJcrProperty property ) throws RepositoryException {
        JcrPropertyDefinition defn = property.getDefinition();
        if (defn == null) return false;
        if (property.isMultiple()) {
            for (Value value : property.getValues()) {
                if (!defn.canCastToTypeAndSatisfyConstraints(value, session)) {
                    // We know it's not valid, so return ...
                    return false;
                }
            }
            // All values appeared to be valid ...
            return true;
        }
        // Just a single value ...
        return defn.canCastToTypeAndSatisfyConstraints(property.getValue(), session);
    }

    @Override
    public void characters( char[] ch,
                            int start,
                            int length ) throws SAXException {
        assert this.delegate != null;
        delegate.characters(ch, start, length);
    }

    @Override
    public void endDocument() throws SAXException {
        postProcessNodes();
        if (saveWhenCompleted) {
            try {
                session.save();
            } catch (RepositoryException e) {
                throw new SAXException(e);
            }
        }
        super.endDocument();
    }

    @Override
    public void endElement( String uri,
                            String localName,
                            String name ) throws SAXException {
        assert this.delegate != null;
        delegate.endElement(uri, localName, name);
    }

    @Override
    public void startElement( String uri,
                              String localName,
                              String name,
                              Attributes atts ) throws SAXException {
        checkDelegate(uri);
        assert this.delegate != null;

        delegate.startElement(uri, localName, name, atts);
    }

    private void checkDelegate( String namespaceUri ) {
        if (delegate != null) return;

        if (JcrSvLexicon.Namespace.URI.equals(namespaceUri)) {
            this.delegate = new SystemViewContentHandler(this.currentNode);
        } else {
            this.delegate = new DocumentViewContentHandler(this.currentNode);
        }
    }

    protected static byte[] decodeBase64( String value ) throws IOException {
        try {
            return Base64.decode(value.getBytes("UTF-8"));
        } catch (IOException e) {
            // try re-reading, in case this was an export from a prior ModeShape version that used URL_SAFE ...
            return Base64.decode(value, Base64.URL_SAFE);
        }
    }

    protected static String decodeBase64AsString( String value ) throws IOException {
        byte[] decoded = decodeBase64(value);
        return new String(decoded, "UTF-8");
    }

    @Override
    public void startPrefixMapping( String prefix,
                                    String uri ) throws SAXException {
        try {
            if (ALT_XML_SCHEMA_NAMESPACE_PREFIX.equals(prefix) && uri.equals(JcrNamespaceRegistry.XML_SCHEMA_NAMESPACE_URI)) {
                prefix = JcrNamespaceRegistry.XML_SCHEMA_NAMESPACE_PREFIX;
            }

            // Read from the workspace's ModeShape registry, as its semantics are more friendly
            String existingUri = namespaces.getNamespaceForPrefix(prefix);

            if (existingUri != null) {
                if (existingUri.equals(uri)) {
                    // prefix/uri mapping is already in registry
                    return;
                }
                // The prefix is used and it does not match the registered namespace. Therefore, register using
                // a generated namespace ...
                this.jcrNamespaceRegistry.registerNamespace(uri);
            } else {
                // It is not yet registered, so register through the JCR workspace to ensure consistency
                this.jcrNamespaceRegistry.registerNamespace(prefix, uri);
            }
        } catch (RepositoryException re) {
            throw new EnclosingSAXException(re);
        }
    }

    class EnclosingSAXException extends SAXException {

        private static final long serialVersionUID = -1044992767566435542L;

        EnclosingSAXException( Exception e ) {
            super(e);

        }

    }

    // ----------------------------------------------------------------------------------------------------------------
    // NodeHandler framework ...
    // ----------------------------------------------------------------------------------------------------------------

    @SuppressWarnings( "unused" )
    protected abstract class NodeHandler {
        public void finish() throws SAXException {
        }

        public AbstractJcrNode node() throws SAXException {
            return null;
        }

        public NodeHandler parentHandler() {
            return null;
        }

        public boolean ignoreAllChildren() {
            return false;
        }

        public void addPropertyValue( Name name,
                                      String value,
                                      boolean forceMultiValued,
                                      int propertyType,
                                      TextDecoder decoder ) throws EnclosingSAXException {
        }

        protected String name() {
            try {
                Path path = node().path();
                return path.isRoot() ? "" : stringFor(path.getLastSegment());
            } catch (Exception e) {
                throw new SystemFailureException(e);
            }
        }

        @Override
        public String toString() {
            NodeHandler parent = parentHandler();
            if (parent != null) {
                return parent.toString() + "/" + name();
            }
            try {
                return node().getPath();
            } catch (Throwable e) {
                try {
                    return node().toString();
                } catch (SAXException e2) {
                    throw new SystemFailureException(e2);
                }
            }
        }
    }

    /**
     * Some nodes need additional post-processing upon import, and this set of property names is used to come up with the nodes
     * that may need to be post-processed.
     * <p>
     * Really, the nodes that need to be post-processed are best found using the node types of each node. However, that is more
     * expensive to compute. Thus, we'll collect the candidate nodes that are candidates for post-processing, then in the
     * post-processing we can more effectively and efficiently use the node types.
     * </p>
     * <p>
     * Currently, we want to post-process nodes that contain repository-level semantics. In other words, nodes that are of the
     * following node types:
     * <ul>
     * <li><code>mix:versionable</code></li>
     * <li><code>mix:lockable</code></li>
     * <li><code>mix:lifecycle</code></li>
     * <li><code>mix:managedRetention</code></li>
     * </ul>
     * The <code>mix:simpleVersionable</code> would normally also be included here, except that the <code>jcr:isCheckedOut</code>
     * property is a boolean value that doesn't need any particular post-processing.
     * </p>
     * <p>
     * Some of these node types has a mandatory property, so the names of these mandatory properties are used to quickly determine
     * candidates for post-processing. In cases where there is no mandatory property, then the set of all properties for that node
     * type are included:
     * <ul>
     * <li><code>mix:versionable</code> --> <code>jcr:baseVersion</code> (mandatory)</li>
     * <li><code>mix:lockable</code> --> <code>jcr:lockOwner</code> and <code>jcr:lockIsDeep</code></li>
     * <li><code>mix:lifecycle</code> --> <code>jcr:lifecyclePolicy</code> and <code>jcr:currentLifecycleState</code></li>
     * <li><code>mix:managedRetention</code> --> <code>jcr:hold</code>, <code>jcr:isDeep</code>, and
     * <code>jcr:retentionPolicy</code></li>
     * </ul>
     * </p>
     */
    protected static final Set<Name> PROPERTIES_FOR_POST_PROCESSING = Collections.unmodifiableSet(
    /* 'mix:lockable' has two optional properties */
    JcrLexicon.LOCK_IS_DEEP, JcrLexicon.LOCK_OWNER,
    /* 'mix:versionable' has several mandatory properties, but we only need to check one */
    JcrLexicon.BASE_VERSION,
    /* 'mix:lifecycle' has two optional properties */
    JcrLexicon.LIFECYCLE_POLICY, JcrLexicon.CURRENT_LIFECYCLE_STATE,
    /* 'mix:managedRetention' has three optional properties */
    JcrLexicon.HOLD, JcrLexicon.IS_DEEP, JcrLexicon.RETENTION_POLICY);

    protected class BasicNodeHandler extends NodeHandler {
        private final Map<Name, List<Value>> properties;
        private final Set<Name> multiValuedPropertyNames;
        private final Name nodeName;
        private NodeHandler parentHandler;
        private AbstractJcrNode node;
        private final int uuidBehavior;
        private boolean postProcessed = false;
        private boolean ignoreAllChildren = false;

        protected BasicNodeHandler( Name name,
                                    NodeHandler parentHandler,
                                    int uuidBehavior ) {
            this.nodeName = name;
            this.parentHandler = parentHandler;
            this.properties = new HashMap<>();
            this.multiValuedPropertyNames = new HashSet<>();
            this.uuidBehavior = uuidBehavior;
        }

        @Override
        public void finish() throws SAXException {
            node();
        }

        @Override
        public boolean ignoreAllChildren() {
            return ignoreAllChildren;
        }

        @Override
        protected String name() {
            return stringFor(nodeName);
        }

        @Override
        public AbstractJcrNode node() throws SAXException {
            if (node == null) create();
            assert node != null;
            return node;
        }

        @Override
        public NodeHandler parentHandler() {
            return parentHandler;
        }

        @Override
        public void addPropertyValue( Name name,
                                      String value,
                                      boolean forceMultiValued,
                                      int propertyType,
                                      TextDecoder decoder ) throws EnclosingSAXException {
            if (forceMultiValued) {
                this.multiValuedPropertyNames.add(name);
            }

            try {
                if (node != null) {
                    if (JcrLexicon.PRIMARY_TYPE.equals(name)) return;
                    if (JcrLexicon.MIXIN_TYPES.equals(name)) return;
                    if (JcrLexicon.UUID.equals(name)) return;

                    // The node was already created, so set the property using the editor ...
                    node.setProperty(name, (JcrValue)valueFor(value, propertyType), true, true, true, false);
                } else {
                    // The node hasn't been created yet, so just enqueue the property value into the map ...
                    List<Value> values = properties.get(name);
                    if (values == null) {
                        values = new ArrayList<>();
                        properties.put(name, values);
                    }
                    if (forceMultiValued && value.indexOf(" ") > 0) {
                        String[] stringValues = value.split(" ");
                        for (String stringValue : stringValues) {
                            processPropertyValue(name, stringValue, propertyType, decoder, values);                            
                        }
                    }
                    else {
                        processPropertyValue(name, value, propertyType, decoder, values);
                    }
                }
                if (!postProcessed && PROPERTIES_FOR_POST_PROCESSING.contains(name)) {
                    postProcessed = true;
                }
            } catch (IOException | SAXException | RepositoryException ioe) {
                throw new EnclosingSAXException(ioe);
            }
        }

        private void processPropertyValue( Name name, String value, int propertyType, TextDecoder decoder,
                                           List<Value> values ) throws UnsupportedEncodingException, RepositoryException, SAXException {
            if (propertyType == PropertyType.BINARY) {
                Base64.InputStream is = new Base64.InputStream(new ByteArrayInputStream(value.getBytes("UTF-8")));
                values.add(valueFor(is));
            } else {
                if (decoder != null) value = decoder.decode(value);
                if (value != null && propertyType == PropertyType.STRING) {
                    // Strings and binaries can be empty -- other data types cannot
                    values.add(valueFor(value, propertyType));
                } else if (!StringUtil.isBlank(value) && isReference(propertyType)) {
                    
                        boolean isInternalReference = isInternal(name);
                        if (!isInternalReference) {
                            // we only prepend the parent information for non-internal references
                            value = parentHandler().node().key().withId(value).toString();
                        }
                        // we only have the identifier of the node, so try to use the parent to determine the workspace &
                        // source key
                        values.add(valueFor(value, propertyType));
                    
                } else if (!StringUtil.isBlank(value)) {
                    values.add(valueFor(value, propertyType));
                }
            }
        }

        protected void create() throws SAXException {
            try {
                AbstractJcrNode parent = parentHandler.node();
                final NodeKey parentKey = parent.key();
                assert parent != null;

                // Figure out the key for the node ...
                NodeKey key = null;
                List<Value> rawUuid = properties.get(JcrLexicon.UUID);
                String uuid = null;
                boolean shareableNodeAlreadyExists = false;
                if (rawUuid != null) {
                    assert rawUuid.size() == 1;
                    uuid = rawUuid.get(0).getString();
                    key = parentKey.withId(uuid);

                    try {
                        // Deal with any existing node ...
                        AbstractJcrNode existingNode = session().node(key, null, parentKey);
                        switch (uuidBehavior) {
                            case ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING:
                                parent = existingNode.getParent();
                                // Attention: this *does not* remove the entry from the DB (ISPN). Therefore, it's always
                                // accessible
                                // to the workspace cache and thus to the current session !!!.
                                // Therefore, *old properties, mixins etc* will be accessible on the new child created later on
                                // until a session.save() is performed.
                                preRemoveNode(existingNode);
                                existingNode.remove();
                                break;
                            case ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW:
                                key = cache().getRootKey().withRandomId();
                                break;
                            case ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING:
                                if (existingNode.path().isAtOrAbove(parent.path())) {
                                    String text = JcrI18n.cannotRemoveParentNodeOfTarget.text(existingNode.getPath(),
                                                                                              key,
                                                                                              parent.getPath());
                                    throw new ConstraintViolationException(text);
                                }
                                // Attention: this *does not* remove the entry from the DB (ISPN). Therefore, it's always
                                // accessible
                                // to the workspace cache and thus to the current session !!!.
                                // Therefore, *old properties, mixins etc* will be accessible on the new child created later on
                                // until a session.save() is performed.
                                preRemoveNode(existingNode);
                                existingNode.remove();
                                break;
                            case ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW:
                                if (existingNode.isShareable()) {
                                    shareableNodeAlreadyExists = true;
                                } else {
                                    throw new ItemExistsException(JcrI18n.itemAlreadyExistsWithUuid.text(key,
                                                                                                         session().workspace()
                                                                                                                  .getName(),
                                                                                                         existingNode.getPath()));
                                }
                        }
                    } catch (ItemNotFoundException e) {
                        // there wasn't an existing item, so just continue
                    }

                }

                // See if the node was already autocreated by the parent
                AbstractJcrNode existingNode = parent.getNodeIfExists(nodeName);
                boolean nodeAlreadyExists = existingNode != null && existingNode.getDefinition().isAutoCreated();

                // Create the new node ...
                AbstractJcrNode child;
                if (!nodeAlreadyExists) {
                    List<Value> primaryTypeValueList = properties.get(JcrLexicon.PRIMARY_TYPE);
                    String typeName = primaryTypeValueList != null ? primaryTypeValueList.get(0).getString() : null;
                    Name primaryTypeName = nameFor(typeName);

                    if (JcrNtLexicon.SHARE.equals(primaryTypeName)) {
                        assert key != null;
                        assert uuid != null;

                        // check if we already have the key of the shareable node
                        NodeKey shareableNodeKey = uuidToNodeKeyMapping.get(uuid);
                        if (shareableNodeKey != null) {
                            // we already know the key of the shareable node, so we need to just link it and return
                            parent.mutable().linkChild(cache, shareableNodeKey, nodeName);
                            node = session().node(shareableNodeKey, null, parentKey);
                        } else {
                            // we haven't processed the shareable node yet, so we need to make sure we process the share later.
                            parent.mutable().linkChild(cache, key, nodeName);
                            node = session().node(key, null, parentKey);
                            // make sure we post-process the share and set the correct id
                            nodesForPostProcessing.add(node);
                            // save the original UUID of the share to be able to track it back to the shareable node
                            shareIdsToUUIDMap.put(key, uuid);
                        }
                        ignoreAllChildren = true;
                        return;
                    }

                    // store the node key that we created for this UUID, so we can create shares
                    uuidToNodeKeyMapping.put(uuid, key);

                    if (shareableNodeAlreadyExists && key != null) {
                        parent.mutable().linkChild(cache, key, nodeName);
                        node = session().node(key, null, parentKey);
                        ignoreAllChildren = true;
                        return;
                    }

                    // Otherwise, it's just a regular node...
                    child = parent.addChildNode(nodeName, primaryTypeName, key, true, false);
                } else {
                    child = existingNode;
                }
                assert child != null;

                // Set the properties on the new node ...

                // Set the mixin types first (before we set any properties that may require the mixins to be present) ...
                List<Value> mixinTypeValueList = properties.get(JcrLexicon.MIXIN_TYPES);
                if (mixinTypeValueList != null) {
                    for (Value value : mixinTypeValueList) {
                        String mixinName = value.getString();
                        // in the case when keys are being reused, the old node at that key is visible (with all its properties)
                        // via the WS cache -> ISPN db. Therefore, there might be the case when even though the child was created
                        // via addChild(), the old node with all the old properties and mixins is still visible at the key() and
                        // so the "new child" reports the mixin as already present (even though it's not)
                        boolean addMixinInternally = (child.isNodeType(mixinName) && !nodeAlreadyExists) ||
                                                     INTERNAL_MIXINS.contains(mixinName.toLowerCase());
                        if (addMixinInternally) {
                            child.mutable().addMixin(child.sessionCache(), nameFor(mixinName));
                        } else {
                            child.addMixin(mixinName);
                        }
                    }
                }

                for (Map.Entry<Name, List<Value>> entry : properties.entrySet()) {
                    Name propertyName = entry.getKey();

                    // These are all handled earlier ...
                    if (JcrLexicon.PRIMARY_TYPE.equals(propertyName)) {
                        continue;
                    }
                    if (JcrLexicon.MIXIN_TYPES.equals(propertyName)) {
                        continue;
                    }
                    if (JcrLexicon.UUID.equals(propertyName)) {
                        continue;
                    }

                    List<Value> values = entry.getValue();

                    boolean allowEmptyValues = !isInternal(propertyName) || JcrLexicon.DATA.equals(propertyName);
                    if (values.size() == 1 && !this.multiValuedPropertyNames.contains(propertyName)) {
                        JcrValue value = (JcrValue)values.get(0);
                        if (!allowEmptyValues && StringUtil.isBlank(value.getString())) {
                            //if an empty value has creeped here it means we weren't able to perform proper type validation earlier
                            continue;
                        }
                        if (isReference(value.getType())) {
                            // if this is a reference, we won't set it on the node until we've finished loading all the nodes
                            ReferenceProperty referenceProperty = new ReferenceProperty(propertyName, value);
                            allReferenceProperties.put(child.key(), referenceProperty);
                            nodesForPostProcessing.add(child);
                        } else {
                            // Don't check references or the protected status ...
                            child.setProperty(propertyName, value, true, true, true, false);
                        }
                    } else {
                        if (!allowEmptyValues) {
                            for (Iterator<Value> iterator = values.iterator(); iterator.hasNext();) {
                                if (StringUtil.isBlank(iterator.next().getString())) {
                                    iterator.remove();
                                }
                            }
                        }
                        if (!values.isEmpty()) {
                            Value[] processedValues = values.toArray(new Value[values.size()]);
                            if (isReference(processedValues[0].getType())) {
                                // if this is a reference, we won't set it on the node until we've finished loading all the nodes
                                ReferenceProperty referenceProperty = new ReferenceProperty(propertyName, processedValues);
                                allReferenceProperties.put(child.key(), referenceProperty);
                                nodesForPostProcessing.add(child);
                            } else {
                                // Don't check references or the protected status ...
                                child.setProperty(propertyName, processedValues, PropertyType.UNDEFINED, true, true, false,
                                                  true);
                            }
                        }
                    }
                }

                node = child;
                importedNodeKeys.add(node.key());

                if (postProcessed) {
                    // This node needs to be post-processed ...
                    nodesForPostProcessing.add(node);
                }

            } catch (RepositoryException re) {
                throw new EnclosingSAXException(re);
            }
        }

        /**
         * Handle any operations before a node is removed or replaced by an imported node.
         *
         * @param removedNode the removed node
         */
        protected void preRemoveNode( AbstractJcrNode removedNode ) {
            // Figure out if the node has backreferences ...
            CachedNode node;
            try {
                node = removedNode.node();
                ReferrerCounts referrers = node.getReferrerCounts(cache);
                if (referrers != null) referrersByNodeKey.put(node.getKey(), referrers);
            } catch (ItemNotFoundException | InvalidItemStateException err) {
                // do nothing ...
            }
        }
    }

    protected boolean isReference( int propertyType ) {
        return (propertyType == PropertyType.REFERENCE ||
            propertyType == PropertyType.WEAKREFERENCE ||
            propertyType == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE);
    }

    protected class ExistingNodeHandler extends NodeHandler {
        private final AbstractJcrNode node;
        private final NodeHandler parentHandler;

        protected ExistingNodeHandler( AbstractJcrNode node,
                                       NodeHandler parentHandler ) {
            this.node = node;
            this.parentHandler = parentHandler;
        }

        @Override
        public AbstractJcrNode node() {
            return node;
        }

        @Override
        public NodeHandler parentHandler() {
            return parentHandler;
        }

        @Override
        public void addPropertyValue( Name propertyName,
                                      String value,
                                      boolean forceMultiValued,
                                      int propertyType,
                                      TextDecoder decoder ) {
            throw new UnsupportedOperationException();
        }
    }

    protected class JcrRootHandler extends ExistingNodeHandler {
        protected JcrRootHandler( AbstractJcrNode root ) {
            super(root, null);
        }

        @Override
        public void addPropertyValue( Name propertyName,
                                      String value,
                                      boolean forceMultiValued,
                                      int propertyType,
                                      TextDecoder decoder ) {
            // do nothing ...
        }
    }

    protected class IgnoreBranchHandler extends NodeHandler {
        private NodeHandler parentHandler;

        protected IgnoreBranchHandler( NodeHandler parentHandler ) {
            this.parentHandler = parentHandler;
        }

        @Override
        public NodeHandler parentHandler() {
            return parentHandler;
        }
    }

    protected class JcrSystemHandler extends IgnoreBranchHandler {

        protected JcrSystemHandler( NodeHandler parentHandler ) {
            super(parentHandler);
        }
    }

    protected interface NodeHandlerFactory {
        NodeHandler createFor( Name nodeName,
                               NodeHandler parentHandler,
                               int uuidBehavior ) throws SAXException;
    }

    protected class StandardNodeHandlerFactory implements NodeHandlerFactory {
        @Override
        public NodeHandler createFor( Name name,
                                      NodeHandler parentHandler,
                                      int uuidBehavior ) throws SAXException {
            if (parentHandler instanceof IgnoreBranchHandler || parentHandler.ignoreAllChildren()) {
                return new IgnoreBranchHandler(parentHandler);
            }
            if (JcrLexicon.ROOT.equals(name)) {
                try {
                    JcrRootNode rootNode = session().getRootNode();
                    return new JcrRootHandler(rootNode);
                } catch (RepositoryException re) {
                    throw new EnclosingSAXException(re);
                }
            }
            if (JcrLexicon.SYSTEM.equals(name)) {
                // Always do this, regardless of where the "jcr:system" branch is located ...
                return new JcrSystemHandler(parentHandler);
            }
            return new BasicNodeHandler(name, parentHandler, uuidBehavior);
        }
    }

    private class SystemViewContentHandler extends DefaultHandler {
        private final String svNameName;
        private final String svTypeName;
        private final String svMultipleName;
        private NodeHandler current;
        private final NodeHandlerFactory nodeHandlerFactory;
        private String currentPropertyName;
        private int currentPropertyType;
        private boolean currentPropertyValueIsBase64Encoded;
        private boolean currentPropertyIsMultiValued;
        private final StringBuilder currentPropertyValue;

        SystemViewContentHandler( AbstractJcrNode parent ) {
            super();
            this.svNameName = JcrSvLexicon.NAME.getString(namespaces());
            this.svTypeName = JcrSvLexicon.TYPE.getString(namespaces());
            this.svMultipleName = JcrSvLexicon.MULTIPLE.getString(namespaces());
            this.current = new ExistingNodeHandler(parent, null);
            this.nodeHandlerFactory = new StandardNodeHandlerFactory();
            this.currentPropertyValue = new StringBuilder();
        }

        @Override
        public void startElement( String uri,
                                  String localName,
                                  String name,
                                  Attributes atts ) throws SAXException {
            // Always reset the string builder at the beginning of an element
            currentPropertyValue.setLength(0);
            currentPropertyValueIsBase64Encoded = false;
            if ("node".equals(localName)) {
                // Finish the parent handler ...
                current.finish();
                // Create a new handler for this element ...
                String nodeName = atts.getValue(SYSTEM_VIEW_NAME_DECODER.decode(svNameName));
                current = nodeHandlerFactory.createFor(nameFor(nodeName), current, uuidBehavior);
            } else if ("property".equals(localName)) {
                currentPropertyName = atts.getValue(SYSTEM_VIEW_NAME_DECODER.decode(svNameName));
                currentPropertyType = org.modeshape.jcr.api.PropertyType.valueFromName(atts.getValue(svTypeName));

                String svMultiple = atts.getValue(svMultipleName);
                currentPropertyIsMultiValued = Boolean.TRUE.equals(Boolean.valueOf(svMultiple));
            } else if ("value".equals(localName)) {
                // See if there is an "xsi:type" attribute on this element, which means the property value contained
                // characters that cannot be represented in XML without escaping. See Section 11.2, Item 11.b ...
                String xsiType = atts.getValue("http://www.w3.org/2001/XMLSchema-instance", "type");
                if (StringUtil.isBlank(xsiType)) {
                    return;
                }

                String propertyPrefix = namespaces.getPrefixForNamespaceUri("http://www.w3.org/2001/XMLSchema", false);
                if (StringUtil.isBlank(propertyPrefix)) {
                    return;
                }
                String base64TypeName = propertyPrefix + ":base64Binary";
                currentPropertyValueIsBase64Encoded = base64TypeName.equals(xsiType);

            } else if (!"value".equals(localName)) {
                throw new IllegalStateException("Unexpected element '" + name + "' in system view");
            }
        }

        @Override
        public void characters( char[] ch,
                                int start,
                                int length ) {
            currentPropertyValue.append(ch, start, length);
        }

        @Override
        public void endElement( String uri,
                                String localName,
                                String name ) throws SAXException {
            switch (localName) {
                case "node":
                    current.finish(); // make sure the node is created
                    current = current.parentHandler();
                    break;
                case "value":
                    // Add the content for the current property ...
                    String currentPropertyString = currentPropertyValue.toString();
                    if (currentPropertyValueIsBase64Encoded) {
                        // The current string is a base64 encoded string, so we need to decode it first ...
                        try {
                            currentPropertyString = decodeBase64AsString(currentPropertyString);
                        } catch (IOException ioe) {
                            throw new EnclosingSAXException(ioe);
                        }
                    }
                    current.addPropertyValue(nameFor(currentPropertyName),
                                             currentPropertyString,
                                             currentPropertyIsMultiValued,
                                             currentPropertyType,
                                             SYSTEM_VIEW_NAME_DECODER);
                    break;
                case "property":
                    break;
                default:
                    throw new IllegalStateException("Unexpected element '" + name + "' in system view");
            }
        }
    }

    private class DocumentViewContentHandler extends DefaultHandler {
        private NodeHandler current;
        private final NodeHandlerFactory nodeHandlerFactory;

        DocumentViewContentHandler( AbstractJcrNode currentNode ) {
            super();
            this.current = new ExistingNodeHandler(currentNode, null);
            this.nodeHandlerFactory = new StandardNodeHandlerFactory();
        }

        @Override
        public void startElement( String uri,
                                  String localName,
                                  String name,
                                  Attributes atts ) throws SAXException {

            // Create the new handler for the new node ...
            String decodedLocalName = DOCUMENT_VIEW_NAME_DECODER.decode(localName);
            current = nodeHandlerFactory.createFor(nameFor(uri, decodedLocalName), current, uuidBehavior);

            List<String> allTypes = new ArrayList<>();
            Map<Name, String> propertiesNamesValues = new HashMap<>();
            for (int i = 0; i < atts.getLength(); i++) {
                Name propertyName = nameFor(atts.getURI(i), DOCUMENT_VIEW_NAME_DECODER.decode(atts.getLocalName(i)));
                String value = atts.getValue(i);
                if (value != null) {
                    propertiesNamesValues.put(propertyName, value);
                    if (JcrLexicon.PRIMARY_TYPE.equals(propertyName) || JcrLexicon.MIXIN_TYPES.equals(propertyName)) {
                        allTypes.add(value);
                    }
                }
            }

            Map<Name, Integer> propertyTypes = new HashMap<>();
            for (String typeName : allTypes) {
                propertyTypes.putAll(propertyTypesFor(typeName));
            }

            for (Map.Entry<Name, String> entry : propertiesNamesValues.entrySet()) {
                Name propertyName = entry.getKey();
                Integer propertyDefinitionType = propertyTypes.get(propertyName);
                int propertyType = propertyDefinitionType != null ? propertyDefinitionType : PropertyType.STRING;
                String value = entry.getValue();
                boolean isMultiValued = value.indexOf(" ") > 0; 
                current.addPropertyValue(propertyName, value, isMultiValued, propertyType, DOCUMENT_VIEW_VALUE_DECODER);
            }

            // Now create the node ...
            current.finish();
        }

        @Override
        public void endElement( String uri,
                                String localName,
                                String name ) throws SAXException {
            current.finish();
            current = current.parentHandler();
        }

        @Override
        public void characters( char[] ch,
                                int start,
                                int length ) throws SAXException {
            String value = new String(ch, start, length);
            value = value.trim();
            if (value.length() == 0) return;
            // Create a 'jcr:xmltext' child node with a single 'jcr:xmlcharacters' property ...
            current = nodeHandlerFactory.createFor(JcrLexicon.XMLTEXT, current, uuidBehavior);
            current.addPropertyValue(JcrLexicon.PRIMARY_TYPE,
                                     stringFor(JcrNtLexicon.UNSTRUCTURED),
                                     false,
                                     PropertyType.NAME,
                                     DOCUMENT_VIEW_NAME_DECODER);
            current.addPropertyValue(JcrLexicon.XMLCHARACTERS, value, false, PropertyType.STRING, null);// don't decode value
            current.finish();
            // Pop the stack ...
            current = current.parentHandler();
        }
    }
    
    private class ReferenceProperty {
        private final Name name;
        private final Value[] values;
        private final boolean singleValued;

        protected ReferenceProperty(Name name, Value value) {
            assert name != null;
            this.name = name;

            assert value != null;
            this.values = new Value[] {value};
            this.singleValued = true;
        } 
        
        protected ReferenceProperty( Name name, Value[] values ) {
            assert name != null;
            this.name = name;
            assert values != null && values.length > 0;
            this.values = values;
            this.singleValued = false;
        }
        
        protected int type() {
            return value().getType();
        }
        
        protected Name name() {
            return name;
        }
        
        protected boolean isMultiple() {
            return !singleValued;
        }
        
        protected JcrValue value() {
            return (JcrValue)values[0];
        }
        
        protected Value[] values() {
            return values;
        }
    }
}
