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

    public static class Namespace {
        public static final String URI = "http://www.metamatrix.com/metamodels/Core";
        public static final String PREFIX = "mmcore";
    }

    public static final String MODEL = PREFIX + ":model";
    public static final String ANNOTATED = PREFIX + ":annotated";
    public static final String TAGS = PREFIX + ":tags";
    public static final String PRIMARY_METAMODEL_URI = PREFIX + ":primaryMetamodelUri";
    public static final String MODEL_TYPE = PREFIX + ":modelType";
    public static final String ORIGINAL_FILE = PREFIX + ":originalFile";
    public static final String IMPORT = PREFIX + ":import";
    public static final String PATH = PREFIX + ":path";
    public static final String DESCRIPTION = PREFIX + ":description";
    public static final String KEYWORDS = PREFIX + ":keywords";
    public static final String PRODUCER_NAME = PREFIX + ":producerName";
    public static final String PRODUCER_VERSION = PREFIX + ":producerVersion";

}
