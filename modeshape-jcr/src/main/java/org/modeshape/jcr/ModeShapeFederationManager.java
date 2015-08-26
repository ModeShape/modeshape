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
import javax.jcr.nodetype.ConstraintViolationException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.federation.FederationManager;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.document.DocumentStore;
import org.modeshape.jcr.value.Path;

/**
 * Implementation of the {@link FederationManager} interface.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ModeShapeFederationManager implements FederationManager {

    private final JcrSession session;
    private final DocumentStore documentStore;

    protected ModeShapeFederationManager( JcrSession session,
                                          DocumentStore documentStore ) {
        this.session = session;
        this.documentStore = documentStore;
    }

    @Override
    public void createProjection( String absNodePath,
                                  String sourceName,
                                  String externalPath,
                                  String alias ) throws RepositoryException {
        AbstractJcrNode node = session.getNode(absNodePath);
        if (session.nodeTypeManager().nodeTypes().isUnorderedCollection(node.getPrimaryTypeName(), node.getMixinTypeNames())) {
            throw new ConstraintViolationException(JcrI18n.operationNotSupportedForUnorderedCollections.text("create projection"));    
        }
        NodeKey parentNodeToBecomeFederatedKey = node.key();

        String projectionAlias = !StringUtil.isBlank(alias) ? alias : externalPath;
        if (projectionAlias.endsWith("/")) {
            projectionAlias = projectionAlias.substring(0, projectionAlias.length() - 1);
        }
        if (projectionAlias.contains("/")) {
            projectionAlias = projectionAlias.substring(projectionAlias.lastIndexOf("/") + 1);
        }

        if (StringUtil.isBlank(projectionAlias)) {
            // we cannot create an external projection without a valid alias
            return;
        }


        SessionCache sessionCache = this.session.spawnSessionCache(false);
        String externalNodeKey = documentStore.createExternalProjection(parentNodeToBecomeFederatedKey.toString(), sourceName,
                                                                        externalPath, projectionAlias);
        MutableCachedNode mutable = sessionCache.mutable(parentNodeToBecomeFederatedKey);
        mutable.addFederatedSegment(externalNodeKey, projectionAlias);
        sessionCache.save();
    }

    @Override
    public void removeProjection( String projectionPath ) throws RepositoryException {
        CheckArg.isNotNull(projectionPath, "projectionPath");

        Path path = session.pathFactory().create(projectionPath);
        if (path.isRoot()) {
            throw new IllegalArgumentException(JcrI18n.invalidProjectionPath.text(projectionPath));
        }

        NodeKey federatedNodeKey = session.getNode(path.getParent().getString()).key();
        NodeKey externalNodeKey = session.getNode(path.getString()).key();

        SessionCache sessionCache = session.spawnSessionCache(false);
        MutableCachedNode federatedNode = sessionCache.mutable(federatedNodeKey);
        federatedNode.removeFederatedSegment(externalNodeKey.toString());
        sessionCache.save();
    }
}
