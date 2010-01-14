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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.XmlNameEncoder;
import org.modeshape.common.util.Base64;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
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

    protected static final TextDecoder DOCUMENT_VIEW_NAME_DECODER = new JcrDocumentViewExporter.JcrDocumentViewPropertyEncoder();

    private final NameFactory nameFactory;
    private final NamespaceRegistry namespaces;
    private final ValueFactory jcrValueFactory;
    private final JcrNodeTypeManager nodeTypes;
    private final javax.jcr.NamespaceRegistry jcrNamespaceRegistry;
    private final SaveMode saveMode;
    protected final int uuidBehavior;

    protected final String primaryTypeName;
    protected final String mixinTypesName;
    protected final String uuidName;

    private AbstractJcrNode currentNode;
    private ContentHandler delegate;

    private SessionCache cache;

    enum SaveMode {
        WORKSPACE,
        SESSION
    }

    JcrContentHandler( JcrSession session,
                       Path parentPath,
                       int uuidBehavior,
                       SaveMode saveMode ) throws PathNotFoundException, RepositoryException {
        assert session != null;
        assert parentPath != null;
        assert uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW
               || uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING
               || uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING
               || uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW;

        ExecutionContext context = session.getExecutionContext();
        this.namespaces = context.getNamespaceRegistry();
        this.nameFactory = context.getValueFactories().getNameFactory();
        this.uuidBehavior = uuidBehavior;

        this.saveMode = saveMode;
        switch (this.saveMode) {
            case SESSION:
                cache = session.cache();
                break;
            case WORKSPACE:
                cache = new SessionCache(session);
                break;
        }
        assert cache != null;

        try {
            this.currentNode = cache.findJcrNode(null, parentPath);
        } catch (ItemNotFoundException e) {
            throw new PathNotFoundException(e.getLocalizedMessage(), e);
        }
        this.jcrValueFactory = session.getValueFactory();
        this.nodeTypes = session.nodeTypeManager();
        this.jcrNamespaceRegistry = session.workspace().getNamespaceRegistry();

        this.primaryTypeName = JcrLexicon.PRIMARY_TYPE.getString(this.namespaces);
        this.mixinTypesName = JcrLexicon.MIXIN_TYPES.getString(this.namespaces);
        this.uuidName = JcrLexicon.UUID.getString(this.namespaces);
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

    protected final Name nameFor( String name ) {
        return nameFactory.create(name);
    }

    protected final Value valueFor( String value,
                                    int type ) throws ValueFormatException {
        return jcrValueFactory.createValue(value, type);
    }

    protected final Value valueFor( InputStream stream ) {
        return jcrValueFactory.createValue(stream);
    }

    protected final SessionCache cache() {
        return cache;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    @Override
    public void characters( char[] ch,
                            int start,
                            int length ) throws SAXException {
        assert this.delegate != null;
        delegate.characters(ch, start, length);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endDocument()
     */
    @Override
    public void endDocument() throws SAXException {
        if (saveMode == SaveMode.WORKSPACE) {
            try {
                cache.save();
            } catch (RepositoryException e) {
                throw new EnclosingSAXException(e);
            }
        }
        super.endDocument();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void endElement( String uri,
                            String localName,
                            String name ) throws SAXException {
        assert this.delegate != null;
        delegate.endElement(uri, localName, name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
     */
    @Override
    public void startPrefixMapping( String prefix,
                                    String uri ) throws SAXException {
        try {
            // Read from the workspace's ModeShape registry, as its semantics are more friendly
            String existingUri = namespaces.getNamespaceForPrefix(prefix);

            if (existingUri != null) {
                if (existingUri.equals(uri)) {
                    // prefix/uri mapping is already in registry
                    return;
                }
                throw new RepositoryException("Prefix " + prefix + " is already permanently mapped");
            }
            // Register through the JCR workspace to ensure consistency
            this.jcrNamespaceRegistry.registerNamespace(prefix, uri);
        } catch (RepositoryException re) {
            throw new EnclosingSAXException(re);
        }
    }

    class EnclosingSAXException extends SAXException {

        /**
         */
        private static final long serialVersionUID = -1044992767566435542L;

        /**
         * @param e
         */
        EnclosingSAXException( Exception e ) {
            super(e);

        }

    }

    private class SystemViewContentHandler extends DefaultHandler {
        private final Stack<AbstractJcrNode> parentStack;

        private final String svNameName;
        private final String svTypeName;

        private String currentNodeName;
        private String currentPropName;
        private int currentPropType;

        private StringBuilder valueBuffer;
        private final Map<String, List<Value>> currentProps;

        /**
         * @param currentNode
         */
        SystemViewContentHandler( AbstractJcrNode currentNode ) {
            super();
            this.parentStack = new Stack<AbstractJcrNode>();
            this.parentStack.push(currentNode);

            this.currentProps = new HashMap<String, List<Value>>();

            this.svNameName = JcrSvLexicon.NAME.getString(namespaces());
            this.svTypeName = JcrSvLexicon.TYPE.getString(namespaces());
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String,
         *      org.xml.sax.Attributes)
         */
        @Override
        public void startElement( String uri,
                                  String localName,
                                  String name,
                                  Attributes atts ) throws SAXException {
            // Always create a new string buffer for the content value, because we're starting a new element ...
            valueBuffer = new StringBuilder();
            if ("node".equals(localName)) {
                if (currentNodeName != null) {
                    addNodeIfPending();
                }

                currentNodeName = atts.getValue(SYSTEM_VIEW_NAME_DECODER.decode(svNameName));
            } else if ("property".equals(localName)) {
                currentPropName = atts.getValue(SYSTEM_VIEW_NAME_DECODER.decode(svNameName));
                currentPropType = PropertyType.valueFromName(atts.getValue(svTypeName));
                currentProps.put(currentPropName, new ArrayList<Value>());
            } else if (!"value".equals(localName)) {
                throw new IllegalStateException("Unexpected element '" + name + "' in system view");
            }
        }

        private void addNodeIfPending() throws SAXException {
            if (currentNodeName != null) {
                try {
                    AbstractJcrNode parentNode = parentStack.peek();

                    UUID uuid = null;
                    List<Value> rawUuid = currentProps.get(uuidName);

                    if (rawUuid != null) {
                        assert rawUuid.size() == 1;
                        uuid = UUID.fromString(rawUuid.get(0).getString());

                        try {
                            // Deal with any existing node ...
                            AbstractJcrNode existingNodeWithUuid = cache().findJcrNode(Location.create(uuid));
                            switch (uuidBehavior) {
                                case ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING:
                                    parentNode = existingNodeWithUuid.getParent();
                                    existingNodeWithUuid.remove();
                                    break;
                                case ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW:
                                    uuid = UUID.randomUUID();
                                    break;
                                case ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING:
                                    if (existingNodeWithUuid.path().isAtOrAbove(parentStack.firstElement().path())) {
                                        throw new ConstraintViolationException(
                                                                               JcrI18n.cannotRemoveParentNodeOfTarget.text(existingNodeWithUuid.getPath(),
                                                                                                                           uuid,
                                                                                                                           parentStack.firstElement()
                                                                                                                                      .getPath()));
                                    }
                                    existingNodeWithUuid.remove();
                                    break;
                                case ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW:
                                    throw new ItemExistsException(
                                                                  JcrI18n.itemAlreadyExistsWithUuid.text(uuid,
                                                                                                         cache().session()
                                                                                                                .workspace()
                                                                                                                .getName(),
                                                                                                         existingNodeWithUuid.getPath()));
                            }
                        } catch (ItemNotFoundException e) {
                            // there wasn't an existing item, so just continue
                        }

                    }

                    String typeName = currentProps.get(primaryTypeName).get(0).getString();
                    AbstractJcrNode newNode = parentNode.editor().createChild(nameFor(currentNodeName), uuid, nameFor(typeName));
                    SessionCache.NodeEditor newNodeEditor = newNode.editor();

                    for (Map.Entry<String, List<Value>> entry : currentProps.entrySet()) {
                        if (entry.getKey().equals(primaryTypeName)) {
                            continue;
                        }

                        if (entry.getKey().equals(mixinTypesName)) {
                            for (Value value : entry.getValue()) {
                                JcrNodeType mixinType = nodeTypeFor(value.getString());
                                newNodeEditor.addMixin(mixinType);
                            }
                            continue;
                        }

                        if (entry.getKey().equals(uuidName)) {
                            continue;
                        }

                        List<Value> values = entry.getValue();

                        if (values.size() == 1) {
                            newNodeEditor.setProperty(nameFor(entry.getKey()), (JcrValue)values.get(0));
                        } else {
                            newNodeEditor.setProperty(nameFor(entry.getKey()),
                                                      values.toArray(new Value[values.size()]),
                                                      PropertyType.UNDEFINED);
                        }
                    }

                    parentStack.push(newNode);
                    currentProps.clear();
                } catch (RepositoryException re) {
                    throw new EnclosingSAXException(re);
                }
            }
        }

        @Override
        public void endElement( String uri,
                                String localName,
                                String name ) throws SAXException {
            if ("node".equals(localName)) {
                addNodeIfPending();
                currentNodeName = null;
                parentStack.pop();
            } else if ("value".equals(localName)) {
                String s = valueBuffer.toString();
                try {
                    if (currentPropType == PropertyType.BINARY) {
                        ByteArrayInputStream is = new ByteArrayInputStream(Base64.decode(s, Base64.URL_SAFE));
                        currentProps.get(currentPropName).add(valueFor(is));
                    } else {
                        currentProps.get(currentPropName).add(valueFor(SYSTEM_VIEW_NAME_DECODER.decode(s), currentPropType));
                    }
                } catch (RepositoryException re) {
                    throw new EnclosingSAXException(re);
                }
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.xml.sax.ContentHandler#characters(char[], int, int)
         */
        @Override
        public void characters( char[] ch,
                                int start,
                                int length ) {
            valueBuffer.append(ch, start, length);
        }
    }

    private class DocumentViewContentHandler extends DefaultHandler {
        private final Stack<AbstractJcrNode> parentStack;

        /**
         * @param currentNode
         */
        DocumentViewContentHandler( AbstractJcrNode currentNode ) {
            super();
            this.parentStack = new Stack<AbstractJcrNode>();
            parentStack.push(currentNode);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String,
         *      org.xml.sax.Attributes)
         */
        @Override
        public void startElement( String uri,
                                  String localName,
                                  String name,
                                  Attributes atts ) throws SAXException {
            try {
                String primaryTypeName = atts.getValue(JcrContentHandler.this.primaryTypeName);
                String rawUuid = atts.getValue(uuidName);
                UUID uuid = (rawUuid != null ? UUID.fromString(rawUuid) : null);
                AbstractJcrNode parentNode = parentStack.peek();

                if (uuid != null) {
                    try {
                        // Deal with any existing node ...
                        AbstractJcrNode existingNodeWithUuid = cache().findJcrNode(Location.create(uuid));
                        switch (uuidBehavior) {
                            case ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING:
                                parentNode = existingNodeWithUuid.getParent();
                                existingNodeWithUuid.remove();
                                break;
                            case ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW:
                                uuid = UUID.randomUUID();
                                break;
                            case ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING:
                                if (existingNodeWithUuid.path().isAtOrAbove(parentStack.firstElement().path())) {
                                    throw new ConstraintViolationException();
                                }
                                existingNodeWithUuid.remove();
                                break;
                            case ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW:
                                throw new ItemExistsException();
                        }
                    } catch (ItemNotFoundException e) {
                        // there wasn't an existing item, so just continue
                    }
                }

                name = DOCUMENT_VIEW_NAME_DECODER.decode(name);
                AbstractJcrNode currentNode = parentNode.editor().createChild(nameFor(name), uuid, nameFor(primaryTypeName));
                SessionCache.NodeEditor currentNodeEditor = currentNode.editor();

                for (int i = 0; i < atts.getLength(); i++) {
                    if (JcrContentHandler.this.primaryTypeName.equals(atts.getQName(i))) {
                        continue;
                    }

                    if (mixinTypesName.equals(atts.getQName(i))) {
                        JcrNodeType mixinType = nodeTypeFor(atts.getValue(i));
                        currentNodeEditor.addMixin(mixinType);
                        continue;
                    }

                    if (uuidName.equals(atts.getQName(i))) {
                        continue;
                    }

                    // We may want to use the workspace context here so that we only use the permanent namespace mappings
                    // Name propName = session.executionContext.getValueFactories().getNameFactory().create(atts.getQName(i));
                    // String value = DOCUMENT_VIEW_NAME_DECODER.decode(atts.getValue(i));
                    String value = atts.getValue(i);
                    String propertyName = DOCUMENT_VIEW_NAME_DECODER.decode(atts.getQName(i));
                    currentNodeEditor.setProperty(nameFor(propertyName), (JcrValue)valueFor(value, PropertyType.STRING));
                }

                parentStack.push(currentNode);
            } catch (RepositoryException re) {
                throw new EnclosingSAXException(re);
            }
        }

        @Override
        public void endElement( String uri,
                                String localName,
                                String name ) {
            parentStack.pop();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.xml.sax.ContentHandler#characters(char[], int, int)
         */
        @Override
        public void characters( char[] ch,
                                int start,
                                int length ) throws SAXException {
            try {
                AbstractJcrNode parentNode = parentStack.peek();
                AbstractJcrNode currentNode = parentNode.editor()
                                                        .createChild(JcrLexicon.XMLTEXT, null, JcrNtLexicon.UNSTRUCTURED);
                String s = new String(ch, start, length);
                currentNode.editor().setProperty(JcrLexicon.XMLCHARACTERS, (JcrValue)valueFor(s, PropertyType.STRING));

            } catch (RepositoryException re) {
                throw new EnclosingSAXException(re);
            }
        }
    }
}
