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
public class AddCustomIndexStorage extends AbstractAddIndexStorage {

    public static final AddCustomIndexStorage INSTANCE = new AddCustomIndexStorage();

    private AddCustomIndexStorage() {
    }

    @Override
    protected void writeIndexStorageConfiguration( final OperationContext context,
                                                   final ModelNode storage,
                                                   EditableDocument indexStorage ) {
        indexStorage.set(FieldName.TYPE, FieldValue.INDEX_STORAGE_CUSTOM);
        indexStorage.set(FieldName.TYPE, FieldValue.INDEX_STORAGE_CUSTOM);
        for (String key : storage.keys()) {
            String value = storage.get(key).asString();
            if (key.equals(ModelKeys.CLASSNAME)) {
                key = FieldName.CLASSNAME;
            } else if (key.equals(ModelKeys.MODULE)) {
                key = FieldName.CLASSLOADER;
            }
            indexStorage.set(key, value);
        }
    }
    
    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model, ModelKeys.CUSTOM_INDEX_STORAGE, ModelAttributes.CUSTOM_INDEX_STORAGE_ATTRIBUTES);
    }

}
