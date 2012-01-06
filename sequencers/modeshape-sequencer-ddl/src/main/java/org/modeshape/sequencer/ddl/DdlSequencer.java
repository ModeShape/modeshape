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
package org.modeshape.sequencer.ddl;

import javax.jcr.*;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.sequencer.ddl.node.AstNode;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

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
     * used
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
    public void initialize( NamespaceRegistry registry, NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        registerNodeTypes("StandardDdl.cnd", nodeTypeManager, true);
        registerNodeTypes("dialect/derby/DerbyDdl.cnd", nodeTypeManager, true);
        registerNodeTypes("dialect/oracle/OracleDdl.cnd", nodeTypeManager, true);
        registerNodeTypes("dialect/postgres/PostgresDdl.cnd", nodeTypeManager, true);
    }

    @Override
    public boolean execute( Property inputProperty, Node outputNode, Context context ) throws Exception {
        Binary ddlContent = inputProperty.getBinary();
        CheckArg.isNotNull(ddlContent, "ddl content binary value");

        // Look at the input path to get the name of the input node (or it's parent if it's "jcr:content") ...
        String fileName = getNameOfDdlContent(inputProperty);

        // Perform the parsing
        final AstNode rootNode;
        try {
            DdlParsers parsers = createParsers(getParserList());
            rootNode = parsers.parse(IoUtil.read(ddlContent.getStream()), fileName);
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
            Node sequenceNode = createFromAstNode(outputNode, astNode);
            appendNodeProperties(astNode, sequenceNode);

            // Add the children to the queue ...
            for (AstNode child : astNode.getChildren()) {
                queue.add(child);
            }      
        }
        return true;
    }

    private void appendNodeProperties(AstNode astNode, Node sequenceNode ) throws RepositoryException {
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

    private Node createFromAstNode( Node parent, AstNode astNode ) throws RepositoryException {
        String relativePath = astNode.getAbsolutePath().substring(1);
        Node sequenceNode = parent.addNode(relativePath, astNode.getPrimaryType());
        for (String mixin : astNode.getMixins()) {
            sequenceNode.addMixin(mixin);
        }
        astNode.removeProperty(JcrConstants.JCR_MIXIN_TYPES);
        astNode.removeProperty(JcrConstants.JCR_PRIMARY_TYPE);
        return sequenceNode;
    }

    private List<Value> convertToPropertyValues( Object objectValue, ValueFactory valueFactory ) {
        List<Value> result = new ArrayList<Value>();
        if (objectValue instanceof Collection) {
            Collection objects = (Collection)objectValue;
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
        } else {
            result.add(valueFactory.createValue(objectValue.toString()));
        }
        return result;
    }

    private String getNameOfDdlContent( Property inputProperty ) throws RepositoryException {
        Node parentNode = inputProperty.getParent();
        if (JcrConstants.JCR_CONTENT.equalsIgnoreCase(parentNode.getName())) {
            parentNode = parentNode.getParent();
        }
        return parentNode.getName();
    }
}
