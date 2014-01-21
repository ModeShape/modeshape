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
package org.modeshape.test.ri;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.core.TransientRepository;
import org.junit.After;
import org.junit.Before;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.StringUtil;

/**
 * 
 */
public abstract class AbstractTest {

    private Repository repository;
    private Session session;
    protected Credentials credentials;
    protected boolean print;
    private boolean deleteAfterTest;

    @Before
    public void beforeEach() {
        File target = new File("target").getAbsoluteFile();
        String targetPath = target.getAbsolutePath();
        System.setProperty("derby.stream.error.file", targetPath + "/derby.log");

        print = false;
        deleteAfterTest = true;
    }

    @After
    public void afterEach() {
        shutdownRepository();
        System.out.flush();
    }

    public void startTransientRepository() {
        startTransientRepository(null);
    }

    public void startTransientRepository( String configFilePath ) {
        File target = new File("target/jackrabbit");
        deleteDirectory(target.getPath());
        if (configFilePath != null) {
            File configFile = new File(configFilePath);
            if (!configFile.exists()) {
                if (!configFilePath.startsWith("/")) configFilePath = "/" + configFilePath;
                configFile = new File("src/test/resources" + configFilePath);
            }
            repository = new TransientRepository(configFile, target);
        } else {
            repository = new TransientRepository(target);
        }
        setCredentials(defaultCredentials());
    }

    public void shutdownRepository() {
        if (repository != null) {
            try {
                if (session != null) session.logout();
            } finally {
                try {
                    if (repository instanceof JackrabbitRepository) {
                        JackrabbitRepository jr = (JackrabbitRepository)repository;
                        jr.shutdown();
                    }
                    if (repository instanceof TransientRepository) {
                        TransientRepository trans = (TransientRepository)repository;
                        deleteDirectory(trans.getHomeDir());
                    }
                } finally {
                    repository = null;
                }
            }
        }
    }

    protected void deleteDirectory( String path ) {
        if (path == null) return;
        File dir = new File(path).getAbsoluteFile();
        if (dir.exists() && dir.canWrite()) {
            if (deleteAfterTest) {
                if (print) {
                    print("Deleting \"" + dir + "\"");
                }
                FileUtil.delete(dir);
            } else if (print) {
                print("Skipping deletion of \"" + dir + "\"");
            }
        }
    }

    protected void print( Object msg ) {
        if (print && msg != null) {
            System.out.println(msg.toString());
        }
    }

    protected Repository repository() {
        if (repository == null) startTransientRepository();
        return repository;
    }

    protected Credentials credentials( String username,
                                       String password ) {
        return new SimpleCredentials(username, password.toCharArray());
    }

    protected Credentials defaultCredentials() {
        return credentials("adminId", "admin");
    }

    protected void setCredentials( Credentials credentials ) {
        this.credentials = credentials;
    }

    protected void setCredentials( String username,
                                   String password ) {
        setCredentials(credentials(username, password));
    }

    protected Session session() throws RepositoryException {
        if (session != null) {
            if (session.isLive()) return session;
            // Otherwise it's not valid anymore ...
            try {
                session.logout();
            } finally {
                session = null;
            }
        }
        if (session == null) {
            Repository repository = repository();
            if (credentials != null) {
                session = repository.login(credentials);
            } else {
                session = repository.login();
            }
        }
        return session;
    }

    protected Node rootNode() throws RepositoryException {
        return session().getRootNode();
    }

    protected Version checkin( Node node ) throws RepositoryException {
        return checkin(node.getPath());
    }

    protected Version checkin( String path ) throws RepositoryException {
        return versionManager().checkin(path);
    }

    protected void checkout( Node node ) throws RepositoryException {
        checkout(node.getPath());
    }

    protected void checkout( String path ) throws RepositoryException {
        versionManager().checkout(path);
    }

    protected void restore( Node node,
                            Version version,
                            boolean removeExisting ) throws RepositoryException {
        versionManager().restore(version, removeExisting);
    }

    protected VersionManager versionManager() throws RepositoryException {
        return session().getWorkspace().getVersionManager();
    }

    protected VersionHistory versionHistory( Node node ) throws RepositoryException {
        return versionManager().getVersionHistory(node.getPath());
    }

    protected VersionHistory versionHistory( String path ) throws RepositoryException {
        return versionManager().getVersionHistory(path);
    }

    protected void printVersionHistory( Node node ) throws RepositoryException {
        printSubgraph(node, 1);
        printSubgraph(versionHistory(node));
    }

    /**
     * Load the subgraph below this node, and print it to System.out if printing is enabled.
     * 
     * @param node the root of the subgraph
     * @throws RepositoryException
     */
    protected void printSubgraph( Node node ) throws RepositoryException {
        printSubgraph(node, Integer.MAX_VALUE);
    }

