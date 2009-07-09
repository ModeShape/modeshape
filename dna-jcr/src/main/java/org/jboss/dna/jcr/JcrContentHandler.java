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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.text.TextDecoder;
import org.jboss.dna.common.text.XmlNameEncoder;
import org.jboss.dna.common.util.Base64;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
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

    protected final JcrSession session;
    protected final int uuidBehavior;

    protected final String primaryTypeName;
    protected final String mixinTypesName;
    protected final String uuidName;

    private AbstractJcrNode currentNode;
    private ContentHandler delegate;

    private Graph.Batch pendingOperations;

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

        this.session = session;
        this.nameFactory = session.getExecutionContext().getValueFactories().getNameFactory();
        this.currentNode = session.getNode(parentPath);
        this.uuidBehavior = uuidBehavior;

        if (saveMode == SaveMode.WORKSPACE) {
            this.pendingOperations = session.createBatch();
        }

        this.primaryTypeName = JcrLexicon.PRIMARY_TYPE.getString(this.session.namespaces());
        this.mixinTypesName = JcrLexicon.MIXIN_TYPES.getString(this.session.namespaces());
        this.uuidName = JcrLexicon.UUID.getString(this.session.namespaces());
    }

    protected final Name nameFor( String name ) {
        return nameFactory.create(name);
    }

    protected final Value valueFor( String value,
                                    int type ) throws ValueFormatException {
        return session.getValueFactory().createValue(value, type);
        // return new JcrValue(session.getExecutionContext().getValueFactories(), cache(), type, value);
    }

    protected final SessionCache cache() {
        return session.cache();
    }

    protected final Graph.Batch operations() {
        return pendingOperations;
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
        if (pendingOperations != null) {
            pendingOperations.execute();
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
            // Read from the workspace's DNA registry, as its semantics are more friendly
            NamespaceRegistry registry = session.workspace().context().getNamespaceRegistry();

            String existingUri = registry.getNamespaceForPrefix(prefix);

            if (existingUri != null) {
                if (existingUri.equals(uri)) {
                    // prefix/uri mapping is already in registry
                    return;
                }

                throw new RepositoryException("Prefix " + prefix + " is already permanently mapped");
            }

            // Register through the JCR workspace to ensure consistency
            session.getWorkspace().getNamespaceRegistry().registerNamespace(prefix, uri);
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
        private Stack<AbstractJcrNode> parentStack;

        private final String svNameName;
        private final String svTypeName;

        private String currentNodeName;
        private String currentPropName;
        private int currentPropType;

        private StringBuffer valueBuffer;
        private Map<String, List<Value>> currentProps;

        /**
         * @param currentNode
         */
        SystemViewContentHandler( AbstractJcrNode currentNode ) {
            super();
            this.parentStack = new Stack<AbstractJcrNode>();
            this.parentStack.push(currentNode);

            this.currentProps = new HashMap<String, List<Value>>();
            this.valueBuffer = new StringBuffer();

            this.svNameName = JcrSvLexicon.NAME.getString(session.namespaces());
            this.svTypeName = JcrSvLexicon.TYPE.getString(session.namespaces());
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
            // if (currentNodeName != null) {
            // try {
            // AbstractJcrNode parentNode = parentStack.peek();
            //
            // UUID uuid = null;
            // List<Value> rawUuid = currentProps.get(uuidName);
            //
            // if (rawUuid != null) {
            // assert rawUuid.size() == 1;
            // uuid = UUID.fromString(rawUuid.get(0).getString());
            // }
            //
            // String typeName = currentProps.get(primaryTypeName).get(0).getString();
            // AbstractJcrNode newNode =
            // cache().findJcrNode(parentNode.editorFor(operations()).createChild(nameFor(currentNodeName),
            // uuid,
            // nameFor(typeName)).getUuid());
            //
            // for (Map.Entry<String, List<Value>> entry : currentProps.entrySet()) {
            // if (entry.getKey().equals(primaryTypeName)) {
            // continue;
            // }
            //
            // if (entry.getKey().equals(mixinTypesName)) {
            // for (Value value : entry.getValue()) {
            // JcrNodeType mixinType = session.workspace().nodeTypeManager().getNodeType(nameFor(value.getString()));
            // newNode.editorFor(operations()).addMixin(mixinType);
            // }
            // continue;
            // }
            //
            // if (entry.getKey().equals(uuidName)) {
            // continue;
            // }
            //
            // List<Value> values = entry.getValue();
            //
            // if (values.size() == 1) {
            // newNode.editorFor(operations()).setProperty(nameFor(entry.getKey()), (JcrValue)values.get(0));
            // } else {
            // newNode.editorFor(operations()).setProperty(nameFor(entry.getKey()),
            // values.toArray(new Value[values.size()]),
            // PropertyType.UNDEFINED);
            // }
            // }
            //
            // parentStack.push(newNode);
            // currentProps.clear();
            // } catch (RepositoryException re) {
            // throw new EnclosingSAXException(re);
            // }
            // }
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
                        currentProps.get(currentPropName).add(session.getValueFactory().createValue(is));
                    } else {
                        currentProps.get(currentPropName).add(session.getValueFactory()
                                                                     .createValue(SYSTEM_VIEW_NAME_DECODER.decode(s),
                                                                                  currentPropType));
                    }
                } catch (RepositoryException re) {
                    throw new EnclosingSAXException(re);
                }
                valueBuffer = new StringBuffer();
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
        private Stack<AbstractJcrNode> parentStack;

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
            // try {
            // String primaryTypeName = atts.getValue(JcrContentHandler.this.primaryTypeName);
            // String rawUuid = atts.getValue(uuidName);
            // UUID uuid = (rawUuid != null ? UUID.fromString(rawUuid) : null);
            // AbstractJcrNode parentNode = parentStack.peek();
            //
            // if (uuid != null) {
            // AbstractJcrNode existingNodeWithUuid = (AbstractJcrNode)session.getNodeByUUID(rawUuid);
            // if (existingNodeWithUuid != null) {
            // switch (uuidBehavior) {
            // case ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING:
            // parentNode = existingNodeWithUuid.getParent();
            // parentNode.editorFor(operations()).destroyChild(uuid);
            // break;
            // case ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW:
            // uuid = UUID.randomUUID();
            // break;
            // case ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING:
            // if (existingNodeWithUuid.path().isAtOrAbove(parentStack.firstElement().path())) {
            // throw new ConstraintViolationException();
            // }
            // AbstractJcrNode temp = existingNodeWithUuid.getParent();
            // temp.editorFor(operations()).destroyChild(uuid);
            // break;
            // case ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW:
            // throw new ItemExistsException();
            // }
            // }
            // }
            //
            // name = DOCUMENT_VIEW_NAME_DECODER.decode(name);
            // AbstractJcrNode currentNode = cache().findJcrNode(parentNode.editorFor(operations()).createChild(nameFor(name),
            // uuid,
            // nameFor(primaryTypeName)).getUuid());
            //
            // for (int i = 0; i < atts.getLength(); i++) {
            // if (JcrContentHandler.this.primaryTypeName.equals(atts.getQName(i))) {
            // continue;
            // }
            //
            // if (mixinTypesName.equals(atts.getQName(i))) {
            // JcrNodeType mixinType = session.workspace().nodeTypeManager().getNodeType(nameFor(atts.getValue(i)));
            // currentNode.editorFor(operations()).addMixin(mixinType);
            // continue;
            // }
            //
            // if (uuidName.equals(atts.getQName(i))) {
            // continue;
            // }
            //
            // // We may want to use the workspace context here so that we only use the permanent namespace mappings
            // // Name propName = session.executionContext.getValueFactories().getNameFactory().create(atts.getQName(i));
            // // String value = DOCUMENT_VIEW_NAME_DECODER.decode(atts.getValue(i));
            // String value = atts.getValue(i);
            // String propertyName = DOCUMENT_VIEW_NAME_DECODER.decode(atts.getQName(i));
            // currentNode.editorFor(operations()).setProperty(nameFor(propertyName),
            // (JcrValue)valueFor(value, PropertyType.STRING));
            // }
            //
            // parentStack.push(currentNode);
            // } catch (RepositoryException re) {
            // throw new EnclosingSAXException(re);
            // }

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
            // try {
            // AbstractJcrNode parentNode = parentStack.peek();
            // AbstractJcrNode currentNode =
            // cache().findJcrNode(parentNode.editorFor(operations()).createChild(JcrLexicon.XMLTEXT,
            // null,
            // JcrNtLexicon.UNSTRUCTURED).getUuid());
            //
            // String s = new String(ch, start, length);
            // currentNode.editorFor(operations()).setProperty(JcrLexicon.XMLCHARACTERS,
            // (JcrValue)valueFor(s, PropertyType.STRING));
            //
            // } catch (RepositoryException re) {
            // throw new EnclosingSAXException(re);
            // }
        }
    }
}
