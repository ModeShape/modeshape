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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import javax.jcr.version.OnParentVersionAction;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.IoUtil;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.SubgraphNode;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.io.Destination;
import org.modeshape.graph.io.GraphBatchDestination;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.basic.LocalNamespaceRegistry;

/**
 * A base class for loading content into a graph with the standard format used by ModeShape.
 * <p>
 * The root node of the graph should have zero or more children. Each child of the root node represents a type to be registered
 * and the name of the node should be the name of the node type to be registered. Additionally, any facets of the node type that
 * are specified should be set in a manner consistent with the JCR specification for the {@code nt:nodeType} built-in node type.
 * The {@code jcr:primaryType} property does not need to be set on these nodes, but the nodes must be semantically valid as if the
 * {@code jcr:primaryType} property was set.
 * </p>
 * <p>
 * Each node type node may have zero or more children, each with the name {@code jcr:propertyDefinition} or {@code
 * jcr:childNodeDefinition}, as per the definition of the {@code nt:nodeType} built-in type. Each property definition and child
 * node definition must obey the semantics of {@code jcr:propertyDefinition} and {@code jcr:childNodeDefinition} respectively
 * However these nodes also do not need to have the {@code jcr:primaryType} property set.
 * </p>
 * <p>
 * For example, one valid graph is:
 * 
 * <pre>
 * &lt;root&gt;
 * +---- test:testMixinType
 *        +--- jcr:nodeTypeName               =  test:testMixinType  (PROPERTY)
 *        +--- jcr:isMixin                    =  true                (PROPERTY)    
 *        +--- jcr:childNodeDefinition                               (CHILD NODE)
 *        |     +--- jcr:name                 =  test:childNodeA     (PROPERTY)
 *        |     +--- jcr:mandatory            =  true                (PROPERTY)
 *        |     +--- jcr:autoCreated          =  true                (PROPERTY)
 *        |     +--- jcr:defaultPrimaryType   =  nt:base             (PROPERTY)
 *        |     +--- jcr:requiredPrimaryTypes =  nt:base             (PROPERTY)
 *        +--- jcr:propertyDefinition                                (CHILD NODE)
 *              +--- jcr:name                 =  test:propertyA      (PROPERTY)
 *              +--- jcr:multiple             =  true                (PROPERTY)
 *              +--- jcr:requiredType         =  String              (PROPERTY)
 * </pre>
 * 
 * This graph (when registered) would create a mixin node named "test:testMixinType" with a mandatory, autocreated child node
 * named "test:childNodeA" with a default and required primary type of "nt:base" and a multi-valued string property named
 * "test:propertyA".
 * </p>
 */
@NotThreadSafe
class GraphNodeTypeReader implements Iterable<NodeTypeDefinition> {

    private static final Map<String, Integer> PROPERTY_TYPE_VALUES_FROM_NAME;

    static {
        Map<String, Integer> temp = new HashMap<String, Integer>();

        temp.put(PropertyType.TYPENAME_BINARY.toUpperCase(), PropertyType.BINARY);
        temp.put(PropertyType.TYPENAME_BOOLEAN.toUpperCase(), PropertyType.BOOLEAN);
        temp.put(PropertyType.TYPENAME_DATE.toUpperCase(), PropertyType.DATE);
        temp.put(PropertyType.TYPENAME_DECIMAL.toUpperCase(), PropertyType.DECIMAL);
        temp.put(PropertyType.TYPENAME_DOUBLE.toUpperCase(), PropertyType.DOUBLE);
        temp.put(PropertyType.TYPENAME_LONG.toUpperCase(), PropertyType.LONG);
        temp.put(PropertyType.TYPENAME_NAME.toUpperCase(), PropertyType.NAME);
        temp.put(PropertyType.TYPENAME_PATH.toUpperCase(), PropertyType.PATH);
        temp.put(PropertyType.TYPENAME_STRING.toUpperCase(), PropertyType.STRING);
        temp.put(PropertyType.TYPENAME_REFERENCE.toUpperCase(), PropertyType.REFERENCE);
        temp.put(PropertyType.TYPENAME_WEAKREFERENCE.toUpperCase(), PropertyType.WEAKREFERENCE);
        temp.put(PropertyType.TYPENAME_UNDEFINED.toUpperCase(), PropertyType.UNDEFINED);
        temp.put(PropertyType.TYPENAME_URI.toUpperCase(), PropertyType.URI);

        PROPERTY_TYPE_VALUES_FROM_NAME = Collections.unmodifiableMap(temp);
    }

