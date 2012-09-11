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

import org.infinispan.schematic.document.EditableDocument;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryConfiguration.FieldValue;

/**
 * 
 */
public class AddDatabaseBinaryStorage extends AbstractAddBinaryStorage {

    public static final AddDatabaseBinaryStorage INSTANCE = new AddDatabaseBinaryStorage();

    private AddDatabaseBinaryStorage() {
    }

    @Override
    protected void writeBinaryStorageConfiguration( String repositoryName,
                                                    OperationContext context,
                                                    ModelNode model,
                                                    EditableDocument binaries ) throws OperationFailedException {
        binaries.set(FieldName.TYPE, FieldValue.BINARY_STORAGE_TYPE_DATABASE);
        int minBinSize = ModelAttributes.MINIMUM_BINARY_SIZE.resolveModelAttribute(context, model).asInt();
        binaries.set(FieldName.MINIMUM_BINARY_SIZE_IN_BYTES, minBinSize);
        // We don't need to add a dependency since we'll look it up by JNDI and we'll
        // not shutdown if the data source is shutdown
        String dataSource = ModelAttributes.DATA_SOURCE_JNDI_NAME.resolveModelAttribute(context, model).asString();
        binaries.set(FieldName.DATA_SOURCE_JNDI_NAME, dataSource);
    }
    
    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model, ModelKeys.DB_BINARY_STORAGE, ModelAttributes.DATABASE_BINARY_STORAGE_ATTRIBUTES);
    }

}
