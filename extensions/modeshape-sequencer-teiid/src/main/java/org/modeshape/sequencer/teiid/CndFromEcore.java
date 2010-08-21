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
package org.modeshape.sequencer.teiid;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.text.Inflector;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.SubgraphNode;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.NamespaceRegistry.Namespace;
import org.modeshape.graph.property.basic.LocalNamespaceRegistry;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Utility class to generate a CND file from an ECore model.
 */
public class CndFromEcore {

    public static void main( String[] args ) {
        CndFromEcore converter = new CndFromEcore();
        JCommander commander = new JCommander(converter, args);
        if (!converter.isValid()) {
            commander.usage();
        } else {
            converter.execute();
            if (converter.getProblems().hasProblems()) {
                System.out.println(converter.getProblems());
            }
        }
    }

    private static final char NEWLINE = '\n';

    @Parameter( description = "Comma-separated list of Ecore input file paths or URLs" )
    private List<String> ecoreFileNames = new ArrayList<String>();
    @Parameter( names = {"-o", "-out"}, description = "Name of the CND output file" )
    private String cndFileName;
    @Parameter( names = "-debug", description = "Debug mode" )
    private boolean debug = false;
    @Parameter( names = "-mixin", description = "EClasses are converted to node types" )
    private boolean mixins = false;
    @Parameter( names = "-shortNames", description = "Generate shorter names where possible" )
    private boolean shortNames = false;

    private Problems problems = new SimpleProblems();

    /**
     * Get the names of the Ecore files that are to be processed into node types.
     * 
     * @return the Ecore file names
     */
    public List<String> getEcoreFileNames() {
        return ecoreFileNames;
    }

    /**
     * @param ecoreFileNames Sets ecoreFileNames to the specified value.
     */
    public void setEcoreFileNames( List<String> ecoreFileNames ) {
        this.ecoreFileNames = ecoreFileNames;
    }

    /**
     * @param ecoreFileNames Sets ecoreFileNames to the specified value.
     */
    public void setEcoreFileNames( String... ecoreFileNames ) {
        this.ecoreFileNames = new ArrayList<String>(Arrays.asList(ecoreFileNames));
    }

    /**
     * @return cndFileName
     */
    public String getCndFileName() {
        return cndFileName;
    }

    /**
     * @param cndFileName Sets cndFileName to the specified value.
     */
    public void setCndFileName( String cndFileName ) {
        this.cndFileName = cndFileName;
    }

    /**
     * @return debug
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * @param debug Sets debug to the specified value.
     */
    public void setDebug( boolean debug ) {
        this.debug = debug;
    }

    /**
     * @return mixins
     */
    public boolean generatesMixins() {
        return mixins;
    }

    /**
     * @param mixins Sets mixins to the specified value.
     */
    public void setGeneratesMixins( boolean mixins ) {
        this.mixins = mixins;
    }

    /**
     * @return shortNames
     */
    public boolean generateShortNames() {
        return shortNames;
    }

    /**
     * @param shortNames Sets shortNames to the specified value.
     */
    public void setGeneratesShortNames( boolean shortNames ) {
        this.shortNames = shortNames;
    }

    public boolean isValid() {
        if (ecoreFileNames.isEmpty()) return false;
        return true;
    }

    /**
     * @return problems
     */
    public Problems getProblems() {
        return problems;
    }

    public void execute() {
        ExecutionContext context = new ExecutionContext();
        NamespaceRegistry registry = context.getNamespaceRegistry();

        // Use a local namespace registry so that we know which namespaces were used ...
        LocalNamespaceRegistry localRegistry = new LocalNamespaceRegistry(registry);
        context = context.with(localRegistry);

        // Read in each of the Ecore files ...
        List<String> ecoreFileContributions = new ArrayList<String>();
        for (String ecoreFileName : getEcoreFileNames()) {
            String ecoreName = ecoreFileName.replace("\\.ecore", "");
            debug(TeiidI18n.readingEcoreFile, ecoreFileName);
            StringBuilder sb = new StringBuilder();
            try {
                InMemoryRepositorySource source = new InMemoryRepositorySource();
                source.setName(ecoreName);
                Graph graph = Graph.create(source, context);
                graph.importXmlFrom(ecoreFileName).into("/"); // file path or URL or even on classpath
                Subgraph subgraph = graph.getSubgraphOfDepth(20).at("/ecore:EPackage");
                GraphReader reader = new GraphReader(subgraph, generatesMixins(), generateShortNames());
                reader.writeTo(sb);

                ecoreFileContributions.add(sb.toString());
            } catch (Throwable t) {
                problems.addError(TeiidI18n.errorReadingEcoreFile, ecoreFileName, t.getLocalizedMessage());
            }
        }

        // Create the output file ...
        StringBuilder output = new StringBuilder();
        // Write the header first ...
        output.append(getHeader());

        // Write the namespaces that were used ...
        for (Namespace namespace : localRegistry.getLocalNamespaces()) {
            write(output, namespace);
        }
        output.append(NEWLINE);

        // And add in the CND contribution from each file ...
        for (String contribution : ecoreFileContributions) {
            output.append(contribution);
            output.append(NEWLINE);
        }

        // Now write it to the file ...
        if (cndFileName != null && cndFileName.trim().length() != 0) {
            try {
                FileWriter writer = new FileWriter(cndFileName);
                try {
                } finally {
                    writer.close();
                }
            } catch (Throwable t) {
                problems.addError(TeiidI18n.errorWritingCndFile, cndFileName, t.getLocalizedMessage());
            }
        } else {
            System.out.println(output);
        }

    }

