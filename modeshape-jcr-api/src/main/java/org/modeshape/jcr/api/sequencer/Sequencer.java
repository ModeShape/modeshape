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
package org.modeshape.jcr.api.sequencer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.jcr.*;
import javax.jcr.nodetype.NodeTypeExistsException;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;

/**
 * A component that reads recently-changed content (often uploaded files) and extracts additional information from the content.
 * <p>
 * Each ModeShape repository can be configured with zero or more sequencers. Each sequencer is configured with a set of match
 * conditions that define the acceptable patterns for the paths of changed nodes, as well as a path specification that defines
 * where the derived (or generated) output should be placed. Then when clients change nodes with paths that satisfy the match
 * conditions, the repository will create a new Session and invoke the sequencer, which is then expected to process the changed
 * content and generate the derived information under the supplied parent node. The session will be saved automatically or, if an
 * exception is thrown, discard the changes and close the session.
 * </p>
 */
public abstract class Sequencer {

    private String name;
    private String description;
    private String repositoryName;
    private Object[] pathExpressions;
    private String pathExpression;

    /**
     * Get the name of this sequencer.
     * 
     * @return the sequencer name; null only if not {@link #initialize initialized}
     */
    public final String getName() {
        return name;
    }

    /**
     * Get the description for this sequencer.
     * 
     * @return the description, or null if there is no description
     */
    public final String getDescription() {
        return description;
    }

    /**
     * Get the name of the repository.
     * 
     * @return the repository name; never null
     */
    public final String getRepositoryName() {
        return repositoryName;
    }

    /**
     * Obtain the path expressions as configured on the sequencer. This method always returns a copy to prevent modification of
     * the values.
     * 
     * @return the path expressions; never null but possibly empty
     */
    public final String[] getPathExpressions() {
        String pathExpression = this.pathExpression;
        Object[] pathExpressions = this.pathExpressions;
        if (pathExpression != null && pathExpressions == null || pathExpressions.length == 0) {
            // There's just one ...
            return new String[] {pathExpression};
        }
        List<String> expressions = new ArrayList<String>(pathExpressions.length + 1);
        addExpression(expressions, pathExpression);
        for (Object value : pathExpressions) {
            addExpression(expressions, value);
        }
        return expressions.toArray(new String[expressions.size()]);
    }

    private void addExpression( List<String> values,
                                Object value ) {
        if (value instanceof String) {
            String str = (String)value;
            str = str.trim();
            if (str.length() != 0) {
                values.add(str);
            }
        }
    }

    /**
     * Initialize the sequencer. This is called automatically by ModeShape, and should not be called by the sequencer.
     * <p>
     * By default this method does nothing, so it should be overridden by implementations to do a one-time initialization of any
     * internal components. For example, sequencers can use the supplied <code>registry</code> and <code>nodeTypeManager</code>
     * objects to register custom namesapces and node types required by the generated content.
     * </p>
     * 
     * @param registry the namespace registry that can be used to register custom namespaces; never null
     * @param nodeTypeManager the node type manager that can be used to register custom node types; never null
     * @throws RepositoryException if operations on the {@link NamespaceRegistry} or {@link NodeTypeManager} fail
     * @throws IOException if any stream based operations fail (like importing cnd files)
     */
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
    }

    /**
     * Execute the sequencing operation on the specified property, which has recently been created or changed.
     * <p>
     * Each sequencer is expected to process the value of the property, extract information from the value, and write a structured
     * representation (in the form of a node or a subgraph of nodes) using the supplied output node. Note that the output node
     * will either be:
     * <ol>
     * <li>the selected node, in which case the sequencer was configured to generate the output information directly under the
     * selected input node; or</li>
     * <li>a newly created node in a different location than node being sequenced (in this case, the primary type of the new node
     * will be 'nt:unstructured', but the sequencer can easily change that using {@link Node#setPrimaryType(String)})</li>
     * </ol>
     * </p>
     * <p>
     * The implementation is expected to always clean up all resources that it acquired, even in the case of exceptions.
     * </p>
     * 
     * @param inputProperty the property that was changed and that should be used as the input; never null
     * @param outputNode the node that represents the output for the derived information; never null, and will either be
     *        {@link Node#isNew() new} if the output is being placed outside of the selected node, or will not be new when the
     *        output is to be placed on the selected input node
     * @param context the context in which this sequencer is executing, and which may contain additional parameters useful when
     *        generating the output structure; never null
     * @return true if the sequencer's output should be saved, or false otherwise
     * @throws Exception if there was a problem with the sequencer that could not be handled. All exceptions will be logged
     *         automatically as errors by ModeShape.
     */
    public abstract boolean execute( Property inputProperty,
                                     Node outputNode,
                                     Context context ) throws Exception;

    @Override
    public String toString() {
        return repositoryName + " -> " + name + (description != null ? (" : " + description) : "");
    }

    /**
     * Registers a namespace using the given {@link NamespaceRegistry}, if the namespace has not been previously registered. 
     * 
     * @param namespacePrefix a non-null {@code String}
     * @param namespaceUri a non-null {@code String}
     * @param namespaceRegistry a {@code NamespaceRegistry} instance.
     * @return true if the namespace has been registered, or false if it was already registered
     * 
     * @throws RepositoryException if anything fails during the registration process
     */
    protected boolean registerNamespace(String namespacePrefix, String namespaceUri, NamespaceRegistry namespaceRegistry) throws RepositoryException {
        if (namespacePrefix == null || namespaceUri == null) {
            throw new IllegalArgumentException("Neither the namespace prefix, nor the uri should be null");
        }
        try {
            //if the call succeeds, means it was previously registered
            namespaceRegistry.getPrefix(namespaceUri);
            return false;
        }
        catch (NamespaceException e) {
            //namespace not registered yet
            namespaceRegistry.registerNamespace(namespacePrefix, namespaceUri);
            return true;
        }
    }

    /**
     * Registers types from a CDN file, using the given {@link NodeTypeManager}
     * @param cndFile the relative path to the cnd file, which is loaded using via {@link Class#getResourceAsStream(String)}
     * @param nodeTypeManager the node type manager with which the cnd will be registered.
     * @throws RepositoryException
     * @throws NodeTypeExistsException
     * @throws IOException
     */
    protected void registerCND(String cndFile, NodeTypeManager nodeTypeManager) throws RepositoryException, NodeTypeExistsException, IOException {
        InputStream cndStream = getClass().getResourceAsStream(cndFile);
        if (cndStream == null) {
            throw new IllegalStateException("Cannot locate cnd file: " + cndFile);
        }
        nodeTypeManager.registerNodeTypes(cndStream, false);
    }
    
    /**
     * The sequencer context represents the complete context of a sequencer invocation. Currently, this information includes the
     * current time of sequencer execution.
     */
    public interface Context {

        /**
         * Get the timestamp of the sequencing. This is always the timestamp of the change event that is being processed.
         * 
         * @return timestamp the "current" timestamp; never null
         */
        Calendar getTimestamp();
    }

}
