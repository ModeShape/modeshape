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
package org.modeshape.jboss.subsystem;

import org.jboss.msc.service.ServiceName;
import org.modeshape.common.util.CheckArg;

public class ModeShapeServiceNames {
    public static ServiceName ENGINE = ServiceName.JBOSS.append("modeshape", "engine");

    public static ServiceName repositoryServiceName( String name ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", name, "repository");
    }

    public static ServiceName sequencerServiceName( String repositoryName,
                                                    String sequencerName ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", repositoryName, "sequencers", sequencerName);
    }  
    
    public static ServiceName persistenceDBServiceName(String repositoryName,
                                                       String type) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", repositoryName, "db-persistence", type);
    }

    public static ServiceName persistenceFSServiceName(String repositoryName,
                                                       String type) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", repositoryName, "fs-persistence", type);
    }

    public static ServiceName sourceServiceName( String repositoryName,
                                                 String sourceName ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", repositoryName, "external-sources", sourceName);
    }

    public static ServiceName textExtractorServiceName( String repositoryName,
                                                        String extractorName ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", repositoryName, "text-extractors", extractorName);
    }

    public static ServiceName authenticatorServiceName( String repositoryName,
                                                        String authenticatorName ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", repositoryName, "authenticators", authenticatorName);
    }

    public static ServiceName indexProviderServiceName( String repositoryName,
                                                        String providerName ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", repositoryName, "indexProviders", providerName);
    }

    public static ServiceName indexDefinitionServiceName( String repositoryName,
                                                          String indexName ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", repositoryName, "indexes", indexName);
    }

    public static ServiceName dataDirectoryServiceName( String name ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", name, "data");
    }

    public static ServiceName binaryStorageDefaultServiceName( String repositoryName ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", repositoryName, "binaries");
    }

    public static ServiceName binaryStorageNestedServiceName( String repositoryName,
                                                              String binaryStoreName ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", repositoryName, binaryStoreName, "binaries");
    }

    public static ServiceName binaryStorageDirectoryServiceName( String repositoryName ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", repositoryName, "binaries", "dir");
    }

    public static ServiceName binaryStorageDirectoryServiceName( String repositoryName,
                                                                 String binaryStoreName ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", repositoryName, binaryStoreName, "binaries", "dir");
    }

    public static ServiceName referenceFactoryServiceName( String repositoryName ) {
        return repositoryServiceName(repositoryName).append("reference-factory");
    }

    /**
     * @param repositoryName the repository name (cannot be <code>null</code>)
     * @return the service name (never <code>null</code> or empty)
     */
    public static ServiceName monitorServiceName( String repositoryName ) {
        CheckArg.isNotNull(repositoryName, "repositoryName");
        return ServiceName.of(ServiceName.JBOSS, "modeshape", repositoryName, "monitor");
    }
}
