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
package org.jboss.dna.connector.filesystem;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connectors.RepositorySourceException;
import org.jboss.dna.graph.properties.DateTimeFactory;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.NameFactory;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathFactory;
import org.jboss.dna.graph.properties.PathNotFoundException;
import org.jboss.dna.graph.properties.PropertyFactory;
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

/**
 * The {@link RequestProcessor} implementation for the file systme connector. This is the class that does the bulk of the work in
 * the file system connector, since it processes all requests.
 * 
 * @author Randall Hauch
 */
public class FileSystemRequestProcessor extends RequestProcessor {

    private final Map<String, File> rootsByName;
    private final String defaultNamespaceUri;
    private final FilenameFilter filenameFilter;
    private final boolean updatesAllowed;

    /**
     * @param sourceName
     * @param context
     * @param rootsByName
     * @param filenameFilter the filename filter to use to restrict the allowable nodes, or null if all files/directories are to
     *        be exposed by this connector
     * @param updatesAllowed true if this connector supports updating the file system, or false if the connector is readonly
     */
    protected FileSystemRequestProcessor( String sourceName,
                                          ExecutionContext context,
                                          Map<String, File> rootsByName,
                                          FilenameFilter filenameFilter,
                                          boolean updatesAllowed ) {
        super(sourceName, context);
        assert rootsByName != null;
        assert rootsByName.size() >= 1;
        this.rootsByName = rootsByName;
        this.defaultNamespaceUri = getExecutionContext().getNamespaceRegistry().getDefaultNamespaceUri();
        this.filenameFilter = filenameFilter;
        this.updatesAllowed = updatesAllowed;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.ReadAllChildrenRequest)
     */
    @Override
    public void process( ReadAllChildrenRequest request ) {
        Location location = request.of();
        Path parentPath = getPathFor(location, request);
        if (parentPath.isRoot()) {
            // Add each of the root files as a child ...
            for (String rootFile : rootsByName.keySet()) {
                Path pathToRoot = pathFactory().create("/" + rootFile);
                request.addChild(new Location(pathToRoot));
            }
        } else {
            // Find the existing file for the parent ...
            File parent = getExistingFileFor(parentPath, location, request);
            if (parent.isDirectory()) {
                // Create a Location for each file and directory contained by the parent directory ...
                PathFactory pathFactory = pathFactory();
                NameFactory nameFactory = nameFactory();
                for (String localName : parent.list(filenameFilter)) {
                    Name childName = nameFactory.create(defaultNamespaceUri, localName);
                    Path childPath = pathFactory.create(parentPath, childName);
                    request.addChild(new Location(childPath));
                }
            } else {
                // The parent is a java.io.File, and the path may refer to the node that is either the "nt:file" parent
                // node, or the child "jcr:content" node...
                if (!parentPath.getLastSegment().getName().equals(JcrLexicon.CONTENT)) {
                    // This node represents the "nt:file" parent node, so the only child is the "jcr:content" node ...
                    Path contentPath = pathFactory().create(parentPath, JcrLexicon.CONTENT);
                    Location content = new Location(contentPath);
                    request.addChild(content);
                }
                // otherwise, the path ends in "jcr:content", and there are no children
            }
        }
        request.setActualLocationOfNode(location);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.ReadAllPropertiesRequest)
     */
    @Override
    public void process( ReadAllPropertiesRequest request ) {
        Location location = request.at();
        Path path = getPathFor(location, request);
        if (path.isRoot()) {
            // There are no properties on the root ...
            request.setActualLocationOfNode(location);
            return;
        }

        // Get the java.io.File object that represents the location ...
        File file = getExistingFileFor(path, location, request);
        PropertyFactory factory = getExecutionContext().getPropertyFactory();
        DateTimeFactory dateFactory = getExecutionContext().getValueFactories().getDateFactory();
        // Note that we don't have 'created' timestamps, just last modified, so we'll have to use them
        if (file.isDirectory()) {
            // Add properties for the directory ...
            request.addProperty(factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FOLDER));
            request.addProperty(factory.create(JcrLexicon.CREATED, dateFactory.create(file.lastModified())));

        } else {
            // It is a file, but ...
            if (path.getLastSegment().getName().equals(JcrLexicon.CONTENT)) {
                // The request is to get properties of the "jcr:content" child node ...
                request.addProperty(factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE));
                request.addProperty(factory.create(JcrLexicon.LAST_MODIFIED, dateFactory.create(file.lastModified())));
                // Don't really know the encoding, either ...
                // request.addProperty(factory.create(JcrLexicon.ENCODED, stringFactory.create("UTF-8")));

                // Discover the mime type ...
                // String mimeType = ...
                // request.addProperty(factory.create(JcrLexicon.MIMETYPE, mimeType));

                // Now put the file's content into the "jcr:data" property ...
                // BinaryFactory binaryFactory = getExecutionContext().getValueFactories().getBinaryFactory();
                // request.addProperty(factory.create(JcrLexicon.DATA, binaryFactory.create(file)));

            } else {
                // The request is to get properties for the node representing the file
                request.addProperty(factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE));
                request.addProperty(factory.create(JcrLexicon.CREATED, dateFactory.create(file.lastModified())));
            }

        }
        request.setActualLocationOfNode(location);
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
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.UpdatePropertiesRequest)
     */
    @Override
    public void process( UpdatePropertiesRequest request ) {
        verifyUpdatesAllowed();
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

    protected void verifyUpdatesAllowed() {
        if (!updatesAllowed) {
            throw new RepositorySourceException(getSourceName(), FileSystemI18n.sourceIsReadOnly.text(getSourceName()));
        }
    }

    protected NameFactory nameFactory() {
        return getExecutionContext().getValueFactories().getNameFactory();
    }

    protected PathFactory pathFactory() {
        return getExecutionContext().getValueFactories().getPathFactory();
    }

    protected Path getPathFor( Location location,
                               Request request ) {
        Path path = location.getPath();
        if (path == null) {
            I18n msg = FileSystemI18n.locationInRequestMustHavePath;
            throw new RepositorySourceException(getSourceName(), msg.text(getSourceName(), request));
        }
        return path;
    }

    /**
     * This utility files the existing {@link File} at the supplied path, and in the process will verify that the path is actually
     * valid.
     * <p>
     * Note that this connector represents a file as two nodes: a parent node with a name that matches the file and a "
     * <code>jcr:primaryType</code>" of "<code>nt:file</code>"; and a child node with the name "<code>jcr:content</code>" and a "
     * <code>jcr:primaryType</code>" of "<code>nt:resource</code>". The parent "<code>nt:file</code>" node and its properties
     * represents the file itself, whereas the child "<code>nt:resource</code>" node and its properties represent the content of
     * the file.
     * </p>
     * <p>
     * As such, this method will return the File object for paths representing both the parent "<code>nt:file</code>" and child "
     * <code>nt:resource</code>" node.
     * </p>
     * 
     * @param path
     * @param location the location containing the path; may not be null
     * @param request the request containing the path (and the location); may not be null
     * @return the existing {@link File file} for the path; never null
     * @throws PathNotFoundException if the path does not represent an existing file
     */
    protected File getExistingFileFor( Path path,
                                       Location location,
                                       Request request ) {
        assert path != null;
        assert location != null;
        assert request != null;
        File file = null;
        // See if the path is a "jcr:content" node ...
        if (path.getLastSegment().getName().equals(JcrLexicon.CONTENT)) {
            // We only want to use the parent path to find the actual file ...
            path = path.getParent();
        }
        for (Path.Segment segment : path) {
            String localName = segment.getName().getLocalName();
            // Verify the segment is valid ...
            if (segment.getIndex() > 1) {
                I18n msg = FileSystemI18n.sameNameSiblingsAreNotAllowed;
                throw new RepositorySourceException(getSourceName(), msg.text(getSourceName(), request));
            }
            if (!segment.getName().getNamespaceUri().equals(defaultNamespaceUri)) {
                I18n msg = FileSystemI18n.onlyTheDefaultNamespaceIsAllowed;
                throw new RepositorySourceException(getSourceName(), msg.text(getSourceName(), request));
            }
            if (file == null) {
                // We're looking to match one of the file roots ...
                file = rootsByName.get(localName);
            } else {
                // The segment should exist as a child of the file ...
                file = new File(file, localName);
            }
            if (file == null || !file.exists()) {
                // Unable to complete the path, so prepare the exception by determining the lowest path that exists ...
                Path lowest = path;
                while (lowest.getLastSegment() != segment) {
                    lowest = lowest.getParent();
                }
                lowest = lowest.getParent();
                throw new PathNotFoundException(location, lowest);
            }
        }
        assert file != null;
        return file;
    }
}
