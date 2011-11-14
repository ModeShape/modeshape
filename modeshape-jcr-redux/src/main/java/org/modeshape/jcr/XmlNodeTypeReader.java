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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import javax.jcr.version.OnParentVersionAction;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A class that reads node types from Jackrabbit XML files. This class is used automatically when the ModeShape
 * {@link NodeTypeManager}'s {@link NodeTypeManager#registerNodeTypeDefinitions registerNodeTypeDefinitions(...)} methods are
 * used:
 * 
 * <pre>
 * Session session = ...
 * org.modeshape.jcr.api.nodetype.NodeTypeManager mgr = 
 *     (org.modeshape.jcr.api.nodetype.NodeTypeManager)session.getWorkspace().getNodeTypeManager();
 * mgr.registerNodeTypes(file); // or stream or URL
 * </pre>
 * 
 * </p>
 * <p>
 * The format of the Jackrabbit XML is defined by this DTD:
 * 
 * <pre>
 * &lt;!ELEMENT nodeTypes (nodeType)*>
 *     &lt;!ELEMENT nodeType (supertypes?|propertyDefinition*|childNodeDefinition*)>
 * 
 *     &lt;!ATTLIST nodeType
 *             name CDATA #REQUIRED
 *             isMixin (true|false) #REQUIRED
 *              hasOrderableChildNodes (true|false) #REQUIRED
 *             primaryItemName CDATA #REQUIRED
 *         >
 *     &lt;!ELEMENT supertypes (supertype+)>
 *     &lt;!ELEMENT supertype (CDATA)>
 * 
 *     &lt;!ELEMENT propertyDefinition (valueConstraints?|defaultValues?)>
 *     &lt;!ATTLIST propertyDefinition
 *             name CDATA #REQUIRED
 *             requiredType (String|Date|Path|Name|Reference|Binary|Double|Long|Boolean|undefined) #REQUIRED
 *             autoCreated (true|false) #REQUIRED
 *             mandatory (true|false) #REQUIRED
 *             onParentVersion (COPY|VERSION|INITIALIZE|COMPUTE|IGNORE|ABORT) #REQUIRED
 *             protected (true|false) #REQUIRED
 *             multiple  (true|false) #REQUIRED
 *         >
 *     &lt;!ELEMENT valueConstraints (valueConstraint+)>
 *     &lt;!ELEMENT valueConstraint (CDATA)>
 *     &lt;!ELEMENT defaultValues (defaultValue+)>
 *     &lt;!ELEMENT defaultValue (CDATA)>
 * 
 *     &lt;!ELEMENT childNodeDefinition (requiredPrimaryTypes)>
 *     &lt;!ATTLIST childNodeDefinition
 *             name CDATA #REQUIRED
 *             defaultPrimaryType  CDATA #REQUIRED
 *             autoCreated (true|false) #REQUIRED
 *             mandatory (true|false) #REQUIRED
 *             onParentVersion (COPY|VERSION|INITIALIZE|COMPUTE|IGNORE|ABORT) #REQUIRED
 *             protected (true|false) #REQUIRED
 *             sameNameSiblings (true|false) #REQUIRED
 *         >
 *     &lt;!ELEMENT requiredPrimaryTypes (requiredPrimaryType+)>
 *     &lt;!ELEMENT requiredPrimaryType (CDATA)>
 * 
 * </pre>
 */
@NotThreadSafe
class XmlNodeTypeReader extends DefaultHandler {

    private static final String NODE_TYPE = "nodeType";
    private static final String PROPERTY_DEFINITION = "propertyDefinition";
    private static final String CHILD_NODE_DEFINITION = "childNodeDefinition";
    private static final String SUPERTYPES = "supertypes";
    private static final String REQUIRED_PRIMARY_TYPES = "requiredPrimaryTypes";
    private static final String DEFAULT_VALUES = "defaultValues";
    private static final String VALUE_CONSTRAINTS = "valueConstraints";
    private static final String SUPERTYPE = "supertype";
    private static final String REQUIRED_PRIMARY_TYPE = "requiredPrimaryType";
    private static final String DEFAULT_VALUE = "defaultValue";
    private static final String VALUE_CONSTRAINT = "valueConstraint";

