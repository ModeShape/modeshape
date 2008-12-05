/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.svn;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connectors.RepositorySourceException;
import org.jboss.dna.graph.properties.Binary;
import org.jboss.dna.graph.properties.DateTimeFactory;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.NameFactory;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathFactory;
import org.jboss.dna.graph.properties.PathNotFoundException;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.properties.PropertyFactory;
import org.jboss.dna.graph.properties.ValueFactory;
import org.jboss.dna.graph.properties.basic.BasicMultiValueProperty;
import org.jboss.dna.graph.properties.basic.InMemoryBinary;
import org.jboss.dna.graph.requests.CopyBranchRequest;
import org.jboss.dna.graph.requests.CreateNodeRequest;
import org.jboss.dna.graph.requests.DeleteBranchRequest;
import org.jboss.dna.graph.requests.MoveBranchRequest;
import org.jboss.dna.graph.requests.ReadAllChildrenRequest;
import org.jboss.dna.graph.requests.ReadAllPropertiesRequest;
import org.jboss.dna.graph.requests.RemovePropertiesRequest;
import org.jboss.dna.graph.requests.RenameNodeRequest;
import org.jboss.dna.graph.requests.Request;
import org.jboss.dna.graph.requests.UpdatePropertiesRequest;
import org.jboss.dna.graph.requests.processor.RequestProcessor;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * The {@link RequestProcessor} implementation for the file subversion repository connector. This is the class that does the bulk
 * of the work in the subversion repository connector, since it processes all requests.
 * 
 * @author Serge Emmanuel Pagop
 */
public class SVNRepositoryRequestProcessor extends RequestProcessor {

    protected static final String BACK_SLASH = "/";

    private final String defaultNamespaceUri;
    private final boolean updatesAllowed;
    private SVNRepository repository;

