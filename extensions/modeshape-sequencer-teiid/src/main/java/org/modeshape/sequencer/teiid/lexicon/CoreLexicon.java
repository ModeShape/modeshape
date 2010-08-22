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

import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;

/**
 * 
 */
public class CoreLexicon {

    public static class Namespace {
        public static final String URI = "http://www.metamatrix.com/metamodels/Core";
        public static final String PREFIX = "mmcore";
    }

    public static final Name MODEL = new BasicName(Namespace.URI, "model");
    public static final Name ANNOTATED = new BasicName(Namespace.URI, "annotated");
    public static final Name TAGS = new BasicName(Namespace.URI, "tags");
    public static final Name PRIMARY_METAMODEL_URI = new BasicName(Namespace.URI, "primaryMetamodelUri");
    public static final Name MODEL_TYPE = new BasicName(Namespace.URI, "modelType");
    public static final Name ORIGINAL_FILE = new BasicName(Namespace.URI, "originalFile");
    public static final Name IMPORT = new BasicName(Namespace.URI, "import");
    public static final Name PATH = new BasicName(Namespace.URI, "path");
    public static final Name DESCRIPTION = new BasicName(Namespace.URI, "description");
    public static final Name KEYWORDS = new BasicName(Namespace.URI, "keywords");
    public static final Name PRODUCER_NAME = new BasicName(Namespace.URI, "producerName");
    public static final Name PRODUCER_VERSION = new BasicName(Namespace.URI, "producerVersion");

}
