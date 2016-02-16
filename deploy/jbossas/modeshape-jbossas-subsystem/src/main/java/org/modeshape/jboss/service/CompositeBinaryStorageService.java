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
package org.modeshape.jboss.service;

import java.util.HashMap;
import java.util.Map;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.document.EditableDocument;
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
