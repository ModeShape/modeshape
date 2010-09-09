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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeDefinition;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.IoUtil;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.SubgraphNode;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.io.Destination;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.ValueFactories;
import org.xml.sax.SAXException;

/**
 * A class that reads node types from Jackrabbit XML files.
 * <p>
 * Typically, the class will be used like this:
 * 
 * <pre>
 * Session session = ...
 * // Instantiate the reader and load one or more files ... 
 * JackrabbitXmlNodeTypeReader reader = new JackrabbitXmlNodeTypeReader(session);
 * reader.read(file); // or stream or resource file
 * 
 * if (!reader.getProblems().isEmpty()) {
 *     // Report problems
 * } else {
 *     // Use the standard JCR API to register the loaded node types ... 
 *     boolean allowUpdate = false;
 *     session.getWorkspace().getNodeTypeManager().registerNodeTypes(reader.getNodeTypeDefinitions(), allowUpdate);
 * }
 * </pre>
 * 
 * </p>
 * <p>
 * The format of the XML is defined by this DTD:
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
public class JackrabbitXmlNodeTypeReader extends GraphNodeTypeReader {

    /**
     * Create a new node type factory that reads the node types from Jackrabbit XML files.
     * 
     * @param session the session that will be used to register the node types; may not be null
     */
    public JackrabbitXmlNodeTypeReader( Session session ) {
        super(session);
    }

    /**
     * Create a new node type factory that reads the node types from Jackrabbit XML files.
     * 
     * @param context the context that will be used to load the node types; may not be null
     */
    public JackrabbitXmlNodeTypeReader( ExecutionContext context ) {
        super(context);
    }

    /**
     * Import the node types from the supplied stream and add all of the node type definitions to this factory's list. This method
     * will close the stream.
     * 
     * @param stream the stream containing the CND content
     * @param resourceName a logical name for the resource name to be used when reporting problems; may be null if there is no
     *        useful name
     * @throws IOException if there is a problem reading from the supplied stream
     */
    @Override
    public void read( InputStream stream,
                      String resourceName ) throws IOException {
        super.read(stream, resourceName);
    }

    /**
     * Import the node types from the supplied file and add all of the node type definitions to this factory's list.
     * 
     * @param file the file containing the node types
     * @throws IllegalArgumentException if the supplied file reference is null, or if the file does not exist or is not readable
     * @throws IOException if there is a problem reading from the supplied stream
     */
    @Override
    public void read( File file ) throws IOException {
        super.read(file);
    }

    /**
     * Import the node types from the file at the supplied URL and add all of the node type definitions to this factory's list.
     * 
     * @param url the URL to the file containing the node types
     * @throws IllegalArgumentException if the supplied URL is null
     * @throws IOException if there is a problem opening or reading the stream to the supplied URL
     */
    @Override
    public void read( URL url ) throws IOException {
        super.read(url);
    }

    /**
     * Import the node types from the file at the supplied path, and add all of the node type definitions to this factory's list.
     * This method first attempts to resolve the supplied path to a resource on the classpath. If such a resource could not be
     * found, this method considers the supplied argument as the path to an existing and readable file. If that does not succeed,
     * this method treats the supplied argument as a valid and resolvable URL.
     * 
     * @param resourceFile the name of the resource file on the classpath containing the node types
     * @throws IllegalArgumentException if the supplied string is null or empty
     * @throws IOException if there is a problem reading from the supplied resource, or if the resource could not be found
     */
    @Override
    public void read( String resourceFile ) throws IOException {
        super.read(resourceFile);
    }

    /**
     * Import the node types from the supplied string and add all of the node type definitions to this factory's list.
     * 
     * @param content the string containing the CND content
     * @param resourceName a logical name for the resource name to be used when reporting problems; may be null if there is no
     *        useful name
     */
    @Override
    public void read( String content,
                      String resourceName ) {
        super.read(content, resourceName);
    }

    /**
     * Import the node types from the supplied location in the specified graph.
     * 
     * @param graph the graph containing the standard ModeShape CND content
     * @param parentOfTypes the path to the parent of the node type definition nodes
     * @param resourceName a logical name for the resource name to be used when reporting problems; may be null if there is no
     *        useful name
     */
    @Override
    public void read( Graph graph,
                      Path parentOfTypes,
                      String resourceName ) {
        super.read(graph, parentOfTypes, resourceName);
    }

    /**
     * Import the node types from the supplied location in the specified graph.
     * 
     * @param subgraph the subgraph containing the standard ModeShape CND content
     * @param locationOfParent the location to the parent of the node type definition nodes
     * @param resourceName a logical name for the resource name to be used when reporting problems; may be null if there is no
     *        useful name
     */
    @Override
    public void read( Subgraph subgraph,
                      Location locationOfParent,
                      String resourceName ) {
        super.read(subgraph, locationOfParent, resourceName);
    }

    /**
     * Get the problems where warnings and error messages were recorded by this factory.
     * 
     * @return the problems; never null
     */
    @Override
    public Problems getProblems() {
        return super.getProblems();
    }

    /**
     * Returns the node type definitions created by this factory.
     * 
     * @return the {@link NodeTypeDefinition}s
     */
    @Override
    public NodeTypeDefinition[] getNodeTypeDefinitions() {
        return super.getNodeTypeDefinitions();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<NodeTypeDefinition> iterator() {
        return super.iterator();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.GraphNodeTypeReader#importFrom(org.modeshape.graph.io.Destination,
     *      org.modeshape.graph.property.Path, java.lang.String, java.lang.String)
     */
    @Override
    protected void importFrom( Destination destination,
                               Path path,
                               String content,
                               String resourceName ) throws Exception {
        XmlImporter importer = new XmlImporter(destination, pathFactory.createRootPath());
        importer.importFrom(content, problems, resourceName);
    }

    protected static class XmlImporter {
        protected final List<String> VALID_ON_PARENT_VERSION = Collections.unmodifiableList(Arrays.asList(new String[] {"COPY",
            "VERSION", "INITIALIZE", "COMPUTE", "IGNORE", "ABORT"}));

        protected final Destination destination;
        protected final Path outputPath;
        protected final PropertyFactory propertyFactory;
        protected final PathFactory pathFactory;
        protected final NameFactory nameFactory;
        protected final ValueFactories valueFactories;

        /**
         * Create a new importer that will place the content in the supplied destination under the supplied path.
         * 
         * @param destination the destination where content is to be written
         * @param parentPath the path in the destination below which the generated content is to appear
         * @param compatibleWithPreJcr2 true if this parser should accept the CND format that was used in the reference
         *        implementation prior to JCR 2.0.
         * @throws IllegalArgumentException if either parameter is null
         */
        public XmlImporter( Destination destination,
                            Path parentPath,
                            boolean compatibleWithPreJcr2 ) {
            CheckArg.isNotNull(destination, "destination");
            CheckArg.isNotNull(parentPath, "parentPath");
            this.destination = destination;
            this.outputPath = parentPath;
            ExecutionContext context = destination.getExecutionContext();
            this.valueFactories = context.getValueFactories();
            this.propertyFactory = context.getPropertyFactory();
            this.pathFactory = valueFactories.getPathFactory();
            this.nameFactory = valueFactories.getNameFactory();
        }

        /**
         * Create a new importer that will place the content in the supplied destination under the supplied path. This parser will
         * accept the Jackrabbit XML format.
         * 
         * @param destination the destination where content is to be written
         * @param parentPath the path in the destination below which the generated content is to appear
         * @throws IllegalArgumentException if either parameter is null
         */
        public XmlImporter( Destination destination,
                            Path parentPath ) {
            this(destination, parentPath, true);
        }

        /**
         * Import the CND content from the supplied stream, placing the content into the importer's destination.
         * 
         * @param stream the stream containing the CND content
         * @param problems where any problems encountered during import should be reported
         * @param resourceName a logical name for the resource name to be used when reporting problems; may be null if there is no
         *        useful name
         * @throws IOException if there is a problem reading from the supplied stream
         * @throws SAXException if there is an error in the XML
         */
        @SuppressWarnings( "cast" )
        public void importFrom( InputStream stream,
                                Problems problems,
                                String resourceName ) throws SAXException, IOException {
            try {
                parse(stream, problems, resourceName);
                destination.submit();
            } catch (RuntimeException e) {
                problems.addError(e, JcrI18n.errorImportingNodeTypeContent, (Object)resourceName, e.getMessage());
            }
        }

        /**
         * Import the CND content from the supplied stream, placing the content into the importer's destination.
         * 
         * @param file the file containing the CND content
         * @param problems where any problems encountered during import should be reported
         * @throws IOException if there is a problem reading from the supplied file
         * @throws SAXException if there is an error in the XML
         */
        public void importFrom( File file,
                                Problems problems ) throws SAXException, IOException {
            importFrom(IoUtil.read(file), problems, file.getCanonicalPath());
        }

        /**
         * Import the CND content from the supplied stream, placing the content into the importer's destination.
         * 
         * @param content the string containing the CND content
         * @param problems where any problems encountered during import should be reported
         * @param resourceName a logical name for the resource name to be used when reporting problems; may be null if there is no
         *        useful name
         * @throws IOException if there is a problem reading from the supplied string
         * @throws SAXException if there is an error in the XML
         */
        public void importFrom( String content,
                                Problems problems,
                                String resourceName ) throws SAXException, IOException {
            importFrom(new ByteArrayInputStream(content.getBytes()), problems, resourceName);
        }

        /**
         * Parse the XML content.
         * 
         * @param content the content
         * @param problems where any problems encountered during import should be reported
         * @param resourceName a logical name for the resource name to be used when reporting problems; may be null if there is no
         *        useful name
         * @throws IOException if there is a problem reading from the supplied stream
         * @throws SAXException if there is an error in the XML
         */
        @SuppressWarnings( "cast" )
        protected void parse( InputStream content,
                              Problems problems,
                              String resourceName ) throws SAXException, IOException {
            ExecutionContext context = destination.getExecutionContext();
            InMemoryRepositorySource source = new InMemoryRepositorySource();
            source.setName("XML Import Source");
            Graph graph = Graph.create(source, context);
            graph.importXmlFrom(content).into("/");

            // Now read the graph and convert it into our format ...
            try {
                Subgraph subgraph = graph.getSubgraphOfDepth(5).at("/nodeTypes");
                Path path = outputPath;
                for (Location location : subgraph.getRoot().getChildren()) {
                    SubgraphNode nodeType = subgraph.getNode(location);
                    parseNodeType(subgraph, nodeType, path);
                }
            } catch (PathNotFoundException e) {
                problems.addError(e, JcrI18n.nodeTypesNotFoundInXml, (Object)resourceName, e.getMessage());
            }
        }

        /**
         * Parse the node type element.
         * 
         * <pre>
         *     &lt;!ELEMENT nodeType (supertypes?|propertyDefinition*|childNodeDefinition*)>
         *     &lt;!ATTLIST nodeType
         *             name CDATA #REQUIRED
         *             isMixin (true|false) #REQUIRED
         *              hasOrderableChildNodes (true|false) #REQUIRED
         *             primaryItemName CDATA #REQUIRED
         *         >
         *     &lt;!ELEMENT supertypes (supertype+)>
         *     &lt;!ELEMENT supertype (CDATA)>
         * </pre>
         * 
         * @param subgraph
         * @param nodeType
         * @param parentPath
         */
        protected void parseNodeType( Subgraph subgraph,
                                      SubgraphNode nodeType,
                                      Path parentPath ) {
            // Parse the name, and create the path and a property for the name ...
            Name name = readName(nodeType, "name", null);
            Path nodeTypePath = pathFactory.create(parentPath, name);
            List<Property> properties = new ArrayList<Property>();
            properties.add(propertyFactory.create(JcrLexicon.NODE_TYPE_NAME, name));

            // Read the node type options ...
            boolean isOrderable = readBoolean(nodeType, "hasOrderableChildNodes", false);
            boolean isMixin = readBoolean(nodeType, "isMixin", false);
            boolean isAbstract = readBoolean(nodeType, "isAbstract", false);
            boolean isQueryable = readBoolean(nodeType, "isQueryable", true);
            String onParentVersion = "COPY";
            Name primaryItemName = readName(nodeType, "primaryItemName", null);
            properties.add(propertyFactory.create(JcrLexicon.HAS_ORDERABLE_CHILD_NODES, isOrderable));
            properties.add(propertyFactory.create(JcrLexicon.IS_MIXIN, isMixin));
            properties.add(propertyFactory.create(JcrLexicon.IS_ABSTRACT, isAbstract));
            properties.add(propertyFactory.create(JcrLexicon.IS_QUERYABLE, isQueryable));
            properties.add(propertyFactory.create(JcrLexicon.ON_PARENT_VERSION, onParentVersion.toUpperCase()));
            if (primaryItemName != null) {
                properties.add(propertyFactory.create(JcrLexicon.PRIMARY_ITEM_NAME, primaryItemName));
            }

            SubgraphNode supertypes = nodeType.getNode(name("supertypes"));
            if (supertypes != null) {
                // Read the supertypes, which are imported as a "supertype" multi-valued property ...
                List<Name> supertypeNames = readNames(supertypes, "supertype", JcrNtLexicon.BASE);
                properties.add(propertyFactory.create(JcrLexicon.SUPERTYPES, supertypeNames)); // even if empty
            }
            destination.create(nodeTypePath, properties);

            for (Location childLocation : nodeType.getChildren()) {
                SubgraphNode child = subgraph.getNode(childLocation);
                Name childName = nameFrom(child);
                if (childName.getLocalName().equals("propertyDefinition")) {
                    parsePropertyDefinition(subgraph, child, nodeTypePath);
                } else if (childName.getLocalName().equals("childNodeDefinition")) {
                    parseChildNodeDefinition(subgraph, child, nodeTypePath);
                }
            }
        }

        /**
         * Read the property definition from the graph and create the corresponding CND content in the destination.
         * 
         * <pre>
         *  &lt;!ELEMENT propertyDefinition (valueConstraints?|defaultValues?)>
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
         * </pre>
         * 
         * @param subgraph
         * @param propertyDefn
         * @param parentPath
         */
        protected void parsePropertyDefinition( Subgraph subgraph,
                                                SubgraphNode propertyDefn,
                                                Path parentPath ) {
            // Parse the name, and create the path and a property for the name ...
            Name name = readName(propertyDefn, "name", null);
            Path nodeTypePath = pathFactory.create(parentPath, JcrLexicon.PROPERTY_DEFINITION);
            List<Property> properties = new ArrayList<Property>();
            properties.add(propertyFactory.create(JcrLexicon.NAME, name));

            // Read the node type options ...
            boolean autoCreated = readBoolean(propertyDefn, "autoCreated", false);
            boolean mandatory = readBoolean(propertyDefn, "mandatory", false);
            boolean multiple = readBoolean(propertyDefn, "multiple", false);
            boolean isProtected = readBoolean(propertyDefn, "protected", false);
            boolean isFullTextSearchable = readBoolean(propertyDefn, "fullTextSearchable", true);
            boolean isQueryOrderable = true;
            String onParentVersion = readString(propertyDefn, "onParentVersion", "COPY");
            String requiredType = readString(propertyDefn, "requiredType", "UNDEFINED");

            properties.add(propertyFactory.create(JcrLexicon.AUTO_CREATED, autoCreated));
            properties.add(propertyFactory.create(JcrLexicon.MANDATORY, mandatory));
            properties.add(propertyFactory.create(JcrLexicon.PROTECTED, isProtected));
            properties.add(propertyFactory.create(JcrLexicon.ON_PARENT_VERSION, onParentVersion.toUpperCase()));
            properties.add(propertyFactory.create(JcrLexicon.MULTIPLE, multiple));
            properties.add(propertyFactory.create(JcrLexicon.IS_FULL_TEXT_SEARCHABLE, isFullTextSearchable));
            properties.add(propertyFactory.create(JcrLexicon.IS_QUERY_ORDERABLE, isQueryOrderable));
            properties.add(propertyFactory.create(JcrLexicon.REQUIRED_TYPE, requiredType.toUpperCase()));

            for (Location childLocation : propertyDefn.getChildren()) {
                SubgraphNode child = subgraph.getNode(childLocation);
                Name childName = nameFrom(child);
                if (childName.getLocalName().equals("valueConstraints")) {
                    // Read the supertypes, which are imported as a "valueConstraint" multi-valued property ...
                    List<String> valueConstraints = readStrings(child, "valueConstraint");
                    if (valueConstraints != null && !valueConstraints.isEmpty()) {
                        properties.add(propertyFactory.create(JcrLexicon.VALUE_CONSTRAINTS, valueConstraints));
                    }
                } else if (childName.getLocalName().equals("defaultValues")) {
                    // Read the supertypes, which are imported as a "valueConstraint" multi-valued property ...
                    List<String> defaultValues = readStrings(child, "defaultValue");
                    if (defaultValues != null && !defaultValues.isEmpty()) {
                        properties.add(propertyFactory.create(JcrLexicon.DEFAULT_VALUES, defaultValues));
                    }
                }
            }

            destination.create(nodeTypePath, properties);
        }

        /**
         * Read the property definition from the graph and create the corresponding CND content in the destination.
         * 
         * <pre>
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
         * </pre>
         * 
         * @param subgraph
         * @param childDefn
         * @param parentPath
         */
        protected void parseChildNodeDefinition( Subgraph subgraph,
                                                 SubgraphNode childDefn,
                                                 Path parentPath ) {
            // Parse the name, and create the path and a property for the name ...
            Name name = readName(childDefn, "name", null);
            Path nodeTypePath = pathFactory.create(parentPath, JcrLexicon.CHILD_NODE_DEFINITION);
            List<Property> properties = new ArrayList<Property>();
            properties.add(propertyFactory.create(JcrLexicon.NAME, name));

            // Read the node type options ...
            boolean autoCreated = readBoolean(childDefn, "autoCreated", false);
            boolean mandatory = readBoolean(childDefn, "mandatory", false);
            String onParentVersion = readString(childDefn, "onParentVersion", "COPY");
            boolean isProtected = readBoolean(childDefn, "protected", false);
            boolean sns = readBoolean(childDefn, "sameNameSiblings", false);
            Name defaultPrimaryType = readName(childDefn, "defaultPrimaryType", null);

            properties.add(propertyFactory.create(JcrLexicon.AUTO_CREATED, autoCreated));
            properties.add(propertyFactory.create(JcrLexicon.MANDATORY, mandatory));
            properties.add(propertyFactory.create(JcrLexicon.PROTECTED, isProtected));
            properties.add(propertyFactory.create(JcrLexicon.ON_PARENT_VERSION, onParentVersion.toUpperCase()));
            properties.add(propertyFactory.create(JcrLexicon.SAME_NAME_SIBLINGS, sns));
            if (defaultPrimaryType != null) {
                properties.add(propertyFactory.create(JcrLexicon.DEFAULT_PRIMARY_TYPE, defaultPrimaryType));
            }

            for (Location childLocation : childDefn.getChildren()) {
                SubgraphNode child = subgraph.getNode(childLocation);
                Name childName = nameFrom(child);
                if (childName.getLocalName().equals("requiredPrimaryTypes")) {
                    // Read the supertypes, which are imported as a "valueConstraint" multi-valued property ...
                    List<Name> requiredPrimaryTypes = readNames(child, "requiredPrimaryType", JcrNtLexicon.BASE);
                    if (requiredPrimaryTypes != null && !requiredPrimaryTypes.isEmpty()) {
                        properties.add(propertyFactory.create(JcrLexicon.REQUIRED_PRIMARY_TYPES, requiredPrimaryTypes));
                    }
                }
            }

            destination.create(nodeTypePath, properties);
        }

        protected Name nameFrom( SubgraphNode node ) {
            return node.getLocation().getPath().getLastSegment().getName();
        }

        protected Name name( String name ) {
            return nameFactory.create(name);
        }

        protected boolean readBoolean( SubgraphNode node,
                                       String propertyName,
                                       boolean defaultValue ) {
            Property property = node.getProperty(propertyName);
            return property != null ? valueFactories.getBooleanFactory().create(property.getFirstValue()) : defaultValue;
        }

        protected String readString( SubgraphNode node,
                                     String propertyName,
                                     String defaultValue ) {
            Property property = node.getProperty(propertyName);
            return property != null ? valueFactories.getStringFactory().create(property.getFirstValue()) : defaultValue;
        }

        protected List<String> readStrings( SubgraphNode node,
                                            String propertyName ) {
            List<String> results = new ArrayList<String>();
            Property property = node.getProperty(propertyName);
            if (property != null) {
                for (Object value : property) {
                    String str = valueFactories.getStringFactory().create(value);
                    if (str != null && str.length() != 0) results.add(str);
                }
            }
            return results;
        }

        protected Name readName( SubgraphNode node,
                                 String propertyName,
                                 Name defaultValue ) {
            Property property = node.getProperty(propertyName);
            if (property != null && !property.isEmpty()) {
                String firstValue = valueFactories.getStringFactory().create(property.getFirstValue());
                if (firstValue != null && firstValue.trim().length() != 0) {
                    return valueFactories.getNameFactory().create(firstValue);
                }
            }
            return defaultValue;
        }

        protected List<Name> readNames( SubgraphNode node,
                                        String propertyName,
                                        Name defaultIfNone ) {
            List<Name> results = new ArrayList<Name>();
            Property property = node.getProperty(propertyName);
            if (property != null) {
                for (Object value : property) {
                    Name name = valueFactories.getNameFactory().create(value);
                    if (name != null) results.add(name);
                }
            }
            if (results.isEmpty() && defaultIfNone != null) results.add(defaultIfNone);
            return results;
        }
    }

}
