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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.modeshape.jcr.api.Logger;
import org.modeshape.jcr.api.mimetype.MimeTypeDetector;
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

    private final UUID uuid = UUID.randomUUID();

    /**
     * The logger instance, set via reflection
     */
    private Logger logger;

    /**
     * The name of this sequencer, set via reflection
     */
    private String name;

    /**
     * The name of the repository that owns this sequencer, set via reflection
     */
    private String repositoryName;

    /**
     * The multiple path expressions for this sequencer, set via reflection
     */
    private Object[] pathExpressions;

    /**
     * The singular path expression of this sequencer, set via reflection
     */
    private String pathExpression;

    /**
     * The set of MIME types that this sequencer will process. Subclasses should set call
     * {@link #registerDefaultMimeTypes(String...)} in the no-arg constructor to set the default MIME types for the sequencer, but
     * the field may be overwritten in the sequencer's configuration by setting the "acceptedMimeTypes" field to an array of
     * string values.
     */
    private String[] acceptedMimeTypes = {};

    private Set<String> acceptedMimeTypesSet = null;

    private boolean initialized = false;

    /**
     * Return the unique identifier for this sequencer.
     * 
     * @return the unique identifier; never null
     */
    public final UUID getUniqueId() {
        return uuid;
    }

    /**
     * Get the name for this sequencer.
     * 
     * @return the name, or null if there is no description
     */
    public final String getName() {
        return name;
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
        assert !initialized : "No expressions can be added after the sequencer has been initialized";
        if (value instanceof String) {
            String str = (String)value;
            str = str.trim();
            if (str.length() != 0) {
                values.add(str);
            }
        }
    }

    /**
     * Initialize the sequencer. This is called automatically by ModeShape once for each Sequencer instance, and should not be
     * called by the sequencer.
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
        // Subclasses may not necessarily call 'super.initialize(...)', but if they do then we can make this assertion ...
        assert !initialized : "The Sequencer.initialize(...) method should not be called by subclasses; ModeShape has already (and automatically) initialized the Sequencer";
    }

    /**
     * Method called by the code calling {@link #initialize(NamespaceRegistry, NodeTypeManager)} (typically via reflection) to
     * signal that the initialize method is completed. See Sequencers.initialize() for details, and no this method is indeed used.
     */
    @SuppressWarnings( "unused" )
    private void postInitialize() {
        if (!initialized) {
            initialized = true;

            // ------------------------------------------------------------------------------------------------------------
            // Add any code here that needs to run after #initialize(...), which will be overwritten by subclasses
            // ------------------------------------------------------------------------------------------------------------

            // Make immutable the Set<String> of accepts MIME types ...
            acceptedMimeTypesSet = Collections.unmodifiableSet(getAcceptedMimeTypes());
        }
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
     * <p>
     * Note: This method <em>must</em> be threadsafe: ModeShape will likely invoke this method concurrently in separate threads,
     * and the method should never modify the state or fields of the Sequencer implementation class. All initialization should be
     * performed in {@link #initialize(NamespaceRegistry, NodeTypeManager)}.
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
        return repositoryName + " -> " + getClass().getName() + " uuid=" + uuid + (name != null ? (" : " + name) : "");
    }

    /**
     * Registers a namespace using the given {@link NamespaceRegistry}, if the namespace has not been previously registered.
     * 
     * @param namespacePrefix a non-null {@code String}
     * @param namespaceUri a non-null {@code String}
     * @param namespaceRegistry a {@code NamespaceRegistry} instance.
     * @return true if the namespace has been registered, or false if it was already registered
     * @throws RepositoryException if anything fails during the registration process
     */
    protected boolean registerNamespace( String namespacePrefix,
                                         String namespaceUri,
                                         NamespaceRegistry namespaceRegistry ) throws RepositoryException {
        if (namespacePrefix == null || namespaceUri == null) {
            throw new IllegalArgumentException("Neither the namespace prefix, nor the uri should be null");
        }
        try {
            // if the call succeeds, means it was previously registered
            namespaceRegistry.getPrefix(namespaceUri);
            return false;
        } catch (NamespaceException e) {
            // namespace not registered yet
            namespaceRegistry.registerNamespace(namespacePrefix, namespaceUri);
            return true;
        }
    }

    /**
     * Registers node types from a CND file, using the given {@link NodeTypeManager}. Any namespaces defined in the CND file will
     * be automatically registered as well.
     * 
     * @param cndFile the relative path to the cnd file, which is loaded using via {@link Class#getResourceAsStream(String)}
     * @param nodeTypeManager the node type manager with which the cnd will be registered
     * @param allowUpdate a boolean which indicates whether updates on existing node types are allowed or no. See
     *        {@link NodeTypeManager#registerNodeType(javax.jcr.nodetype.NodeTypeDefinition, boolean)}
     * @throws RepositoryException if anything fails
     * @throws IOException if any stream related operations fail
     */
    protected void registerNodeTypes( String cndFile,
                                      NodeTypeManager nodeTypeManager,
                                      boolean allowUpdate ) throws RepositoryException, IOException {
        InputStream cndStream = getClass().getResourceAsStream(cndFile);
        registerNodeTypes(cndStream, nodeTypeManager, allowUpdate);
    }

    /**
     * See {@link Sequencer#registerNodeTypes(String, org.modeshape.jcr.api.nodetype.NodeTypeManager, boolean)}
     * 
     * @param cndStream the input stream containing the CND file; may not be null
     * @param nodeTypeManager the node type manager with which the node types in the CND file should be registered; may not be
     *        null
     * @param allowUpdate a boolean which indicates whether updates on existing node types are allowed or no. See
     *        {@link NodeTypeManager#registerNodeType(javax.jcr.nodetype.NodeTypeDefinition, boolean)}
     * @throws RepositoryException if anything fails
     * @throws IOException if any stream related operations fail
     */
    protected void registerNodeTypes( InputStream cndStream,
                                      NodeTypeManager nodeTypeManager,
                                      boolean allowUpdate ) throws RepositoryException, IOException {
        if (cndStream == null) {
            throw new IllegalArgumentException("The stream to the given cnd file is null");
        }
        nodeTypeManager.registerNodeTypes(cndStream, allowUpdate);
    }

    protected final Logger getLogger() {
        return logger;
    }

    /**
     * Set the MIME types that are accepted by default, if there are any. This method should be called from the
     * {@link #initialize(NamespaceRegistry, NodeTypeManager)} method in the subclass.
     * <p>
     * This method can be called more than once to add additional mime types.
     * </p>
     * 
     * @param mimeTypes the array of MIME types that are accepted by this sequencer
     * @see #isAccepted(String)
     */
    protected final void registerDefaultMimeTypes( String... mimeTypes ) {
        assert !initialized : "No default MIME types can be registered after the sequencer has been initialized";
        if (mimeTypes != null && mimeTypes.length != 0 && acceptedMimeTypes.length == 0) {
            // There are no overridden mime types, so we can regiser the default MIME types ...
            if (acceptedMimeTypesSet == null) acceptedMimeTypesSet = new HashSet<String>();
            for (String mimeType : mimeTypes) {
                if (mimeType == null) continue;
                mimeType = mimeType.trim();
                if (mimeType.length() == 0) continue;
                acceptedMimeTypesSet.add(mimeType);
            }
        }
    }

    /**
     * Utility method to obtain the set of accepted MIME types. The resulting set will either be those set by default in the
     * subclass' overridden {@link #initialize(NamespaceRegistry, NodeTypeManager)} method or the MIME types explicitly set in the
     * sequencers configuration.
     * 
     * @return the set of MIME types that are accepted by this Sequencer instance; never null but possibly empty if this Sequencer
     *         instance accepts all MIME types
     */
    protected final Set<String> getAcceptedMimeTypes() {
        if (acceptedMimeTypesSet == null) {
            // No defaults are registered, so use those non-defaults ...
            acceptedMimeTypesSet = new HashSet<String>();
            for (String mimeType : acceptedMimeTypes) {
                if (mimeType == null) continue;
                mimeType = mimeType.trim();
                if (mimeType.length() == 0) continue;
                acceptedMimeTypesSet.add(mimeType);
            }
        }
        return acceptedMimeTypesSet;
    }

    /**
     * Determine if this sequencer requires the content to have a specific MIME type
     * 
     * @return true if this sequencer can only process certain MIME types, or false if there are no restrictions
     */
    public final boolean hasAcceptedMimeTypes() {
        return !getAcceptedMimeTypes().isEmpty();
    }

    /**
     * Determine if this sequencer has been configured to accept and process content with the supplied MIME type.
     * 
     * @param mimeType the MIME type
     * @return true if content with the supplied the MIME type is to be processed (or when <code>mimeType</code> is null and
     *         therefore not known), or false otherwise
     * @see #hasAcceptedMimeTypes()
     */
    public final boolean isAccepted( String mimeType ) {
        if (mimeType != null && hasAcceptedMimeTypes()) {
            return getAcceptedMimeTypes().contains(mimeType.trim());
        }
        return true; // accept all mime types
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

        /**
         * Returns a {@link org.modeshape.jcr.api.ValueFactory} instance which can be used to perform additional type conversions,
         * from what {@link javax.jcr.ValueFactory} offers
         * 
         * @return a non-null value factory, using the output node's session as context
         */
        org.modeshape.jcr.api.ValueFactory valueFactory();

        /**
         * Returns a {@link MimeTypeDetector} implementation which can be used to determine content mime-type.
         * 
         * @return a non-null value, using the output node's session as context
         */
        MimeTypeDetector mimeTypeDetector();
    }

}