    protected void write( StringBuilder writer,
                          Namespace namespace ) {
        writer.append("<")
              .append(namespace.getPrefix())
              .append("='")
              .append(namespace.getNamespaceUri())
              .append("'>")
              .append(NEWLINE);
    }

    protected String getHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append("/*").append(NEWLINE);
        sb.append(" * Generated using the ").append(getClass().getCanonicalName()).append(" program.").append(NEWLINE);
        sb.append(" *").append(NEWLINE);
        sb.append(" */").append(NEWLINE);
        return sb.toString();
    }

    protected void debug( I18n msg,
                          Object... params ) {
        if (isDebug()) {
            System.out.println(msg.text(params));
        }
    }

    protected static class GraphReader {
        private final ExecutionContext context;
        private final ValueFactory<String> stringFactory;
        private final ValueFactory<Boolean> booleanFactory;
        private final NameFactory nameFactory;
        private final Subgraph subgraph;
        private final NamespaceRegistry namespaces;
        private final Inflector inflector;
        private final boolean generateMixins;
        private final boolean generateShortNames;
        private String currentNamespaceUri;

        protected GraphReader( Subgraph subgraph,
                               boolean generateMixins,
                               boolean generateShortNames ) {
            this.subgraph = subgraph;
            this.context = subgraph.getGraph().getContext();
            this.nameFactory = this.context.getValueFactories().getNameFactory();
            this.stringFactory = this.context.getValueFactories().getStringFactory();
            this.booleanFactory = this.context.getValueFactories().getBooleanFactory();
            this.namespaces = this.context.getNamespaceRegistry();
            this.inflector = Inflector.getInstance();
            this.generateMixins = generateMixins;
            this.generateShortNames = generateShortNames;
        }

        protected void writeTo( StringBuilder sb ) {
            SubgraphNode pkg = subgraph.getRoot();
            String pkgName = inflector.titleCase(firstValue(pkg, "name"));
            String uri = firstValue(pkg, "nsURI");
            String prefix = firstValue(pkg, "nsPrefix");

            // Add the CND output from this file to the complete output ...
            sb.append("// -------------------------------------------").append(NEWLINE);
            sb.append("// ").append(pkgName).append(NEWLINE);
            sb.append("// -------------------------------------------").append(NEWLINE);

            // Register the namespace ...
            namespaces.register(prefix, uri);
            this.currentNamespaceUri = uri;

            // Look for EEnums ...
            Multimap<Name, String> literalsByEnumName = ArrayListMultimap.create();
            for (Location child : pkg.getChildren()) {
                SubgraphNode classifier = pkg.getNode(child.getPath().getLastSegment());
                String type = firstValue(classifier, "xsi:type"); // e.g., 'ecore:EClass' or 'ecore:EEnum'
                if ("ecore:EEnum".equals(type)) {
                    Name enumName = nameFrom(firstValue(classifier, "name"));
                    for (Location feature : classifier.getChildren()) {
                        SubgraphNode literal = classifier.getNode(feature.getPath().getLastSegment());
                        String literalValue = firstValue(literal, "name");
                        literalsByEnumName.put(enumName, literalValue);
                    }
                }
            }

            for (Location child : pkg.getChildren()) {
                // Classifier ...
                SubgraphNode classifier = pkg.getNode(child.getPath().getLastSegment());
                String type = firstValue(classifier, "xsi:type"); // e.g., 'ecore:EClass' or 'ecore:EEnum'
                if ("ecore:EEnum".equals(type)) continue;

                Name nodeTypeName = nameFrom(firstValue(classifier, "name"));
                boolean isAbstract = firstValue(classifier, "abstract", false);
                List<Name> supertypes = names(classifier, "eSuperTypes", "\\s");

                // Write out the CND node type ...
                sb.append("[").append(stringFrom(nodeTypeName)).append("] ");

                // Write out the CND supertypes ...
                if (!supertypes.isEmpty()) {
                    sb.append("> ");
                    boolean first = true;
                    for (Name supertype : supertypes) {
                        if (first) first = false;
                        else sb.append(",");
                        sb.append(stringFrom(supertype)).append(" ");
                    }
                }
                if (isAbstract) sb.append("abstract ");
                if (generateMixins) sb.append("mixin ");

                // Write out the property and child node definitions ...
                for (Location feature : classifier.getChildren()) {
                    SubgraphNode structuralFeature = classifier.getNode(feature.getPath().getLastSegment());
                    String featureType = firstValue(structuralFeature, "xsi:type"); // e.g., 'ecore:EAttribute'
                    Name featureName = nameFrom(firstValue(structuralFeature, "name"));
                    long upperBound = firstValue(structuralFeature, "upperBound", 1L);
                    long lowerBound = firstValue(structuralFeature, "lowerBound", 0L);
                    boolean isSingle = upperBound == 1;
                    boolean isRequired = lowerBound > 0;
                    boolean isReference = "ecore:EReference".equals(featureType);
                    boolean isTransient = firstValue(structuralFeature, "transient", false);
                    boolean isContainment = firstValue(structuralFeature, "containment", false);
                    boolean isUnsettable = firstValue(structuralFeature, "unsettable", false);
                    boolean isVolatile = firstValue(structuralFeature, "volatile", false);
                    boolean isChangeble = firstValue(structuralFeature, "changeable", true);
                    Name dataType = nameFrom(firstValue(structuralFeature, "eType"));
                    String defaultValue = firstValue(structuralFeature, "defaultValueLiteral");

                    // Figure out the JCR primary type and constraint values ...
                    String jcrType = jcrTypeNameFor(dataType);
                    Collection<String> constraints = literalsByEnumName.get(dataType);
                    if (!constraints.isEmpty()) {
                        // Then the values are literals, so we do have constraints ...
                        jcrType = "STRING";
                    }

                    if (isContainment) {
                        // This is a child node definition ...
                        int x = 0;
                    } else {
                        // This is a property definition ...
                        String propDefnName = stringFrom(featureName);
                        sb.append(NEWLINE);
                        sb.append(" - ").append(propDefnName).append(" (").append(jcrType).append(") ");
                        if (defaultValue != null) sb.append("= '").append(defaultValue).append("' ");
                        if (isRequired) sb.append("mandatory ");
                        if (!isSingle) sb.append("multiple ");
                        if (!isChangeble) sb.append("protected autocreated");
                        if (!constraints.isEmpty()) {
                            sb.append(NEWLINE);
                            sb.append("   < ");
                            boolean first = true;
                            for (String constraint : constraints) {
                                if (first) first = false;
                                else sb.append(", ");
                                sb.append("'").append(constraint).append("'");
                            }
                        }
                    }
                }
                sb.append(NEWLINE);
                sb.append(NEWLINE);
            }
        }

        protected String firstValue( SubgraphNode node,
                                     String propertyName ) {
            return firstValue(node, propertyName, null);
        }

        protected String firstValue( SubgraphNode node,
                                     String propertyName,
                                     String defaultValue ) {
            Property property = node.getProperty(propertyName);
            if (property == null || property.isEmpty()) {
                return defaultValue;
            }
            return stringFactory.create(property.getFirstValue());
        }

        protected boolean firstValue( SubgraphNode node,
                                      String propertyName,
                                      boolean defaultValue ) {
            Property property = node.getProperty(propertyName);
            if (property == null || property.isEmpty()) {
                return defaultValue;
            }
            return booleanFactory.create(property.getFirstValue());
        }

        protected long firstValue( SubgraphNode node,
                                   String propertyName,
                                   long defaultValue ) {
            Property property = node.getProperty(propertyName);
            if (property == null || property.isEmpty()) {
                return defaultValue;
            }
            return context.getValueFactories().getLongFactory().create(property.getFirstValue());
        }

        protected List<String> values( SubgraphNode node,
                                       String propertyName,
                                       String regexDelimiter ) {
            Property property = node.getProperty(propertyName);
            if (property == null || property.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> values = new ArrayList<String>();
            for (Object value : property) {
                String stringValue = stringFactory.create(value);
                for (String val : stringValue.split(regexDelimiter)) {
                    if (val != null) values.add(val);
                }
            }
            return values;
        }

        protected List<Name> names( SubgraphNode node,
                                    String propertyName,
                                    String regexDelimiter ) {
            Property property = node.getProperty(propertyName);
            if (property == null || property.isEmpty()) {
                return Collections.emptyList();
            }
            List<Name> values = new ArrayList<Name>();
            for (Object value : property) {
                String stringValue = stringFactory.create(value);
                for (String val : stringValue.split(regexDelimiter)) {
                    if (val != null) {
                        values.add(nameFrom(val));
                    }
                }
            }
            return values;
        }

        protected String jcrTypeNameFor( Name dataType ) {
            if (ECORE_NAMESPACE.equals(dataType.getNamespaceUri())) {
                // This is a built-in datatype ...
                String localName = dataType.getLocalName();
                String result = EDATATYPE_TO_JCR_TYPENAME.get(localName);
                if (result != null) return result;
            }
            return "UNDEFINED";
        }

        protected Name nameFrom( String name ) {
            String value = name;
            if (value.startsWith("#//")) {
                value = value.substring(3);
            } else if (value.startsWith("//")) {
                value = value.substring(2);
            }
            if (value.startsWith("ecore:EDataType ")) {
                value = value.substring("ecore:DataType ".length() + 1);
                // otherwise, just continue ...
            }
            // Look for the namespace ...
            Name result = null;
            String[] parts = value.split("\\#");
            if (parts.length == 2) {
                String uri = parts[0].trim();
                String localName = nameFrom(parts[1]).getLocalName(); // removes '#//'
                result = shortenName(nameFactory.create(uri, localName));
            } else {
                value = inflector.underscore(value, '_', '-', '.');
                value = inflector.lowerCamelCase(value, '_');
                result = shortenName(nameFactory.create(value));
            }
            if (this.currentNamespaceUri != null && result.getNamespaceUri().length() == 0) {
                result = nameFactory.create(this.currentNamespaceUri, result.getLocalName());
            }
            return result;
        }

        protected String stringFrom( Object value ) {
            return stringFactory.create(value);
        }

        protected Name shortenName( Name name ) {
            if (generateShortNames) {
                // If the local name begins with the namespace prefix, remove it ...
                String localName = name.getLocalName();
                String prefix = namespaces.getPrefixForNamespaceUri(name.getNamespaceUri(), false);
                if (prefix != null && localName.startsWith(prefix) && localName.length() > prefix.length()) {
                    localName = localName.substring(prefix.length());
                    localName = inflector.underscore(localName, '_', '-', '.');
                    localName = inflector.lowerCamelCase(localName, '_');
                    if (localName.length() > 0) return nameFactory.create(name.getNamespaceUri(), localName);
                }
            }
            return name;
        }
    }

    protected static final String ECORE_NAMESPACE = "http://www.eclipse.org/emf/2002/Ecore";

    protected static final Map<String, String> EDATATYPE_TO_JCR_TYPENAME;

    static {
        Map<String, String> types = new HashMap<String, String>();
        types.put("eBoolean", "BOOLEAN");
        types.put("eShort", "LONG");
        types.put("eInt", "LONG");
        types.put("eLong", "LONG");
        types.put("eFloat", "DOUBLE");
        types.put("eDouble", "DOUBLE");
        types.put("eBigDecimal", "DECIMAL");
        types.put("eBigInteger", "DECIMAL");
        types.put("eDate", "DATE");
        types.put("eString", "STRING");
        types.put("eChar", "STRING");
        types.put("eByte", "BINARY");
        types.put("eByteArray", "BINARY");
        types.put("eObject", "UNDEFINED");
        types.put("eBooleanObject", "BOOLEAN");
        types.put("eShortObject", "LONG");
        types.put("eIntObject", "LONG");
        types.put("eLongObject", "LONG");
        types.put("eFloatObject", "DOUBLE");
        types.put("eDoubleObject", "DOUBLE");
        types.put("eCharObject", "STRING");
        types.put("eByteObject", "BINARY");
        EDATATYPE_TO_JCR_TYPENAME = Collections.unmodifiableMap(types);
    }

}
