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
package org.modeshape.jboss.service;

import org.infinispan.manager.CacheContainer;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * 
 */
public class IndexStorage {

    private final EditableDocument queryConfig;
    /**
     * Optional member, which will be set only if an ISPN cache is configured
     */
    private CacheContainer cacheContainer;

    IndexStorage( EditableDocument queryConfig ) {
        this.queryConfig = queryConfig;
    }

    static IndexStorage defaultStorage(String repositoryName, String dataDirPath) {
        EditableDocument query = Schematic.newDocument();

        EditableDocument indexing = query.getOrCreateDocument(RepositoryConfiguration.FieldName.INDEXING);
        EditableDocument indexStorage = query.getOrCreateDocument(RepositoryConfiguration.FieldName.INDEX_STORAGE);
        EditableDocument backend = indexing.getOrCreateDocument(RepositoryConfiguration.FieldName.INDEXING_BACKEND);
        query.set(RepositoryConfiguration.FieldName.REBUILD_UPON_STARTUP, RepositoryConfiguration.QueryRebuild.IF_MISSING.toString().toLowerCase());
        backend.set(RepositoryConfiguration.FieldName.TYPE, RepositoryConfiguration.FieldValue.INDEXING_BACKEND_TYPE_LUCENE);
        indexStorage.set(RepositoryConfiguration.FieldName.TYPE, RepositoryConfiguration.FieldValue.INDEX_STORAGE_FILESYSTEM);
        indexStorage.set(RepositoryConfiguration.FieldName.INDEX_STORAGE_LOCATION, dataDirPath + "/" + repositoryName + "/indexes");

        return new IndexStorage(query);
    }

    /**
     * @return the repository's query configuration
     */
    EditableDocument getQueryConfiguration() {
        return queryConfig;
    }

    CacheContainer getCacheContainer() {
        return cacheContainer;
    }

    void setCacheContainer( CacheContainer cacheContainer ) {
        this.cacheContainer = cacheContainer;
    }
}
