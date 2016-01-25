/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.sequencer.ddl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.Session;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 * A sequencer of DDL files.
 */
@NotThreadSafe
public class DdlSequencer extends Sequencer {

    private static final Logger LOGGER = Logger.getLogger(DdlSequencer.class);

    protected static final URL[] DEFAULT_CLASSPATH = new URL[] {};
    protected static final List<String> DEFAULT_GRAMMARS;
    protected static final Map<String, DdlParser> STANDARD_PARSERS_BY_NAME;

    static {
        List<String> grammarNames = new ArrayList<String>();
        Map<String, DdlParser> parsersByName = new HashMap<String, DdlParser>();
        for (DdlParser parser : DdlParsers.BUILTIN_PARSERS) {
            String grammarName = parser.getId().toLowerCase();
            grammarNames.add(grammarName);
            parsersByName.put(grammarName, parser);
        }
        DEFAULT_GRAMMARS = Collections.unmodifiableList(grammarNames);
        STANDARD_PARSERS_BY_NAME = Collections.unmodifiableMap(parsersByName);
    }

    private String[] parserGrammars = DEFAULT_GRAMMARS.toArray(new String[DEFAULT_GRAMMARS.size()]);
    private URL[] classpath = DEFAULT_CLASSPATH;
    private final Map<AstNode, Node> nodeMap = new HashMap<AstNode, Node>();

    /**
     * Get the names of the grammars that should be considered during processing. The grammar names may be the case-insensitive
     * {@link DdlParser#getId() identifier} of a built-in grammar, or the name of a {@link DdlParser} implementation class.
     * 
     * @return the array of grammar names or classes; never null but possibly empty
     */
    public String[] getGrammars() {
        return parserGrammars;
    }

    /**
     * Set the names of the grammars that should be considered during processing. The grammar names may be the case-insensitive
     * {@link DdlParser#getId() identifier} of a built-in grammar, or the name of a {@link DdlParser} implementation class.
     * 
     * @param grammarNamesOrClasses the names; may be null if the default grammar list should be used
     */
    public void setGrammars( String[] grammarNamesOrClasses ) {
        this.parserGrammars = grammarNamesOrClasses != null && grammarNamesOrClasses.length != 0 ? grammarNamesOrClasses : DEFAULT_GRAMMARS.toArray(new String[DEFAULT_GRAMMARS.size()]);
    }

    /**
     * Get the names of the classloaders that should be used to load any non-standard DdlParser implementations specified in the
     * list of grammars.
     * 
     * @return the classloader names that make up the classpath; never null but possibly empty if the default classpath should be
     *         used
     */
    public URL[] getClasspath() {
        return classpath;
    }

    /**
     * Set the names of the classloaders that should be used to load any non-standard DdlParser implementations specified in the
     * list of grammars.
     * 
     * @param classpath the classloader names that make up the classpath; may be null or empty if the default classpath should be
     *        used
     */
    public void setClasspath( URL[] classpath ) {
        this.classpath = classpath != null ? classpath : DEFAULT_CLASSPATH;
    }

    /**
     * Method that creates the DdlParsers instance. This may be overridden in subclasses to creates specific implementations.
     * 
     * @param parsers the list of DdlParser instances to use; may be empty or null
     * @return the DdlParsers implementation; may not be null
     */
    protected DdlParsers createParsers( List<DdlParser> parsers ) {
        return new DdlParsers(parsers);
    }

