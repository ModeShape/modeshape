/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.jboss.dna.cnd;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.RewriteCardinalityException;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.io.Destination;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.property.ValueFormatException;
import org.jboss.dna.graph.property.basic.LocalNamespaceRegistry;

/**
 * A class that imports the node types contained in a JCR Compact Node Definition (CND) file into graph content. The content is
 * written using the graph structured defined by JCR and the "{@code nt:nodeType}", "{@code nt:propertyDefinition}", and "{@code
 * nt:childNodeDefinition}" node types.
 * <p>
 * Although instances of this class never change their behavior and all processing is done in local contexts, {@link Destination}
 * is not thread-safe and therefore this component may not be considered thread-safe.
 * </p>
 */
@NotThreadSafe
public class CndImporter {

    private static final Set<String> VALID_PROPERTY_TYPES = Collections.unmodifiableSet(new HashSet<String>(
                                                                                                            Arrays.asList(new String[] {
                                                                                                                "STRING",
                                                                                                                "BINARY", "LONG",
                                                                                                                "DOUBLE",
                                                                                                                "BOOLEAN",
                                                                                                                "DECIMAL",
                                                                                                                "DATE", "NAME",
                                                                                                                "PATH",
                                                                                                                "REFERENCE",
                                                                                                                "WEAKREFERENCE",
                                                                                                                "URI",
                                                                                                                "UNDEFINED"})));
    private static final Set<String> VALID_ON_PARENT_VERSION = Collections.unmodifiableSet(new HashSet<String>(
                                                                                                               Arrays.asList(new String[] {
                                                                                                                   "COPY",
                                                                                                                   "VERSION",
                                                                                                                   "INITIALIZE",
                                                                                                                   "COMPUTE",
                                                                                                                   "IGNORE",
                                                                                                                   "ABORT"})));
    protected final Destination destination;
    protected final Path parentPath;
    private boolean debug = false;

    /**
     * Create a new importer that will place the content in the supplied destination under the supplied path.
     * 
     * @param destination the destination where content is to be written
     * @param parentPath the path in the destination below which the generated content is to appear
     * @throws IllegalArgumentException if either parameter is null
     */
    public CndImporter( Destination destination,
                        Path parentPath ) {
        CheckArg.isNotNull(destination, "destination");
        CheckArg.isNotNull(parentPath, "parentPath");
        this.destination = destination;
        this.parentPath = parentPath;
    }

    void setDebug( boolean value ) {
        this.debug = value;
    }

    protected ExecutionContext context() {
        return this.destination.getExecutionContext();
    }

    protected NamespaceRegistry namespaces() {
        return context().getNamespaceRegistry();
    }

    protected NameFactory nameFactory() {
        return context().getValueFactories().getNameFactory();
    }

    protected ValueFactory<String> stringFactory() {
        return context().getValueFactories().getStringFactory();
    }

    protected ValueFactory<Boolean> booleanFactory() {
        return context().getValueFactories().getBooleanFactory();
    }

    /**
     * Import the CND content from the supplied stream, placing the content into the importer's destination.
     * 
     * @param stream the stream containing the CND content
     * @param problems where any problems encountered during import should be reported
     * @param resourceName a logical name for the resource name to be used when reporting problems; may be null if there is no
     *        useful name
     * @throws IOException if there is a problem reading from the supplied stream
     */
    public void importFrom( InputStream stream,
                            Problems problems,
                            String resourceName ) throws IOException {
        CndLexer lex = new CndLexer(new CaseInsensitiveInputStream(stream));
        importFrom(lex, resourceName, problems);
    }

