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
 * 
 */
public class CoreLexicon {

    public interface ModelType {
        // These are not the only valid model types, but they're the only ones we care about at the moment
        String PHYSICAL = "PHYSICAL";
        String VIRTUAL = "VIRTUAL";
    }

    public static class Namespace {
        public static final String URI = "http://www.metamatrix.com/metamodels/Core";
        public static final String PREFIX = "mmcore";
    }

    public interface ModelIds {
        String ANNOTATED_OBJECT = "annotatedObject";
        String ANNOTATION = "annotations";
        String ANNOTATION_CONTAINER = "AnnotationContainer";
        String DESCRIPTION = "description";
        String KEY = "key";
        String MAX_SET_SIZE = "maxSetSize";
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
        String VALUE = "value";
        String VISIBLE = "visible";
    }

    public static final String MODEL = PREFIX + ":model";
    public static final String ANNOTATED = PREFIX + ":annotated";
    public static final String TAGS = PREFIX + ":tags";
    public static final String PRIMARY_METAMODEL_URI = PREFIX + ":primaryMetamodelUri";
    public static final String MODEL_TYPE = PREFIX + ":modelType";
    public static final String ORIGINAL_FILE = PREFIX + ":originalFile";
    public static final String IMPORT = PREFIX + ":import";
    public static final String MODEL_LOCATION = PREFIX + ":modelLocation";
    public static final String PATH = PREFIX + ":path";
    public static final String DESCRIPTION = PREFIX + ":description";
    public static final String KEYWORDS = PREFIX + ":keywords";
    public static final String PRODUCER_NAME = PREFIX + ":producerName";
    public static final String PRODUCER_VERSION = PREFIX + ":producerVersion";
    public static final String MAX_SET_SIZE = PREFIX + ":maxSetSize";
    public static final String NAME_IN_SOURCE = PREFIX + ":nameInSource";
    public static final String SUPPORTS_DISTINCT = PREFIX + ":supportsDistinct";
    public static final String SUPPORTS_JOIN = PREFIX + ":supportsJoin";
    public static final String SUPPORTS_ORDER_BY = PREFIX + ":supportsOrderBy";
    public static final String SUPPORTS_OUTER_JOIN = PREFIX + ":supportsOuterJoin";
    public static final String SUPPORTS_WHERE_ALL = PREFIX + ":supportsWhereAll";
    public static final String VISIBLE = PREFIX + ":visible";
}
