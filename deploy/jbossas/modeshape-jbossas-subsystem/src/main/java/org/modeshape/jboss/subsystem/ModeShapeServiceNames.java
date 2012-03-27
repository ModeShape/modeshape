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
package org.modeshape.jboss.subsystem;

import org.jboss.msc.service.ServiceName;
import org.modeshape.jboss.service.IndexStorage;

public class ModeShapeServiceNames {
    public static ServiceName ENGINE = ServiceName.JBOSS.append("modeshape", "engine");

    public static ServiceName repositoryServiceName( String name ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", name, "repository");
    }

    public static ServiceName sequencerServiceName( String name ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", name, "sequencers");
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
        return ServiceName.of(ServiceName.JBOSS, "modeshape", name, "indexDir");
    }

    public static ServiceName indexSourceStorageDirectoryServiceName( String name ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", name, "indexSourceDir");
    }

    public static ServiceName binaryStorageServiceName( String name ) {
        return ServiceName.of(ServiceName.JBOSS, "modeshape", name, "binaries");
    }

}