    protected abstract class CharHandler {

        private Set<String> values = new LinkedHashSet<String>();
        private int stack = 0;

        public void characters( String chars ) {
            if (chars == null) return;
            chars = chars.trim();
            if (chars.length() == 0) return;
            values.add(chars);
        }

        public void incrementStack() {
            ++stack;
        }

        public boolean finish() throws RepositoryException {
            if (--stack < 1) {
                doFinish();
                stack = 0;
                return true;
            }
            return false;
        }

        protected abstract void doFinish() throws RepositoryException;

        protected final String[] getStringValues() {
            Set<String> values = this.values;
            this.values = new LinkedHashSet<String>();
            return values.toArray(new String[values.size()]);
        }

        protected final Value[] getJcrValues() throws RepositoryException {
            Value[] values = new Value[this.values.size()];
            ValueFactory factory = session.getValueFactory();
            int i = 0;
            for (String value : this.values) {
                values[i++] = factory.createValue(value);
            }
            this.values = new LinkedHashSet<String>();
            return values;
        }

    }

    protected class SupertypeNameHandler extends CharHandler {
        @Override
        protected void doFinish() throws RepositoryException {
            currentNodeType.setDeclaredSuperTypeNames(getStringValues());
        }
    }

    protected class RequiredPrimaryTypeHandler extends CharHandler {
        @Override
        protected void doFinish() throws RepositoryException {
            currentChildDefn.setRequiredPrimaryTypeNames(getStringValues());
        }
    }

    protected class DefaultValueHandler extends CharHandler {
        @Override
        protected void doFinish() throws RepositoryException {
            currentPropDefn.setDefaultValues(getJcrValues());
        }
    }

    protected class ConstraintHandler extends CharHandler {
        @Override
        protected void doFinish() {
            currentPropDefn.setValueConstraints(getStringValues());
        }
    }

    protected JcrSession session;
    private final javax.jcr.nodetype.NodeTypeManager nodeTypeManager;
    private List<NodeTypeTemplate> nodeTypes = new ArrayList<NodeTypeTemplate>();
    private final Map<String, CharHandler> charHandlers = new HashMap<String, CharHandler>();
    private CharHandler charHandler;
    protected NodeTypeTemplate currentNodeType;
    protected PropertyDefinitionTemplate currentPropDefn;
    protected NodeDefinitionTemplate currentChildDefn;
    private NamespaceRegistry namespaces;

    /**
     * Create a new node type factory that reads the node types from Jackrabbit XML files.
     * 
     * @param session the session that will be used to register the node types; may not be null
     * @throws RepositoryException if there is a problem
     */
    XmlNodeTypeReader( JcrSession session ) throws RepositoryException {
        this.session = session;
        this.nodeTypeManager = session.getWorkspace().getNodeTypeManager();
        this.namespaces = this.session.getWorkspace().getNamespaceRegistry();
        CharHandler supertypeHandler = new SupertypeNameHandler();
        CharHandler requiredPrimaryTypeHandler = new RequiredPrimaryTypeHandler();
        CharHandler defaultHandler = new DefaultValueHandler();
        CharHandler constraintHandler = new ConstraintHandler();
        this.charHandlers.put(SUPERTYPES, supertypeHandler);
        this.charHandlers.put(REQUIRED_PRIMARY_TYPES, requiredPrimaryTypeHandler);
        this.charHandlers.put(DEFAULT_VALUES, defaultHandler);
        this.charHandlers.put(VALUE_CONSTRAINTS, constraintHandler);
        this.charHandlers.put(SUPERTYPE, supertypeHandler);
        this.charHandlers.put(REQUIRED_PRIMARY_TYPE, requiredPrimaryTypeHandler);
        this.charHandlers.put(DEFAULT_VALUE, defaultHandler);
        this.charHandlers.put(VALUE_CONSTRAINT, constraintHandler);
    }

