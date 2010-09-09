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
package org.modeshape.jcr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeDefinition;
import org.modeshape.cnd.CndImporter;
import org.modeshape.common.collection.Problems;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.io.Destination;
import org.modeshape.graph.property.Path;

/**
 * A class that reads files in the standard CND format defined by the JCR 2.0 specification.
 * <p>
 * Typically, the class will be used like this:
 * 
 * <pre>
 * Session session = ...
 * CndNodeTypeReader reader = new CndNodeTypeReader(session);
 * reader.read(file); // or stream or resource file
 * 
 * if (!reader.getProblems().isEmpty()) {
 *     // Report problems
 * } else {
 *     boolean allowUpdate = false;
 *     session.getWorkspace().getNodeTypeManager().registerNodeTypes(reader.getNodeTypeDefinitions(), allowUpdate);
 * }
 * </pre>
 * 
 * </p>
 */
public class CndNodeTypeReader extends GraphNodeTypeReader {

    /**
     * Create a new node type factory that reads CND files.
     * 
     * @param session the session that will be used to register the node types; may not be null
     */
    public CndNodeTypeReader( Session session ) {
        super(session);
    }

    /**
     * Create a new node type factory that reads CND files.
     * 
     * @param context the context that will be used to load the node types; may not be null
     */
    public CndNodeTypeReader( ExecutionContext context ) {
        super(context);
    }

    /**
     * Import the node types from the supplied stream and add all of the node type definitions to this factory's list. This method
     * will close the stream.
     * 
     * @param stream the stream containing the node types
     * @param resourceName a logical name for the resource name to be used when reporting problems; may be null if there is no
     *        useful name
     * @throws IOException if there is a problem reading from the supplied stream
     */
    @Override
    public void read( InputStream stream,
                      String resourceName ) throws IOException {
        super.read(stream, resourceName);
    }

    /**
     * Import the node types from the supplied file and add all of the node type definitions to this factory's list.
     * 
     * @param file the file containing the node types
     * @throws IllegalArgumentException if the supplied file reference is null, or if the file does not exist or is not readable
     * @throws IOException if there is a problem reading from the supplied stream
     */
    @Override
    public void read( File file ) throws IOException {
        super.read(file);
    }

    /**
     * Import the node types from the file at the supplied URL and add all of the node type definitions to this factory's list.
     * 
     * @param url the URL to the file containing the node types
     * @throws IllegalArgumentException if the supplied URL is null
     * @throws IOException if there is a problem opening or reading the stream to the supplied URL
     */
    @Override
    public void read( URL url ) throws IOException {
        super.read(url);
    }

    /**
     * Import the node types from the file at the supplied path, and add all of the node type definitions to this factory's list.
     * This method first attempts to resolve the supplied path to a resource on the classpath. If such a resource could not be
     * found, this method considers the supplied argument as the path to an existing and readable file. If that does not succeed,
     * this method treats the supplied argument as a valid and resolvable URL.
     * 
     * @param resourceFile the name of the resource file on the classpath containing the node types
     * @throws IllegalArgumentException if the supplied string is null or empty
     * @throws IOException if there is a problem reading from the supplied resource, or if the resource could not be found
     */
    @Override
    public void read( String resourceFile ) throws IOException {
        super.read(resourceFile);
    }

    /**
     * Import the node types from the supplied string and add all of the node type definitions to this factory's list.
     * 
     * @param content the string containing the node types
     * @param resourceName a logical name for the resource name to be used when reporting problems; may be null if there is no
     *        useful name
     */
    @Override
    public void read( String content,
                      String resourceName ) {
        super.read(content, resourceName);
    }

    /**
     * Import the node types from the supplied location in the specified graph.
     * 
     * @param graph the graph containing the standard ModeShape CND content
     * @param parentOfTypes the path to the parent of the node type definition nodes
     * @param resourceName a logical name for the resource name to be used when reporting problems; may be null if there is no
     *        useful name
     */
    @Override
    public void read( Graph graph,
                      Path parentOfTypes,
                      String resourceName ) {
        super.read(graph, parentOfTypes, resourceName);
    }

    /**
     * Import the node types from the supplied location in the specified graph.
     * 
     * @param subgraph the subgraph containing the standard ModeShape CND content
     * @param locationOfParent the location to the parent of the node type definition nodes
     * @param resourceName a logical name for the resource name to be used when reporting problems; may be null if there is no
     *        useful name
     */
    @Override
    public void read( Subgraph subgraph,
                      Location locationOfParent,
                      String resourceName ) {
        super.read(subgraph, locationOfParent, resourceName);
    }

    /**
     * Get the problems where warnings and error messages were recorded by this factory.
     * 
     * @return the problems; never null
     */
    @Override
    public Problems getProblems() {
        return super.getProblems();
    }

    /**
     * Returns the node type definitions created by this factory.
     * 
     * @return the {@link NodeTypeDefinition}s
     */
    @Override
    public NodeTypeDefinition[] getNodeTypeDefinitions() {
        return super.getNodeTypeDefinitions();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<NodeTypeDefinition> iterator() {
        return super.iterator();
    }

    protected void readBuiltInTypes() throws IOException {
        read("/org/modeshape/jcr/jsr_283_builtins.cnd");
        read("/org/modeshape/jcr/modeshape_builtins.cnd");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.GraphNodeTypeReader#importFrom(org.modeshape.graph.io.Destination,
     *      org.modeshape.graph.property.Path, java.lang.String, java.lang.String)
     */
    @Override
    protected void importFrom( Destination destination,
                               Path path,
                               String content,
                               String resourceName ) throws Exception {
        CndImporter importer = new CndImporter(destination, pathFactory.createRootPath());
        importer.importFrom(content, problems, resourceName);
    }
}
