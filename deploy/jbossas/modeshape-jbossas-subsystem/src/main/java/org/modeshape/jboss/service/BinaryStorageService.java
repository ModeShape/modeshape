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

import org.infinispan.manager.CacheContainer;
import org.infinispan.schematic.document.EditableDocument;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;

public class BinaryStorageService implements Service<BinaryStorage> {

    private final Injector<CacheContainer> binaryManagerInjector;
    private final InjectedValue<String> binaryStorageBasePathInjector = new InjectedValue<String>();

    private final EditableDocument binaryConfig;
    protected final BinaryStorage binaryStorage;

    public BinaryStorageService( String repositoryName,
                                 EditableDocument binaryConfig ) {
        this.binaryConfig = binaryConfig;
        this.binaryStorage = new BinaryStorage(repositoryName, binaryConfig);
        this.binaryManagerInjector = new Injector<CacheContainer>() {
            @Override
            public void inject( CacheContainer value ) throws InjectionException {
                binaryStorage.setCacheContainer(value);
            }

            @Override
            public void uninject() {
                binaryStorage.setCacheContainer(null);
            }
        };
    }

    /**
     * Get the repository name.
     * 
     * @return repositoryName
     */
    public String getRepositoryName() {
        return binaryStorage.getRepositoryName();
    }

    private String getBinaryStorageBasePath() {
        return appendDirDelim(binaryStorageBasePathInjector.getOptionalValue());
    }

    private String appendDirDelim( String value ) {
        if (value != null && value.endsWith("/")) {
            value = value + "/";
        }
        return value;
    }

    /**
     * @return the injector used to set the path where the binary files are to be stored
     */
    public InjectedValue<String> getBinaryStorageBasePathInjector() {
        return binaryStorageBasePathInjector;
    }

    /**
     * @return the injector used to set the CacheManager reference used for binary storage
     */
    public Injector<CacheContainer> getBinaryCacheManagerInjector() {
        return binaryManagerInjector;
    }

    @Override
    public BinaryStorage getValue() throws IllegalStateException, IllegalArgumentException {
        return binaryStorage;
    }

    @Override
    public void start( StartContext arg0 ) {
        // Not much to do, since we've already captured the properties for the index storage.
        // When this is injected into the RepositoryService, the RepositoryService will use the
        // properties to update the configuration.

        // All we need to do is update the relative paths and make them absolute, given the absolute paths that are injected ...

        String binaryStorageBasePath = getBinaryStorageBasePath();
        if (binaryStorageBasePath != null) {
            // Set the binary storage directory ...
            String relativePath = binaryConfig.getString(FieldName.DIRECTORY);
            if (relativePath != null) {
                binaryConfig.set(FieldName.DIRECTORY, binaryStorageBasePath + relativePath);
            }
        }
    }

    @Override
    public void stop( StopContext arg0 ) {
        // Nothing to do
    }
}
