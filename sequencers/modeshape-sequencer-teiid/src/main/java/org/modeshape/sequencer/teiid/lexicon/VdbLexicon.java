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


import static org.modeshape.sequencer.teiid.lexicon.VdbLexicon.Namespace.PREFIX;

/**
 * 
 */
public class VdbLexicon {

    public static class Namespace {
        public static final String URI = "http://www.metamatrix.com/metamodels/VirtualDatabase";
        public static final String PREFIX = "vdb";
    }

    public static final String VIRTUAL_DATABASE = PREFIX + ":virtualDatabase";
    public static final String DESCRIPTION = PREFIX + ":description";
    public static final String VERSION = PREFIX + ":version";
    public static final String PREVIEW = PREFIX + ":preview";
    public static final String ORIGINAL_FILE = PREFIX + ":originalFile";

    public static final String MODEL = PREFIX + ":model";
    public static final String VISIBLE = PREFIX + ":visible";
    public static final String CHECKSUM = PREFIX + ":checksum";
    public static final String BUILT_IN = PREFIX + ":builtIn";
    public static final String PATH_IN_VDB = PREFIX + ":pathInVdb";
    public static final String SOURCE_TRANSLATOR = PREFIX + ":sourceTranslator";
    public static final String SOURCE_JNDI_NAME = PREFIX + ":sourceJndiName";
    public static final String SOURCE_NAME = PREFIX + ":sourceName";

    public static final String MARKERS = PREFIX + ":markers";

    public static final String MARKER = PREFIX + ":marker";
    public static final String SEVERITY = PREFIX + ":severity";
    public static final String PATH = PREFIX + ":path";
    public static final String MESSAGE = PREFIX + ":message";

}