    @Override
    public void startPrefixMapping( String prefix,
                                    String uri ) throws SAXException {
        try {
            try {
                namespaces.getPrefix(uri);
            } catch (NamespaceException e) {
                namespaces.registerNamespace(prefix, uri);
            }
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void startElement( String uri,
                              String localName,
                              String name,
                              Attributes atts ) throws SAXException {
        String value = null;
        try {
            if (NODE_TYPE.equals(localName)) {
                currentNodeType = nodeTypeManager.createNodeTypeTemplate();
                if ((value = atts.getValue("name")) != null) currentNodeType.setName(value);
                if ((value = atts.getValue("isMixin")) != null) currentNodeType.setMixin(bool(value));
                if ((value = atts.getValue("hasOrderableChildNodes")) != null) currentNodeType.setOrderableChildNodes(bool(value));
                if ((value = atts.getValue("primaryItemName")) != null) currentNodeType.setPrimaryItemName(value);
            } else if (PROPERTY_DEFINITION.equals(localName)) {
                currentPropDefn = nodeTypeManager.createPropertyDefinitionTemplate();
                currentChildDefn = null;
                if ((value = atts.getValue("name")) != null) currentPropDefn.setName(value);
                if ((value = atts.getValue("requiredType")) != null) currentPropDefn.setRequiredType(type(value));
                if ((value = atts.getValue("autoCreated")) != null) currentPropDefn.setAutoCreated(bool(value));
                if ((value = atts.getValue("mandatory")) != null) currentPropDefn.setMandatory(bool(value));
                if ((value = atts.getValue("onParentVersion")) != null) currentPropDefn.setOnParentVersion(opv(value));
                if ((value = atts.getValue("protected")) != null) currentPropDefn.setProtected(bool(value));
                if ((value = atts.getValue("multiple")) != null) currentPropDefn.setMultiple(bool(value));
            } else if (CHILD_NODE_DEFINITION.equals(localName)) {
                currentChildDefn = nodeTypeManager.createNodeDefinitionTemplate();
                currentPropDefn = null;
                if ((value = atts.getValue("name")) != null) currentChildDefn.setName(value);
                if ((value = atts.getValue("defaultPrimaryType")) != null) currentChildDefn.setDefaultPrimaryTypeName(value);
                if ((value = atts.getValue("autoCreated")) != null) currentChildDefn.setAutoCreated(bool(value));
                if ((value = atts.getValue("mandatory")) != null) currentChildDefn.setMandatory(bool(value));
                if ((value = atts.getValue("onParentVersion")) != null) currentChildDefn.setOnParentVersion(opv(value));
                if ((value = atts.getValue("protected")) != null) currentChildDefn.setProtected(bool(value));
                if ((value = atts.getValue("sameNameSiblings")) != null) currentChildDefn.setSameNameSiblings(bool(value));
            }
            charHandler = charHandlers.get(localName);
            if (charHandler != null) charHandler.incrementStack();
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public void endElement( String uri,
                            String localName,
                            String name ) throws SAXException {
        if (charHandler != null) {
            try {
                if (charHandler.finish()) charHandler = null;
            } catch (RepositoryException e) {
                throw new SAXException(e);
            }
        }
        if (NODE_TYPE.equals(localName)) {
            nodeTypes.add(currentNodeType);
            currentNodeType = null;
        } else if (PROPERTY_DEFINITION.equals(localName)) {
            currentNodeType.getPropertyDefinitionTemplates().add(currentPropDefn);
            currentPropDefn = null;
        } else if (CHILD_NODE_DEFINITION.equals(localName)) {
            currentNodeType.getNodeDefinitionTemplates().add(currentChildDefn);
            currentChildDefn = null;
        }
    }

    @Override
    public void characters( char[] ch,
                            int start,
                            int length ) {
        if (charHandler != null) {
            String value = new String(ch, start, length);
            charHandler.characters(value);
        }
    }

    protected boolean bool( String value ) {
        return Boolean.parseBoolean(value);
    }

    protected int type( String value ) {
        return PropertyType.valueFromName(value);
    }

    protected int opv( String value ) {
        return OnParentVersionAction.valueFromName(value);
    }

    /**
     * @return nodeTypes
     */
    public List<NodeTypeDefinition> getNodeTypeDefinitions() {
        return Collections.unmodifiableList(new ArrayList<NodeTypeDefinition>(nodeTypes));
    }

}
