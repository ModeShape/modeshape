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

import static org.modeshape.common.text.TokenStream.ANY_VALUE;
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
import org.modeshape.common.collection.Problems;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.common.text.TokenStream;
import org.modeshape.common.text.TokenStream.Tokenizer;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.IoUtil;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.io.Destination;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFormatException;

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

    protected final List<String> VALID_PROPERTY_TYPES = Collections.unmodifiableList(Arrays.asList(new String[] {"STRING",
        "BINARY", "LONG", "DOUBLE", "BOOLEAN", "DATE", "NAME", "PATH", "REFERENCE", "WEAKREFERENCE", "DECIMAL", "URI",
        "UNDEFINED", "*", "?"}));

    protected final List<String> VALID_ON_PARENT_VERSION = Collections.unmodifiableList(Arrays.asList(new String[] {"COPY",
        "VERSION", "INITIALIZE", "COMPUTE", "IGNORE", "ABORT"}));

    protected final Set<String> VALID_QUERY_OPERATORS = Collections.unmodifiableSet(new HashSet<String>(
                                                                                                        Arrays.asList(new String[] {
                                                                                                            "=", "<>", "<", "<=",
                                                                                                            ">", ">=", "LIKE"})));

    protected final Destination destination;
    protected final Path outputPath;
    protected final PropertyFactory propertyFactory;
    protected final PathFactory pathFactory;
    protected final NameFactory nameFactory;
    protected final ValueFactories valueFactories;
    protected final boolean jcr170;

    /**
     * Create a new importer that will place the content in the supplied destination under the supplied path.
     * 
     * @param destination the destination where content is to be written
     * @param parentPath the path in the destination below which the generated content is to appear
     * @param compatibleWithPreJcr2 true if this parser should accept the CND format that was used in the reference implementation
     *        prior to JCR 2.0.
     * @throws IllegalArgumentException if either parameter is null
     */
    public CndImporter( Destination destination,
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
        this.jcr170 = compatibleWithPreJcr2;
    }

    /**
     * Create a new importer that will place the content in the supplied destination under the supplied path. This parser will
     * accept the CND format that was used in the reference implementation prior to JCR 2.0.
     * 
     * @param destination the destination where content is to be written
     * @param parentPath the path in the destination below which the generated content is to appear
     * @throws IllegalArgumentException if either parameter is null
     */
    public CndImporter( Destination destination,
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
     */
    public void importFrom( InputStream stream,
                            Problems problems,
                            String resourceName ) throws IOException {
        importFrom(IoUtil.read(stream), problems, resourceName);
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
        importFrom(IoUtil.read(file), problems, file.getCanonicalPath());
    }

    /**
     * Import the CND content from the supplied stream, placing the content into the importer's destination.
     * 
     * @param content the string containing the CND content
     * @param problems where any problems encountered during import should be reported
     * @param resourceName a logical name for the resource name to be used when reporting problems; may be null if there is no
     *        useful name
     */
    public void importFrom( String content,
                            Problems problems,
                            String resourceName ) {
        try {
            parse(content);
            destination.submit();
        } catch (RuntimeException e) {
            problems.addError(e, CndI18n.errorImportingCndContent, (Object)resourceName, e.getMessage());
        }
    }

    /**
     * Parse the CND content.
     * 
     * @param content the content
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parse( String content ) {
        Tokenizer tokenizer = new CndTokenizer(false, false);
        TokenStream tokens = new TokenStream(content, tokenizer, false);
        tokens.start();
        while (tokens.hasNext()) {
            // Keep reading while we can recognize one of the two types of statements ...
            if (tokens.matches("<", ANY_VALUE, "=", ANY_VALUE, ">")) {
                parseNamespaceMapping(tokens);
            } else if (tokens.matches("[", ANY_VALUE, "]")) {
                parseNodeTypeDefinition(tokens, outputPath);
            } else {
                Position position = tokens.previousPosition();
                throw new ParsingException(position, CndI18n.expectedNamespaceOrNodeDefinition.text(tokens.consume(),
                                                                                                    position.getLine(),
                                                                                                    position.getColumn()));
            }
        }
    }

    /**
     * Parse the namespace mapping statement that is next on the token stream.
     * 
     * @param tokens the tokens containing the namespace statement; never null
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parseNamespaceMapping( TokenStream tokens ) {
        tokens.consume('<');
        String prefix = removeQuotes(tokens.consume());
        tokens.consume('=');
        String uri = removeQuotes(tokens.consume());
        tokens.consume('>');
        // Register the namespace ...
        destination.getExecutionContext().getNamespaceRegistry().register(prefix, uri);
    }

    /**
     * Parse the node type definition that is next on the token stream.
     * 
     * @param tokens the tokens containing the node type definition; never null
     * @param path the path in the destination under which the node type definition should be stored; never null
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parseNodeTypeDefinition( TokenStream tokens,
                                            Path path ) {
        // Parse the name, and create the path and a property for the name ...
        Name name = parseNodeTypeName(tokens);
        Path nodeTypePath = pathFactory.create(path, name);
        List<Property> properties = new ArrayList<Property>();
        properties.add(propertyFactory.create(JcrLexicon.NODE_TYPE_NAME, name));
        properties.add(propertyFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.NODE_TYPE));

        // Read the (optional) supertypes ...
        List<Name> supertypes = parseSupertypes(tokens);
        properties.add(propertyFactory.create(JcrLexicon.SUPERTYPES, supertypes)); // even if empty

        // Read the node type options ...
        parseNodeTypeOptions(tokens, properties);
        destination.create(nodeTypePath, properties);

        // Parse property and child node definitions ...
        parsePropertyOrChildNodeDefinitions(tokens, nodeTypePath);
    }

    /**
     * Parse a node type name that appears next on the token stream.
     * 
     * @param tokens the tokens containing the node type name; never null
     * @return the node type name
     * @throws ParsingException if there is a problem parsing the content
     */
    protected Name parseNodeTypeName( TokenStream tokens ) {
        tokens.consume('[');
        Name name = parseName(tokens);
        tokens.consume(']');
        return name;
    }

    /**
     * Parse an optional list of supertypes if they appear next on the token stream.
     * 
     * @param tokens the tokens containing the supertype names; never null
     * @return the list of supertype names; never null, but possibly empty
     * @throws ParsingException if there is a problem parsing the content
     */
    protected List<Name> parseSupertypes( TokenStream tokens ) {
        if (tokens.canConsume('>')) {
            // There is at least one supertype ...
            return parseNameList(tokens);
        }
        return Collections.emptyList();
    }

    /**
     * Parse a list of strings, separated by commas. Any quotes surrounding the strings are removed.
     * 
     * @param tokens the tokens containing the comma-separated strings; never null
     * @return the list of string values; never null, but possibly empty
     * @throws ParsingException if there is a problem parsing the content
     */
    protected List<String> parseStringList( TokenStream tokens ) {
        List<String> strings = new ArrayList<String>();
        if (tokens.canConsume('?')) {
            // This list is variant ...
            strings.add("?");
        } else {
            // Read names until we see a ','
            do {
                strings.add(removeQuotes(tokens.consume()));
            } while (tokens.canConsume(','));
        }
        return strings;
    }

    /**
     * Parse a list of names, separated by commas. Any quotes surrounding the names are removed.
     * 
     * @param tokens the tokens containing the comma-separated strings; never null
     * @return the list of string values; never null, but possibly empty
     * @throws ParsingException if there is a problem parsing the content
     */
    protected List<Name> parseNameList( TokenStream tokens ) {
        List<Name> names = new ArrayList<Name>();
        if (!tokens.canConsume('?')) {
            // Read names until we see a ','
            do {
                names.add(parseName(tokens));
            } while (tokens.canConsume(','));
        }
        return names;
    }

    /**
     * Parse the options for the node types, including whether the node type is orderable, a mixin, abstract, whether it supports
     * querying, and which property/child node (if any) is the primary item for the node type.
     * 
     * @param tokens the tokens containing the comma-separated strings; never null
     * @param properties the list into which the properties that represent the options should be placed
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parseNodeTypeOptions( TokenStream tokens,
                                         List<Property> properties ) {
        // Set up the defaults ...
        boolean isOrderable = false;
        boolean isMixin = false;
        boolean isAbstract = false;
        boolean isQueryable = true;
        Name primaryItem = null;
        String onParentVersion = "COPY";
        while (true) {
            // Keep reading while we see a valid option ...
            if (tokens.canConsumeAnyOf("ORDERABLE", "ORD", "O")) {
                tokens.canConsume('?');
                isOrderable = true;
            } else if (tokens.canConsumeAnyOf("MIXIN", "MIX", "M")) {
                tokens.canConsume('?');
                isMixin = true;
            } else if (tokens.canConsumeAnyOf("ABSTRACT", "ABS", "A")) {
                tokens.canConsume('?');
                isAbstract = true;
            } else if (tokens.canConsumeAnyOf("NOQUERY", "NOQ")) {
                tokens.canConsume('?');
                isQueryable = false;
            } else if (tokens.canConsumeAnyOf("QUERY", "Q")) {
                tokens.canConsume('?');
                isQueryable = true;
            } else if (tokens.canConsumeAnyOf("PRIMARYITEM", "!")) {
                primaryItem = parseName(tokens);
                tokens.canConsume('?');
            } else if (tokens.matchesAnyOf(VALID_ON_PARENT_VERSION)) {
                onParentVersion = tokens.consume();
                tokens.canConsume('?');
            } else if (tokens.matches("OPV")) {
                // variant on-parent-version
                onParentVersion = tokens.consume();
                tokens.canConsume('?');
            } else {
                // No more valid options on the stream, so stop ...
                break;
            }
        }
        properties.add(propertyFactory.create(JcrLexicon.HAS_ORDERABLE_CHILD_NODES, isOrderable));
        properties.add(propertyFactory.create(JcrLexicon.IS_MIXIN, isMixin));
        properties.add(propertyFactory.create(JcrLexicon.IS_ABSTRACT, isAbstract));
        properties.add(propertyFactory.create(JcrLexicon.IS_QUERYABLE, isQueryable));
        properties.add(propertyFactory.create(JcrLexicon.ON_PARENT_VERSION, onParentVersion.toUpperCase()));
        if (primaryItem != null) {
            properties.add(propertyFactory.create(JcrLexicon.PRIMARY_ITEM_NAME, primaryItem));
        }
    }

    /**
     * Parse a node type's property or child node definitions that appear next on the token stream.
     * 
     * @param tokens the tokens containing the definitions; never null
     * @param nodeTypePath the path in the destination where the node type has been created, and under which the property and
     *        child node type definitions should be placed
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parsePropertyOrChildNodeDefinitions( TokenStream tokens,
                                                        Path nodeTypePath ) {
        while (true) {
            // Keep reading while we see a property definition or child node definition ...
            if (tokens.matches('-')) {
                parsePropertyDefinition(tokens, nodeTypePath);
            } else if (tokens.matches('+')) {
                parseChildNodeDefinition(tokens, nodeTypePath);
            } else {
                // The next token does not signal either one of these, so stop ...
                break;
            }
        }
    }

    /**
     * Parse a node type's property definition from the next tokens on the stream.
     * 
     * @param tokens the tokens containing the definition; never null
     * @param nodeTypePath the path in the destination where the node type has been created, and under which the property and
     *        child node type definitions should be placed
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parsePropertyDefinition( TokenStream tokens,
                                            Path nodeTypePath ) {
        tokens.consume('-');
        Name name = parseName(tokens);
        Path path = pathFactory.create(nodeTypePath, JcrLexicon.PROPERTY_DEFINITION);
        List<Property> properties = new ArrayList<Property>();
        properties.add(propertyFactory.create(JcrLexicon.NAME, name));
        properties.add(propertyFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.PROPERTY_DEFINITION));

        // Parse the (optional) required type ...
        parsePropertyType(tokens, properties, PropertyType.STRING.getName());

        // Parse the default values ...
        parseDefaultValues(tokens, properties);

        // Parse the property attributes ...
        parsePropertyAttributes(tokens, properties, name, path);

        // Parse the property constraints ...
        parseValueConstraints(tokens, properties);

        // Create the node in the destination ...
        destination.create(path, properties);
    }

    /**
     * Parse the property type, if a valid one appears next on the token stream.
     * 
     * @param tokens the tokens containing the definition; never null
     * @param properties the list into which the property that represents the property type should be placed
     * @param defaultPropertyType the default property type if none is actually found
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parsePropertyType( TokenStream tokens,
                                      List<Property> properties,
                                      String defaultPropertyType ) {
        if (tokens.canConsume('(')) {
            // Parse the (optional) property type ...
            String propertyType = defaultPropertyType;
            if (tokens.matchesAnyOf(VALID_PROPERTY_TYPES)) {
                propertyType = tokens.consume();
                if ("*".equals(propertyType)) propertyType = "UNDEFINED";
            }
            tokens.consume(')');
            properties.add(propertyFactory.create(JcrLexicon.REQUIRED_TYPE, propertyType.toUpperCase()));
        }
    }

    /**
     * Parse the property definition's default value, if they appear next on the token stream.
     * 
     * @param tokens the tokens containing the definition; never null
     * @param properties the list into which the property that represents the default values should be placed
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parseDefaultValues( TokenStream tokens,
                                       List<Property> properties ) {
        if (tokens.canConsume('=')) {
            List<String> defaultValues = parseStringList(tokens);
            if (!defaultValues.isEmpty()) {
                properties.add(propertyFactory.create(JcrLexicon.DEFAULT_VALUES, defaultValues));
            }
        }
    }

    /**
     * Parse the property definition's value constraints, if they appear next on the token stream.
     * 
     * @param tokens the tokens containing the definition; never null
     * @param properties the list into which the property that represents the value constraints should be placed
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parseValueConstraints( TokenStream tokens,
                                          List<Property> properties ) {
        if (tokens.canConsume('<')) {
            List<String> defaultValues = parseStringList(tokens);
            if (!defaultValues.isEmpty()) {
                properties.add(propertyFactory.create(JcrLexicon.VALUE_CONSTRAINTS, defaultValues));
            }
        }
    }

    /**
     * Parse the property definition's attributes, if they appear next on the token stream.
     * 
     * @param tokens the tokens containing the attributes; never null
     * @param properties the list into which the properties that represents the attributes should be placed
     * @param propDefnName the name of the property definition; never null
     * @param propDefnPath the path in the destination to the property definition node; never null
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parsePropertyAttributes( TokenStream tokens,
                                            List<Property> properties,
                                            Name propDefnName,
                                            Path propDefnPath ) {
        boolean autoCreated = false;
        boolean mandatory = false;
        boolean isProtected = false;
        boolean multiple = false;
        boolean isFullTextSearchable = true;
        boolean isQueryOrderable = true;
        String onParentVersion = "COPY";
        while (true) {
            if (tokens.canConsumeAnyOf("AUTOCREATED", "AUT", "A")) {
                tokens.canConsume('?');
                autoCreated = true;
            } else if (tokens.canConsumeAnyOf("MANDATORY", "MAN", "M")) {
                tokens.canConsume('?');
                mandatory = true;
            } else if (tokens.canConsumeAnyOf("PROTECTED", "PRO", "P")) {
                tokens.canConsume('?');
                isProtected = true;
            } else if (tokens.canConsumeAnyOf("MULTIPLE", "MUL", "*")) {
                tokens.canConsume('?');
                multiple = true;
            } else if (tokens.matchesAnyOf(VALID_ON_PARENT_VERSION)) {
                onParentVersion = tokens.consume();
                tokens.canConsume('?');
            } else if (tokens.matches("OPV")) {
                // variant on-parent-version
                onParentVersion = tokens.consume();
                tokens.canConsume('?');
            } else if (tokens.canConsumeAnyOf("NOFULLTEXT", "NOF")) {
                tokens.canConsume('?');
                isFullTextSearchable = false;
            } else if (tokens.canConsumeAnyOf("NOQUERYORDER", "NQORD")) {
                tokens.canConsume('?');
                isQueryOrderable = false;
            } else if (tokens.canConsumeAnyOf("QUERYOPS", "QOP")) {
                parseQueryOperators(tokens, properties);
            } else if (tokens.canConsumeAnyOf("PRIMARY", "PRI", "!")) {
                if (!jcr170) {
                    Position pos = tokens.previousPosition();
                    int line = pos.getLine();
                    int column = pos.getColumn();
                    throw new ParsingException(tokens.previousPosition(),
                                               CndI18n.primaryKeywordNotValidInJcr2CndFormat.text(line, column));
                }
                // Then this child node is considered the primary item ...
                Property primaryItem = propertyFactory.create(JcrLexicon.PRIMARY_ITEM_NAME, propDefnName);
                destination.setProperties(propDefnPath.getParent(), primaryItem);
            } else {
                break;
            }
        }
        properties.add(propertyFactory.create(JcrLexicon.AUTO_CREATED, autoCreated));
        properties.add(propertyFactory.create(JcrLexicon.MANDATORY, mandatory));
        properties.add(propertyFactory.create(JcrLexicon.PROTECTED, isProtected));
        properties.add(propertyFactory.create(JcrLexicon.ON_PARENT_VERSION, onParentVersion.toUpperCase()));
        properties.add(propertyFactory.create(JcrLexicon.MULTIPLE, multiple));
        properties.add(propertyFactory.create(JcrLexicon.IS_FULL_TEXT_SEARCHABLE, isFullTextSearchable));
        properties.add(propertyFactory.create(JcrLexicon.IS_QUERY_ORDERABLE, isQueryOrderable));
    }

    /**
     * Parse the property definition's query operators, if they appear next on the token stream.
     * 
     * @param tokens the tokens containing the definition; never null
     * @param properties the list into which the property that represents the value constraints should be placed
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parseQueryOperators( TokenStream tokens,
                                        List<Property> properties ) {
        if (tokens.canConsume('?')) {
            return;
        }
        // The query operators are expected to be enclosed in a single quote, so therefore will be a single token ...
        List<String> operators = new ArrayList<String>();
        String operatorList = removeQuotes(tokens.consume());
        // Now split this string on ',' ...
        for (String operatorValue : operatorList.split(",")) {
            String operator = operatorValue.trim();
            if (!VALID_QUERY_OPERATORS.contains(operator)) {
                throw new ParsingException(tokens.previousPosition(), CndI18n.expectedValidQueryOperator.text(operator));
            }
            operators.add(operator);
        }
        if (operators.isEmpty()) {
            operators.addAll(VALID_QUERY_OPERATORS);
        }
        properties.add(propertyFactory.create(JcrLexicon.QUERY_OPERATORS, operators));
    }

    /**
     * Parse a node type's child node definition from the next tokens on the stream.
     * 
     * @param tokens the tokens containing the definition; never null
     * @param nodeTypePath the path in the destination where the node type has been created, and under which the child node type
     *        definitions should be placed
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parseChildNodeDefinition( TokenStream tokens,
                                             Path nodeTypePath ) {
        tokens.consume('+');
        Name name = parseName(tokens);
        Path path = pathFactory.create(nodeTypePath, JcrLexicon.CHILD_NODE_DEFINITION);
        List<Property> properties = new ArrayList<Property>();
        properties.add(propertyFactory.create(JcrLexicon.NAME, name));
        properties.add(propertyFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.CHILD_NODE_DEFINITION));

        parseRequiredPrimaryTypes(tokens, properties);
        parseDefaultType(tokens, properties);
        parseNodeAttributes(tokens, properties, name, path);

        // Create the node in the destination ...
        destination.create(path, properties);
    }

    /**
     * Parse the child node definition's list of required primary types, if they appear next on the token stream.
     * 
     * @param tokens the tokens containing the definition; never null
     * @param properties the list into which the property that represents the required types should be placed
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parseRequiredPrimaryTypes( TokenStream tokens,
                                              List<Property> properties ) {
        if (tokens.canConsume('(')) {
            List<Name> requiredTypes = parseNameList(tokens);
            if (requiredTypes.isEmpty()) {
                requiredTypes.add(JcrNtLexicon.BASE);
            }
            properties.add(propertyFactory.create(JcrLexicon.REQUIRED_PRIMARY_TYPES, requiredTypes));
            tokens.consume(')');
        }
    }

    /**
     * Parse the child node definition's default type, if they appear next on the token stream.
     * 
     * @param tokens the tokens containing the definition; never null
     * @param properties the list into which the property that represents the default primary type should be placed
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parseDefaultType( TokenStream tokens,
                                     List<Property> properties ) {
        if (tokens.canConsume('=')) {
            if (!tokens.canConsume('?')) {
                Name defaultType = parseName(tokens);
                properties.add(propertyFactory.create(JcrLexicon.DEFAULT_PRIMARY_TYPE, defaultType));
            }
        }
    }

    /**
     * Parse the child node definition's attributes, if they appear next on the token stream.
     * 
     * @param tokens the tokens containing the attributes; never null
     * @param properties the list into which the properties that represents the attributes should be placed
     * @param childNodeDefnName the name of the child node definition; never null
     * @param childNodeDefnPath the path in the destination to the child node definition node; never null
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parseNodeAttributes( TokenStream tokens,
                                        List<Property> properties,
                                        Name childNodeDefnName,
                                        Path childNodeDefnPath ) {
        boolean autoCreated = false;
        boolean mandatory = false;
        boolean isProtected = false;
        boolean sns = false;
        String onParentVersion = "COPY";
        while (true) {
            if (tokens.canConsumeAnyOf("AUTOCREATED", "AUT", "A")) {
                tokens.canConsume('?');
                autoCreated = true;
            } else if (tokens.canConsumeAnyOf("MANDATORY", "MAN", "M")) {
                tokens.canConsume('?');
                mandatory = true;
            } else if (tokens.canConsumeAnyOf("PROTECTED", "PRO", "P")) {
                tokens.canConsume('?');
                isProtected = true;
            } else if (tokens.canConsumeAnyOf("SNS", "*")) { // standard JCR 2.0 keywords for SNS ...
                tokens.canConsume('?');
                sns = true;
            } else if (tokens.canConsumeAnyOf("MULTIPLE", "MUL", "*")) { // from pre-JCR 2.0 ref impl
                if (!jcr170) {
                    Position pos = tokens.previousPosition();
                    int line = pos.getLine();
                    int column = pos.getColumn();
                    throw new ParsingException(tokens.previousPosition(),
                                               CndI18n.multipleKeywordNotValidInJcr2CndFormat.text(line, column));
                }
                tokens.canConsume('?');
                sns = true;
            } else if (tokens.matchesAnyOf(VALID_ON_PARENT_VERSION)) {
                onParentVersion = tokens.consume();
                tokens.canConsume('?');
            } else if (tokens.matches("OPV")) {
                // variant on-parent-version
                onParentVersion = tokens.consume();
                tokens.canConsume('?');
            } else if (tokens.canConsumeAnyOf("PRIMARYITEM", "PRIMARY", "PRI", "!")) {
                // Then this child node is considered the primary item ...
                Property primaryItem = propertyFactory.create(JcrLexicon.PRIMARY_ITEM_NAME, childNodeDefnName);
                destination.setProperties(childNodeDefnPath.getParent(), primaryItem);
            } else {
                break;
            }
        }
        properties.add(propertyFactory.create(JcrLexicon.AUTO_CREATED, autoCreated));
        properties.add(propertyFactory.create(JcrLexicon.MANDATORY, mandatory));
        properties.add(propertyFactory.create(JcrLexicon.PROTECTED, isProtected));
        properties.add(propertyFactory.create(JcrLexicon.ON_PARENT_VERSION, onParentVersion.toUpperCase()));
        properties.add(propertyFactory.create(JcrLexicon.SAME_NAME_SIBLINGS, sns));
    }

    /**
     * Parse the name that is expected to be next on the token stream.
     * 
     * @param tokens the tokens containing the name; never null
     * @return the name; never null
     * @throws ParsingException if there is a problem parsing the content
     */
    protected Name parseName( TokenStream tokens ) {
        String value = tokens.consume();
        try {
            return nameFactory.create(removeQuotes(value));
        } catch (ValueFormatException e) {
            throw new ParsingException(tokens.previousPosition(), CndI18n.expectedValidNameLiteral.text(value));
        }
    }

    protected final String removeQuotes( String text ) {
        // Remove leading and trailing quotes, if there are any ...
        return text.replaceFirst("^['\"]+", "").replaceAll("['\"]+$", "");
    }
}
