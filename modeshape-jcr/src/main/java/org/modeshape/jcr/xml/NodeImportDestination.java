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

package org.modeshape.jcr.xml;

import java.util.LinkedHashMap;
import javax.jcr.RepositoryException;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.value.Path;

/**
 * Interface which is expected to be implemented by clients that perform node importing via XML, using the
 * {@link NodeImportXmlHandler} class.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface NodeImportDestination {

    /**
     * Retrieves the execution context of the destination, in which the import will take place and which is used for creating
     * values via the {@link org.modeshape.jcr.value.ValueFactories} and for registering namespaces via the
     * {@link org.modeshape.jcr.value.NamespaceRegistry}
     * 
     * @return a non-null {@link ExecutionContext}
     */
    public ExecutionContext getExecutionContext();

    /**
     * Processes the given [nodePath, parseElement] mappings, which represent the results of the xml parsing.
     * 
     * @param parseResults a {@link java.util.Map} of import elements enqueued by path, never null.
     * @throws RepositoryException if any error occurs while processing the parse results
     */
    public void submit( LinkedHashMap<Path, NodeImportXmlHandler.ImportElement> parseResults ) throws RepositoryException;
}
