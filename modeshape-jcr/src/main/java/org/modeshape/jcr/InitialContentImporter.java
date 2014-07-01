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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import org.modeshape.common.collection.Multimap;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.cache.document.WorkspaceCache;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyType;
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

    private InputStream getInitialContentFileStream( String workspaceName ) {
        String initialContentFileString = initialContentConfig.getInitialContentFile(workspaceName);
        InputStream stream = IoUtil.getResourceAsStream(initialContentFileString,
                                                        repository.environment()
                                                                  .getClassLoader(InitialContentImporter.class.getClassLoader()),
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
        public void submit( LinkedHashMap<Path, NodeImportXmlHandler.ImportElement> parseResults ) throws RepositoryException {
            List<NodeImportXmlHandler.ImportElement> elementsWithReferences = new ArrayList<>();
            JcrValueFactory valueFactory = session.getValueFactory();
            for (Path nodePath : parseResults.keySet()) {
                LOGGER.debug("Importing node at path {0}", nodePath);
                NodeImportXmlHandler.ImportElement element = parseResults.get(nodePath);

                Path parentPath = nodePath.getParent();
                AbstractJcrNode parentNode = session.node(parentPath);

                // create the new node
                AbstractJcrNode newNode = null;
                // make sure the path is not encoded, because that's how the node xml handler generates it
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
                    org.modeshape.jcr.value.PropertyType propertyType = element.getPropertyType(propertyName);
                    if (isReference(propertyType)) {
                        elementsWithReferences.add(element);
                        // references will be processed later
                        continue;
                    }
                    Collection<String> propertyValues = element.getProperties().get(propertyName);
                    if (propertyValues.size() == 1) {
                        String stringValue = propertyValues.iterator().next();
                        if (isBinary(propertyType)) {
                            // we have a binary prop, try to resolve its stream
                            InputStream inputStream = readBinaryStream(stringValue);
                            if (inputStream != null) {
                                newNode.setProperty(propertyName, inputStream);
                                continue;
                            }
                            // we were unable to read its stream, to we'll set the binary value UTF-8 bytes (default behavior)
                        }
                        newNode.setProperty(propertyName, stringValue, propertyType.jcrType());
                    } else {
                        String[] stringValues = propertyValues.toArray(new String[propertyValues.size()]);
                        if (isBinary(propertyType)) {
                            // try to parse each individual binary value and read its stream
                            boolean allValuesRead = true;
                            Value[] binaryValues = new Value[propertyValues.size()];
                            for (int i = 0; i < stringValues.length; i++) {
                                String stringValue = stringValues[i];
                                InputStream inputStream = readBinaryStream(stringValue);
                                if (inputStream == null) {
                                    // we were unable to read the stream, so we abort this approach altogether and we'll
                                    // set the binary value using the string bytes
                                    allValuesRead = false;
                                    break;
                                }
                                binaryValues[i] = valueFactory.createValue(inputStream);
                            }
                            if (allValuesRead) {
                                newNode.setProperty(propertyName, binaryValues);
                                // we managed to set the binary multi value streams, so move onto the next property
                                continue;
                            }
                        }
                        newNode.setProperty(propertyName, stringValues, propertyType.jcrType());
                    }
                }
            }

            if (!elementsWithReferences.isEmpty()) {
                // after we've processed all the nodes (and created them) set the reference properties
                setReferenceProperties(elementsWithReferences);
            }
        }

        private boolean isReference( org.modeshape.jcr.value.PropertyType propertyType ) {
            return org.modeshape.jcr.value.PropertyType.REFERENCE == propertyType
                   || org.modeshape.jcr.value.PropertyType.WEAKREFERENCE == propertyType
                   || org.modeshape.jcr.value.PropertyType.SIMPLEREFERENCE == propertyType;
        }

        private boolean isBinary( org.modeshape.jcr.value.PropertyType propertyType ) {
            return PropertyType.BINARY == propertyType;
        }

        private InputStream readBinaryStream( String resourcePath ) {
            // attempt to resolve the resource as the path to a file
            File file = new File(resourcePath);
            if (file.exists() && file.canRead()) {
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    // we cannot locate/read the file
                }
            }
            // try resolving it via the classpath
            return InitialContentImporter.class.getClassLoader().getResourceAsStream(resourcePath);
        }

        private void setReferenceProperties( List<NodeImportXmlHandler.ImportElement> elementsWithReferences )
            throws RepositoryException {
            for (NodeImportXmlHandler.ImportElement element : elementsWithReferences) {
                Node node = session.node(element.getPath());
                Multimap<String, String> properties = element.getProperties();
                for (String propertyName : properties.keySet()) {
                    org.modeshape.jcr.value.PropertyType propertyType = element.getPropertyType(propertyName);
                    if (!isReference(propertyType)) {
                        continue;
                    }
                    setReferenceProperty(node, propertyName, properties.get(propertyName), propertyType);
                }
            }
        }

        private void setReferenceProperty( Node node,
                                           String propertyName,
                                           Collection<String> values,
                                           org.modeshape.jcr.value.PropertyType referenceType ) throws RepositoryException {
            List<Value> referenceValues = new ArrayList<>();
            for (String absPath : values) {
                AbstractJcrNode referredNode = session.getNode(absPath);

                Value reference = null;
                switch (referenceType) {
                    case REFERENCE: {
                        reference = session.getValueFactory().createValue(referredNode, false);
                        break;
                    }
                    case WEAKREFERENCE: {
                        reference = session.getValueFactory().createValue(referredNode, true);
                        break;
                    }
                    case SIMPLEREFERENCE: {
                        reference = session.getValueFactory().createSimpleReference(referredNode);
                        break;
                    }
                    default: {
                        throw new IllegalArgumentException("Invalid reference type:" + referenceType);
                    }
                }
                referenceValues.add(reference);
            }
            if (referenceValues.size() == 1) {
                node.setProperty(propertyName, referenceValues.get(0));
            } else {
                node.setProperty(propertyName, referenceValues.toArray(new Value[referenceValues.size()]));
            }
        }
    }
}
