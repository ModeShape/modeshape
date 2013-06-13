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

import java.util.HashMap;
import java.util.Map;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.EditableDocument;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.modeshape.jboss.subsystem.ModeShapeServiceNames;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * A special type of binary storage service, associated with {@link org.modeshape.jcr.value.binary.CompositeBinaryStore}
 *
 * @author hchiorean
 */
public class CompositeBinaryStorageService implements Service<BinaryStorage> {

    private final String repositoryName;
    private final EditableDocument binaries;
    private final Map<String, InjectedValue<BinaryStorage>> nestedStoresValues;

    public CompositeBinaryStorageService(String repositoryName, EditableDocument binaries) {
        this.repositoryName = repositoryName;
        this.binaries = binaries;
        this.nestedStoresValues = new HashMap<String, InjectedValue<BinaryStorage>>();
    }

    @Override
    public BinaryStorage getValue() throws IllegalStateException, IllegalArgumentException {
        return new BinaryStorage(binaries);
    }

    public InjectedValue<BinaryStorage> nestedStoreConfiguration(String storeName) {
        if (!nestedStoresValues.containsKey(storeName)) {
            nestedStoresValues.put(storeName, new InjectedValue<BinaryStorage>());
        }
        return nestedStoresValues.get(storeName);
    }

    @Override
    public void start( StartContext startContext ) {
        EditableDocument compositeDocument = Schematic.newDocument();
        //iterate through all the contained nested stored values (which should've been started) and update this configuration
        for (String storeName : nestedStoresValues.keySet()) {
            compositeDocument.set(storeName, nestedStoresValues.get(storeName).getValue().getBinaryConfiguration());
        }
        binaries.setDocument(RepositoryConfiguration.FieldName.COMPOSITE_STORE_NAMED_BINARY_STORES, compositeDocument);
    }

    @Override
    public void stop( StopContext stopContext ) {
        //stop all the services which correspond to the inner stores
        ServiceContainer serviceContainer = stopContext.getController().getServiceContainer();
        for (String storeName : nestedStoresValues.keySet()) {
            ServiceName serviceName = ModeShapeServiceNames.binaryStorageNestedServiceName(repositoryName, storeName);
            Service<?> service = serviceContainer.getService(serviceName).getService();
            service.stop(stopContext);
        }
    }
}
