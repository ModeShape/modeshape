/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
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
package org.jboss.dna.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import net.jcip.annotations.Immutable;
import org.jboss.dna.cnd.CndImporter;
import org.jboss.dna.common.collection.ImmutableProblems;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.collection.SimpleProblems;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.io.Destination;
import org.jboss.dna.graph.io.GraphBatchDestination;
import org.jboss.dna.graph.property.PathFactory;

/**
 * Class to parse one or more Compact Node Definition (CND) files containing custom node type definitions into a format that can
 * be registered with the {@link RepositoryNodeTypeManager}.
 * <p>
 * The class contains methods for determining whether the CND files were parsed successfully and what errors occurred. Typically,
 * the class will be used like this:
 * 
 * <pre>
 * try {
 *  String[] cndFilePaths = // The URIs for the resource files on the classpath
 *  JcrNodeTypeSource nodeTypeSource = new CndNodeTypeSource(cndFilePaths);
 *  
 *  if (!nodeTypeSource.isValid()) {
 *      Problems problems = nodeTypeSource.getProblems();
 *      // Report problems
 *  }
 *  else {
 *      repositoryNodeTypeManager.registerNodeTypes(nodeTypeSource);
 *  }
 * }
 * catch (IOException ioe) {
 *  System.err.println(&quot;Could not find one of the CND files.&quot;);
 *  ioe.printStackTrace();
 * }
 * 
 * </pre>
 * 
 * </p>
 */
@Immutable
public class CndNodeTypeSource implements JcrNodeTypeSource {

    private final Graph graph;
    private final Problems problems;

    /**
     * Creates a new {@link JcrNodeTypeSource} with based on the CND file with the given resource name.
     * 
     * @param resourceName the name of the resource; this resource must be on the classpath
     * @throws IOException if an error loading or reading the resource occurs
     */
    public CndNodeTypeSource( String resourceName ) throws IOException {
        this(new String[] {resourceName});
    }

    /**
     * Creates a new {@link JcrNodeTypeSource} based on the CND files at the given resource names.
     * 
     * @param resourceNames the name of the resources to load; these resources must be on the classpath
     * @throws IOException if an error loading or reading the any of the resources occurs
     */
    public CndNodeTypeSource( String resourceNames[] ) throws IOException {

        Problems problems = new SimpleProblems();

        // Graph creation requires a context, but there are no security implications for this and namespace mappings are
        // specified in the CND file itself
        ExecutionContext context = new ExecutionContext();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        InMemoryRepositorySource source = new InMemoryRepositorySource();
        source.setName("CND Import Source");
        this.graph = Graph.create(source, context);
        for (String resourceName : Arrays.asList(resourceNames)) {
            Graph.Batch batch = graph.batch();
            Destination destination = new GraphBatchDestination(batch);
            CndImporter importer = new CndImporter(destination, pathFactory.createRootPath());
            InputStream is = getClass().getResourceAsStream(resourceName);

            // This submits the batch
            importer.importFrom(is, problems, resourceName);
        }
        this.problems = new ImmutableProblems(problems);
    }

    /**
     * Returns true if no errors were encountered while parsing the CND file or files
     * 
     * @return true if no errors were encountered while parsing the CND file or files
     */
    public boolean isValid() {
        return !problems.hasErrors();
    }

    /**
     * Returns the problems (if any) that were encountered parsing the CND files. Node type registration errors or warnings will
     * NOT be added to this set of problems.
     * 
     * @return returns the problems (if any) that were encountered parsing the CND files.
     */
    public Problems getProblems() {
        return problems;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.JcrNodeTypeSource#getNodeTypes()
     */
    public final Graph getNodeTypes() {
        return graph;
    }

}
