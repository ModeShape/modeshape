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

package org.modeshape.jcr.federation;

import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.document.DocumentStore;
import org.modeshape.jcr.cache.document.LocalDocumentStore;

/**
 * An implementation of {@link DocumentStore} which is used when federation is enabled
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class FederatedDocumentStore implements DocumentStore {

    private final LocalDocumentStore localDocumentStore;
    private final ConnectorManager connectorManager;

    private String localSourceKey;

    public FederatedDocumentStore( ConnectorManager connectorManager,
                                   SchematicDb localDb ) {
        this.connectorManager = connectorManager;
        this.localDocumentStore = new LocalDocumentStore(localDb);
    }

    @Override
    public LocalDocumentStore localStore() {
        return localDocumentStore;
    }

    @Override
    public SchematicEntry putIfAbsent( String key,
                                       Document document ) {
        if (isLocalSource(key)) {
            return localStore().putIfAbsent(key, document);
        } else {
            Connector connector = connectorManager.getConnectorForSourceKey(sourceKey(key));
            if (connector != null) {
                //TODO author=Horia Chiorean date=11/1/12 description=write to connector
            }
        }
        return null;
    }

    @Override
    public void put( String key,
                     Document document ) {
        if (isLocalSource(key)) {
            localStore().put(key, document);
        } else {
            Connector connector = connectorManager.getConnectorForSourceKey(sourceKey(key));
            if (connector != null) {
                //TODO author=Horia Chiorean date=11/1/12 description=write to connector
            }
        }
    }

    @Override
    public void put( Document entryDocument ) {
        localStore().put(entryDocument);
    }

    @Override
    public void replace( String key,
                         Document document ) {
        if (isLocalSource(key)) {
            localStore().replace(key, document);
        } else {
            Connector connector = connectorManager.getConnectorForSourceKey(sourceKey(key));
            if (connector != null) {
                //TODO author=Horia Chiorean date=11/1/12 description=write to connector
            }
        }
    }

    @Override
    public SchematicEntry get( String key ) {
        if (isLocalSource(key)) {
            return localStore().get(key);
        } else {
            Connector connector = connectorManager.getConnectorForSourceKey(sourceKey(key));
            if (connector != null) {
                //TODO author=Horia Chiorean date=11/1/12 description=read from connector & compose document
            }
        }
        return null;
    }

    @Override
    public boolean containsKey( String key ) {
        if (isLocalSource(key)) {
            return localStore().containsKey(key);
        } else {
            Connector connector = connectorManager.getConnectorForSourceKey(sourceKey(key));
            if (connector != null) {
                //TODO author=Horia Chiorean date=11/1/12 description=query connector
            }
            return false;
        }
    }

    @Override
    public void remove( String key ) {
        if (isLocalSource(key)) {
            localStore().remove(key);
        } else {
            Connector connector = connectorManager.getConnectorForSourceKey(sourceKey(key));
            if (connector != null) {
                //TODO author=Horia Chiorean date=11/1/12 description=remove via connector
            }
        }
    }

    public TransactionManager transactionManager() {
        //TODO author=Horia Chiorean date=11/1/12 description=do we need some other transaction manager ?
        return localStore().transactionManager();
    }

    public XAResource xaResource() {
        //TODO author=Horia Chiorean date=11/1/12 description=do we need some other xaResource ?
        return localStore().xaResource();
    }

    public void setLocalSourceKey( String localSourceKey ) {
        this.localSourceKey = localSourceKey;
    }

    private boolean isLocalSource( String key ) {
        return !NodeKey.isValidFormat(key) //the key isn't a std key format (probably some internal format)
                || StringUtil.isBlank(localSourceKey) //there isn't a local source configured yet (e.g. system startup)
                || key.startsWith(localSourceKey); //the sources differ

    }

    private String sourceKey( String nodeKey ) {
        return NodeKey.sourceKey(nodeKey);
    }
}
