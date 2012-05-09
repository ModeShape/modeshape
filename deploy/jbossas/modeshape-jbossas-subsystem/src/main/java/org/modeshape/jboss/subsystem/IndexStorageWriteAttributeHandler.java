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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.registry.AttributeAccess;

/**
 * {@link OperationStepHandler} implementations that handles changes to the model values for the index storage submodel's
 * {@link AttributeDefinition attribute definitions}, including the different kinds of index storage. Those attributes that can be
 * changed {@link AttributeAccess.Flag#RESTART_NONE RESTART_NONE without restarting} will be immediately reflected in the
 * repository's configuration; other attributes will be changed in the submodel and used upon the next restart.
 */
public class IndexStorageWriteAttributeHandler extends AbstractRepositoryConfigWriteAttributeHandler {

    static final IndexStorageWriteAttributeHandler INSTANCE = new IndexStorageWriteAttributeHandler(
                                                                                                    ModelAttributes.RAM_INDEX_STORAGE_ATTRIBUTES,
                                                                                                    ModelAttributes.LOCAL_FILE_INDEX_STORAGE_ATTRIBUTES,
                                                                                                    ModelAttributes.MASTER_FILE_INDEX_STORAGE_ATTRIBUTES,
                                                                                                    ModelAttributes.SLAVE_FILE_INDEX_STORAGE_ATTRIBUTES,
                                                                                                    ModelAttributes.CACHE_INDEX_STORAGE_ATTRIBUTES,
                                                                                                    ModelAttributes.CUSTOM_INDEX_STORAGE_ATTRIBUTES);

    private IndexStorageWriteAttributeHandler( AttributeDefinition[]... modelAttributes ) {
        super(allBut(unique(modelAttributes), ModelKeys.INDEX_STORAGE_TYPE));
    }

}
