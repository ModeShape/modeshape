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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.util.IoUtil;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencer;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 * A sequencer of DDL files.
 */
@NotThreadSafe
public class DdlSequencer implements StreamSequencer {

    protected static final String[] DEFAULT_CLASSPATH = new String[] {};
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
    private String[] classpath = DEFAULT_CLASSPATH;

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
    public String[] getClasspath() {
        return classpath;
    }

    /**
     * Set the names of the classloaders that should be used to load any non-standard DdlParser implementations specified in the
     * list of grammars.
     * 
     * @param classpath the classloader names that make up the classpath; may be null or empty if the default classpath should be
     *        used
     */
    public void setClasspath( String[] classpath ) {
        this.classpath = classpath != null ? classpath : DEFAULT_CLASSPATH;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.sequencer.StreamSequencer#sequence(java.io.InputStream,
     *      org.modeshape.graph.sequencer.SequencerOutput, org.modeshape.graph.sequencer.StreamSequencerContext)
     */
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          StreamSequencerContext context ) {
        try {
            // Look at the input path to get the name of the input node (or it's parent if it's "jcr:content") ...
            String fileName = getNameOfDdlContent(context);

            // Perform the parsing
            DdlParsers parsers = createParsers(getParserList(context));
            final AstNode rootNode = parsers.parse(IoUtil.read(stream), fileName);

            // Convert the AST graph into graph nodes in the output ...
            Queue<AstNode> queue = new LinkedList<AstNode>();
            queue.add(rootNode);
            while (queue.peek() != null) {
                AstNode astNode = queue.poll();
                Path path = astNode.getPath(context);
                // Write the AST node properties to the output ...
                for (Property property : astNode.getProperties()) {
                    output.setProperty(path, property.getName(), property.getValuesAsArray());
                }
                // Add the children to the queue ...
                for (AstNode child : astNode.getChildren()) {
                    queue.add(child);
                }
            }
        } catch (ParsingException e) {
            context.getProblems().addError(e, DdlSequencerI18n.errorParsingDdlContent, e.getLocalizedMessage());
        } catch (IOException e) {
            context.getProblems().addError(e, DdlSequencerI18n.errorSequencingDdlContent, e.getLocalizedMessage());
        }
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

    /**
     * Utility method that attempts to discover the "name" of the DDL content being sequenced, which may help identify the
     * dialect.
     * 
     * @param context the sequencing context; never null
     * @return the name, or null if no name could be identified
     */
    protected String getNameOfDdlContent( StreamSequencerContext context ) {
        Path inputPath = context.getInputPath();
        if (inputPath.isRoot()) return null;
        Path.Segment segment = inputPath.getLastSegment();
        if (JcrLexicon.CONTENT.equals(segment.getName()) && inputPath.size() > 1) {
            // Get the name of the parent ...
            segment = inputPath.getParent().getLastSegment();
        }
        return segment.getName().getLocalName();
    }

    @SuppressWarnings( "unchecked" )
    protected List<DdlParser> getParserList( StreamSequencerContext context ) {
        List<DdlParser> parserList = new LinkedList<DdlParser>();
        for (String grammar : getGrammars()) {
            if (grammar == null) continue;
            // Look for a standard parser using a case-insensitive name ...
            String lowercaseGrammar = grammar.toLowerCase();
            DdlParser parser = STANDARD_PARSERS_BY_NAME.get(lowercaseGrammar);
            if (parser == null) {
                // Attempt to instantiate the parser if its a classname ...
                String[] classpath = getClasspath();
                try {
                    ClassLoader classloader = context.getClassLoader(classpath);
                    Class<DdlParser> componentClass = (Class<DdlParser>)Class.forName(grammar, true, classloader);
                    parser = componentClass.newInstance();
                } catch (Throwable e) {
                    if (classpath == null || classpath.length == 0) {
                        context.getProblems().addError(e,
                                                       DdlSequencerI18n.errorInstantiatingParserForGrammarUsingDefaultClasspath,
                                                       grammar,
                                                       e.getLocalizedMessage());
                    } else {
                        context.getProblems().addError(e,
                                                       DdlSequencerI18n.errorInstantiatingParserForGrammarClasspath,
                                                       grammar,
                                                       classpath,
                                                       e.getLocalizedMessage());
                    }
                }
            }
            if (parser != null) parserList.add(parser);
        }
        return parserList; // okay if empty
    }
}