    /**
     * @param sourceName
     * @param context
     * @param repository
     * @param updatesAllowed true if this connector supports updating the subversion repository, or false if the connector is read
     *        only
     */
    protected SVNRepositoryRequestProcessor( String sourceName,
                                             ExecutionContext context,
                                             SVNRepository repository,
                                             boolean updatesAllowed ) {
        super(sourceName, context);
        this.defaultNamespaceUri = getExecutionContext().getNamespaceRegistry().getDefaultNamespaceUri();
        this.updatesAllowed = updatesAllowed;
        this.repository = repository;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.CopyBranchRequest)
     */
    @Override
    public void process( CopyBranchRequest request ) {
        verifyUpdatesAllowed();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.CreateNodeRequest)
     */
    @Override
    public void process( CreateNodeRequest request ) {
        verifyUpdatesAllowed();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.DeleteBranchRequest)
     */
    @Override
    public void process( DeleteBranchRequest request ) {
        verifyUpdatesAllowed();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.MoveBranchRequest)
     */
    @Override
    public void process( MoveBranchRequest request ) {
        verifyUpdatesAllowed();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.ReadAllChildrenRequest)
     */
    @SuppressWarnings( "unchecked" )
    @Override
    public void process( ReadAllChildrenRequest request ) {
        Location myLocation = request.of();
        Path nodePath = getPathFor(myLocation, request);
        try {
            SVNNodeKind kind = validateNodeKind(nodePath);
            String requestedNodePath = nodePath.getString(getExecutionContext().getNamespaceRegistry());
            if (kind == SVNNodeKind.FILE) { // the requested node is a file.
                SVNDirEntry entry = getEntryInfo(requestedNodePath);
                if (!nodePath.getLastSegment().getName().equals(JcrLexicon.CONTENT)) {
                    String localName = entry.getName();
                    Name childName = nameFactory().create(defaultNamespaceUri, localName);
                    String url = entry.getURL().toString();
                    Property idProperty = propertyFactory().create(childName, url);
                    request.addChild(new Location(pathFactory().create(nodePath, JcrLexicon.CONTENT), idProperty));
                }
            } else if (kind == SVNNodeKind.DIR) { // the requested node is a directory.
                final Collection<SVNDirEntry> dirEntries = getRepository().getDir(requestedNodePath,
                                                                                  -1,
                                                                                  null,
                                                                                  (Collection<SVNDirEntry>)null);
                for (SVNDirEntry dirEntry : dirEntries) {
                    if (dirEntry.getKind() == SVNNodeKind.FILE) {
                        String localName = dirEntry.getName();
                        Path newPath = pathFactory().create(requestedNodePath + BACK_SLASH + localName);
                        if (!newPath.getLastSegment().getName().equals(JcrLexicon.CONTENT)) {
                            Name childName = nameFactory().create(defaultNamespaceUri, localName);
                            String url = dirEntry.getURL().toString();
                            Property idProperty = propertyFactory().create(childName, url);
                            Location location = new Location(pathFactory().create(newPath, JcrLexicon.CONTENT), idProperty);
                            request.addChild(location);
                        }
                    } else if (dirEntry.getKind() == SVNNodeKind.DIR) {
                        String localName = dirEntry.getName();
                        Name childName = nameFactory().create(defaultNamespaceUri, localName);
                        Path childPath = pathFactory().create(nodePath, childName);
                        String url = dirEntry.getURL().toString();
                        Property idProperty = propertyFactory().create(childName, url);
                        request.addChild(childPath, idProperty);
                    }
                }
            }
            request.setActualLocationOfNode(myLocation);
        } catch (SVNException e) {
            // if a failure occured while connecting to a repository
            // or the user's authentication failed (see
            // path not found in the specified revision
            // or is not a directory
            e.printStackTrace();
            request.setError(e);
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.ReadAllPropertiesRequest)
     */
    @Override
    public void process( ReadAllPropertiesRequest request ) {
        Location myLocation = request.at();
        Path nodePath = getPathFor(myLocation, request);
        if (nodePath.isRoot()) {
            // There are no properties on the root ...
            request.setActualLocationOfNode(myLocation);
            return;
        }
        try {
            // See if the path is a "jcr:content" node ...
            if (nodePath.getLastSegment().getName().equals(JcrLexicon.CONTENT)) {
                // //"jcr:primaryType" property value of "nt:resource",
                // "jcr:data" property whose value are the contents of the file
                // and a few other properties, like "jcr:encoding", "jcr:mimeType" and "jcr:lastModified" and
                // also "jcr:created" property
                Path parent = nodePath.getParent();
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                SVNProperties fileProperties = new SVNProperties();
                List<Property> properties = new ArrayList<Property>();
                getData(parent.getString(getExecutionContext().getNamespaceRegistry()), fileProperties, os);
                Property ntResourceproperty = propertyFactory().create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE);
                properties.add(ntResourceproperty);
                String mimeType = fileProperties.getStringValue(SVNProperty.MIME_TYPE);
                if (mimeType != null) {
                    Property jcrMimeTypeProperty = propertyFactory().create(JcrLexicon.MIMETYPE, mimeType);
                    properties.add(jcrMimeTypeProperty);
                }
                SVNDirEntry entry = getEntryInfo(parent.getString(getExecutionContext().getNamespaceRegistry()));
                Date lastModified = entry.getDate();
                if (lastModified != null) {
                    Property jcrLastModifiedProperty = propertyFactory().create(JcrLexicon.LAST_MODIFIED,
                                                                                dateFactory().create(lastModified));
                    properties.add(jcrLastModifiedProperty);
                }
                if (os.toByteArray().length > 0) {
                    Property jcrDataProperty = propertyFactory().create(JcrLexicon.DATA,
                                                                        binaryFactory().create(new InMemoryBinary(
                                                                                                                  os.toByteArray())));
                    properties.add(jcrDataProperty);
                }
                request.addProperties(properties.toArray(new BasicMultiValueProperty[0]));
            } else {
                SVNNodeKind kind = validateNodeKind(nodePath);
                if (kind == SVNNodeKind.FILE) {
                    // "jcr:primaryType" property whose value is "nt:file".
                    Property ntFileProperty = propertyFactory().create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE);
                    request.addProperty(ntFileProperty);
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    SVNProperties fileProperties = new SVNProperties();
                    getData(nodePath.getString(getExecutionContext().getNamespaceRegistry()), fileProperties, os);
                    String created = fileProperties.getStringValue(SVNProperty.COMMITTED_DATE);
                    if (created != null) {
                        Property jcrCreatedProperty = propertyFactory().create(JcrLexicon.CREATED, created);
                        request.addProperty(jcrCreatedProperty);
                    }

                } else if (kind == SVNNodeKind.DIR) {
                    // A directory maps to a single node with a name that represents the name of the directory and a
                    // "jcr:primaryType" property whose value is "nt:folder"
                    Property property = propertyFactory().create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FOLDER);
                    request.addProperty(property);
                    SVNDirEntry dirEntry = getEntryInfo(nodePath.getString(getExecutionContext().getNamespaceRegistry()));
                    request.addProperty(propertyFactory().create(JcrLexicon.CREATED, dateFactory().create(dirEntry.getDate())));
                }
            }
            request.setActualLocationOfNode(myLocation);

        } catch (SVNException e) {
            e.printStackTrace();
            request.setError(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.RemovePropertiesRequest)
     */
    @Override
    public void process( RemovePropertiesRequest request ) {
        verifyUpdatesAllowed();
        super.process(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.RenameNodeRequest)
     */
    @Override
    public void process( RenameNodeRequest request ) {
        verifyUpdatesAllowed();
        super.process(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.UpdatePropertiesRequest)
     */
    @Override
    public void process( UpdatePropertiesRequest request ) {
    }

    protected void verifyUpdatesAllowed() {
        if (!updatesAllowed) {
            throw new RepositorySourceException(getSourceName(),
                                                SVNRepositoryConnectorI18n.sourceIsReadOnly.text(getSourceName()));
        }
    }

    /**
     * Factory for sample name.
     * 
     * @return the name factory
     */
    protected NameFactory nameFactory() {
        return getExecutionContext().getValueFactories().getNameFactory();
    }

    protected PathFactory pathFactory() {
        return getExecutionContext().getValueFactories().getPathFactory();
    }

    protected PropertyFactory propertyFactory() {
        return getExecutionContext().getPropertyFactory();
    }

    protected DateTimeFactory dateFactory() {
        return getExecutionContext().getValueFactories().getDateFactory();
    }

    protected ValueFactory<Binary> binaryFactory() {
        return getExecutionContext().getValueFactories().getBinaryFactory();
    }

    protected Path getPathFor( Location location,
                               Request request ) {
        Path path = location.getPath();
        if (path == null) {
            I18n msg = SVNRepositoryConnectorI18n.locationInRequestMustHavePath;
            throw new RepositorySourceException(getSourceName(), msg.text(getSourceName(), request));
        }
        return path;
    }

    /**
     * Get the content of a file.
     * 
     * @param path - the path to that file.
     * @param properties - the properties of the file.
     * @param os - the output stream where to store the content.
     * @throws SVNException - throws if such path is not at that revision or in case of a connection problem.
     */
    protected void getData( String path,
                            SVNProperties properties,
                            OutputStream os ) throws SVNException {
        getRepository().getFile(path, -1, properties, os);

    }

    /**
     * @return repository
     */
    public SVNRepository getRepository() {
        return repository;
    }

    /**
     * Validate the kind of node and throws an exception if necessary.
     * 
     * @param requestedPath
     * @return the kind.
     */
    protected SVNNodeKind validateNodeKind( final Path requestedPath ) {
        String myPath = requestedPath.getString(getExecutionContext().getNamespaceRegistry());
        SVNNodeKind kind = null;
        try {
            kind = getRepository().checkPath(myPath, -1);
            if (kind == SVNNodeKind.NONE) {
                // node does not exist or requested node is not correct.
                throw new PathNotFoundException(new Location(requestedPath), null,
                                                SVNRepositoryConnectorI18n.nodeDoesNotExist.text(myPath));
            } else if (kind == SVNNodeKind.UNKNOWN) {
                // node is unknown
                throw new PathNotFoundException(new Location(requestedPath), null,
                                                SVNRepositoryConnectorI18n.nodeIsActuallyUnknow.text(myPath));
            }
        } catch (SVNException e) {
            // if a failure occured while connecting to a repository
            // * or the user's authentication failed (see
            // * {@link org.tmatesoft.svn.core.SVNAuthenticationException})
            // TODO RepositorySourceAuthenticationException
        }

        return kind;
    }

    /**
     * Get some important informations of a path
     * 
     * @param path - the path
     * @return - the {@link SVNDirEntry}.
     */
    protected SVNDirEntry getEntryInfo( String path ) {
        assert path != null;
        SVNDirEntry entry = null;
        try {
            entry = getRepository().info(path, -1);
        } catch (SVNException e) {
            // if a failure occured while connecting to a repository
            // * or the user's authentication failed (see
            // * {@link org.tmatesoft.svn.core.SVNAuthenticationException})
            // TODO RepositorySourceAuthenticationException
        }
        return entry;
    }
}