    /**
     * Import the CND content from the supplied stream, placing the content into the importer's destination.
     * 
     * @param content the string containing the CND content
     * @param problems where any problems encountered during import should be reported
     * @param resourceName a logical name for the resource name to be used when reporting problems; may be null if there is no
     *        useful name
     * @throws IOException if there is a problem reading from the supplied stream
     */
    public void importFrom( String content,
                            Problems problems,
                            String resourceName ) throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(content.getBytes());
        importFrom(stream, problems, resourceName);
    }

    /**
     * Import the CND content from the supplied stream, placing the content into the importer's destination.
     * 
     * @param file the file containing the CND content
     * @param problems where any problems encountered during import should be reported
     * @throws IOException if there is a problem reading from the supplied stream
     */
    public void importFrom( File file,
                            Problems problems ) throws IOException {
        CndLexer lex = new CndLexer(new CaseInsensitiveFileStream(file.getAbsolutePath()));
        importFrom(lex, file.getCanonicalPath(), problems);
    }

    protected void importFrom( CndLexer lexer,
                               String resourceName,
                               Problems problems ) {
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CndParser parser = new Parser(tokens, problems, resourceName);

        // Create a new context with our own namespace registry ...
        ImportContext context = new ImportContext(context(), problems, resourceName);
        CommonTree ast = null;
        try {
            ast = (CommonTree)parser.cnd().getTree();
        } catch (RecognitionException e) {
            // already handled by Parser, so we should not handle twice
        } catch (RewriteCardinalityException e) {
            // already handled by Parser, so we should not handle twice
        } catch (RuntimeException e) {
            problems.addError(e, CndI18n.errorImportingCndContent, (Object)resourceName, e.getMessage());
        }

        if (ast != null && problems.isEmpty()) {

            // --------------
            // Namespaces ...
            // --------------

            /* 
              NAMESPACES
               +- NODE (multiple)
                   +- PREFIX
                       +- string value
                   +- URI
                       +- string value
             */

            // Get the namespaces before we do anything else ...
            CommonTree namespaces = (CommonTree)ast.getFirstChildWithType(CndLexer.NAMESPACES);
            if (namespaces != null) {
                for (int i = 0; i != namespaces.getChildCount(); ++i) {
                    CommonTree namespace = (CommonTree)namespaces.getChild(i);
                    String prefix = namespace.getFirstChildWithType(CndLexer.PREFIX).getChild(0).getText();
                    String uri = namespace.getFirstChildWithType(CndLexer.URI).getChild(0).getText();
                    // Register the namespace ...
                    context.register(removeQuotes(prefix), removeQuotes(uri));
                }
            }

            // --------------
            // Node Types ...
            // --------------

            /*
            NODE_TYPES
             +- NODE (multiple)
                 +- NAME                                 [nt:nodeType/@jcr:name]
                     +- string value
                 +- PRIMARY_TYPE                         [nt:base/@jcr:primaryType]
                     +- string with value 'nt:nodeType'
                 +- SUPERTYPES                           [nt:nodeType/@jcr:supertypes]
                     +- string value(s)
                 +- IS_ABSTRACT                          [nt:nodeType/@jcr:isAbstract]
                     +- string containing boolean value (or false if not present)
                 +- HAS_ORDERABLE_CHILD_NODES            [nt:nodeType/@jcr:hasOrderableChildNodes]
                     +- string containing boolean value (or false if not present)
                 +- IS_MIXIN                             [nt:nodeType/@jcr:isMixin]
                     +- string containing boolean value (or false if not present)
                 +- IS_QUERYABLE                         [nt:nodeType/@jcr:isQueryable]
                     +- string containing boolean value (or true if not present)
                 +- PRIMARY_ITEM_NAME                    [nt:nodeType/@jcr:primaryItemName]
                     +- string containing string value
                 +- PROPERTY_DEFINITION                  [nt:nodeType/@jcr:propertyDefinition]
                     +- NODE (multiple)
                         +- NAME                         [nt:propertyDefinition/@jcr:name]
                             +- string value
                         +- PRIMARY_TYPE                 [nt:base/@jcr:primaryType]
                             +- string with value 'nt:propertyDefinition'
                         +- REQUIRED_TYPE                [nt:propertyDefinition/@jcr:propertyType]
                             +- string value (limited to one of the predefined types)
                         +- DEFAULT_VALUES               [nt:propertyDefinition/@jcr:defaultValues]
                             +- string value(s)
                         +- MULTIPLE                     [nt:propertyDefinition/@jcr:multiple]
                             +- string containing boolean value (or false if not present)
                         +- MANDATORY                    [nt:propertyDefinition/@jcr:mandatory]
                             +- string containing boolean value (or false if not present)
                         +- AUTO_CREATED                 [nt:propertyDefinition/@jcr:autoCreated]
                             +- string containing boolean value (or false if not present)
                         +- PROTECTED                    [nt:propertyDefinition/@jcr:protected]
                             +- string containing boolean value (or false if not present)
                         +- ON_PARENT_VERSION            [nt:propertyDefinition/@jcr:onParentVersion]
                             +- string value (limited to one of the predefined literal values)
                         +- QUERY_OPERATORS              
                             +- string value (containing a comma-separated list of operator literals)
                         +- IS_FULL_TEXT_SEARCHABLE      [nt:propertyDefinition/@jcr:isFullTextSearchable]
                             +- string containing boolean value (or true if not present)
                         +- IS_QUERY_ORDERABLE           [nt:propertyDefinition/@jcr:isQueryOrderable]
                             +- string containing boolean value (or true if not present)
                         +- VALUE_CONSTRAINTS            [nt:propertyDefinition/@jcr:valueConstraints]
                             +- string value(s)
                 +- CHILD_NODE_DEFINITION                [nt:nodeType/@jcr:childNodeDefinition]
                     +- NODE (multiple)
                         +- NAME                         [nt:childNodeDefinition/@jcr:name]
                             +- string value
                         +- PRIMARY_TYPE                 [nt:base/@jcr:primaryType]
                             +- string with value 'nt:childNodeDefinition'
                         +- REQUIRED_PRIMARY_TYPES       [nt:childNodeDefinition/@jcr:requiredPrimaryTypes]
                             +- string values (limited to names)
                         +- DEFAULT_PRIMARY_TYPE         [nt:childNodeDefinition/@jcr:defaultPrimaryType]
                             +- string value (limited to a name)
                         +- MANDATORY                    [nt:childNodeDefinition/@jcr:mandatory]
                             +- string containing boolean value (or false if not present)
                         +- AUTO_CREATED                 [nt:childNodeDefinition/@jcr:autoCreated]
                             +- string containing boolean value (or false if not present)
                         +- PROTECTED                    [nt:childNodeDefinition/@jcr:protected]
                             +- string containing boolean value (or false if not present)
                         +- SAME_NAME_SIBLINGS           [nt:childNodeDefinition/@jcr:sameNameSiblings]
                             +- string containing boolean value (or false if not present)
                         +- ON_PARENT_VERSION            [nt:childNodeDefinition/@jcr:onParentVersion]
                             +- string value (limited to one of the predefined literal values)
            */

            // Get the node types ...
            CommonTree nodeTypes = (CommonTree)ast.getFirstChildWithType(CndLexer.NODE_TYPES);
            if (nodeTypes != null) {
                int numNodeTypes = 0;
                // Walk each of the nodes underneath the NODE_TYPES parent node ...
                for (int i = 0; i != nodeTypes.getChildCount(); ++i) {
                    CommonTree nodeType = (CommonTree)nodeTypes.getChild(i);
                    if (this.debug) System.out.println(nodeType.toStringTree());
                    Path nodeTypePath = context.createNodeType(nodeType, parentPath);
                    if (nodeTypePath == null) continue;
                    ++numNodeTypes;

                    CommonTree propertyDefinitions = (CommonTree)nodeType.getFirstChildWithType(CndLexer.PROPERTY_DEFINITION);
                    if (propertyDefinitions != null) {
                        // Walk each of the nodes under PROPERTY_DEFINITION ...
                        for (int j = 0; j != propertyDefinitions.getChildCount(); ++j) {
                            CommonTree propDefn = (CommonTree)propertyDefinitions.getChild(j);
                            context.createPropertyDefinition(propDefn, nodeTypePath);
                        }
                    }

                    CommonTree childNodeDefinitions = (CommonTree)nodeType.getFirstChildWithType(CndLexer.CHILD_NODE_DEFINITION);
                    if (childNodeDefinitions != null) {
                        // Walk each of the nodes under CHILD_NODE_DEFINITION ...
                        for (int j = 0; j != childNodeDefinitions.getChildCount(); ++j) {
                            CommonTree childDefn = (CommonTree)childNodeDefinitions.getChild(j);
                            context.createChildDefinition(childDefn, nodeTypePath);
                        }
                    }
                }

                // Submit the destination
                destination.submit();
            }
        }
    }

    protected final String removeQuotes( String text ) {
        // Remove leading and trailing quotes, if there are any ...
        return text.replaceFirst("^['\"]+", "").replaceAll("['\"]+$", "");
    }

    /**
     * Utility class that uses a context with a local namespace registry, along with the problems and resource name.
     */
    protected final class ImportContext {
        private final ExecutionContext context;
        private final ExecutionContext originalContext;
        private final Problems problems;
        private final String resourceName;

        protected ImportContext( ExecutionContext context,
                                 Problems problems,
                                 String resourceName ) {
            // Create a context that has a local namespace registry
            NamespaceRegistry localNamespaces = new LocalNamespaceRegistry(context.getNamespaceRegistry());
            this.originalContext = context;
            this.context = context.with(localNamespaces);
            this.problems = problems;
            this.resourceName = resourceName;
        }

        protected ExecutionContext context() {
            return this.context;
        }

        protected void register( String prefix,
                                 String uri ) {
            // Register it in the local registry with the supplied prefix ...
            context.getNamespaceRegistry().register(prefix, uri);

            // See if it is already registered in the original context ...
            NamespaceRegistry registry = originalContext.getNamespaceRegistry();
            if (!registry.isRegisteredNamespaceUri(uri)) {
                // It is not, so register it ...
                registry.register(prefix, uri);
            }
        }

        protected NameFactory nameFactory() {
            return this.context.getValueFactories().getNameFactory();
        }

        protected PathFactory pathFactory() {
            return this.context.getValueFactories().getPathFactory();
        }

        protected ValueFactory<String> stringFactory() {
            return this.context.getValueFactories().getStringFactory();
        }

        protected ValueFactory<Boolean> booleanFactory() {
            return this.context.getValueFactories().getBooleanFactory();
        }

        protected void recordError( CommonTree node,
                                    I18n msg,
                                    Object... params ) {
            String location = CndI18n.locationFromLineNumberAndCharacter.text(node.getLine(), node.getCharPositionInLine());
            problems.addError(msg, resourceName, location, params);
        }

        protected void recordError( Throwable throwable,
                                    CommonTree node,
                                    I18n msg,
                                    Object... params ) {
            String location = CndI18n.locationFromLineNumberAndCharacter.text(node.getLine(), node.getCharPositionInLine());
            problems.addError(throwable, msg, resourceName, location, params);
        }

        protected Name nameFrom( CommonTree node,
                                 int childType ) {
            CommonTree childNode = (CommonTree)node.getFirstChildWithType(childType);
            if (childNode != null && childNode.getChildCount() > 0) {
                CommonTree textNode = (CommonTree)childNode.getChild(0);
                if (textNode.getToken().getTokenIndex() < 0) return null;
                String text = removeQuotes(childNode.getChild(0).getText());
                try {
                    return nameFactory().create(text);
                } catch (ValueFormatException e) {
                    recordError(e, node, CndI18n.expectedValidNameLiteral, text);
                }
            }
            return null;
        }

        protected Name[] namesFrom( CommonTree node,
                                    int childType ) {
            CommonTree childNode = (CommonTree)node.getFirstChildWithType(childType);
            if (childNode != null && childNode.getChildCount() > 0) {
                List<Name> names = new ArrayList<Name>();
                for (int i = 0; i != childNode.getChildCount(); ++i) {
                    String text = removeQuotes(childNode.getChild(i).getText());
                    try {
                        names.add(nameFactory().create(text));
                    } catch (ValueFormatException e) {
                        recordError(e, node, CndI18n.expectedValidNameLiteral, text);
                    }
                }
                return names.toArray(new Name[names.size()]);
            }
            return new Name[] {};
        }

        protected String stringFrom( CommonTree node,
                                     int childType ) {
            CommonTree childNode = (CommonTree)node.getFirstChildWithType(childType);
            if (childNode != null && childNode.getChildCount() > 0) {
                String text = removeQuotes(childNode.getChild(0).getText().trim());
                try {
                    return stringFactory().create(text);
                } catch (ValueFormatException e) {
                    recordError(e, node, CndI18n.expectedStringLiteral, text);
                }
            }
            return null;
        }

        protected String[] stringsFrom( CommonTree node,
                                        int childType ) {
            CommonTree childNode = (CommonTree)node.getFirstChildWithType(childType);
            if (childNode != null && childNode.getChildCount() > 0) {
                List<String> names = new ArrayList<String>();
                for (int i = 0; i != childNode.getChildCount(); ++i) {
                    String text = removeQuotes(childNode.getChild(i).getText().trim());
                    try {
                        names.add(stringFactory().create(text));
                    } catch (ValueFormatException e) {
                        recordError(e, node, CndI18n.expectedStringLiteral, text);
                    }
                }
                return names.toArray(new String[names.size()]);
            }
            return new String[] {};
        }

        protected boolean booleanFrom( CommonTree node,
                                       int childType,
                                       boolean defaultValue ) {
            CommonTree childNode = (CommonTree)node.getFirstChildWithType(childType);
            if (childNode != null && childNode.getChildCount() > 0) {
                String text = removeQuotes(childNode.getChild(0).getText());
                try {
                    return booleanFactory().create(text);
                } catch (ValueFormatException e) {
                    recordError(e, node, CndI18n.expectedBooleanLiteral, text);
                }
            }
            return defaultValue;
        }

        protected QueryOperator[] queryOperatorsFrom( CommonTree node,
                                                      int childType ) {
            String text = stringFrom(node, childType);
            if (text != null) {
                String[] literals = text.split(",");
                if (literals.length != 0) {
                    Set<QueryOperator> operators = new HashSet<QueryOperator>();
                    for (String literal : literals) {
                        literal = literal.trim();
                        if (literal.length() == 0) continue;
                        QueryOperator operator = QueryOperator.forText(literal);
                        if (operator != null) {
                            operators.add(operator);
                        } else {
                            recordError(node, CndI18n.expectedValidQueryOperator, literal);
                        }
                    }
                    return operators.toArray(new QueryOperator[operators.size()]);
                }
            }
            return new QueryOperator[] {};
        }

        protected String propertyTypeNameFrom( CommonTree node,
                                               int childType ) {
            String text = stringFrom(node, childType);
            if ("*".equals(text)) text = "undefined";
            String upperText = text.toUpperCase();
            if (!VALID_PROPERTY_TYPES.contains(upperText)) {
                recordError(node, CndI18n.expectedValidPropertyTypeName, text, VALID_PROPERTY_TYPES);
                return null;
            }
            return upperText;
        }

        protected String onParentVersionFrom( CommonTree node,
                                              int childType ) {
            String text = stringFrom(node, childType);
            if (text == null) return "COPY";
            String upperText = text.toUpperCase();
            if (!VALID_ON_PARENT_VERSION.contains(upperText)) {
                recordError(node, CndI18n.expectedValidOnParentVersion, text, VALID_ON_PARENT_VERSION);
                return null;
            }
            return upperText;
        }

        protected Path createNodeType( CommonTree nodeType,
                                       Path parentPath ) {
            Name name = nameFrom(nodeType, CndLexer.NAME);
            Name[] supertypes = namesFrom(nodeType, CndLexer.SUPERTYPES);
            boolean isAbstract = booleanFrom(nodeType, CndLexer.IS_ABSTRACT, false);
            boolean hasOrderableChildNodes = booleanFrom(nodeType, CndLexer.HAS_ORDERABLE_CHILD_NODES, false);
            boolean isMixin = booleanFrom(nodeType, CndLexer.IS_MIXIN, false);
            boolean isQueryable = booleanFrom(nodeType, CndLexer.IS_QUERYABLE, true);
            Name primaryItemName = nameFrom(nodeType, CndLexer.PRIMARY_ITEM_NAME);

            if (primaryItemName == null) {
                // See if one of the property definitions is marked as the primary ...
                CommonTree propertyDefinitions = (CommonTree)nodeType.getFirstChildWithType(CndLexer.PROPERTY_DEFINITION);
                if (propertyDefinitions != null) {
                    // Walk each of the nodes under PROPERTY_DEFINITION ...
                    for (int j = 0; j != propertyDefinitions.getChildCount(); ++j) {
                        CommonTree propDefn = (CommonTree)propertyDefinitions.getChild(j);
                        if (booleanFrom(propDefn, CndLexer.IS_PRIMARY_PROPERTY, false)) {
                            primaryItemName = nameFrom(propDefn, CndLexer.NAME);
                            break;
                        }
                    }
                }
            }
            if (primaryItemName == null) {
                // See if one of the child definitions is marked as the primary ...
                CommonTree childNodeDefinitions = (CommonTree)nodeType.getFirstChildWithType(CndLexer.CHILD_NODE_DEFINITION);
                if (childNodeDefinitions != null) {
                    // Walk each of the nodes under CHILD_NODE_DEFINITION ...
                    for (int j = 0; j != childNodeDefinitions.getChildCount(); ++j) {
                        CommonTree childDefn = (CommonTree)childNodeDefinitions.getChild(j);
                        if (booleanFrom(childDefn, CndLexer.IS_PRIMARY_PROPERTY, false)) {
                            primaryItemName = nameFrom(childDefn, CndLexer.NAME);
                            break;
                        }
                    }
                }
            }

            // Create the node for the node type ...
            if (name == null) return null;
            Path path = pathFactory().create(parentPath, name);

            PropertyFactory factory = context.getPropertyFactory();
            destination.create(path,
                               factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.NODE_TYPE),
                               factory.create(JcrLexicon.SUPERTYPES, (Object[])supertypes),
                               factory.create(JcrLexicon.IS_ABSTRACT, isAbstract),
                               factory.create(JcrLexicon.HAS_ORDERABLE_CHILD_NODES, hasOrderableChildNodes),
                               factory.create(JcrLexicon.IS_MIXIN, isMixin),
                               factory.create(JcrLexicon.IS_QUERYABLE, isQueryable),
                               factory.create(JcrLexicon.NODE_TYPE_NAME, name),
                               factory.create(JcrLexicon.PRIMARY_ITEM_NAME, primaryItemName));

            return path;
        }

        protected Path createPropertyDefinition( CommonTree propDefn,
                                                 Path parentPath ) {
            Name name = nameFrom(propDefn, CndLexer.NAME);
            String requiredType = propertyTypeNameFrom(propDefn, CndLexer.REQUIRED_TYPE);
            String[] defaultValues = stringsFrom(propDefn, CndLexer.DEFAULT_VALUES);
            boolean multiple = booleanFrom(propDefn, CndLexer.MULTIPLE, false);
            boolean mandatory = booleanFrom(propDefn, CndLexer.MANDATORY, false);
            boolean autoCreated = booleanFrom(propDefn, CndLexer.AUTO_CREATED, false);
            boolean isProtected = booleanFrom(propDefn, CndLexer.PROTECTED, false);
            String onParentVersion = onParentVersionFrom(propDefn, CndLexer.ON_PARENT_VERSION);
            /*QueryOperator[] queryOperators =*/queryOperatorsFrom(propDefn, CndLexer.QUERY_OPERATORS);
            boolean isFullTextSearchable = booleanFrom(propDefn, CndLexer.IS_FULL_TEXT_SEARCHABLE, true);
            boolean isQueryOrderable = booleanFrom(propDefn, CndLexer.IS_QUERY_ORDERERABLE, true);
            String[] valueConstraints = stringsFrom(propDefn, CndLexer.VALUE_CONSTRAINTS);

            // Create the node for the node type ...
            if (name == null) return null;
            Path path = pathFactory().create(parentPath, JcrLexicon.PROPERTY_DEFINITION);

            PropertyFactory factory = context.getPropertyFactory();
            destination.create(path,
                               factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.PROPERTY_DEFINITION),
                               factory.create(JcrLexicon.REQUIRED_TYPE, requiredType),
                               factory.create(JcrLexicon.DEFAULT_VALUES, (Object[])defaultValues),
                               factory.create(JcrLexicon.MULTIPLE, multiple),
                               factory.create(JcrLexicon.MANDATORY, mandatory),
                               factory.create(JcrLexicon.NAME, name),
                               factory.create(JcrLexicon.AUTO_CREATED, autoCreated),
                               factory.create(JcrLexicon.PROTECTED, isProtected),
                               factory.create(JcrLexicon.ON_PARENT_VERSION, onParentVersion),
                               // factory.create(DnaLexicon.QUERY_OPERATORS, queryOperators),
                               factory.create(JcrLexicon.IS_FULL_TEXT_SEARCHABLE, isFullTextSearchable),
                               factory.create(JcrLexicon.IS_QUERY_ORDERABLE, isQueryOrderable),
                               factory.create(JcrLexicon.VALUE_CONSTRAINTS, (Object[])valueConstraints));

            return path;
        }

        protected Path createChildDefinition( CommonTree childDefn,
                                              Path parentPath ) {
            Name name = nameFrom(childDefn, CndLexer.NAME);
            Name[] requiredPrimaryTypes = namesFrom(childDefn, CndLexer.REQUIRED_PRIMARY_TYPES);
            Name defaultPrimaryType = nameFrom(childDefn, CndLexer.DEFAULT_PRIMARY_TYPE);
            boolean mandatory = booleanFrom(childDefn, CndLexer.MANDATORY, false);
            boolean autoCreated = booleanFrom(childDefn, CndLexer.AUTO_CREATED, false);
            boolean isProtected = booleanFrom(childDefn, CndLexer.PROTECTED, false);
            String onParentVersion = onParentVersionFrom(childDefn, CndLexer.ON_PARENT_VERSION);
            boolean sameNameSiblings = booleanFrom(childDefn, CndLexer.SAME_NAME_SIBLINGS, false);

            // Create the node for the node type ...
            if (name == null) return null;
            Path path = pathFactory().create(parentPath, JcrLexicon.CHILD_NODE_DEFINITION);

            PropertyFactory factory = context.getPropertyFactory();
            destination.create(path,
                               factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.CHILD_NODE_DEFINITION),
                               factory.create(JcrLexicon.REQUIRED_PRIMARY_TYPES, (Object[])requiredPrimaryTypes),
                               factory.create(JcrLexicon.DEFAULT_PRIMARY_TYPE, defaultPrimaryType),
                               factory.create(JcrLexicon.MANDATORY, mandatory),
                               factory.create(JcrLexicon.NAME, name),
                               factory.create(JcrLexicon.AUTO_CREATED, autoCreated),
                               factory.create(JcrLexicon.PROTECTED, isProtected),
                               factory.create(JcrLexicon.ON_PARENT_VERSION, onParentVersion),
                               factory.create(JcrLexicon.SAME_NAME_SIBLINGS, sameNameSiblings));

            return path;
        }
    }

    protected class Parser extends CndParser {
        private final Problems problems;
        private final String nameOfResource;

        public Parser( TokenStream input,
                       Problems problems,
                       String nameOfResource ) {
            super(input);
            this.problems = problems;
            this.nameOfResource = nameOfResource;
        }

        @Override
        public void displayRecognitionError( String[] tokenNames,
                                             RecognitionException e ) {
            if (problems != null) {
                String hdr = getErrorHeader(e);
                String msg = getErrorMessage(e, tokenNames);
                problems.addError(CndI18n.passthrough, nameOfResource, hdr, msg);
            } else {
                super.displayRecognitionError(tokenNames, e);
            }
        }
    }

    /**
     * Specialization of an {@link ANTLRInputStream} that converts all tokens to lowercase, allowing the grammar to be
     * case-insensitive. See the <a href="http://www.antlr.org/wiki/pages/viewpage.action?pageId=1782">ANTLR documentation</a>.
     */
    protected class CaseInsensitiveInputStream extends ANTLRInputStream {
        protected CaseInsensitiveInputStream( InputStream stream ) throws IOException {
            super(stream);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.antlr.runtime.ANTLRStringStream#LA(int)
         */
        @Override
        public int LA( int i ) {
            if (i == 0) {
                return 0; // undefined
            }
            if (i < 0) {
                i++; // e.g., translate LA(-1) to use offset 0
            }

            if ((p + i - 1) >= n) {
                return CharStream.EOF;
            }
            return Character.toLowerCase(data[p + i - 1]);
        }
    }

    /**
     * Specialization of an {@link ANTLRInputStream} that converts all tokens to lowercase, allowing the grammar to be
     * case-insensitive. See the <a href="http://www.antlr.org/wiki/pages/viewpage.action?pageId=1782">ANTLR documentation</a>.
     */
    protected class CaseInsensitiveFileStream extends ANTLRFileStream {
        protected CaseInsensitiveFileStream( String fileName ) throws IOException {
            super(fileName, null);
        }

        protected CaseInsensitiveFileStream( String fileName,
                                             String encoding ) throws IOException {
            super(fileName, encoding);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.antlr.runtime.ANTLRStringStream#LA(int)
         */
        @Override
        public int LA( int i ) {
            if (i == 0) {
                return 0; // undefined
            }
            if (i < 0) {
                i++; // e.g., translate LA(-1) to use offset 0
            }

            if ((p + i - 1) >= n) {

                return CharStream.EOF;
            }
            return Character.toLowerCase(data[p + i - 1]);
        }
    }

}
