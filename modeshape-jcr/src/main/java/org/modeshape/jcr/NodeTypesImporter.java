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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeDefinition;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.value.NamespaceRegistry;

/**
 * Class which performs the import of the optional, repository configured node-types
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class NodeTypesImporter {

    protected final JcrRepository.RunningState repository;
    private final List<String> nodeTypesFiles;

    protected NodeTypesImporter( List<String> nodeTypesFiles,
                                 JcrRepository.RunningState repository ) {
        this.nodeTypesFiles = nodeTypesFiles;
        this.repository = repository;
    }

    void importNodeTypes() throws RepositoryException {
        if (nodeTypesFiles.isEmpty()) {
            return;
        }

        List<NodeTypeDefinition> nodeTypeDefinitions = new ArrayList<NodeTypeDefinition>();
        Set<NamespaceRegistry.Namespace> namespaces = new HashSet<NamespaceRegistry.Namespace>();
        for (String cndFile : nodeTypesFiles) {
            CndImportOperation cndImportOperation = new CndImportOperation();
            cndImportOperation.execute(cndFile);

            nodeTypeDefinitions.addAll(cndImportOperation.getNodeTypeDefinitions());
            namespaces.addAll(cndImportOperation.getNamespaces());
        }

        if (!nodeTypeDefinitions.isEmpty()) {
            repository.nodeTypeManager().registerNodeTypes(nodeTypeDefinitions, false, false, true);
        }

        if (!namespaces.isEmpty()) {
            repository.persistentRegistry().register(namespaces);
        }
    }

    private final class CndImportOperation {
        private List<NodeTypeDefinition> nodeTypeDefinitions = Collections.emptyList();
        private Set<NamespaceRegistry.Namespace> namespaces = Collections.emptySet();

        protected CndImportOperation() {
        }

        void execute( String cndFile ) {
            try {
                InputStream cndFileStream = getInputStreamForFile(cndFile);

                if (cndFileStream == null) {
                    repository.warn(JcrI18n.cannotLoadCndFile, cndFile);
                    return;
                }

                CndImporter cndImporter = new CndImporter(repository.context());
                Problems importProblems = new SimpleProblems();
                cndImporter.importFrom(cndFileStream, importProblems, cndFile);

                for (Problem problem : importProblems) {
                    if (problem.getStatus() == Problem.Status.ERROR) {
                        if (problem.getThrowable() != null) {
                            repository.error(problem.getThrowable(), problem.getMessage(), problem.getParameters());
                        } else {
                            repository.error(problem.getMessage(), problem.getParameters());
                        }
                    } else if (problem.getStatus() == Problem.Status.WARNING) {
                         repository.warn(problem.getMessage(), problem.getParameters());
                    }
                }
                if (importProblems.hasErrors()) {
                    return;
                }
                this.nodeTypeDefinitions = cndImporter.getNodeTypeDefinitions();
                this.namespaces = cndImporter.getNamespaces();
            } catch (IOException e) {
                repository.error(e, JcrI18n.errorReadingCndFile, cndFile);
            }
        }

        private InputStream getInputStreamForFile( String cndFileString ) {
            return IoUtil.getResourceAsStream(cndFileString,
                                              repository.environment().getClassLoader(
                                                      NodeTypesImporter.class.getClassLoader()),
                                              null);
        }

        Set<NamespaceRegistry.Namespace> getNamespaces() {
            return namespaces;
        }

        List<NodeTypeDefinition> getNodeTypeDefinitions() {
            return nodeTypeDefinitions;
        }
    }
}
