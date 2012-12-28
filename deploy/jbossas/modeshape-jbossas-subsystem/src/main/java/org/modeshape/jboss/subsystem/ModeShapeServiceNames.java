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
package org.modeshape.jboss.subsystem;

import org.jboss.msc.service.ServiceName;
import org.modeshape.jboss.service.IndexStorage;

public class ModeShapeServiceNames {
    public static ServiceName ENGINE = ServiceName.JBOSS.append("modeshape", "engine");

    public static ServiceName repositoryServiceName( String name ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", name, "repository");
    }

    public static ServiceName sequencerServiceName( String repositoryName,
                                                    String sequencerName ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", repositoryName, "sequencers", sequencerName);
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

    public static ServiceName dataDirectoryServiceName( String name ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", name, "data");
    }

    /**
     * Obtain the name of the service for the {@link IndexStorage} for the given repository name
     * 
     * @param name the repository name
     * @return the service name
     */
    public static ServiceName indexStorageServiceName( String name ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", name, "indexes");
    }

    public static ServiceName indexStorageDirectoryServiceName( String name ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", name, "indexes.dir");
    }

    public static ServiceName indexSourceStorageDirectoryServiceName( String name ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", name, "indexes.source-dir");
    }

    public static ServiceName binaryStorageServiceName( String name ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", name, "binaries");
    }

    public static ServiceName binaryStorageDirectoryServiceName( String name ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", name, "binaries.dir");
    }

}
