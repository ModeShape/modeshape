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
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * 
 */
public class AddRamIndexStorage extends AbstractAddIndexStorage {

    public static final AddRamIndexStorage INSTANCE = new AddRamIndexStorage();

    private AddRamIndexStorage() {
    }

    @Override
    protected void writeIndexStorageConfiguration( final OperationContext context,
                                                   final ModelNode storage,
                                                   EditableDocument indexStorage,
                                                   String repositoryName ) {
        indexStorage.set(RepositoryConfiguration.FieldName.TYPE, RepositoryConfiguration.FieldValue.INDEX_STORAGE_RAM);
    }
    
    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model, ModelKeys.RAM_INDEX_STORAGE, ModelAttributes.RAM_INDEX_STORAGE_ATTRIBUTES);
    }
}
