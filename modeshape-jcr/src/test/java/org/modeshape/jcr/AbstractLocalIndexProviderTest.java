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

import javax.jcr.RepositoryException;
import org.modeshape.jcr.api.index.IndexDefinition.IndexKind;

public abstract class AbstractLocalIndexProviderTest extends AbstractIndexProviderTest {

    private static final String PROVIDER_NAME = "local";

    @Override
    protected void registerValueIndex( String indexName,
                                       String indexedNodeType,
                                       String desc,
                                       String workspaceNamePattern,
                                       String propertyName,
                                       int propertyType ) throws RepositoryException {
        registerIndex(indexName, IndexKind.VALUE, PROVIDER_NAME, indexedNodeType, desc, workspaceNamePattern, propertyName,
                      propertyType);
    }

    @Override
    protected void registerNodeTypeIndex( String indexName,
                                          String indexedNodeType,
                                          String desc,
                                          String workspaceNamePattern,
                                          String propertyName,
                                          int propertyType ) throws RepositoryException {
        registerIndex(indexName, IndexKind.NODE_TYPE, PROVIDER_NAME, indexedNodeType, desc, workspaceNamePattern, propertyName,
                      propertyType);
    }
}