    @SuppressWarnings( "unchecked" )
    protected List<DdlParser> getParserList() {
        List<DdlParser> parserList = new LinkedList<DdlParser>();
        for (String grammar : getGrammars()) {
            if (grammar == null) {
                continue;
            }
            // Look for a standard parser using a case-insensitive name ...
            String lowercaseGrammar = grammar.toLowerCase();
            DdlParser parser = STANDARD_PARSERS_BY_NAME.get(lowercaseGrammar);
            if (parser == null) {
                // Attempt to instantiate the parser if its a classname ...
                try {
                    ClassLoader classloader = new URLClassLoader(getClasspath(), Thread.currentThread().getContextClassLoader());
                    Class<DdlParser> componentClass = (Class<DdlParser>)Class.forName(grammar, true, classloader);
                    parser = componentClass.newInstance();
                } catch (Throwable e) {
                    if (classpath == null || classpath.length == 0) {
                        LOGGER.error(e,
                                     DdlSequencerI18n.errorInstantiatingParserForGrammarUsingDefaultClasspath,
                                     grammar,
                                     e.getLocalizedMessage());
                    } else {
                        LOGGER.error(e,
                                     DdlSequencerI18n.errorInstantiatingParserForGrammarClasspath,
                                     grammar,
                                     classpath,
                                     e.getLocalizedMessage());
                    }
                }
            }
            if (parser != null) {
                parserList.add(parser);
            }
        }
        return parserList; // okay if empty
    }

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        registerNodeTypes("StandardDdl.cnd", nodeTypeManager, true);
        registerNodeTypes("dialect/derby/DerbyDdl.cnd", nodeTypeManager, true);
        registerNodeTypes("dialect/oracle/OracleDdl.cnd", nodeTypeManager, true);
        registerNodeTypes("dialect/postgres/PostgresDdl.cnd", nodeTypeManager, true);
    }

    @Override
    public boolean execute( Property inputProperty,
                            Node outputNode,
                            Context context ) throws Exception {
        Binary ddlContent = inputProperty.getBinary();
        CheckArg.isNotNull(ddlContent, "ddl content binary value");

        // make sure node map is empty
        this.nodeMap.clear();

        // Look at the input path to get the name of the input node (or it's parent if it's "jcr:content") ...
        String fileName = getNameOfDdlContent(inputProperty);

        // Perform the parsing
        final AstNode rootNode;
        DdlParsers parsers = createParsers(getParserList());
        try (InputStream stream = ddlContent.getStream()) {
            rootNode = parsers.parse(IoUtil.read(stream), fileName);
        } catch (ParsingException e) {
            LOGGER.error(e, DdlSequencerI18n.errorParsingDdlContent, e.getLocalizedMessage());
            return false;
        } catch (IOException e) {
            LOGGER.error(e, DdlSequencerI18n.errorSequencingDdlContent, e.getLocalizedMessage());
            return false;
        }

        Queue<AstNode> queue = new LinkedList<AstNode>();
        queue.add(rootNode);
        while (queue.peek() != null) {
            AstNode astNode = queue.poll();
            createFromAstNode(outputNode, astNode);

            // Add the children to the queue ...
            for (AstNode child : astNode.getChildren()) {
                queue.add(child);
            }
        }

        // second pass to lookup references (this allows for DDL to have forward references)
        for (final Entry<AstNode, Node> entry : this.nodeMap.entrySet()) {
            appendNodeProperties(entry.getKey(), entry.getValue());
        }

        return true;
    }

    private void appendNodeProperties( AstNode astNode,
                                       Node sequenceNode ) throws RepositoryException {
        ValueFactory valueFactory = sequenceNode.getSession().getValueFactory();

        for (String propertyName : astNode.getPropertyNames()) {
            Object astNodePropertyValue = astNode.getProperty(propertyName);
            List<Value> valuesList = convertToPropertyValues(astNodePropertyValue, valueFactory);
            if (valuesList.size() == 1) {
                sequenceNode.setProperty(propertyName, valuesList.get(0));
            } else {
                sequenceNode.setProperty(propertyName, valuesList.toArray(new Value[0]));
            }
        }
    }

    private Node createFromAstNode( Node parent,
                                    AstNode astNode ) throws RepositoryException {
        String relativePath = astNode.getAbsolutePath().substring(1);
        Node sequenceNode = null;

        // for SNS the absolute path will use first node it finds as the parent so find real parent if possible
        Node parentNode = getNode(astNode.getParent());

        if (parentNode == null) {
            sequenceNode = parent.addNode(relativePath, astNode.getPrimaryType());
        } else {
            final Session session = (Session)parentNode.getSession();
            String jcrName = astNode.getName();

            // if first character is a '{' then the name is prefixed by the namespace URL
            if ((jcrName.charAt(0) == '{') && (jcrName.indexOf('}') != -1)) {
                final int index = jcrName.indexOf('}');
                String localName = jcrName.substring(index + 1);
                localName = session.encode(localName);

                jcrName = jcrName.substring(0, (index + 1)) + localName;
            } else {
                jcrName = session.encode(jcrName);
            }

            sequenceNode = parentNode.addNode(jcrName, astNode.getPrimaryType());
        }

        this.nodeMap.put(astNode, sequenceNode);
        for (String mixin : astNode.getMixins()) {
            sequenceNode.addMixin(mixin);
        }
        astNode.removeProperty(JcrConstants.JCR_MIXIN_TYPES);
        astNode.removeProperty(JcrConstants.JCR_PRIMARY_TYPE);
        return sequenceNode;
    }

    private List<Value> convertToPropertyValues( Object objectValue,
                                                 ValueFactory valueFactory ) throws RepositoryException {
        List<Value> result = new ArrayList<Value>();
        if (objectValue instanceof Collection) {
            Collection<?> objects = (Collection<?>)objectValue;
            for (Object childObjectValue : objects) {
                List<Value> childValues = convertToPropertyValues(childObjectValue, valueFactory);
                result.addAll(childValues);
            }
        } else if (objectValue instanceof Boolean) {
            result.add(valueFactory.createValue((Boolean)objectValue));
        } else if (objectValue instanceof Integer) {
            result.add(valueFactory.createValue((Integer)objectValue));
        } else if (objectValue instanceof Long) {
            result.add(valueFactory.createValue((Long)objectValue));
        } else if (objectValue instanceof Double) {
            result.add(valueFactory.createValue((Double)objectValue));
        } else if (objectValue instanceof Float) {
            result.add(valueFactory.createValue((Float)objectValue));
        } else if (objectValue instanceof AstNode) {
            result.add(valueFactory.createValue(getNode((AstNode)objectValue)));
        } else {
            result.add(valueFactory.createValue(objectValue.toString()));
        }
        return result;
    }

    private Node getNode( final AstNode node ) {
        return this.nodeMap.get(node);
    }

    private String getNameOfDdlContent( Property inputProperty ) throws RepositoryException {
        Node parentNode = inputProperty.getParent();
        if (JcrConstants.JCR_CONTENT.equalsIgnoreCase(parentNode.getName())) {
            parentNode = parentNode.getParent();
        }
        return parentNode.getName();
    }
}
