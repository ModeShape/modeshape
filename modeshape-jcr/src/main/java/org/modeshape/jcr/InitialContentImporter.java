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

import javax.jcr.RepositoryException;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.cache.document.WorkspaceCache;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.xml.NodeImportDestination;
import org.modeshape.jcr.xml.NodeImportXmlHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.TreeMap;

/**
 * Class which handles the import of initial content files for new workspaces, using a {@link NodeImportXmlHandler}. It is important
 * that any new content is imported through the "JCR layer", so that the JCR specific validations are performed.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class InitialContentImporter {

    private static final Logger LOGGER = Logger.getLogger(InitialContentImporter.class);

    private final JcrRepository.RunningState runningState;
    private final RepositoryConfiguration.InitialContent initialContentConfig;

    InitialContentImporter( RepositoryConfiguration.InitialContent initialContentConfig,
                            JcrRepository.RunningState runningState ) {
        this.initialContentConfig = initialContentConfig;
        this.runningState = runningState;
    }

    protected void importInitialContent( String workspaceName ) throws RepositoryException {
        //check that there is something which should be imported
        if (!initialContentConfig.hasInitialContentFile(workspaceName)) {
            return;
        }

        RepositoryCache repositoryCache = runningState.repositoryCache();
        WorkspaceCache wsCache = repositoryCache.getWorkspaceCache(workspaceName);
        if (!wsCache.isEmpty()) {
            //the ws cache must be empty for initial content to be imported
            LOGGER.debug("Skipping import of initial content into workspace {0} as it is not empty", workspaceName);
            return;
        }

        InputStream stream = getInitialContentFileStream(workspaceName);
        if (stream == null) {
            return;
        }

        doImport(workspaceName, stream);
    }

    private void doImport( String workspaceName,
                           InputStream initialContentFileStream ) throws RepositoryException {
        JcrSession internalSession = runningState.loginInternalSession(workspaceName);
        ImportDestination importDestination = new ImportDestination(internalSession);
        NodeImportXmlHandler handler = new NodeImportXmlHandler(importDestination);
        try {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            reader.parse(new InputSource(initialContentFileStream));
            internalSession.save();
        } catch (SAXException e) {
            if (e.getCause() instanceof RepositoryException) {
                throw (RepositoryException)e.getCause();
            } else {
                throw new RepositoryException(JcrI18n.errorWhileParsingInitialContentFile.text(e.getMessage()), e);
            }
        } catch (IOException e) {
            throw new RepositoryException(JcrI18n.errorWhileReadingInitialContentFile.text(e.getMessage()), e);
        } finally {
            try {
                initialContentFileStream.close();
            } catch (IOException e) {
                LOGGER.debug("Cannot close initial content file initialContentFileStream", e);
            }
            internalSession.logout();
        }
    }

    private InputStream getInitialContentFileStream( String workspaceName ) {
        String initialContentFile = initialContentConfig.getInitialContentFile(workspaceName);
        ClassLoader importLoader = runningState.environment().getClassLoader(InitialContentImporter.class.getClassLoader());
        InputStream stream = importLoader.getResourceAsStream(initialContentFile);
        if (stream == null) {
            LOGGER.warn(JcrI18n.cannotLoadInitialContentFile, initialContentFile);
            return null;
        }
        return stream;
    }

    private class ImportDestination implements NodeImportDestination {
        private final JcrSession session;

        private ImportDestination( JcrSession session ) {
            this.session = session;
        }

        @Override
        public ExecutionContext getExecutionContext() {
            return session.context();
        }

        @Override
        public void submit( TreeMap<Path, NodeImportXmlHandler.ImportElement> parseResults ) throws RepositoryException {
            for (Path nodePath : parseResults.keySet()) {
                LOGGER.debug("Importing node at path {0}", nodePath);
                NodeImportXmlHandler.ImportElement element = parseResults.get(nodePath);

                Path parentPath = nodePath.getParent();
                AbstractJcrNode parentNode = session.node(parentPath);

                //create the new node
                AbstractJcrNode newNode = null;
                String newNodeRelativePath = nodePath.getLastSegment().getName().getString();
                if (StringUtil.isBlank(element.getType())) {
                    newNode = parentNode.addNode(newNodeRelativePath);
                } else {
                    newNode = parentNode.addNode(newNodeRelativePath, element.getType());
                }

                //add any mixins
                for (String mixin : element.getMixins()) {
                    newNode.addMixin(mixin);
                }

                //set the properties
                for (String propertyName : element.getProperties().keySet()) {
                    Collection<String> propertyValues = element.getProperties().get(propertyName);
                    if (propertyValues.size() == 1) {
                        newNode.setProperty(propertyName, propertyValues.iterator().next());
                    } else {
                        newNode.setProperty(propertyName, propertyValues.toArray(new String[propertyValues.size()]));
                    }
                }
            }
        }
    }
}
