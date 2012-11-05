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
import org.modeshape.jcr.api.federation.FederationManager;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.document.WritableSessionCache;

/**
 * Implementation of the {@link FederationManager} interface.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ModeShapeFederationManager implements FederationManager {

    private final JcrSession session;

    public ModeShapeFederationManager( JcrSession session ) {
        this.session = session;
    }

    @Override
    public void linkExternalLocation( String absNodePath,
                                      String sourceName,
                                      String externalLocation,
                                      String... filters ) throws RepositoryException {
        //TODO author=Horia Chiorean date=11/2/12 description=Decide if this is the right level of abstraction
        //TODO author=Horia Chiorean date=11/2/12 description=Decide how to integrate with session transactions
        //TODO author=Horia Chiorean date=11/2/12 description=Decide how to use filters
        NodeKey key = session.getNode(absNodePath).key();

        WritableSessionCache writableSessionCache = (WritableSessionCache)session.spawnSessionCache(false);
        writableSessionCache.linkExternalLocation(key, sourceName, externalLocation, filters);
        writableSessionCache.save();
    }
}
