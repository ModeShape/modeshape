/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.modeshape.jboss.service;

import org.infinispan.schematic.document.EditableDocument;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;

public class IndexStorageService implements Service<IndexStorage> {

    private final InjectedValue<String> indexStorageBasePathInjector = new InjectedValue<String>();
    private final InjectedValue<String> indexStorageSourceBasePathInjector = new InjectedValue<String>();

    private final IndexStorage indexStorage;

    public IndexStorageService( String repositoryName,
                                EditableDocument queryConfig ) {
        this.indexStorage = new IndexStorage(repositoryName, queryConfig);
    }

    /**
     * Get the repository name.
     * 
     * @return repositoryName
     */
    public String getRepositoryName() {
        return indexStorage.getRepositoryName();
    }

    private String getIndexStorageBasePath() {
        return appendDirDelim(indexStorageBasePathInjector.getOptionalValue());
    }

    private String getIndexStorageSourceBasePath() {
        return appendDirDelim(indexStorageSourceBasePathInjector.getOptionalValue());
    }

    private String appendDirDelim( String value ) {
        if (value != null && value.endsWith("/")) {
            value = value + "/";
        }
        return value;
    }

    /**
     * @return the injector used to set the path where the indexes are to be stored
     */
    public InjectedValue<String> getIndexStorageBasePathInjector() {
        return indexStorageBasePathInjector;
    }

    /**
     * @return the injector used to set the path where the source indexes are to be stored
     */
    public InjectedValue<String> getIndexStorageSourceBasePathInjector() {
        return indexStorageSourceBasePathInjector;
    }

    @Override
    public IndexStorage getValue() throws IllegalStateException, IllegalArgumentException {
        return indexStorage;
    }

    @Override
    public void start( StartContext arg0 ) {
        // Not much to do, since we've already captured the properties for the index storage.
        // When this is injected into the RepositoryService, the RepositoryService will use the
        // properties to update the configuration.

        // All we need to do is update the relative paths and make them absolute, given the absolute paths that are injected ...

        String indexStorageBasePath = getIndexStorageBasePath();
        if (indexStorageBasePath != null) {
            // Set the index storage directory ...
            EditableDocument indexStorage = this.indexStorage.getQueryConfiguration().getDocument(FieldName.INDEX_STORAGE);
            String relativePath = indexStorage.getString(FieldName.INDEX_STORAGE_LOCATION);
            if (relativePath != null) {
                indexStorage.set(FieldName.INDEX_STORAGE_LOCATION, indexStorageBasePath + relativePath);
            }
        }

        String indexStorageSourceBasePath = getIndexStorageSourceBasePath();
        if (indexStorageSourceBasePath != null) {
            // Set the index source storage directory ...
            EditableDocument indexStorage = this.indexStorage.getQueryConfiguration().getDocument(FieldName.INDEX_STORAGE);
            String relativePath = indexStorage.getString(FieldName.INDEX_STORAGE_LOCATION);
            if (relativePath != null) {
                indexStorage.set(FieldName.INDEX_STORAGE_LOCATION, indexStorageSourceBasePath + relativePath);
            }
        }
    }

    @Override
    public void stop( StopContext arg0 ) {
        // Nothing to do
    }
}
