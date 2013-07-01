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
        NodeKey parentNodeToBecomeFederatedKey = session.getNode(absNodePath).key();

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
        MutableCachedNode node = sessionCache.mutable(parentNodeToBecomeFederatedKey);
        node.addFederatedSegment(externalNodeKey, projectionAlias);
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
