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

import java.io.InputStream;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import org.xml.sax.ContentHandler;

/**
 * @author John Verhaeg
 * @author Randall Hauch
 */
final class JcrWorkspace implements Workspace {

    private final String name;
    private final JcrSession session;
    private final NamespaceRegistry namespaceRegistry;

    /**
     * @param session the session that owns this workspace; may not be null
     * @param name the name of the workspace; may not be null
     * @throws RepositoryException
     */
    JcrWorkspace( JcrSession session,
                  String name ) throws RepositoryException {
        assert session != null;
        assert name != null;
        this.session = session;
        this.name = name;
        this.namespaceRegistry = new JcrNamespaceRegistry(session.getExecutionContext().getNamespaceRegistry());
        // Ensure workspace with supplied name is accessible
        // if (name == null) name = session.getDnaRepository().getSource(session.getSubject()).getName();
        // String matchedName = null;
        // for (String accessibleName : getAccessibleWorkspaceNames()) {
        // if (name.equalsIgnoreCase(accessibleName)) {
        // matchedName = name;
        // break;
        // }
        // }
        // if (matchedName == null) {
        // throw new LoginException();
        // }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Workspace#clone(java.lang.String, java.lang.String, java.lang.String, boolean)
     */
    public void clone( String srcWorkspace,
                       String srcAbsPath,
                       String destAbsPath,
                       boolean removeExisting ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void copy( String srcAbsPath,
                      String destAbsPath ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void copy( String srcWorkspace,
                      String srcAbsPath,
                      String destAbsPath ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getAccessibleWorkspaceNames() {
        // try {
        // Node node = session.getRootNode().getNode("dna:jcr");
        // if (node != null) {
        // Property property = node.getProperty("dna:workspaceNames");
        // if (property != null) {
        // Value[] values = property.getValues();
        // if (values.length > 0) {
        // String[] names = new String[values.length];
        // for (int ndx = values.length; --ndx >= 0;) {
        // names[ndx] = values[ndx].getString();
        // }
        // return names;
        // }
        // }
        // }
        // } catch (PathNotFoundException meansOnlyDefaultWorkspaceNameAvailable) {
        // // TODO: Check permissions and, if writable, create node & property and, if allowed, set to include this source's name
        // }
        // // Repository is read-only, so just return this source's name
        // return new String[] {session.getDnaRepository().getSource(session.getSubject()).getName()};
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public ContentHandler getImportContentHandler( String parentAbsPath,
                                                   int uuidBehavior ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Workspace#getNamespaceRegistry()
     */
    public NamespaceRegistry getNamespaceRegistry() {
        return namespaceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    public NodeTypeManager getNodeTypeManager() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public ObservationManager getObservationManager() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public QueryManager getQueryManager() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Session getSession() {
        return this.session;
    }

    /**
     * {@inheritDoc}
     */
    public void importXML( String parentAbsPath,
                           InputStream in,
                           int uuidBehavior ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void move( String srcAbsPath,
                      String destAbsPath ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void restore( Version[] versions,
                         boolean removeExisting ) {
        throw new UnsupportedOperationException();
    }
}
