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

import static org.modeshape.common.text.TokenStream.ANY_VALUE;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.version.OnParentVersionAction;
import org.infinispan.util.FileLookup;
import org.infinispan.util.FileLookupFactory;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.common.text.TokenStream;
import org.modeshape.common.text.TokenStream.Tokenizer;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.cache.PropertyTypeUtil;
import org.modeshape.jcr.core.ExecutionContext;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.ValueFormatException;
import org.modeshape.jcr.value.basic.LocalNamespaceRegistry;

/**
 * A class that imports the node types contained in a JCR Compact Node Definition (CND) file into {@link NodeTypeDefinition}
 * instances.
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

    protected final List<String> BUILT_INS = Collections.unmodifiableList(Arrays.asList(new String[] {
        "org/modeshape/jcr/jsr_283_builtins.cnd", "org/modeshape/jcr/modeshape_builtins.cnd"}));

    /**
     * The default flag for using vendor extensions is {@value} .
     */
    public static final boolean DEFAULT_USE_VENDOR_EXTENSIONS = true;

    /**
     * The default flag for supporting pre-JCR 2.0 CND format is {@value} .
     */
    public static final boolean DEFAULT_COMPATIBLE_WITH_PREJCR2 = true;

    /**
     * The regular expression used to capture the vendor property name and the value. The expression is "
     * <code>([^\s]+)(\s+(.*))</code>".
     */
    protected final String VENDOR_PATTERN_STRING = "([^\\s]+)(\\s+(.*))";
    protected final Pattern VENDOR_PATTERN = Pattern.compile(VENDOR_PATTERN_STRING);

    protected final ExecutionContext context;
    protected final LocalNamespaceRegistry localRegistry;
    protected final NameFactory nameFactory;
    protected final org.modeshape.jcr.value.ValueFactory<String> stringFactory;
    protected final ValueFactory valueFactory;
    protected final boolean jcr170;
    protected final List<NodeTypeDefinition> nodeTypes;

    /**
     * Create a new importer that will place the content in the supplied destination under the supplied path.
     * 
     * @param context the context in which the importing should be performed; may not be null
     * @param compatibleWithPreJcr2 true if this parser should accept the CND format that was used in the reference implementation
     *        prior to JCR 2.0.
     */
    public CndImporter( ExecutionContext context,
                        boolean compatibleWithPreJcr2 ) {
        assert context != null;
        this.localRegistry = new LocalNamespaceRegistry(context.getNamespaceRegistry());
        this.context = context.with(this.localRegistry);
        this.valueFactory = new JcrValueFactory(this.context);
        this.nameFactory = this.context.getValueFactories().getNameFactory();
        this.stringFactory = this.context.getValueFactories().getStringFactory();
        this.jcr170 = compatibleWithPreJcr2;
        this.nodeTypes = new LinkedList<NodeTypeDefinition>();
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
        } catch (RuntimeException e) {
            problems.addError(e, CndI18n.errorImportingCndContent, resourceName, e.getMessage());
        }
    }

    public void importBuiltIns( Problems problems ) throws IOException {
        for (String resource : BUILT_INS) {
            FileLookup factory = FileLookupFactory.newInstance();
            InputStream stream = factory.lookupFile(resource, Thread.currentThread().getContextClassLoader());
            if (stream == null) {
                stream = factory.lookupFile(resource, getClass().getClassLoader());
            }
            importFrom(stream, problems, resource);
        }
    }

    public Set<NamespaceRegistry.Namespace> getNamespaces() {
        return new HashSet<NamespaceRegistry.Namespace>(this.localRegistry.getLocalNamespaces());
    }

    /**
     * @return nodeTypes
     */
    public List<NodeTypeDefinition> getNodeTypeDefinitions() {
        return Collections.unmodifiableList(new ArrayList<NodeTypeDefinition>(nodeTypes));
    }

    /**
     * Parse the CND content.
     * 
     * @param content the content
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parse( String content ) {
        Tokenizer tokenizer = new CndTokenizer(false, true);
        TokenStream tokens = new TokenStream(content, tokenizer, false);
        tokens.start();
        while (tokens.hasNext()) {
            // Keep reading while we can recognize one of the two types of statements ...
            if (tokens.matches("<", ANY_VALUE, "=", ANY_VALUE, ">")) {
                parseNamespaceMapping(tokens);
            } else if (tokens.matches("[", ANY_VALUE, "]")) {
                parseNodeTypeDefinition(tokens);
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
        context.getNamespaceRegistry().register(prefix, uri);
    }

    /**
     * Parse the node type definition that is next on the token stream.
     * 
     * @param tokens the tokens containing the node type definition; never null
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parseNodeTypeDefinition( TokenStream tokens ) {
        // Parse the name, and create the path and a property for the name ...
        Name name = parseNodeTypeName(tokens);
        JcrNodeTypeTemplate nodeType = new JcrNodeTypeTemplate(context);
        try {
            nodeType.setName(string(name));
        } catch (ConstraintViolationException e) {
            assert false : "Names should always be syntactically valid";
        }

        // Read the (optional) supertypes ...
        List<Name> supertypes = parseSupertypes(tokens);
        try {
            nodeType.setDeclaredSuperTypeNames(names(supertypes));

            // Read the node type options (and vendor extensions) ...
            parseNodeTypeOptions(tokens, nodeType);

            // Parse property and child node definitions ...
            parsePropertyOrChildNodeDefinitions(tokens, nodeType);
        } catch (ConstraintViolationException e) {
            assert false : "Names should always be syntactically valid";
        }

        this.nodeTypes.add(nodeType);
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
     * @param nodeType the node type being created; may not be null
     * @throws ParsingException if there is a problem parsing the content
     * @throws ConstraintViolationException not expected
     */
    protected void parseNodeTypeOptions( TokenStream tokens,
                                         JcrNodeTypeTemplate nodeType ) throws ConstraintViolationException {
        // Set up the defaults ...
        boolean isOrderable = false;
        boolean isMixin = false;
        boolean isAbstract = false;
        boolean isQueryable = true;
        Name primaryItem = null;
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
            } else if (tokens.matches(CndTokenizer.VENDOR_EXTENSION)) {
                List<Property> properties = new LinkedList<Property>();
                parseVendorExtensions(tokens, properties);
                applyVendorExtensions(nodeType, properties);
            } else {
                // No more valid options on the stream, so stop ...
                break;
            }
        }
        nodeType.setAbstract(isAbstract);
        nodeType.setMixin(isMixin);
        nodeType.setOrderableChildNodes(isOrderable);
        nodeType.setQueryable(isQueryable);
        // nodeType.setOnParentVersion();
        if (primaryItem != null) {
            nodeType.setPrimaryItemName(string(primaryItem));
        }
    }

    /**
     * Parse a node type's property or child node definitions that appear next on the token stream.
     * 
     * @param tokens the tokens containing the definitions; never null
     * @param nodeType the node type being created; never null
     * @throws ParsingException if there is a problem parsing the content
     * @throws ConstraintViolationException not expected
     */
    protected void parsePropertyOrChildNodeDefinitions( TokenStream tokens,
                                                        JcrNodeTypeTemplate nodeType ) throws ConstraintViolationException {
        while (true) {
            // Keep reading while we see a property definition or child node definition ...
            if (tokens.matches('-')) {
                parsePropertyDefinition(tokens, nodeType);
            } else if (tokens.matches('+')) {
                parseChildNodeDefinition(tokens, nodeType);
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
     * @param nodeType the node type definition; never null
     * @throws ParsingException if there is a problem parsing the content
     * @throws ConstraintViolationException not expected
     */
    protected void parsePropertyDefinition( TokenStream tokens,
                                            JcrNodeTypeTemplate nodeType ) throws ConstraintViolationException {
        tokens.consume('-');
        Name name = parseName(tokens);
        JcrPropertyDefinitionTemplate propDefn = new JcrPropertyDefinitionTemplate(context);
        propDefn.setName(string(name));

        // Parse the (optional) required type ...
        parsePropertyType(tokens, propDefn, PropertyType.STRING.getName());

        // Parse the default values ...
        parseDefaultValues(tokens, propDefn);

        // Parse the property attributes (and vendor extensions) ...
        parsePropertyAttributes(tokens, propDefn, nodeType);

        // Parse the property constraints ...
        parseValueConstraints(tokens, propDefn);

        // Parse the vendor extensions (appearing after the constraints) ...
        List<Property> properties = new LinkedList<Property>();
        parseVendorExtensions(tokens, properties);
        applyVendorExtensions(nodeType, properties);

        nodeType.getPropertyDefinitionTemplates().add(propDefn);
    }

    /**
     * Parse the property type, if a valid one appears next on the token stream.
     * 
     * @param tokens the tokens containing the definition; never null
     * @param propDefn the property definition; never null
     * @param defaultPropertyType the default property type if none is actually found
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parsePropertyType( TokenStream tokens,
                                      JcrPropertyDefinitionTemplate propDefn,
                                      String defaultPropertyType ) {
        if (tokens.canConsume('(')) {
            // Parse the (optional) property type ...
            String propertyType = defaultPropertyType;
            if (tokens.matchesAnyOf(VALID_PROPERTY_TYPES)) {
                propertyType = tokens.consume();
                if ("*".equals(propertyType)) propertyType = "UNDEFINED";
            }
            tokens.consume(')');
            PropertyType type = PropertyType.valueFor(propertyType.toLowerCase());
            int jcrType = PropertyTypeUtil.jcrPropertyTypeFor(type);
            propDefn.setRequiredType(jcrType);
        }
    }

    /**
     * Parse the property definition's default value, if they appear next on the token stream.
     * 
     * @param tokens the tokens containing the definition; never null
     * @param propDefn the property definition; never null
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parseDefaultValues( TokenStream tokens,
                                       JcrPropertyDefinitionTemplate propDefn ) {
        if (tokens.canConsume('=')) {
            List<String> defaultValues = parseStringList(tokens);
            if (!defaultValues.isEmpty()) {
                propDefn.setDefaultValues(values(defaultValues));
            }
        }
    }

    /**
     * Parse the property definition's value constraints, if they appear next on the token stream.
     * 
     * @param tokens the tokens containing the definition; never null
     * @param propDefn the property definition; never null
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parseValueConstraints( TokenStream tokens,
                                          JcrPropertyDefinitionTemplate propDefn ) {
        if (tokens.canConsume('<')) {
            List<String> defaultValues = parseStringList(tokens);
            if (!defaultValues.isEmpty()) {
                propDefn.setValueConstraints(strings(defaultValues));
            }
        }
    }

    /**
     * Parse the property definition's attributes, if they appear next on the token stream.
     * 
     * @param tokens the tokens containing the attributes; never null
     * @param propDefn the property definition; never null
     * @param nodeType the node type; never null
     * @throws ParsingException if there is a problem parsing the content
     * @throws ConstraintViolationException not expected
     */
    protected void parsePropertyAttributes( TokenStream tokens,
                                            JcrPropertyDefinitionTemplate propDefn,
                                            JcrNodeTypeTemplate nodeType ) throws ConstraintViolationException {
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
                parseQueryOperators(tokens, propDefn);
            } else if (tokens.canConsumeAnyOf("PRIMARY", "PRI", "!")) {
                if (!jcr170) {
                    Position pos = tokens.previousPosition();
                    int line = pos.getLine();
                    int column = pos.getColumn();
                    throw new ParsingException(tokens.previousPosition(),
                                               CndI18n.primaryKeywordNotValidInJcr2CndFormat.text(line, column));
                }
                // Then this child node is considered the primary item ...
                nodeType.setPrimaryItemName(propDefn.getName());
            } else if (tokens.matches(CndTokenizer.VENDOR_EXTENSION)) {
                List<Property> properties = new LinkedList<Property>();
                parseVendorExtensions(tokens, properties);
                applyVendorExtensions(propDefn, properties);
            } else {
                break;
            }
        }
        propDefn.setAutoCreated(autoCreated);
        propDefn.setMandatory(mandatory);
        propDefn.setProtected(isProtected);
        propDefn.setOnParentVersion(OnParentVersionAction.valueFromName(onParentVersion.toUpperCase()));
        propDefn.setMultiple(multiple);
        propDefn.setFullTextSearchable(isFullTextSearchable);
        propDefn.setQueryOrderable(isQueryOrderable);
    }

    /**
     * Parse the property definition's query operators, if they appear next on the token stream.
     * 
     * @param tokens the tokens containing the definition; never null
     * @param propDefn the property definition; never null
     * @throws ParsingException if there is a problem parsing the content
     */
    protected void parseQueryOperators( TokenStream tokens,
                                        JcrPropertyDefinitionTemplate propDefn ) {
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
        propDefn.setAvailableQueryOperators(strings(operators));
    }

    /**
     * Parse a node type's child node definition from the next tokens on the stream.
     * 
     * @param tokens the tokens containing the definition; never null
     * @param nodeType the node type being created; never null
     * @throws ParsingException if there is a problem parsing the content
     * @throws ConstraintViolationException not expected
     */
    protected void parseChildNodeDefinition( TokenStream tokens,
                                             JcrNodeTypeTemplate nodeType ) throws ConstraintViolationException {
        tokens.consume('+');
        Name name = parseName(tokens);

        JcrNodeDefinitionTemplate childDefn = new JcrNodeDefinitionTemplate(context);
        childDefn.setName(string(name));

        parseRequiredPrimaryTypes(tokens, childDefn);
        parseDefaultType(tokens, childDefn);
        parseNodeAttributes(tokens, childDefn, nodeType);

        nodeType.getNodeDefinitionTemplates().add(childDefn);

    }

    /**
     * Parse the child node definition's list of required primary types, if they appear next on the token stream.
     * 
     * @param tokens the tokens containing the definition; never null
     * @param childDefn the child node definition; never null
     * @throws ParsingException if there is a problem parsing the content
     * @throws ConstraintViolationException not expected
     */
    protected void parseRequiredPrimaryTypes( TokenStream tokens,
                                              JcrNodeDefinitionTemplate childDefn ) throws ConstraintViolationException {
        if (tokens.canConsume('(')) {
            List<Name> requiredTypes = parseNameList(tokens);
            if (requiredTypes.isEmpty()) {
                requiredTypes.add(JcrNtLexicon.BASE);
            }
            childDefn.setRequiredPrimaryTypeNames(names(requiredTypes));
            tokens.consume(')');
        }
    }

    /**
     * Parse the child node definition's default type, if they appear next on the token stream.
     * 
     * @param tokens the tokens containing the definition; never null
     * @param childDefn the child node definition; never null
     * @throws ParsingException if there is a problem parsing the content
     * @throws ConstraintViolationException not expected
     */
    protected void parseDefaultType( TokenStream tokens,
                                     JcrNodeDefinitionTemplate childDefn ) throws ConstraintViolationException {
        if (tokens.canConsume('=')) {
            if (!tokens.canConsume('?')) {
                Name defaultType = parseName(tokens);
                childDefn.setDefaultPrimaryTypeName(string(defaultType));
            }
        }
    }

    /**
     * Parse the child node definition's attributes, if they appear next on the token stream.
     * 
     * @param tokens the tokens containing the attributes; never null
     * @param childDefn the child node definition; never null
     * @param nodeType the node type being created; never null
     * @throws ParsingException if there is a problem parsing the content
     * @throws ConstraintViolationException not expected
     */
    protected void parseNodeAttributes( TokenStream tokens,
                                        JcrNodeDefinitionTemplate childDefn,
                                        JcrNodeTypeTemplate nodeType ) throws ConstraintViolationException {
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
                nodeType.setPrimaryItemName(childDefn.getName());
            } else if (tokens.matches(CndTokenizer.VENDOR_EXTENSION)) {
                List<Property> properties = new LinkedList<Property>();
                parseVendorExtensions(tokens, properties);
                applyVendorExtensions(childDefn, properties);
            } else {
                break;
            }
        }
        childDefn.setAutoCreated(autoCreated);
        childDefn.setMandatory(mandatory);
        childDefn.setProtected(isProtected);
        childDefn.setOnParentVersion(OnParentVersionAction.valueFromName(onParentVersion.toUpperCase()));
        childDefn.setSameNameSiblings(sns);
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

    /**
     * Parse the vendor extensions that may appear next on the tokenzied stream.
     * 
     * @param tokens token stream; may not be null
     * @param properties the list of properties to which any vendor extension properties should be added
     */
    protected final void parseVendorExtensions( TokenStream tokens,
                                                List<Property> properties ) {
        while (tokens.matches(CndTokenizer.VENDOR_EXTENSION)) {
            Property extension = parseVendorExtension(tokens.consume());
            if (extension != null) properties.add(extension);
        }
    }

    /**
     * Parse the vendor extension, including the curly braces in the CND content.
     * 
     * @param vendorExtension the vendor extension string
     * @return the property representing the vendor extension, or null if the vendor extension is incomplete
     */
    protected final Property parseVendorExtension( String vendorExtension ) {
        if (vendorExtension == null) return null;
        // Remove the curly braces ...
        String extension = vendorExtension.replaceFirst("^[{]", "").replaceAll("[}]$", "");
        if (extension.trim().length() == 0) return null;
        return parseVendorExtensionContent(extension);
    }

    /**
     * Parse the content of the vendor extension excluding the curly braces in the CND content.
     * 
     * @param vendorExtension the vendor extension string; never null
     * @return the property representing the vendor extension, or null if the vendor extension is incomplete
     */
    protected final Property parseVendorExtensionContent( String vendorExtension ) {
        Matcher matcher = VENDOR_PATTERN.matcher(vendorExtension);
        if (!matcher.find()) return null;
        String vendorName = removeQuotes(matcher.group(1));
        String vendorValue = removeQuotes(matcher.group(3));
        assert vendorName != null;
        assert vendorValue != null;
        assert vendorName.length() != 0;
        assert vendorValue.length() != 0;
        return context.getPropertyFactory().create(nameFactory.create(vendorName), vendorValue);
    }

    /**
     * Method that is responsible for setting the vendor extensions on the supplied node type template. By default this method
     * does nothing; subclasses should override this method for custom extensions.
     * 
     * @param nodeType the node type definition; never null
     * @param extensions the extensions; never null but possibly empty
     */
    protected void applyVendorExtensions( JcrNodeTypeTemplate nodeType,
                                          List<Property> extensions ) {
    }

    /**
     * Method that is responsible for setting the vendor extensions on the supplied child node type template. By default this
     * method does nothing; subclasses should override this method for custom extensions.
     * 
     * @param childDefn the child definition; never null
     * @param extensions the extensions; never null but possibly empty
     */
    protected void applyVendorExtensions( JcrNodeDefinitionTemplate childDefn,
                                          List<Property> extensions ) {
    }

    /**
     * Method that is responsible for setting the vendor extensions on the supplied property definition template. By default this
     * method does nothing; subclasses should override this method for custom extensions.
     * 
     * @param propDefn the property definition; never null
     * @param extensions the extensions; never null but possibly empty
     */
    protected void applyVendorExtensions( JcrPropertyDefinitionTemplate propDefn,
                                          List<Property> extensions ) {
    }

    protected final String string( Object name ) {
        return stringFactory.create(name);
    }

    protected final String[] names( Collection<Name> names ) {
        String[] result = new String[names.size()];
        int i = 0;
        for (Name name : names) {
            result[i++] = string(name);
        }
        return result;
    }

    protected final String[] strings( Collection<String> values ) {
        String[] result = new String[values.size()];
        int i = 0;
        for (String value : values) {
            result[i++] = value;
        }
        return result;
    }

    protected final Value[] values( Collection<String> values ) {
        Value[] result = new Value[values.size()];
        int i = 0;
        for (String value : values) {
            result[i++] = valueFactory.createValue(value);
        }
        return result;
    }

}
