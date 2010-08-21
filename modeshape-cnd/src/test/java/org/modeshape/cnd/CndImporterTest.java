/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.cnd;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.modeshape.graph.IsNodeWithProperty.hasProperty;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.text.ParsingException;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.Node;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.io.Destination;
import org.modeshape.graph.io.GraphBatchDestination;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;

/**
 * 
 */
public class CndImporterTest {

    public static final String CND_FILE_PATH = "src/test/resources/cnd/";

    private ExecutionContext context;
    private InMemoryRepositorySource repository;
    private Graph graph;
    private Destination destination;
    private Path rootPath;
    private CndImporter importer;
    private SimpleProblems problems;

    @Before
    public void beforeEach() {
        problems = new SimpleProblems();
        context = new ExecutionContext();
        context.getNamespaceRegistry().register(ModeShapeLexicon.Namespace.PREFIX, ModeShapeLexicon.Namespace.URI);
        context.getNamespaceRegistry().register(JcrLexicon.Namespace.PREFIX, JcrLexicon.Namespace.URI);
        context.getNamespaceRegistry().register(JcrNtLexicon.Namespace.PREFIX, JcrNtLexicon.Namespace.URI);

        // Set up the repository and graph ...
        repository = new InMemoryRepositorySource();
        repository.setName("NodeTypes");
        graph = Graph.create(repository, context);

        // Set up the path where the content will go, and make sure that path exists in the repository ...
        rootPath = context.getValueFactories().getPathFactory().create("/a");
        graph.create(rootPath).and();

        // Now set up the destination ...
        destination = new GraphBatchDestination(graph.batch());

        // Set up the importer ...
        importer = new CndImporter(destination, rootPath);
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected void printProblems() {
        for (Problem problem : problems) {
            System.out.println(problem);
        }
    }

    protected Node node( String pathToNode,
                         String childNodeName,
                         String nameValue ) {
        Node a = graph.getNodeAt("/a/" + pathToNode);
        List<Location> children = a.getChildren();

        for (Location childLocation : a.getChildren()) {
            if (!childLocation.getPath().getLastSegment().getName().equals(name(childNodeName))) continue;
            Node child = graph.getNodeAt(childLocation);
            Property nameProp = child.getProperty(JcrLexicon.NAME);
            if (nameProp != null && nameProp.getFirstValue().equals(name(nameValue))) {
                return child;
            }
        }

        return null;
    }

    protected Node node( String pathToNode ) {
        return graph.getNodeAt("/a/" + pathToNode);
    }

    protected InputStream openCndStream( String cndFileName ) throws IOException {
        return this.getClass().getClassLoader().getResourceAsStream("cnd/" + cndFileName);
    }

    protected File openCndFile( String cndFileName ) throws IOException, URISyntaxException {
        File result = new File(CND_FILE_PATH + cndFileName);
        assertThat(result.exists(), is(true));
        return result;
    }

    @Test( expected = ParsingException.class )
    public void shouldReportErrorIfTheNodeTypeNameIsEmpty() {
        String cnd = "<ns = 'http://namespace.com/ns'> [] abstract";
        importer.parse(cnd);
    }

    @Test( expected = ParsingException.class )
    public void shouldReportErrorIfTheNodeTypeNameIsBlank() {
        String cnd = "<ns = 'http://namespace.com/ns'> [ ] abstract";
        importer.parse(cnd);
    }

    @Test( expected = ParsingException.class )
    public void shouldReportErrorIfTheNodeTypeNameIsNotFollowedByClosingBracket() {
        String cnd = "<ns = 'http://namespace.com/ns'> [  abstract";
        importer.parse(cnd);
    }

    @Test( expected = ParsingException.class )
    public void shouldReportErrorIfTheNodeTypeNameUsesInvalidNamespace() {
        String cnd = "<ns = 'http://namespace.com/ns'> [xyz:acme] abstract";
        importer.parse(cnd);
    }

    @Test
    public void shouldParseNamespaceDeclarationWithQuotedUriAndQuotedPrefix() {
        String cnd = "<'ns' = 'http://namespace.com/ns'>";
        importer.parse(cnd);
    }

    @Test
    public void shouldParseNamespaceDeclarationWithUnquotedUriAndQuotedPrefix() {
        String cnd = "<'ns' = http_namespace.com_ns>";
        importer.parse(cnd);
    }

    @Test
    public void shouldParseNamespaceDeclarationWithQuotedUriAndUnquotedPrefix() {
        String cnd = "<ns = 'http://namespace.com/ns'>";
        importer.parse(cnd);
    }

    @Test
    public void shouldParseNamespaceDeclarationWithUnquotedUriAndUnquotedPrefix() {
        String cnd = "<ns = http_namespace.com_ns>";
        importer.parse(cnd);
    }

    @Test
    public void shouldParseMinimalNodeDefinition() {
        String cnd = "[nodeTypeName]";
        importer.parse(cnd);
    }

    @Test
    public void shouldParseMinimalNodeDefinitionWithSupertype() {
        String cnd = "[nodeTypeName] > supertype";
        importer.parse(cnd);
    }

    @Test
    public void shouldParseMinimalNodeDefinitionWithSupertypes() {
        String cnd = "[nodeTypeName] > supertype1, supertype2";
        importer.parse(cnd);
    }

    @Test
    public void shouldParseNodeDefinitionWithNameThatIsKeyword() {
        String cnd = "[abstract] > supertype1, supertype2";
        importer.parse(cnd);
    }

    @Test
    public void shouldImportCndThatUsesAllFeatures() throws IOException {
        // importer.setDebug(true);
        String cnd = "<ex = 'http://namespace.com/ns'>\n"
                     + "[ex:NodeType] > ex:ParentType1, ex:ParentType2 abstract orderable mixin noquery primaryitem ex:property\n"
                     + "- ex:property (STRING) = 'default1', 'default2' mandatory autocreated protected multiple VERSION\n"
                     + " queryops '=, <>, <, <=, >, >=, LIKE' nofulltext noqueryorder < 'constraint1', 'constraint2'"
                     + "+ ex:node (ex:reqType1, ex:reqType2) = ex:defaultType mandatory autocreated protected sns version";
        importer.importFrom(cnd, problems, "string");
        // assertThat(problems.size(), is(1));
        // printProblems();
        context.getNamespaceRegistry().register("ex", "http://namespace.com/ns");
        Node nodeType = node("ex:NodeType");
        assertThat(nodeType, hasProperty(JcrLexicon.IS_ABSTRACT, true));
        Node prop = node("ex:NodeType/jcr:propertyDefinition");
        assertThat(prop, hasProperty(JcrLexicon.NAME, name("ex:property")));
        assertThat(prop, hasProperty(JcrLexicon.REQUIRED_TYPE, "STRING"));
        assertThat(prop, hasProperty(JcrLexicon.DEFAULT_VALUES, new Object[] {"default1", "default2"}));
        assertThat(prop, hasProperty(JcrLexicon.AUTO_CREATED, true));
        assertThat(prop, hasProperty(JcrLexicon.MANDATORY, true));
        assertThat(prop, hasProperty(JcrLexicon.PROTECTED, true));
        assertThat(prop, hasProperty(JcrLexicon.MULTIPLE, true));
        assertThat(prop, hasProperty(JcrLexicon.ON_PARENT_VERSION, "VERSION"));
        assertThat(prop, hasProperty(JcrLexicon.VALUE_CONSTRAINTS, new Object[] {"constraint1", "constraint2"}));
        assertThat(prop, hasProperty(JcrLexicon.IS_FULL_TEXT_SEARCHABLE, false));
        assertThat(prop, hasProperty(JcrLexicon.IS_QUERY_ORDERABLE, false));
        Node node = node("ex:NodeType/jcr:childNodeDefinition");
        assertThat(node, hasProperty(JcrLexicon.NAME, name("ex:node")));
        assertThat(node, hasProperty(JcrLexicon.REQUIRED_PRIMARY_TYPES, new Object[] {name("ex:reqType1"), name("ex:reqType2")}));
        assertThat(node, hasProperty(JcrLexicon.DEFAULT_PRIMARY_TYPE, name("ex:defaultType")));
        assertThat(node, hasProperty(JcrLexicon.AUTO_CREATED, true));
        assertThat(node, hasProperty(JcrLexicon.MANDATORY, true));
        assertThat(node, hasProperty(JcrLexicon.PROTECTED, true));
        assertThat(node, hasProperty(JcrLexicon.SAME_NAME_SIBLINGS, true));
        assertThat(node, hasProperty(JcrLexicon.ON_PARENT_VERSION, "VERSION"));
    }

    @Test
    public void shouldImportCndThatIsOnOneLine() throws IOException {
        String cnd = "<ns = 'http://namespace.com/ns'> "
                     + "[ns:NodeType] > ns:ParentType1, ns:ParentType2 abstract orderable mixin noquery primaryitem ex:property "
                     + "- ex:property (STRING) = 'default1', 'default2' mandatory autocreated protected multiple VERSION < 'constraint1', 'constraint2' "
                     + " queryops '=, <>, <, <=, >, >=, LIKE' nofulltext noqueryorder "
                     + "+ ns:node (ns:reqType1, ns:reqType2) = ns:defaultType mandatory autocreated protected sns version";
        // importer.importFrom(cnd, problems, "string");
    }

    @Test
    public void shouldImportCndThatHasNoChildren() throws IOException {
        String cnd = "<ns = 'http://namespace.com/ns'>\n"
                     + "[ns:NodeType] > ns:ParentType1, ns:ParentType2 abstract orderable mixin noquery primaryitem ex:property\n"
                     + "- ex:property (STRING) = 'default1', 'default2' mandatory autocreated protected multiple VERSION < 'constraint1', 'constraint2'\n"
                     + " queryops '=, <>, <, <=, >, >=, LIKE' nofulltext noqueryorder";
        // importer.importFrom(cnd, problems, "string");
    }

    @Test
    public void shouldImportJcrBuiltinNodeTypesForJSR170() throws Exception {
        importer.importFrom(openCndFile("jcr-builtins-170.cnd"), problems);
        if (problems.size() != 0) printProblems();
        assertThat(problems.size(), is(0));

        // [nt:base]
        // - jcr:primaryType (name) mandatory autocreated protected compute
        // - jcr:mixinTypes (name) protected multiple compute
        assertNodeType("nt:base", NO_SUPERTYPES, NO_PRIMARY_NAME);
        assertProperty("nt:base", "jcr:primaryType", "Name", NO_DEFAULTS, new PropertyOptions[] {PropertyOptions.Mandatory,
            PropertyOptions.Autocreated, PropertyOptions.Protected}, OnParentVersion.Compute);
        assertProperty("nt:base", "jcr:mixinTypes", "Name", NO_DEFAULTS, new PropertyOptions[] {PropertyOptions.Multiple,
            PropertyOptions.Protected}, OnParentVersion.Compute);

        // [nt:unstructured]
        // orderable
        // - * (undefined) multiple
        // - * (undefined)
        // + * (nt:base) = nt:unstructured multiple version
        assertNodeType("nt:unstructured", NO_SUPERTYPES, NO_PRIMARY_NAME, NodeOptions.Ordered);
        assertProperty("nt:unstructured", "*", "Undefined", NO_DEFAULTS, PropertyOptions.Multiple);
        // We should test for this, but we'd have to rewrite node() to look more like
        // RepositoryNodeTypeManager.findChildNodeDefinition
        // assertProperty("nt:unstructured", "*", "Undefined", NO_DEFAULTS);
        assertChild("nt:unstructured", "*", "nt:base", "nt:unstructured", OnParentVersion.Version, ChildOptions.Multiple);

        // [mix:referenceable]
        // mixin
        // - jcr:uuid (string) mandatory autocreated protected initialize
        assertNodeType("mix:referenceable", NO_SUPERTYPES, NO_PRIMARY_NAME, NodeOptions.Mixin);
        assertProperty("mix:referenceable",
                       "jcr:uuid",
                       "String",
                       NO_DEFAULTS,
                       OnParentVersion.Initialize,
                       PropertyOptions.Mandatory,
                       PropertyOptions.Autocreated,
                       PropertyOptions.Protected);

        // [mix:lockable]
        // mixin
        // - jcr:lockOwner (string) protected ignore
        // - jcr:lockIsDeep (boolean) protected ignore
        assertNodeType("mix:lockable", new String[] {"mix:referenceable"}, NO_PRIMARY_NAME, NodeOptions.Mixin);
        assertProperty("mix:lockable", "jcr:lockOwner", "String", NO_DEFAULTS, OnParentVersion.Ignore, PropertyOptions.Protected);
        assertProperty("mix:lockable",
                       "jcr:lockIsDeep",
                       "Boolean",
                       NO_DEFAULTS,
                       OnParentVersion.Ignore,
                       PropertyOptions.Protected);

        // [nt:propertyDefinition]
        // - jcr:name (name)
        // - jcr:autoCreated (boolean) mandatory
        // - jcr:mandatory (boolean) mandatory
        // - jcr:onParentVersion (string) mandatory
        // < 'COPY', 'VERSION', 'INITIALIZE', 'COMPUTE', 'IGNORE', 'ABORT'
        // - jcr:protected (boolean) mandatory
        // - jcr:requiredType (string) mandatory
        // < 'STRING', 'BINARY', 'LONG', 'DOUBLE', 'BOOLEAN', 'DATE', 'NAME', 'PATH', 'REFERENCE', 'UNDEFINED'
        // - jcr:valueConstraints (string) multiple
        // - jcr:defaultValues (undefined) multiple
        // - jcr:multiple (boolean) mandatory
        assertNodeType("nt:propertyDefinition", NO_SUPERTYPES, NO_PRIMARY_NAME);
        assertProperty("nt:propertyDefinition", "jcr:name", "Name", NO_DEFAULTS);
        assertProperty("nt:propertyDefinition", "jcr:autoCreated", "Boolean", NO_DEFAULTS, PropertyOptions.Mandatory);
        assertProperty("nt:propertyDefinition", "jcr:mandatory", "Boolean", NO_DEFAULTS, PropertyOptions.Mandatory);
        assertProperty("nt:propertyDefinition",
                       "jcr:onParentVersion",
                       "String",
                       NO_DEFAULTS,
                       new PropertyOptions[] {PropertyOptions.Mandatory},
                       null,
                       new String[] {"COPY", "VERSION", "INITIALIZE", "COMPUTE", "IGNORE", "ABORT"});
        assertProperty("nt:propertyDefinition", "jcr:protected", "Boolean", NO_DEFAULTS, PropertyOptions.Mandatory);
        assertProperty("nt:propertyDefinition",
                       "jcr:requiredType",
                       "String",
                       NO_DEFAULTS,
                       new PropertyOptions[] {PropertyOptions.Mandatory},
                       null,
                       new String[] {"STRING", "BINARY", "LONG", "DOUBLE", "BOOLEAN", "DATE", "NAME", "PATH", "REFERENCE",
                           "UNDEFINED"});
        assertProperty("nt:propertyDefinition", "jcr:valueConstraints", "String", NO_DEFAULTS, PropertyOptions.Multiple);
        assertProperty("nt:propertyDefinition", "jcr:defaultValues", "Undefined", NO_DEFAULTS, PropertyOptions.Multiple);
        assertProperty("nt:propertyDefinition", "jcr:multiple", "Boolean", NO_DEFAULTS, PropertyOptions.Mandatory);
    }

    @Test
    public void shouldImportJcrBuiltinNodeTypesForJSR283() throws Exception {
        importer.importFrom(openCndFile("jcr-builtins-283-early-draft.cnd"), problems);
        if (problems.size() != 0) printProblems();
        assertThat(problems.size(), is(0));

        // [nt:base]
        // - jcr:primaryType (name) mandatory autocreated protected compute
        // - jcr:mixinTypes (name) protected multiple compute
        assertNodeType("nt:base", new String[] {"mode:defined"}, NO_PRIMARY_NAME, NodeOptions.Abstract);
        assertProperty("nt:base", "jcr:primaryType", "Name", NO_DEFAULTS, new PropertyOptions[] {PropertyOptions.Mandatory,
            PropertyOptions.Autocreated, PropertyOptions.Protected}, OnParentVersion.Compute);
        assertProperty("nt:base", "jcr:mixinTypes", "Name", NO_DEFAULTS, new PropertyOptions[] {PropertyOptions.Multiple,
            PropertyOptions.Protected}, OnParentVersion.Compute);

        // [nt:unstructured]
        // orderable
        // - * (undefined) multiple
        // - * (undefined)
        // + * (nt:base) = nt:unstructured multiple version
        assertNodeType("nt:unstructured", NO_SUPERTYPES, NO_PRIMARY_NAME, NodeOptions.Ordered);
        assertProperty("nt:unstructured", "*", "Undefined", NO_DEFAULTS, PropertyOptions.Multiple);
        // We should test for this, but we'd have to rewrite node() to look more like
        // RepositoryNodeTypeManager.findChildNodeDefinition
        // assertProperty("nt:unstructured", "*", "Undefined", NO_DEFAULTS);
        assertChild("nt:unstructured", "*", "nt:base", "nt:unstructured", OnParentVersion.Version, ChildOptions.Multiple);

        // [mix:referenceable]
        // mixin
        // - jcr:uuid (string) mandatory autocreated protected initialize
        assertNodeType("mix:referenceable", NO_SUPERTYPES, NO_PRIMARY_NAME, NodeOptions.Mixin);
        assertProperty("mix:referenceable",
                       "jcr:uuid",
                       "String",
                       NO_DEFAULTS,
                       OnParentVersion.Initialize,
                       PropertyOptions.Mandatory,
                       PropertyOptions.Autocreated,
                       PropertyOptions.Protected);

        // [mix:lockable]
        // mixin
        // - jcr:lockOwner (string) protected ignore
        // - jcr:lockIsDeep (boolean) protected ignore
        assertNodeType("mix:lockable", NO_SUPERTYPES, NO_PRIMARY_NAME, NodeOptions.Mixin);
        assertProperty("mix:lockable", "jcr:lockOwner", "String", NO_DEFAULTS, OnParentVersion.Ignore, PropertyOptions.Protected);
        assertProperty("mix:lockable",
                       "jcr:lockIsDeep",
                       "Boolean",
                       NO_DEFAULTS,
                       OnParentVersion.Ignore,
                       PropertyOptions.Protected);

        // [nt:propertyDefinition]
        // - jcr:name (name)
        // - jcr:autoCreated (boolean) mandatory
        // - jcr:mandatory (boolean) mandatory
        // - jcr:onParentVersion (string) mandatory
        // < 'COPY', 'VERSION', 'INITIALIZE', 'COMPUTE', 'IGNORE', 'ABORT'
        // - jcr:protected (boolean) mandatory
        // - jcr:requiredType (string) mandatory
        // < 'STRING', 'BINARY', 'LONG', 'DOUBLE', 'BOOLEAN', 'DATE', 'NAME', 'PATH', 'REFERENCE', 'UNDEFINED'
        // - jcr:valueConstraints (string) multiple
        // - jcr:defaultValues (undefined) multiple
        // - jcr:multiple (boolean) mandatory
        assertNodeType("nt:propertyDefinition", NO_SUPERTYPES, NO_PRIMARY_NAME);
        assertProperty("nt:propertyDefinition", "jcr:name", "Name", NO_DEFAULTS);
        assertProperty("nt:propertyDefinition", "jcr:autoCreated", "Boolean", NO_DEFAULTS, PropertyOptions.Mandatory);
        assertProperty("nt:propertyDefinition", "jcr:mandatory", "Boolean", NO_DEFAULTS, PropertyOptions.Mandatory);
        assertProperty("nt:propertyDefinition",
                       "jcr:onParentVersion",
                       "String",
                       NO_DEFAULTS,
                       new PropertyOptions[] {PropertyOptions.Mandatory},
                       null,
                       new String[] {"COPY", "VERSION", "INITIALIZE", "COMPUTE", "IGNORE", "ABORT"});
        assertProperty("nt:propertyDefinition", "jcr:protected", "Boolean", NO_DEFAULTS, PropertyOptions.Mandatory);
        assertProperty("nt:propertyDefinition",
                       "jcr:requiredType",
                       "String",
                       NO_DEFAULTS,
                       new PropertyOptions[] {PropertyOptions.Mandatory},
                       null,
                       new String[] {"STRING", "BINARY", "LONG", "DOUBLE", "BOOLEAN", "DATE", "NAME", "PATH", "REFERENCE",
                           "UNDEFINED"});
        assertProperty("nt:propertyDefinition", "jcr:valueConstraints", "String", NO_DEFAULTS, PropertyOptions.Multiple);
        assertProperty("nt:propertyDefinition", "jcr:defaultValues", "Undefined", NO_DEFAULTS, PropertyOptions.Multiple);
        assertProperty("nt:propertyDefinition", "jcr:multiple", "Boolean", NO_DEFAULTS, PropertyOptions.Mandatory);
    }

    @Test
    public void shouldImportCndThatIsEmpty() throws Exception {
        importer.importFrom(openCndFile("empty.cnd"), problems);
        if (problems.size() != 0) printProblems();
        assertThat(problems.size(), is(0));
    }

    @Test
    public void shouldImportCndForImageSequencer() throws Exception {
        importer.importFrom(openCndFile("images.cnd"), problems);
        if (problems.size() != 0) printProblems();
        assertThat(problems.size(), is(0));
    }

    @Test
    public void shouldImportCndForMp3Sequencer() throws Exception {
        importer.importFrom(openCndFile("mp3.cnd"), problems);
        if (problems.size() != 0) printProblems();
        assertThat(problems.size(), is(0));
    }

    @Test
    public void shouldImportCndForTeiidSequencer() throws Exception {
        importer.importFrom(openCndFile("teiid.cnd"), problems);
        if (problems.size() != 0) printProblems();
        assertThat(problems.size(), is(0));
        assertNodeType("relational:catalog", new String[] {"nt:unstructured", "relational:relationalEntity"}, NO_PRIMARY_NAME);

    }

    @Test
    public void shouldNotImportFileThatIsNotAValidCnd() throws Exception {
        importer.importFrom(openCndFile("invalid.cnd"), problems);
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldImportCndForAircraft() throws Exception {
        importer.importFrom(openCndFile("aircraft.cnd"), problems);
        if (problems.size() != 0) printProblems();
        assertThat(problems.size(), is(0));
    }

    @Test
    public void shouldImportCndForCars() throws Exception {
        importer.importFrom(openCndFile("cars.cnd"), problems);
        if (problems.size() != 0) printProblems();
        assertThat(problems.size(), is(0));
    }

    @Test
    public void shouldImportCndForJavaSequencer() throws Exception {
        importer.importFrom(openCndFile("javaSource.cnd"), problems);
        if (problems.size() != 0) printProblems();
        assertThat(problems.size(), is(0));
    }

    public static final String[] NO_DEFAULTS = {};
    public static final String[] NO_SUPERTYPES = {};
    public static final String[] NO_VALUE_CONSTRAINTS = {};
    public static final String NO_PRIMARY_NAME = null;

    public static enum PropertyOptions {
        Mandatory,
        Autocreated,
        Protected,
        Multiple,
        FullTextSearchable,
        QueryOrderable
    }

    public static enum ChildOptions {
        Mandatory,
        Autocreated,
        Protected,
        Multiple
    }

    public static enum NodeOptions {
        Abstract,
        Mixin,
        Ordered,
        Queryable
    }

    public static enum OnParentVersion {
        Copy,
        Version,
        Initialize,
        Compute,
        Ignore,
        Abort
    }

    protected void assertNodeType( String name,
                                   String[] superTypes,
                                   String primaryItemName,
                                   NodeOptions... nodeOptions ) {
        Set<NodeOptions> options = new HashSet<NodeOptions>();
        for (NodeOptions option : nodeOptions)
            options.add(option);

        Node nodeType = node(name);
        assertThat(nodeType, hasProperty(JcrLexicon.IS_MIXIN, options.contains(NodeOptions.Mixin)));
        assertThat(nodeType, hasProperty(JcrLexicon.IS_ABSTRACT, options.contains(NodeOptions.Abstract)));
        // assertThat(nodeType, hasProperty(JcrLexicon.HAS_ORDERABLE_CHILD_NODES, options.contains(NodeOptions.Ordered)));
        assertThat(nodeType, hasProperty(JcrLexicon.IS_QUERYABLE, !options.contains(NodeOptions.Queryable)));
        if (primaryItemName != null) {
            assertThat(nodeType, hasProperty(JcrLexicon.PRIMARY_ITEM_NAME, name(primaryItemName)));
        } else {
            assertThat(nodeType.getPropertiesByName().containsKey(JcrLexicon.PRIMARY_ITEM_NAME), is(false));
        }
        if (superTypes.length != 0) {
            Name[] superTypeNames = new Name[superTypes.length];
            for (int i = 0; i != superTypes.length; ++i) {
                String requiredType = superTypes[i];
                superTypeNames[i] = name(requiredType);
            }
            assertThat(nodeType, hasProperty(JcrLexicon.SUPERTYPES, (Object[])superTypeNames));
        } else {
            assertThat(nodeType.getProperty(JcrLexicon.SUPERTYPES), is(nullValue()));
        }
    }

    protected void assertProperty( String nodeTypeName,
                                   String propertyName,
                                   String requiredType,
                                   String[] defaultValues,
                                   PropertyOptions... propertyOptions ) {
        assertProperty(nodeTypeName, propertyName, requiredType, defaultValues, propertyOptions, null);
    }

    protected void assertProperty( String nodeTypeName,
                                   String propertyName,
                                   String requiredType,
                                   String[] defaultValues,
                                   OnParentVersion onParentVersion,
                                   PropertyOptions... propertyOptions ) {
        assertProperty(nodeTypeName, propertyName, requiredType, defaultValues, propertyOptions, onParentVersion);
    }

    protected void assertProperty( String nodeTypeName,
                                   String propertyName,
                                   String requiredType,
                                   String[] defaultValues,
                                   PropertyOptions[] propertyOptions,
                                   OnParentVersion onParentVersioning,
                                   String... valueConstraints ) {
        Set<PropertyOptions> options = new HashSet<PropertyOptions>();
        for (PropertyOptions option : propertyOptions)
            options.add(option);

        Node nodeType = node(nodeTypeName, "jcr:propertyDefinition", propertyName);
        assertThat(nodeType, hasProperty(JcrLexicon.REQUIRED_TYPE, requiredType.toUpperCase()));
        assertThat(nodeType, hasProperty(JcrLexicon.MULTIPLE, options.contains(PropertyOptions.Multiple)));
        assertThat(nodeType, hasProperty(JcrLexicon.MANDATORY, options.contains(PropertyOptions.Mandatory)));
        assertThat(nodeType, hasProperty(JcrLexicon.AUTO_CREATED, options.contains(PropertyOptions.Autocreated)));
        assertThat(nodeType, hasProperty(JcrLexicon.PROTECTED, options.contains(PropertyOptions.Protected)));
        if (onParentVersioning != null) {
            switch (onParentVersioning) {
                case Abort:
                    assertThat(nodeType, hasProperty(JcrLexicon.ON_PARENT_VERSION, "ABORT"));
                    break;
                case Compute:
                    assertThat(nodeType, hasProperty(JcrLexicon.ON_PARENT_VERSION, "COMPUTE"));
                    break;
                case Copy:
                    assertThat(nodeType, hasProperty(JcrLexicon.ON_PARENT_VERSION, "COPY"));
                    break;
                case Ignore:
                    assertThat(nodeType, hasProperty(JcrLexicon.ON_PARENT_VERSION, "IGNORE"));
                    break;
                case Initialize:
                    assertThat(nodeType, hasProperty(JcrLexicon.ON_PARENT_VERSION, "INITIALIZE"));
                    break;
                case Version:
                    assertThat(nodeType, hasProperty(JcrLexicon.ON_PARENT_VERSION, "VERSION"));
                    break;
            }
        } else {
            assertThat(nodeType, hasProperty(JcrLexicon.ON_PARENT_VERSION, "COPY")); // it's copy by default
        }
        assertThat(nodeType, hasProperty(JcrLexicon.IS_FULL_TEXT_SEARCHABLE,
                                         !options.contains(PropertyOptions.FullTextSearchable)));
        assertThat(nodeType, hasProperty(JcrLexicon.IS_QUERY_ORDERABLE, !options.contains(PropertyOptions.QueryOrderable)));
        if (valueConstraints.length != 0) {
            assertThat(nodeType, hasProperty(JcrLexicon.VALUE_CONSTRAINTS, (Object[])valueConstraints));
        } else {
            assertThat(nodeType.getProperty(JcrLexicon.VALUE_CONSTRAINTS), is(nullValue()));
        }
        if (defaultValues.length != 0) {
            assertThat(nodeType, hasProperty(JcrLexicon.DEFAULT_VALUES, (Object[])defaultValues));
        } else {
            assertThat(nodeType.getProperty(JcrLexicon.DEFAULT_VALUES), is(nullValue()));
        }
    }

    protected void assertChild( String nodeTypeName,
                                String childName,
                                String requiredType,
                                String defaultPrimaryType,
                                ChildOptions[] childOptions,
                                OnParentVersion onParentVersioning ) {
        assertChild(nodeTypeName, childName, new String[] {requiredType}, defaultPrimaryType, childOptions, onParentVersioning);
    }

    protected void assertChild( String nodeTypeName,
                                String childName,
                                String requiredType,
                                String defaultPrimaryType,
                                OnParentVersion onParentVersioning,
                                ChildOptions... childOptions ) {
        assertChild(nodeTypeName, childName, new String[] {requiredType}, defaultPrimaryType, childOptions, onParentVersioning);
    }

    protected void assertChild( String nodeTypeName,
                                String childName,
                                String[] requiredTypes,
                                String defaultPrimaryType,
                                ChildOptions[] childOptions,
                                OnParentVersion onParentVersioning ) {
        Set<ChildOptions> options = new HashSet<ChildOptions>();
        for (ChildOptions option : childOptions)
            options.add(option);

        Node nodeType = node(nodeTypeName, "jcr:childNodeDefinition", childName);
        assertThat(nodeType, hasProperty(JcrLexicon.DEFAULT_PRIMARY_TYPE, name(defaultPrimaryType)));
        assertThat(nodeType, hasProperty(JcrLexicon.SAME_NAME_SIBLINGS, options.contains(ChildOptions.Multiple)));
        assertThat(nodeType, hasProperty(JcrLexicon.MANDATORY, options.contains(ChildOptions.Mandatory)));
        assertThat(nodeType, hasProperty(JcrLexicon.AUTO_CREATED, options.contains(ChildOptions.Autocreated)));
        assertThat(nodeType, hasProperty(JcrLexicon.PROTECTED, options.contains(ChildOptions.Protected)));
        switch (onParentVersioning) {
            case Abort:
                assertThat(nodeType, hasProperty(JcrLexicon.ON_PARENT_VERSION, "ABORT"));
                break;
            case Compute:
                assertThat(nodeType, hasProperty(JcrLexicon.ON_PARENT_VERSION, "COMPUTE"));
                break;
            case Copy:
                assertThat(nodeType, hasProperty(JcrLexicon.ON_PARENT_VERSION, "COPY"));
                break;
            case Ignore:
                assertThat(nodeType, hasProperty(JcrLexicon.ON_PARENT_VERSION, "IGNORE"));
                break;
            case Initialize:
                assertThat(nodeType, hasProperty(JcrLexicon.ON_PARENT_VERSION, "INITIALIZE"));
                break;
            case Version:
                assertThat(nodeType, hasProperty(JcrLexicon.ON_PARENT_VERSION, "VERSION"));
                break;
            default:
                assertThat(nodeType, hasProperty(JcrLexicon.ON_PARENT_VERSION, is(nullValue())));
        }
        if (requiredTypes.length != 0) {
            Name[] requiredTypeNames = new Name[requiredTypes.length];
            for (int i = 0; i != requiredTypes.length; ++i) {
                String requiredType = requiredTypes[i];
                requiredTypeNames[i] = name(requiredType);
            }
            assertThat(nodeType, hasProperty(JcrLexicon.REQUIRED_PRIMARY_TYPES, (Object[])requiredTypeNames));
        } else {
            assertThat(nodeType.getProperty(JcrLexicon.REQUIRED_PRIMARY_TYPES), is(nullValue()));
        }
    }
}