    protected final Problems problems = new SimpleProblems();
    protected final List<NodeTypeDefinition> types = new ArrayList<NodeTypeDefinition>();
    protected final List<NodeTypeDefinition> immutableTypes = Collections.unmodifiableList(types);
    protected final ExecutionContext context;
    protected final ValueFactories valueFactories;
    protected final PathFactory pathFactory;
    protected final NameFactory nameFactory;
    protected final ValueFactory<Boolean> booleanFactory;
    protected final ValueFactory<String> stringFactory;
    protected final Path root;

    /**
     * Create a new node type factory that reads CND files.
     * 
     * @param session the session that will be used to register the node types; may not be null and must be a ModeShape Session.
     */
    protected GraphNodeTypeReader( Session session ) {
        this(CheckArg.getInstanceOf(session, JcrSession.class, "session").getExecutionContext());
    }

    /**
     * Create a new node type factory that reads CND files.
     * 
     * @param executionContext the session that will be used to load the node types; may not be null and must be a ModeShape
     *        Session.
     */
    protected GraphNodeTypeReader( ExecutionContext executionContext ) {
        LocalNamespaceRegistry localRegistry = new LocalNamespaceRegistry(executionContext.getNamespaceRegistry());
        context = executionContext.with(localRegistry);
        valueFactories = context.getValueFactories();
        pathFactory = valueFactories.getPathFactory();
        nameFactory = valueFactories.getNameFactory();
        booleanFactory = valueFactories.getBooleanFactory();
        stringFactory = valueFactories.getStringFactory();
        root = pathFactory.createRootPath();
    }

    /**
     * Import the node types from the supplied stream and add all of the node type definitions to this factory's list. This method
     * will close the stream.
     * 
     * @param stream the stream containing the node types
     * @param resourceName a logical name for the resource name to be used when reporting problems; may be null if there is no
     *        useful name
     * @throws IOException if there is a problem reading from the supplied stream
     */
    public void read( InputStream stream,
                      String resourceName ) throws IOException {
        read(IoUtil.read(stream), resourceName);
    }

    /**
     * Import the node types from the supplied file and add all of the node type definitions to this factory's list.
     * 
     * @param file the file containing the node types
     * @throws IllegalArgumentException if the supplied file reference is null, or if the file does not exist or is not readable
     * @throws IOException if there is a problem reading from the supplied stream
     */
    public void read( File file ) throws IOException {
        CheckArg.isNotNull(file, "file");
        if (!file.exists() || !file.canRead()) {
            throw new IOException(JcrI18n.fileMustExistAndBeReadable.text(file.getCanonicalPath()));
        }
        read(IoUtil.read(file), file.getCanonicalPath());
    }