    /**
     * Load the subgraph below this node, and print it to System.out if printing is enabled.
     * 
     * @param node the root of the subgraph
     * @param maxDepth the maximum depth of the subgraph that should be printed
     * @throws RepositoryException
     */
    protected void printSubgraph( Node node,
                                  int maxDepth ) throws RepositoryException {
        printSubgraph(node, " ", node.getDepth(), maxDepth);
    }

    /**
     * Print this node and its properties to System.out if printing is enabled.
     * 
     * @param node the node to be printed
     * @throws RepositoryException
     */
    protected void printNode( Node node ) throws RepositoryException {
        printSubgraph(node, " ", node.getDepth(), 1);
    }

    /**
     * Load the subgraph below this node, and print it to System.out if printing is enabled.
     * 
     * @param node the root of the subgraph
     * @param lead the string that each line should begin with; may be null if there is no such string
     * @param depthOfSubgraph the depth of this subgraph's root node
     * @param maxDepthOfSubgraph the maximum depth of the subgraph that should be printed
     * @throws RepositoryException
     */
    private void printSubgraph( Node node,
                                String lead,
                                int depthOfSubgraph,
                                int maxDepthOfSubgraph ) throws RepositoryException {
        if (!print) return;

        int currentDepth = node.getDepth() - depthOfSubgraph + 1;
        if (currentDepth > maxDepthOfSubgraph) return;
        if (lead == null) lead = "";
        String nodeLead = lead + StringUtil.createString(' ', (currentDepth - 1) * 2);

        StringBuilder sb = new StringBuilder();
        sb.append(nodeLead);
        if (node.getDepth() == 0) {
            sb.append("/");
        } else {
            sb.append(node.getName());
            if (node.getIndex() != 1) {
                sb.append('[').append(node.getIndex()).append(']');
            }
        }
        sb.append(" jcr:primaryType=" + node.getPrimaryNodeType().getName());
        boolean referenceable = node.isNodeType("mix:referenceable");
        if (node.getMixinNodeTypes().length != 0) {
            sb.append(" jcr:mixinTypes=[");
            boolean first = true;
            for (NodeType mixin : node.getMixinNodeTypes()) {
                if (first) first = false;
                else sb.append(',');
                sb.append(mixin.getName());
            }
            sb.append(']');
        }
        if (referenceable) {
            sb.append(" jcr:uuid=" + node.getIdentifier());
        }
        System.out.println(sb);

        List<String> propertyNames = new LinkedList<String>();
        for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
            Property property = iter.nextProperty();
            String name = property.getName();
            if (name.equals("jcr:primaryType") || name.equals("jcr:mixinTypes") || name.equals("jcr:uuid")) continue;
            propertyNames.add(property.getName());
        }
        Collections.sort(propertyNames);
        for (String propertyName : propertyNames) {
            Property property = node.getProperty(propertyName);
            sb = new StringBuilder();
            sb.append(nodeLead).append("  - ").append(propertyName).append('=');
            boolean binary = property.getType() == PropertyType.BINARY;
            if (property.isMultiple()) {
                sb.append('[');
                boolean first = true;
                for (Value value : property.getValues()) {
                    if (first) first = false;
                    else sb.append(',');
                    if (binary) {
                        sb.append(value.getBinary());
                    } else {
                        sb.append(value.getString());
                    }
                }
                sb.append(']');
            } else {
                Value value = property.getValue();
                if (binary) {
                    sb.append(value.getBinary());
                } else {
                    sb.append(value.getString());
                }
            }
            System.out.println(sb);
        }

        if (currentDepth < maxDepthOfSubgraph) {
            for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                Node child = iter.nextNode();
                printSubgraph(child, lead, depthOfSubgraph, maxDepthOfSubgraph);
            }
        }
    }

    protected void loadNodeTypes( Reader reader ) throws IOException, RepositoryException, ParseException {
        assertThat(reader, is(notNullValue()));
        CndImporter.registerNodeTypes(reader, session());
    }

    protected void loadNodeTypes( String cndResourcePath ) throws IOException, RepositoryException, ParseException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(cndResourcePath);
        assertThat("The resource \"" + cndResourcePath + "\" could not be found on the classpath", stream, is(notNullValue()));
        Reader reader = new InputStreamReader(stream);
        loadNodeTypes(reader);
    }

    protected NodeType verifyNodeTypeExists( String nodeTypeName ) throws RepositoryException {
        NodeType nodeType = session().getWorkspace().getNodeTypeManager().getNodeType(nodeTypeName);
        assertThat(nodeType, is(notNullValue()));
        return nodeType;
    }
}
