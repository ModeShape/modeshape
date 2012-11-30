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
package org.modeshape.sequencer.teiid.lexicon;

import static org.modeshape.sequencer.teiid.lexicon.CoreLexicon.Namespace.PREFIX;

/**
 * Constants associated with the core namespace used in reading XMI models and writing JCR nodes.
 */
public interface CoreLexicon {

    /**
     * The URI and prefix constants of the core namespace.
     */
    public interface Namespace {
        String URI = "http://www.metamatrix.com/metamodels/Core";
        String PREFIX = "mmcore";
    }

    /**
     * The model types processed by the Teiid sequencers.
     */
    public interface ModelType {
        String PHYSICAL = "PHYSICAL";
        String VIRTUAL = "VIRTUAL";
    }

    /**
     * Constants associated with the core namespace that identify XMI model identifiers.
     */
    public interface ModelId {
        String ANNOTATED_OBJECT = "annotatedObject";
        String ANNOTATION = "annotations";
        String ANNOTATION_CONTAINER = "AnnotationContainer";
        String DESCRIPTION = "description";
        String HREF = "href";
        String KEY = "key";
        String KEYWORD = "keywords";
        String MAX_SET_SIZE = "maxSetSize";
        String MM_HREF_PREFIX = "mmuuid/";
        String MM_UUID_PREFIX = "mmuuid:";
        String MODEL_ANNOTATION = "ModelAnnotation";
        String MODEL_IMPORT = "modelImports";
        String MODEL_LOCATION = "modelLocation";
        String MODEL_TYPE = "modelType";
        String NAME = "name";
        String NAME_IN_SOURCE = "nameInSource";
        String PATH = "path";
        String PRIMARY_METAMODEL_URI = "primaryMetamodelUri";
        String PRODUCER_NAME = "ProducerName";
        String PRODUCER_VERSION = "ProducerVersion";
        String SUPPORTS_DISTINCT = "supportsDistinct";
        String SUPPORTS_JOIN = "supportsJoin";
        String SUPPORTS_ORDER_BY = "supportsOrderBy";
        String SUPPORTS_OUTER_JOIN = "supportsOuterJoin";
        String SUPPORTS_WHERE_ALL = "supportsWhereAll";
        String TAGS = "tags";
        String UUID = "uuid";
        String VALUE = "value";
        String VISIBLE = "visible";
    }

    /**
     * JCR identifiers relating to the core namespace.
     */
    public interface JcrId {
        String MODEL = PREFIX + ":model";
        String ANNOTATED = PREFIX + ":annotated";
        String TAGS = PREFIX + ":tags";
        String PRIMARY_METAMODEL_URI = PREFIX + ":primaryMetamodelUri";
        String MODEL_TYPE = PREFIX + ":modelType";
        String ORIGINAL_FILE = PREFIX + ":originalFile";
        String IMPORT = PREFIX + ":import";
        String MODEL_LOCATION = PREFIX + ":modelLocation";
        String PATH = PREFIX + ":path";
        String DESCRIPTION = PREFIX + ":description";
        String KEYWORDS = PREFIX + ":keywords";
        String PRODUCER_NAME = PREFIX + ":producerName";
        String PRODUCER_VERSION = PREFIX + ":producerVersion";
        String MAX_SET_SIZE = PREFIX + ":maxSetSize";
        String NAME_IN_SOURCE = PREFIX + ":nameInSource";
        String SUPPORTS_DISTINCT = PREFIX + ":supportsDistinct";
        String SUPPORTS_JOIN = PREFIX + ":supportsJoin";
        String SUPPORTS_ORDER_BY = PREFIX + ":supportsOrderBy";
        String SUPPORTS_OUTER_JOIN = PREFIX + ":supportsOuterJoin";
        String SUPPORTS_WHERE_ALL = PREFIX + ":supportsWhereAll";
        String VISIBLE = PREFIX + ":visible";
        String MODEL_EXTENSION_DEFINITIONS_GROUP_NODE = PREFIX + ":modelExtensionDefinitions";
    }
}
