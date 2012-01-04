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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.text.ParsingException;
import org.modeshape.jcr.cache.PropertyTypeUtil;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;

/**
 * 
 */
public class CndImporterTest {

    public static final String CND_FILE_PATH = "src/test/resources/cnd/";

    private ExecutionContext context;
    private CndImporter importer;
    private SimpleProblems problems;

    @Before
    public void beforeEach() {
        problems = new SimpleProblems();
        context = new ExecutionContext();
        context.getNamespaceRegistry().register(ModeShapeLexicon.Namespace.PREFIX, ModeShapeLexicon.Namespace.URI);
        context.getNamespaceRegistry().register(JcrLexicon.Namespace.PREFIX, JcrLexicon.Namespace.URI);
        context.getNamespaceRegistry().register(JcrNtLexicon.Namespace.PREFIX, JcrNtLexicon.Namespace.URI);

        // Set up the importer ...
        importer = new CndImporter(context, true);
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected void printProblems() {
        for (Problem problem : problems) {
            System.out.println(problem);
        }
    }

    protected InputStream openCndStream( String cndFileName ) {
        return this.getClass().getClassLoader().getResourceAsStream("cnd/" + cndFileName);
    }

    protected File openCndFile( String cndFileName ) {
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
    public void shouldImportCndThatUsesAllFeatures() throws RepositoryException {
        // importer.setDebug(true);
        String cnd = "<ex = 'http://namespace.com/ns'>\n"
                     + "[ex:NodeType] > ex:ParentType1, ex:ParentType2 abstract orderable mixin noquery primaryitem ex:property\n"
                     + "- ex:property (STRING) = 'default1', 'default2' mandatory autocreated protected multiple VERSION\n"
                     + " queryops '=, <>, <, <=, >, >=, LIKE' nofulltext noqueryorder < 'constraint1', 'constraint2'"
                     + "+ ex:node (ex:reqType1, ex:reqType2) = ex:defaultType mandatory autocreated protected sns version";
        importer.importFrom(cnd, problems, "string");
        if (!problems.isEmpty()) printProblems();

        // Check the namespace ...
        context.getNamespaceRegistry().register("ex", "http://namespace.com/ns");
        assertThat(importer.getNamespaces().size(), is(1));
        NamespaceRegistry.Namespace ns = importer.getNamespaces().iterator().next();
        assertThat(ns.getNamespaceUri(), is("http://namespace.com/ns"));

        List<NodeTypeDefinition> defns = importer.getNodeTypeDefinitions();
        assertThat(defns.size(), is(1));

        NodeTypeDefinition defn = defns.get(0);
        assertThat(defn.getName(), is("ex:NodeType"));
        assertThat(defn.isAbstract(), is(true));
        assertThat(defn.hasOrderableChildNodes(), is(true));
        assertThat(defn.isMixin(), is(true));
        assertThat(defn.isQueryable(), is(false));
        assertThat(defn.getPrimaryItemName(), is("ex:property"));
        String[] supertypeNames = defn.getDeclaredSupertypeNames();
        assertThat(supertypeNames[0], is("ex:ParentType1"));
        assertThat(supertypeNames[1], is("ex:ParentType2"));

        PropertyDefinition[] propDefns = defn.getDeclaredPropertyDefinitions();
        assertThat(propDefns.length, is(1));
        PropertyDefinition propDefn = propDefns[0];
        assertThat(propDefn.getName(), is("ex:property"));
        assertThat(propDefn.getRequiredType(), is(PropertyType.STRING));
        assertThat(propDefn.isMandatory(), is(true));
        assertThat(propDefn.isAutoCreated(), is(true));
        assertThat(propDefn.isProtected(), is(true));
        assertThat(propDefn.isMultiple(), is(true));
        assertThat(propDefn.getOnParentVersion(), is(OnParentVersionAction.VERSION));
        assertThat(propDefn.isFullTextSearchable(), is(false));
        assertThat(propDefn.isQueryOrderable(), is(false));
        Value[] defaultValues = propDefn.getDefaultValues();
        assertThat(defaultValues[0].getString(), is("default1"));
        assertThat(defaultValues[1].getString(), is("default2"));
        String[] queryOps = propDefn.getAvailableQueryOperators();
        assertThat(queryOps[0], is("="));
        assertThat(queryOps[1], is("<>"));
        assertThat(queryOps[2], is("<"));
        assertThat(queryOps[3], is("<="));
        assertThat(queryOps[4], is(">"));
        assertThat(queryOps[5], is(">="));
        assertThat(queryOps[6], is("LIKE"));
        String[] constraints = propDefn.getValueConstraints();
        assertThat(constraints[0], is("constraint1"));
        assertThat(constraints[1], is("constraint2"));

        NodeDefinition[] childDefns = defn.getDeclaredChildNodeDefinitions();
        assertThat(childDefns.length, is(1));
        NodeDefinition childDefn = childDefns[0];
        assertThat(childDefn.getName(), is("ex:node"));
        assertThat(childDefn.getDefaultPrimaryTypeName(), is("ex:defaultType"));
        assertThat(childDefn.isMandatory(), is(true));
        assertThat(childDefn.isAutoCreated(), is(true));
        assertThat(childDefn.isProtected(), is(true));
        assertThat(childDefn.allowsSameNameSiblings(), is(true));
        assertThat(childDefn.getOnParentVersion(), is(OnParentVersionAction.VERSION));
        String[] requiredTypeNames = childDefn.getRequiredPrimaryTypeNames();
        assertThat(requiredTypeNames[0], is("ex:reqType1"));
        assertThat(requiredTypeNames[1], is("ex:reqType2"));
    }

    @Test
    public void shouldImportCndThatUsesExtensions() throws RepositoryException {
        // importer.setDebug(true);
        String cnd = "<ex = 'http://namespace.com/ns'>\n"
                     + "[ex:NodeType] > ex:ParentType1, ex:ParentType2 abstract {mode:desc 'ex:NodeType description'} orderable mixin noquery primaryitem ex:property\n"
                     + "- ex:property (STRING) = 'default1', 'default2' mandatory autocreated protected multiple VERSION\n"
                     + " queryops '=, <>, <, <=, >, >=, LIKE' {mode:desc 'ex:property description'} {mode:altName Cool Property} nofulltext noqueryorder < 'constraint1', 'constraint2'"
                     + "+ ex:node (ex:reqType1, ex:reqType2) = ex:defaultType {} mandatory autocreated protected sns version";
        importer.importFrom(cnd, problems, "string");

        // Check the namespace ...
        context.getNamespaceRegistry().register("ex", "http://namespace.com/ns");
        assertThat(importer.getNamespaces().size(), is(1));
        NamespaceRegistry.Namespace ns = importer.getNamespaces().iterator().next();
        assertThat(ns.getNamespaceUri(), is("http://namespace.com/ns"));

        List<NodeTypeDefinition> defns = importer.getNodeTypeDefinitions();
        assertThat(defns.size(), is(1));

        NodeTypeDefinition defn = defns.get(0);
        assertThat(defn.getName(), is("ex:NodeType"));
        assertThat(defn.isAbstract(), is(true));
        assertThat(defn.hasOrderableChildNodes(), is(true));
        assertThat(defn.isMixin(), is(true));
        assertThat(defn.isQueryable(), is(false));
        assertThat(defn.getPrimaryItemName(), is("ex:property"));
        String[] supertypeNames = defn.getDeclaredSupertypeNames();
        assertThat(supertypeNames[0], is("ex:ParentType1"));
        assertThat(supertypeNames[1], is("ex:ParentType2"));

        PropertyDefinition[] propDefns = defn.getDeclaredPropertyDefinitions();
        assertThat(propDefns.length, is(1));
        PropertyDefinition propDefn = propDefns[0];
        assertThat(propDefn.getName(), is("ex:property"));
        assertThat(propDefn.getRequiredType(), is(PropertyType.STRING));
        assertThat(propDefn.isMandatory(), is(true));
        assertThat(propDefn.isAutoCreated(), is(true));
        assertThat(propDefn.isProtected(), is(true));
        assertThat(propDefn.isMultiple(), is(true));
        assertThat(propDefn.getOnParentVersion(), is(OnParentVersionAction.VERSION));
        assertThat(propDefn.isFullTextSearchable(), is(false));
        assertThat(propDefn.isQueryOrderable(), is(false));
        Value[] defaultValues = propDefn.getDefaultValues();
        assertThat(defaultValues[0].getString(), is("default1"));
        assertThat(defaultValues[1].getString(), is("default2"));
        String[] queryOps = propDefn.getAvailableQueryOperators();
        assertThat(queryOps[0], is("="));
        assertThat(queryOps[1], is("<>"));
        assertThat(queryOps[2], is("<"));
        assertThat(queryOps[3], is("<="));
        assertThat(queryOps[4], is(">"));
        assertThat(queryOps[5], is(">="));
        assertThat(queryOps[6], is("LIKE"));
        String[] constraints = propDefn.getValueConstraints();
        assertThat(constraints[0], is("constraint1"));
        assertThat(constraints[1], is("constraint2"));

        NodeDefinition[] childDefns = defn.getDeclaredChildNodeDefinitions();
        assertThat(childDefns.length, is(1));
        NodeDefinition childDefn = childDefns[0];
        assertThat(childDefn.getName(), is("ex:node"));
        assertThat(childDefn.getDefaultPrimaryTypeName(), is("ex:defaultType"));
        assertThat(childDefn.isMandatory(), is(true));
        assertThat(childDefn.isAutoCreated(), is(true));
        assertThat(childDefn.isProtected(), is(true));
        assertThat(childDefn.allowsSameNameSiblings(), is(true));
        assertThat(childDefn.getOnParentVersion(), is(OnParentVersionAction.VERSION));
        String[] requiredTypeNames = childDefn.getRequiredPrimaryTypeNames();
        assertThat(requiredTypeNames[0], is("ex:reqType1"));
        assertThat(requiredTypeNames[1], is("ex:reqType2"));
    }

    @Test
    public void shouldImportCndThatIsOnOneLine() {
        String cnd = "<ns = 'http://namespace.com/ns'> "
                     + "<ex = 'http://namespace.com/ex'>\n"
                     + "[ns:NodeType] > ns:ParentType1, ns:ParentType2 abstract orderable mixin noquery primaryitem ex:property "
                     + "- ex:property (STRING) = 'default1', 'default2' mandatory autocreated protected multiple VERSION < 'constraint1', 'constraint2' "
                     + " queryops '=, <>, <, <=, >, >=, LIKE' nofulltext noqueryorder "
                     + "+ ns:node (ns:reqType1, ns:reqType2) = ns:defaultType mandatory autocreated protected sns version";
        importer.importFrom(cnd, problems, "string");
    }

    @Test
    public void shouldImportCndThatHasNoChildren() {
        String cnd = "<ns = 'http://namespace.com/ns'>\n"
                     + "<ex = 'http://namespace.com/ex'>\n"
                     + "[ns:NodeType] > ns:ParentType1, ns:ParentType2 abstract orderable mixin noquery primaryitem ex:property\n"
                     + "- ex:property (STRING) = 'default1', 'default2' mandatory autocreated protected multiple VERSION < 'constraint1', 'constraint2'\n"
                     + " queryops '=, <>, <, <=, >, >=, LIKE' nofulltext noqueryorder";
        importer.importFrom(cnd, problems, "string");
    }

    @Test
    public void shouldImportJcrBuiltinNodeTypesForJSR170() throws Exception {
        importer.importFrom(openCndFile("jcr-builtins-170.cnd"), problems);
        if (problems.size() != 0) printProblems();
        registerImportedNamespaces();
        assertThat(problems.size(), is(0));

        // [nt:base]
        // - jcr:primaryType (name) mandatory autocreated protected compute
        // - jcr:mixinTypes (name) protected multiple compute
        assertNodeType("nt:base", NO_SUPERTYPES, NO_PRIMARY_NAME, NodeOptions.Queryable);
        assertProperty("nt:base", "jcr:primaryType", "Name", NO_DEFAULTS, new PropertyOptions[] {PropertyOptions.Mandatory,
            PropertyOptions.Autocreated, PropertyOptions.Protected, PropertyOptions.FullTextSearchable,
            PropertyOptions.QueryOrderable}, OnParentVersion.Compute);
        assertProperty("nt:base",
                       "jcr:mixinTypes",
                       "Name",
                       NO_DEFAULTS,
                       new PropertyOptions[] {PropertyOptions.Multiple, PropertyOptions.Protected,
                           PropertyOptions.FullTextSearchable, PropertyOptions.QueryOrderable},
                       OnParentVersion.Compute);

        // [nt:unstructured]
        // orderable
        // - * (undefined) multiple
        // - * (undefined)
        // + * (nt:base) = nt:unstructured multiple version
        assertNodeType("nt:unstructured", NO_SUPERTYPES, NO_PRIMARY_NAME, NodeOptions.Ordered, NodeOptions.Queryable);
        assertProperty("nt:unstructured",
                       "*",
                       "Undefined",
                       NO_DEFAULTS,
                       PropertyOptions.Multiple,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);
        // We should test for this, but we'd have to rewrite node() to look more like
        // RepositoryNodeTypeManager.findChildNodeDefinition
        // assertProperty("nt:unstructured", "*", "Undefined", NO_DEFAULTS);
        assertChild("nt:unstructured",
                    "*",
                    "nt:base",
                    "nt:unstructured",
                    OnParentVersion.Version,
                    ChildOptions.Multiple,
                    ChildOptions.Sns);

        // [mix:referenceable]
        // mixin
        // - jcr:uuid (string) mandatory autocreated protected initialize
        assertNodeType("mix:referenceable", NO_SUPERTYPES, NO_PRIMARY_NAME, NodeOptions.Mixin, NodeOptions.Queryable);
        assertProperty("mix:referenceable",
                       "jcr:uuid",
                       "String",
                       NO_DEFAULTS,
                       OnParentVersion.Initialize,
                       PropertyOptions.Mandatory,
                       PropertyOptions.Autocreated,
                       PropertyOptions.Protected,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);

        // [mix:lockable]
        // mixin
        // - jcr:lockOwner (string) protected ignore
        // - jcr:lockIsDeep (boolean) protected ignore
        assertNodeType("mix:lockable",
                       new String[] {"mix:referenceable"},
                       NO_PRIMARY_NAME,
                       NodeOptions.Mixin,
                       NodeOptions.Queryable);
        assertProperty("mix:lockable",
                       "jcr:lockOwner",
                       "String",
                       NO_DEFAULTS,
                       OnParentVersion.Ignore,
                       PropertyOptions.Protected,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);
        assertProperty("mix:lockable",
                       "jcr:lockIsDeep",
                       "Boolean",
                       NO_DEFAULTS,
                       OnParentVersion.Ignore,
                       PropertyOptions.Protected,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);

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
        assertNodeType("nt:propertyDefinition", NO_SUPERTYPES, NO_PRIMARY_NAME, NodeOptions.Queryable);
        assertProperty("nt:propertyDefinition",
                       "jcr:name",
                       "Name",
                       NO_DEFAULTS,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);
        assertProperty("nt:propertyDefinition",
                       "jcr:autoCreated",
                       "Boolean",
                       NO_DEFAULTS,
                       PropertyOptions.Mandatory,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);
        assertProperty("nt:propertyDefinition",
                       "jcr:mandatory",
                       "Boolean",
                       NO_DEFAULTS,
                       PropertyOptions.Mandatory,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);
        assertProperty("nt:propertyDefinition", "jcr:onParentVersion", "String", NO_DEFAULTS, new PropertyOptions[] {
            PropertyOptions.Mandatory, PropertyOptions.FullTextSearchable, PropertyOptions.QueryOrderable}, null, new String[] {
            "COPY", "VERSION", "INITIALIZE", "COMPUTE", "IGNORE", "ABORT"});
        assertProperty("nt:propertyDefinition",
                       "jcr:protected",
                       "Boolean",
                       NO_DEFAULTS,
                       PropertyOptions.Mandatory,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);
        assertProperty("nt:propertyDefinition", "jcr:requiredType", "String", NO_DEFAULTS, new PropertyOptions[] {
            PropertyOptions.Mandatory, PropertyOptions.FullTextSearchable, PropertyOptions.QueryOrderable}, null, new String[] {
            "STRING", "BINARY", "LONG", "DOUBLE", "BOOLEAN", "DATE", "NAME", "PATH", "REFERENCE", "UNDEFINED"});
        assertProperty("nt:propertyDefinition",
                       "jcr:valueConstraints",
                       "String",
                       NO_DEFAULTS,
                       PropertyOptions.Multiple,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);
        assertProperty("nt:propertyDefinition",
                       "jcr:defaultValues",
                       "Undefined",
                       NO_DEFAULTS,
                       PropertyOptions.Multiple,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);
        assertProperty("nt:propertyDefinition",
                       "jcr:multiple",
                       "Boolean",
                       NO_DEFAULTS,
                       PropertyOptions.Mandatory,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);
    }

    @Test
    public void shouldImportJcrBuiltinNodeTypesForJSR283() throws Exception {
        importer.importFrom(openCndFile("jcr-builtins-283-early-draft.cnd"), problems);
        if (problems.size() != 0) printProblems();
        registerImportedNamespaces();
        assertThat(problems.size(), is(0));

        // [nt:base]
        // - jcr:primaryType (name) mandatory autocreated protected compute
        // - jcr:mixinTypes (name) protected multiple compute
        assertNodeType("nt:base", new String[] {"mode:defined"}, NO_PRIMARY_NAME, NodeOptions.Abstract, NodeOptions.Queryable);
        assertProperty("nt:base", "jcr:primaryType", "Name", NO_DEFAULTS, new PropertyOptions[] {PropertyOptions.Mandatory,
            PropertyOptions.Autocreated, PropertyOptions.Protected, PropertyOptions.FullTextSearchable,
            PropertyOptions.QueryOrderable}, OnParentVersion.Compute);
        assertProperty("nt:base",
                       "jcr:mixinTypes",
                       "Name",
                       NO_DEFAULTS,
                       new PropertyOptions[] {PropertyOptions.Multiple, PropertyOptions.Protected,
                           PropertyOptions.FullTextSearchable, PropertyOptions.QueryOrderable},
                       OnParentVersion.Compute);

        // [nt:unstructured]
        // orderable
        // - * (undefined) multiple
        // - * (undefined)
        // + * (nt:base) = nt:unstructured multiple version
        assertNodeType("nt:unstructured", NO_SUPERTYPES, NO_PRIMARY_NAME, NodeOptions.Ordered, NodeOptions.Queryable);
        assertProperty("nt:unstructured",
                       "*",
                       "Undefined",
                       NO_DEFAULTS,
                       PropertyOptions.Multiple,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);
        // We should test for this, but we'd have to rewrite node() to look more like
        // RepositoryNodeTypeManager.findChildNodeDefinition
        // assertProperty("nt:unstructured", "*", "Undefined", NO_DEFAULTS);
        assertChild("nt:unstructured",
                    "*",
                    "nt:base",
                    "nt:unstructured",
                    OnParentVersion.Version,
                    ChildOptions.Multiple,
                    ChildOptions.Sns);

        // [mix:referenceable]
        // mixin
        // - jcr:uuid (string) mandatory autocreated protected initialize
        assertNodeType("mix:referenceable", NO_SUPERTYPES, NO_PRIMARY_NAME, NodeOptions.Mixin, NodeOptions.Queryable);
        assertProperty("mix:referenceable",
                       "jcr:uuid",
                       "String",
                       NO_DEFAULTS,
                       OnParentVersion.Initialize,
                       PropertyOptions.Mandatory,
                       PropertyOptions.Autocreated,
                       PropertyOptions.Protected,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);

        // [mix:lockable]
        // mixin
        // - jcr:lockOwner (string) protected ignore
        // - jcr:lockIsDeep (boolean) protected ignore
        assertNodeType("mix:lockable", NO_SUPERTYPES, NO_PRIMARY_NAME, NodeOptions.Mixin, NodeOptions.Queryable);
        assertProperty("mix:lockable",
                       "jcr:lockOwner",
                       "String",
                       NO_DEFAULTS,
                       OnParentVersion.Ignore,
                       PropertyOptions.Protected,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);
        assertProperty("mix:lockable",
                       "jcr:lockIsDeep",
                       "Boolean",
                       NO_DEFAULTS,
                       OnParentVersion.Ignore,
                       PropertyOptions.Protected,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);

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
        assertNodeType("nt:propertyDefinition", NO_SUPERTYPES, NO_PRIMARY_NAME, NodeOptions.Queryable);
        assertProperty("nt:propertyDefinition",
                       "jcr:name",
                       "Name",
                       NO_DEFAULTS,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);
        assertProperty("nt:propertyDefinition",
                       "jcr:autoCreated",
                       "Boolean",
                       NO_DEFAULTS,
                       PropertyOptions.Mandatory,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);
        assertProperty("nt:propertyDefinition",
                       "jcr:mandatory",
                       "Boolean",
                       NO_DEFAULTS,
                       PropertyOptions.Mandatory,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);
        assertProperty("nt:propertyDefinition", "jcr:onParentVersion", "String", NO_DEFAULTS, new PropertyOptions[] {
            PropertyOptions.Mandatory, PropertyOptions.FullTextSearchable, PropertyOptions.QueryOrderable}, null, new String[] {
            "COPY", "VERSION", "INITIALIZE", "COMPUTE", "IGNORE", "ABORT"});
        assertProperty("nt:propertyDefinition",
                       "jcr:protected",
                       "Boolean",
                       NO_DEFAULTS,
                       PropertyOptions.Mandatory,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);
        assertProperty("nt:propertyDefinition", "jcr:requiredType", "String", NO_DEFAULTS, new PropertyOptions[] {
            PropertyOptions.Mandatory, PropertyOptions.FullTextSearchable, PropertyOptions.QueryOrderable}, null, new String[] {
            "STRING", "BINARY", "LONG", "DOUBLE", "BOOLEAN", "DATE", "NAME", "PATH", "REFERENCE", "UNDEFINED"});
        assertProperty("nt:propertyDefinition",
                       "jcr:valueConstraints",
                       "String",
                       NO_DEFAULTS,
                       PropertyOptions.Multiple,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);
        assertProperty("nt:propertyDefinition",
                       "jcr:defaultValues",
                       "Undefined",
                       NO_DEFAULTS,
                       PropertyOptions.Multiple,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);
        assertProperty("nt:propertyDefinition",
                       "jcr:multiple",
                       "Boolean",
                       NO_DEFAULTS,
                       PropertyOptions.Mandatory,
                       PropertyOptions.FullTextSearchable,
                       PropertyOptions.QueryOrderable);
    }

    @Test
    public void shouldImportBuiltInNodeTypes() throws Exception {
        importer.importBuiltIns(problems);
        if (problems.size() != 0) printProblems();
        assertThat(problems.size(), is(0));

        // Verify a select few from the JCR and ModeShape builtin types ...
        registerImportedNamespaces();
        assertNodeType("nt:base", new String[] {}, NO_PRIMARY_NAME, NodeOptions.Abstract, NodeOptions.Queryable);
        assertNodeType("mode:root",
                       new String[] {"nt:base", "mix:referenceable"},
                       NO_PRIMARY_NAME,
                       NodeOptions.Queryable,
                       NodeOptions.Ordered);
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
        registerImportedNamespaces();
        assertThat(problems.size(), is(0));
        assertNodeType("relational:catalog",
                       new String[] {"nt:unstructured", "relational:relationalEntity"},
                       NO_PRIMARY_NAME,
                       NodeOptions.Queryable,
                       NodeOptions.Ordered);
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

    protected void registerImportedNamespaces() {
        for (NamespaceRegistry.Namespace ns : importer.getNamespaces()) {
            context.getNamespaceRegistry().register(ns.getPrefix(), ns.getNamespaceUri());
        }
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
        Multiple,
        Sns
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

    protected int opv( OnParentVersion onParentVersioning ) {
        int opv = OnParentVersionAction.COPY;
        if (onParentVersioning != null) {
            switch (onParentVersioning) {
                case Abort:
                    opv = OnParentVersionAction.ABORT;
                    break;
                case Compute:
                    opv = OnParentVersionAction.COMPUTE;
                    break;
                case Copy:
                    opv = OnParentVersionAction.COPY;
                    break;
                case Ignore:
                    opv = OnParentVersionAction.IGNORE;
                    break;
                case Initialize:
                    opv = OnParentVersionAction.INITIALIZE;
                    break;
                case Version:
                    opv = OnParentVersionAction.VERSION;
                    break;
            }
        }
        return opv;
    }

    protected NodeTypeDefinition defn( String name ) {
        NodeTypeDefinition result = null;
        for (NodeTypeDefinition defn : importer.getNodeTypeDefinitions()) {
            if (defn.getName().equals(name)) {
                result = defn;
                break;
            }
        }
        assertThat("Failed to find node type definition \"" + name + "\"", result, is(notNullValue()));
        return result;
    }

    protected PropertyDefinition propDefn( NodeTypeDefinition nodeType,
                                           String name ) {
        for (PropertyDefinition defn : nodeType.getDeclaredPropertyDefinitions()) {
            if (defn.getName().equals(name)) return defn;
        }
        assertThat("Failed to find property type definition \"" + name + "\"", false, is(true));
        return null;
    }

    protected NodeDefinition childDefn( NodeTypeDefinition nodeType,
                                        String name ) {
        for (NodeDefinition defn : nodeType.getDeclaredChildNodeDefinitions()) {
            if (defn.getName().equals(name)) return defn;
        }
        assertThat("Failed to find child node definition \"" + name + "\"", false, is(true));
        return null;
    }

    protected void assertNodeType( String name,
                                   String[] superTypes,
                                   String primaryItemName,
                                   NodeOptions... nodeOptions ) {
        Set<NodeOptions> options = new HashSet<NodeOptions>();
        for (NodeOptions option : nodeOptions)
            options.add(option);

        NodeTypeDefinition defn = defn(name);
        assertThat(defn.getName(), is(name));
        assertThat(defn.isAbstract(), is(options.contains(NodeOptions.Abstract)));
        assertThat(defn.hasOrderableChildNodes(), is(options.contains(NodeOptions.Ordered)));
        assertThat(defn.isMixin(), is(options.contains(NodeOptions.Mixin)));
        assertThat(defn.isQueryable(), is(options.contains(NodeOptions.Queryable)));
        assertThat(defn.getPrimaryItemName(), is(primaryItemName));
        String[] supertypeNames = defn.getDeclaredSupertypeNames();
        assertThat(supertypeNames, is(superTypes));
    }

    protected void assertProperty( String nodeTypeName,
                                   String propertyName,
                                   String requiredType,
                                   String[] defaultValues,
                                   PropertyOptions... propertyOptions ) throws RepositoryException {
        assertProperty(nodeTypeName, propertyName, requiredType, defaultValues, propertyOptions, null);
    }

    protected void assertProperty( String nodeTypeName,
                                   String propertyName,
                                   String requiredType,
                                   String[] defaultValues,
                                   OnParentVersion onParentVersion,
                                   PropertyOptions... propertyOptions ) throws RepositoryException {
        assertProperty(nodeTypeName, propertyName, requiredType, defaultValues, propertyOptions, onParentVersion);
    }

    protected int jcrPropertyType( String typeName ) {
        org.modeshape.jcr.value.PropertyType type = org.modeshape.jcr.value.PropertyType.valueFor(typeName.toLowerCase());
        return PropertyTypeUtil.jcrPropertyTypeFor(type);
    }

    protected void assertProperty( String nodeTypeName,
                                   String propertyName,
                                   String requiredType,
                                   String[] defaultValues,
                                   PropertyOptions[] propertyOptions,
                                   OnParentVersion onParentVersioning,
                                   String... valueConstraints ) throws RepositoryException {
        Set<PropertyOptions> options = new HashSet<PropertyOptions>();
        for (PropertyOptions option : propertyOptions)
            options.add(option);

        NodeTypeDefinition defn = defn(nodeTypeName);
        PropertyDefinition propDefn = propDefn(defn, propertyName);

        assertThat(propDefn.getName(), is(propertyName));
        assertThat(propDefn.getRequiredType(), is(jcrPropertyType(requiredType)));
        assertThat(propDefn.isMandatory(), is(options.contains(PropertyOptions.Mandatory)));
        assertThat(propDefn.isAutoCreated(), is(options.contains(PropertyOptions.Autocreated)));
        assertThat(propDefn.isProtected(), is(options.contains(PropertyOptions.Protected)));
        assertThat(propDefn.isMultiple(), is(options.contains(PropertyOptions.Multiple)));
        assertThat(propDefn.isFullTextSearchable(), is(options.contains(PropertyOptions.FullTextSearchable)));
        assertThat(propDefn.isQueryOrderable(), is(options.contains(PropertyOptions.QueryOrderable)));

        int opv = opv(onParentVersioning);
        assertThat(propDefn.getOnParentVersion(), is(opv));
        if (defaultValues == null || defaultValues.length == 0) {
            assertThat(propDefn.getDefaultValues(), is(nullValue()));
        } else {
            int i = 0;
            for (Value defaultValue : propDefn.getDefaultValues()) {
                assertThat(defaultValues[i++], is(defaultValue.getString()));
            }
        }
        if (valueConstraints == null || valueConstraints.length == 0) {
            assertThat(propDefn.getValueConstraints(), is(nullValue()));
        } else {
            assertThat(propDefn.getValueConstraints(), is(valueConstraints));
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

        NodeTypeDefinition defn = defn(nodeTypeName);
        NodeDefinition childDefn = childDefn(defn, childName);

        assertThat(childDefn.getName(), is(childName));
        assertThat(childDefn.getDefaultPrimaryTypeName(), is(defaultPrimaryType));
        assertThat(childDefn.isMandatory(), is(options.contains(ChildOptions.Mandatory)));
        assertThat(childDefn.isAutoCreated(), is(options.contains(ChildOptions.Autocreated)));
        assertThat(childDefn.isProtected(), is(options.contains(ChildOptions.Protected)));
        assertThat(childDefn.allowsSameNameSiblings(), is(options.contains(ChildOptions.Sns)));
        assertThat(childDefn.getOnParentVersion(), is(opv(onParentVersioning)));
        assertThat(childDefn.getRequiredPrimaryTypeNames(), is(requiredTypes));
    }
}
