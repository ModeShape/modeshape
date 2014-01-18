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

package org.modeshape.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.TreeMap;
import javax.jcr.RepositoryException;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.IoUtil;
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

/**
 * Class which handles the import of initial content files for new workspaces, using a {@link NodeImportXmlHandler}. It is
 * important that any new content is imported through the "JCR layer", so that the JCR specific validations are performed.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class InitialContentImporter {

    private static final Logger LOGGER = Logger.getLogger(InitialContentImporter.class);

    private final JcrRepository.RunningState repository;
    private final RepositoryConfiguration.InitialContent initialContentConfig;

    InitialContentImporter( RepositoryConfiguration.InitialContent initialContentConfig,
                            JcrRepository.RunningState repository ) {
        this.initialContentConfig = initialContentConfig;
        this.repository = repository;
    }

    protected void importInitialContent( String workspaceName ) throws RepositoryException {
        // check that there is something which should be imported
        if (!initialContentConfig.hasInitialContentFile(workspaceName)) {
            return;
        }

        RepositoryCache repositoryCache = repository.repositoryCache();
        WorkspaceCache wsCache = repositoryCache.getWorkspaceCache(workspaceName);
        if (!wsCache.isEmpty()) {
            // the ws cache must be empty for initial content to be imported
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
        JcrSession internalSession = repository.loginInternalSession(workspaceName);
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
            }
            throw new RepositoryException(JcrI18n.errorWhileParsingInitialContentFile.text(e.getMessage()), e);
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

    private InputStream getInitialContentFileStream( String workspaceName )  {
        String initialContentFileString = initialContentConfig.getInitialContentFile(workspaceName);
        InputStream stream = IoUtil.getResourceAsStream(initialContentFileString,
                                                        repository.environment().getClassLoader(
                                                                InitialContentImporter.class.getClassLoader()),
                                                        null);
        if (stream == null) {
            repository.warn(JcrI18n.cannotLoadInitialContentFile, initialContentFileString);
        }
        return stream;
    }

    private class ImportDestination implements NodeImportDestination {
        private final JcrSession session;

        protected ImportDestination( JcrSession session ) {
            this.session = session;
        }

        @Override
        public ExecutionContext getExecutionContext() {
            return session.context();
        }

        @SuppressWarnings( "synthetic-access" )
        @Override
        public void submit( TreeMap<Path, NodeImportXmlHandler.ImportElement> parseResults ) throws RepositoryException {
            for (Path nodePath : parseResults.keySet()) {
                LOGGER.debug("Importing node at path {0}", nodePath);
                NodeImportXmlHandler.ImportElement element = parseResults.get(nodePath);

                Path parentPath = nodePath.getParent();
                AbstractJcrNode parentNode = session.node(parentPath);

                // create the new node
                AbstractJcrNode newNode = null;
                //make sure the path is not encoded, because that's how the node xml handler generates it
                String newNodeRelativePath = nodePath.getLastSegment().getName().toString();
                if (StringUtil.isBlank(element.getType())) {
                    newNode = parentNode.addNode(newNodeRelativePath);
                } else {
                    newNode = parentNode.addNode(newNodeRelativePath, element.getType());
                }

                // add any mixins
                for (String mixin : element.getMixins()) {
                    newNode.addMixin(mixin);
                }

                // set the properties
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
