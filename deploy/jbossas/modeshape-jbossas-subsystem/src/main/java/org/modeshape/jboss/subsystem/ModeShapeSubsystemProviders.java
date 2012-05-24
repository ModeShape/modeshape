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

import java.util.Locale;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

/**
 *
 */
public class ModeShapeSubsystemProviders {

   static final DescriptionProvider SEQUENCER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription( Locale locale ) {
            return ModeShapeDescriptions.getSequencerDescription(locale);
        }
    };

    static final DescriptionProvider SEQUENCER_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription( Locale locale ) {
            return ModeShapeDescriptions.getSequencerAddDescription(locale);
        }
    };
    static final DescriptionProvider SEQUENCER_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription( Locale locale ) {
            return ModeShapeDescriptions.getSequencerRemoveDescription(locale);
        }
    };

    static final DescriptionProvider INDEX_STORAGE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription( Locale locale ) {
            return ModeShapeDescriptions.getIndexStorageDescription(locale);
        }
    };
    static final DescriptionProvider INDEX_STORAGE_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription( Locale locale ) {
            return ModeShapeDescriptions.getIndexStorageRemoveDescription(locale);
        }
    };

    static final DescriptionProvider RAM_INDEX_STORAGE_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription( Locale locale ) {
            return ModeShapeDescriptions.getRamIndexStorageAddDescription(locale);
        }
    };
    static final DescriptionProvider CACHE_INDEX_STORAGE_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription( Locale locale ) {
            return ModeShapeDescriptions.getCacheIndexStorageAddDescription(locale);
        }
    };
    static final DescriptionProvider CUSTOM_INDEX_STORAGE_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription( Locale locale ) {
            return ModeShapeDescriptions.getCustomIndexStorageAddDescription(locale);
        }
    };

    static final DescriptionProvider LOCAL_FILE_INDEX_STORAGE_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription( Locale locale ) {
            return ModeShapeDescriptions.getLocalFileIndexStorageAddDescription(locale);
        }
    };
    static final DescriptionProvider MASTER_FILE_INDEX_STORAGE_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription( Locale locale ) {
            return ModeShapeDescriptions.getMasterFileIndexStorageAddDescription(locale);
        }
    };
    static final DescriptionProvider SLAVE_FILE_INDEX_STORAGE_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription( Locale locale ) {
            return ModeShapeDescriptions.getSlaveFileIndexStorageAddDescription(locale);
        }
    };

    static final DescriptionProvider BINARY_STORAGE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription( Locale locale ) {
            return ModeShapeDescriptions.getBinaryStorageDescription(locale);
        }
    };
    static final DescriptionProvider BINARY_STORAGE_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription( Locale locale ) {
            return ModeShapeDescriptions.getBinaryStorageRemoveDescription(locale);
        }
    };
    static final DescriptionProvider FILE_BINARY_STORAGE_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription( Locale locale ) {
            return ModeShapeDescriptions.getFileBinaryStorageAddDescription(locale);
        }
    };
    static final DescriptionProvider DB_BINARY_STORAGE_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription( Locale locale ) {
            return ModeShapeDescriptions.getDatabaseBinaryStorageAddDescription(locale);
        }
    };
    static final DescriptionProvider CACHE_BINARY_STORAGE_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription( Locale locale ) {
            return ModeShapeDescriptions.getCacheBinaryStorageAddDescription(locale);
        }
    };
    static final DescriptionProvider CUSTOM_BINARY_STORAGE_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription( Locale locale ) {
            return ModeShapeDescriptions.getCustomBinaryStorageAddDescription(locale);
        }
    };
}