    /**
     * Import the node types from the file at the supplied URL and add all of the node type definitions to this factory's list.
     * 
     * @param url the URL to the file containing the node types
     * @throws IllegalArgumentException if the supplied URL is null
     * @throws IOException if there is a problem opening or reading the stream to the supplied URL
     */
    public void read( URL url ) throws IOException {
        CheckArg.isNotNull(url, "url");
        InputStream stream = url.openStream();
        boolean error = false;
        try {
            read(IoUtil.read(stream), url.toString());
        } catch (IOException e) {
            error = true;
            throw e;
        } catch (RuntimeException e) {
            error = true;
            throw e;
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                if (!error) throw e;
            } catch (RuntimeException e) {
                if (!error) throw e;
            }
        }
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
    public void read( String resourceFile ) throws IOException {
        CheckArg.isNotEmpty(resourceFile, "resourceFile");
        ClassLoader classLoader = context.getClassLoader();
        InputStream stream = IoUtil.getResourceAsStream(resourceFile, classLoader, getClass());
        if (stream == null) {
            throw new IOException(JcrI18n.unableToFindResourceOnClasspathOrFileOrUrl.text(resourceFile));
        }
        boolean error = false;
        try {
            read(IoUtil.read(stream), resourceFile);
        } catch (IOException e) {
            error = true;
            throw e;
        } catch (RuntimeException e) {
            error = true;
            throw e;
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                if (!error) throw e;
            } catch (RuntimeException e) {
                if (!error) throw e;
            }
        }
    }

    /**
     * Import the node types from the supplied string and add all of the node type definitions to this factory's list.
     * 
     * @param content the string containing the node types
     * @param resourceName a logical name for the resource name to be used when reporting problems; may be null if there is no
     *        useful name
     */
    @SuppressWarnings( "cast" )
    public void read( String content,
                      String resourceName ) {
        InMemoryRepositorySource source = new InMemoryRepositorySource();
        source.setName("Node Type Import Source");
        Graph graph = Graph.create(source, context);

        // Create the batch and load the graph ...
        Graph.Batch batch = graph.batch();
        Destination destination = new GraphBatchDestination(batch);
        try {
            importFrom(destination, root, content, resourceName);
            List<NodeTypeDefinition> types = readTypesFrom(graph, root, null);
            this.types.addAll(types);
        } catch (Throwable t) {
            problems.addError(t, JcrI18n.errorImportingNodeTypeContent, (Object)resourceName, t.getMessage());
        }
    }

    /**
     * Import the node types from the supplied location in the specified graph.
     * 
     * @param graph the graph containing the standard ModeShape CND content
     * @param parentOfTypes the path to the parent of the node type definition nodes
     * @param resourceName a logical name for the resource name to be used when reporting problems; may be null if there is no
     *        useful name
     */
    public void read( Graph graph,
                      Path parentOfTypes,
                      String resourceName ) {
        this.types.addAll(readTypesFrom(graph, parentOfTypes, null));
    }

    /**
     * Import the node types from the supplied location in the specified graph.
     * 
     * @param graph the graph containing the standard ModeShape CND content
     * @param parentOfTypes the path to the parent of the node type definition nodes
     * @param nodeTypesToRead the names of the node types that should be read; null means that all node types should be read
     * @param resourceName a logical name for the resource name to be used when reporting problems; may be null if there is no
     *        useful name
     */
    public void read( Graph graph,
                      Path parentOfTypes,
                      Collection<Name> nodeTypesToRead,
                      String resourceName ) {
        this.types.addAll(readTypesFrom(graph, parentOfTypes, nodeTypesToRead));
    }

    /**
     * Import the node types from the supplied location in the specified graph.
     * 
     * @param subgraph the subgraph containing the standard ModeShape CND content
     * @param locationOfParent the location to the parent of the node type definition nodes
     * @param resourceName a logical name for the resource name to be used when reporting problems; may be null if there is no
     *        useful name
     */
    public void read( Subgraph subgraph,
                      Location locationOfParent,
                      String resourceName ) {
        this.types.addAll(readTypesFrom(subgraph, locationOfParent, null));
    }

    /**
     * Get the problems where warnings and error messages were recorded by this factory.
     * 
     * @return the problems; never null
     */
    public Problems getProblems() {
        return problems;
    }

    /**
     * Returns the node type definitions created by this factory.
     * 
     * @return the {@link NodeTypeDefinition}s
     */
    public NodeTypeDefinition[] getNodeTypeDefinitions() {
        return types.toArray(new NodeTypeDefinition[types.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<NodeTypeDefinition> iterator() {
        return immutableTypes.iterator();
    }

    protected List<NodeTypeDefinition> readTypesFrom( Graph graph,
                                                      Path parentOfTypes,
                                                      Collection<Name> nodeTypesToRead ) {
        Subgraph subgraph = graph.getSubgraphOfDepth(5).at(parentOfTypes);
        return readTypesFrom(subgraph, subgraph.getLocation(), nodeTypesToRead);
    }

    protected List<NodeTypeDefinition> readTypesFrom( Subgraph nodeTypeSubgraph,
                                                      Location locationOfParentOfNodeTypes,
                                                      Collection<Name> nodeTypesToRead ) {
        List<Location> nodeTypeLocations = nodeTypeSubgraph.getNode(locationOfParentOfNodeTypes).getChildren();
        List<NodeTypeDefinition> results = new ArrayList<NodeTypeDefinition>(nodeTypeLocations.size());
        boolean shouldFilterNodes = locationOfParentOfNodeTypes.hasPath() && nodeTypesToRead != null;

        for (Location location : nodeTypeLocations) {
            SubgraphNode nodeTypeNode = nodeTypeSubgraph.getNode(location);
            assert location.getPath() != null;

            Path relativeNodeTypePath = shouldFilterNodes ? location.getPath().relativeTo(locationOfParentOfNodeTypes.getPath()) : null;
            if (shouldFilterNodes && !nodeTypesToRead.contains(relativeNodeTypePath.getSegment(0).getName())) {
                continue;
            }

            try {
                NodeTypeDefinition nodeType = nodeTypeFrom(nodeTypeNode, nodeTypeSubgraph);
                results.add(nodeType);
            } catch (ConstraintViolationException e) {
                String resource = stringFactory.create(locationOfParentOfNodeTypes.getPath());
                problems.addError(e, JcrI18n.errorImportingNodeTypeContent, resource, e.getMessage());
            }
        }
        return results;
    }

    @SuppressWarnings( "unchecked" )
    protected NodeTypeTemplate nodeTypeFrom( SubgraphNode nodeTypeNode,
                                             Subgraph subgraph ) throws ConstraintViolationException {
        List<Location> children = nodeTypeNode.getChildren();

        String name = readString(nodeTypeNode, JcrLexicon.NODE_TYPE_NAME, null);
        String primaryItemName = readString(nodeTypeNode, JcrLexicon.PRIMARY_ITEM_NAME, null);

        boolean mixin = readBoolean(nodeTypeNode, JcrLexicon.IS_MIXIN, false);
        boolean isAbstract = readBoolean(nodeTypeNode, JcrLexicon.IS_ABSTRACT, false);
        boolean queryable = readBoolean(nodeTypeNode, JcrLexicon.IS_QUERYABLE, true);
        boolean orderableChildNodes = readBoolean(nodeTypeNode, JcrLexicon.HAS_ORDERABLE_CHILD_NODES, false);
        List<String> supertypes = readStrings(nodeTypeNode, JcrLexicon.SUPERTYPES);

        NodeTypeTemplate template = new JcrNodeTypeTemplate(context);
        template.setAbstract(isAbstract);
        template.setDeclaredSuperTypeNames(supertypes.toArray(new String[supertypes.size()]));
        template.setMixin(mixin);
        template.setName(name);
        template.setOrderableChildNodes(orderableChildNodes);
        template.setPrimaryItemName(primaryItemName);
        template.setQueryable(queryable);

        for (Location childLocation : children) {
            if (JcrLexicon.PROPERTY_DEFINITION.equals(childLocation.getPath().getLastSegment().getName())) {
                template.getPropertyDefinitionTemplates().add(propertyDefinitionFrom(subgraph, childLocation));
            } else if (JcrLexicon.CHILD_NODE_DEFINITION.equals(childLocation.getPath().getLastSegment().getName())) {
                template.getNodeDefinitionTemplates().add(childNodeDefinitionFrom(subgraph, childLocation));
            } else {
                throw new IllegalStateException("Unexpected child of node type at: " + childLocation);
            }
        }

        return template;
    }

    protected PropertyDefinitionTemplate propertyDefinitionFrom( Subgraph nodeTypeGraph,
                                                                 Location propertyLocation ) throws ConstraintViolationException {
        SubgraphNode propertyDefinitionNode = nodeTypeGraph.getNode(propertyLocation);

        String name = readString(propertyDefinitionNode, JcrLexicon.NAME, null);
        String onParentVersionName = readString(propertyDefinitionNode,
                                                JcrLexicon.ON_PARENT_VERSION,
                                                OnParentVersionAction.nameFromValue(OnParentVersionAction.COPY));
        int onParentVersion = OnParentVersionAction.valueFromName(onParentVersionName);

        int requiredType = PROPERTY_TYPE_VALUES_FROM_NAME.get(readString(propertyDefinitionNode, JcrLexicon.REQUIRED_TYPE, null));

        boolean mandatory = readBoolean(propertyDefinitionNode, JcrLexicon.MANDATORY, false);
        boolean multiple = readBoolean(propertyDefinitionNode, JcrLexicon.MULTIPLE, false);
        boolean autoCreated = readBoolean(propertyDefinitionNode, JcrLexicon.AUTO_CREATED, false);
        boolean isProtected = readBoolean(propertyDefinitionNode, JcrLexicon.PROTECTED, false);
        boolean isSearchable = readBoolean(propertyDefinitionNode, JcrLexicon.IS_FULL_TEXT_SEARCHABLE, true);
        boolean isOrderable = readBoolean(propertyDefinitionNode, JcrLexicon.IS_QUERY_ORDERABLE, true);
        List<Value> defaultValues = readValues(propertyDefinitionNode, JcrLexicon.DEFAULT_VALUES, requiredType);
        List<String> constraints = readStrings(propertyDefinitionNode, JcrLexicon.VALUE_CONSTRAINTS);
        List<String> queryOps = readStrings(propertyDefinitionNode, JcrLexicon.QUERY_OPERATORS);

        PropertyDefinitionTemplate template = new JcrPropertyDefinitionTemplate(context);
        if (name != null) {
            template.setName(name);
        }
        template.setAutoCreated(autoCreated);
        template.setMandatory(mandatory);
        template.setMultiple(multiple);
        template.setProtected(isProtected);
        template.setFullTextSearchable(isSearchable);
        template.setAvailableQueryOperators(queryOps.toArray(new String[queryOps.size()]));
        template.setQueryOrderable(isOrderable);
        template.setProtected(isProtected);
        template.setOnParentVersion(onParentVersion);
        template.setDefaultValues(defaultValues.toArray(new Value[defaultValues.size()]));
        template.setRequiredType(requiredType);
        template.setValueConstraints(constraints.toArray(new String[constraints.size()]));
        return template;
    }

    protected NodeDefinitionTemplate childNodeDefinitionFrom( Subgraph nodeTypeGraph,
                                                              Location childNodeLocation ) throws ConstraintViolationException {
        SubgraphNode childNodeDefinitionNode = nodeTypeGraph.getNode(childNodeLocation);

        String childNodeName = readString(childNodeDefinitionNode, JcrLexicon.NAME, null);
        String defaultPrimaryTypeName = readString(childNodeDefinitionNode, JcrLexicon.DEFAULT_PRIMARY_TYPE, null);
        String onParentVersionName = readString(childNodeDefinitionNode,
                                                JcrLexicon.ON_PARENT_VERSION,
                                                OnParentVersionAction.nameFromValue(OnParentVersionAction.COPY));
        int onParentVersion = OnParentVersionAction.valueFromName(onParentVersionName);

        boolean mandatory = readBoolean(childNodeDefinitionNode, JcrLexicon.MANDATORY, false);
        boolean allowsSns = readBoolean(childNodeDefinitionNode, JcrLexicon.SAME_NAME_SIBLINGS, false);
        boolean autoCreated = readBoolean(childNodeDefinitionNode, JcrLexicon.AUTO_CREATED, false);
        boolean isProtected = readBoolean(childNodeDefinitionNode, JcrLexicon.PROTECTED, false);
        List<String> requiredTypes = readStrings(childNodeDefinitionNode, JcrLexicon.REQUIRED_PRIMARY_TYPES);

        NodeDefinitionTemplate template = new JcrNodeDefinitionTemplate(context);
        if (childNodeName != null) {
            template.setName(childNodeName);
        }
        template.setAutoCreated(autoCreated);
        template.setMandatory(mandatory);
        template.setSameNameSiblings(allowsSns);
        template.setProtected(isProtected);
        template.setOnParentVersion(onParentVersion);
        template.setDefaultPrimaryTypeName(defaultPrimaryTypeName);
        template.setRequiredPrimaryTypeNames(requiredTypes.toArray(new String[requiredTypes.size()]));
        return template;
    }

    protected Name nameFrom( SubgraphNode node ) {
        return node.getLocation().getPath().getLastSegment().getName();
    }

    protected Name name( String name ) {
        return nameFactory.create(name);
    }

    protected String string( Object value ) {
        return stringFactory.create(value);
    }

    protected boolean readBoolean( SubgraphNode node,
                                   String propertyName,
                                   boolean defaultValue ) {
        return readBoolean(node, nameFactory.create(propertyName), defaultValue);
    }

    protected boolean readBoolean( SubgraphNode node,
                                   Name propertyName,
                                   boolean defaultValue ) {
        Property property = node.getProperty(propertyName);
        return property != null ? booleanFactory.create(property.getFirstValue()) : defaultValue;
    }

    protected String readString( SubgraphNode node,
                                 String propertyName,
                                 String defaultValue ) {
        return readString(node, nameFactory.create(propertyName), defaultValue);
    }

    protected String readString( SubgraphNode node,
                                 Name propertyName,
                                 String defaultValue ) {
        Property property = node.getProperty(propertyName);
        return property != null ? stringFactory.create(property.getFirstValue()) : defaultValue;
    }

    protected List<String> readStrings( SubgraphNode node,
                                        String propertyName ) {
        return readStrings(node, nameFactory.create(propertyName));
    }

    protected List<String> readStrings( SubgraphNode node,
                                        Name propertyName ) {
        List<String> results = new ArrayList<String>();
        Property property = node.getProperty(propertyName);
        if (property != null) {
            for (Object value : property) {
                String str = stringFactory.create(value);
                if (str != null && str.length() != 0) results.add(str);
            }
        }
        return results;
    }

    protected List<Value> readValues( SubgraphNode node,
                                      Name propertyName,
                                      int requiredType ) {
        List<String> results = readStrings(node, propertyName);
        List<Value> values = new ArrayList<Value>(results.size());
        for (String result : results) {
            values.add(new JcrValue(valueFactories, null, requiredType, result));
        }
        return values;
    }

    protected Name readName( SubgraphNode node,
                             String propertyName,
                             Name defaultValue ) {
        return readName(node, nameFactory.create(propertyName), defaultValue);
    }

    protected Name readName( SubgraphNode node,
                             Name propertyName,
                             Name defaultValue ) {
        Property property = node.getProperty(propertyName);
        if (property != null && !property.isEmpty()) {
            String firstValue = stringFactory.create(property.getFirstValue());
            if (firstValue != null && firstValue.trim().length() != 0) {
                return valueFactories.getNameFactory().create(firstValue);
            }
        }
        return defaultValue;
    }

    protected List<Name> readNames( SubgraphNode node,
                                    String propertyName,
                                    Name defaultIfNone ) {
        return readNames(node, nameFactory.create(propertyName), defaultIfNone);
    }

    protected List<Name> readNames( SubgraphNode node,
                                    Name propertyName,
                                    Name defaultIfNone ) {
        List<Name> results = new ArrayList<Name>();
        Property property = node.getProperty(propertyName);
        if (property != null) {
            for (Object value : property) {
                Name name = nameFactory.create(value);
                if (name != null) results.add(name);
            }
        }
        if (results.isEmpty() && defaultIfNone != null) results.add(defaultIfNone);
        return results;
    }

    /**
     * Method that loads into the graph destination the content containing the node type definitions.
     * 
     * @param graphDestination the destination to which the node type content should be written; never null
     * @param path the path within the destination at which the node type content should be rooted; never null
     * @param content the content containing some string representation of the node types to be imported; never null
     * @param resourceName a descriptive name for this import (used only for error messages); may be null
     * @throws Exception if there is a problem importing from the content; this will be automatically recorded in the problems
     */
    protected void importFrom( Destination graphDestination,
                               Path path,
                               String content,
                               String resourceName ) throws Exception {
        throw new UnsupportedOperationException();
    }

}
