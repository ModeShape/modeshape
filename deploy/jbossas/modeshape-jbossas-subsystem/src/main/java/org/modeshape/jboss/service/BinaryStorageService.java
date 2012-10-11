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
import org.infinispan.schematic.document.EditableDocument;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;

public class BinaryStorageService implements Service<BinaryStorage> {

    private final Injector<CacheContainer> binaryManagerInjector = new Injector<CacheContainer>() {
        @Override
        public void inject( CacheContainer value ) throws InjectionException {
            binaryStorage.setCacheContainer(value);
        }

        @Override
        public void uninject() {
            binaryStorage.setCacheContainer(null);
        }
    };
    private final InjectedValue<String> binaryStorageBasePathInjector = new InjectedValue<String>();
    private final InjectedValue<String> dataDirectoryPathInjector = new InjectedValue<String>();

    private final String repositoryName;
    private final BinaryStorage binaryStorage;

    public BinaryStorageService( String repositoryName,
                                 EditableDocument binaryConfig ) {
        this.repositoryName = repositoryName;
        this.binaryStorage = new BinaryStorage(binaryConfig);
    }

    public BinaryStorageService( String repositoryName ) {
        this.repositoryName = repositoryName;
        this.binaryStorage = null;
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
        return binaryStorage != null ? binaryStorage : BinaryStorage.defaultStorage(repositoryName, dataDirectoryPathInjector.getValue());
    }

    /**
     * @return the injector used to retrieve the path to the data directory
     */
    public InjectedValue<String> getDataDirectoryPathInjector() {
        return dataDirectoryPathInjector;
    }

    @Override
    public void start( StartContext arg0 ) {
        // Not much to do, since we've already captured the properties for the index storage.
        // When this is injected into the RepositoryService, the RepositoryService will use the
        // properties to update the configuration.

        // All we need to do is update the relative paths and make them absolute, given the absolute paths that are injected ...

        String binaryStorageBasePath = getBinaryStorageBasePath();
        if (binaryStorageBasePath != null) {
            EditableDocument binaryConfig = binaryStorage.getBinaryConfiguration();
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
