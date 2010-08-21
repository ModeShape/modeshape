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
public class VdbLexicon {

    public static class Namespace {
        public static final String URI = "http://www.metamatrix.com/metamodels/VirtualDatabase";
        public static final String PREFIX = "vdb";
    }

    public static final Name VIRTUAL_DATABASE = new BasicName(Namespace.URI, "virtualDatabase");
    public static final Name DESCRIPTION = new BasicName(Namespace.URI, "description");
    public static final Name VERSION = new BasicName(Namespace.URI, "version");
    public static final Name PREVIEW = new BasicName(Namespace.URI, "preview");
    public static final Name ORIGINAL_FILE = new BasicName(Namespace.URI, "originalFile");

    public static final Name MODEL = new BasicName(Namespace.URI, "model");
    public static final Name VISIBLE = new BasicName(Namespace.URI, "visible");
    public static final Name CHECKSUM = new BasicName(Namespace.URI, "checksum");
    public static final Name BUILT_IN = new BasicName(Namespace.URI, "builtIn");
    public static final Name PATH_IN_VDB = new BasicName(Namespace.URI, "pathInVdb");
    public static final Name SOURCE_TRANSLATOR = new BasicName(Namespace.URI, "sourceTranslator");
    public static final Name SOURCE_JNDI_NAME = new BasicName(Namespace.URI, "sourceJndiName");
    public static final Name SOURCE_NAME = new BasicName(Namespace.URI, "sourceName");

    public static final Name MARKERS = new BasicName(Namespace.URI, "markers");

    public static final Name MARKER = new BasicName(Namespace.URI, "marker");
    public static final Name SEVERITY = new BasicName(Namespace.URI, "severity");
    public static final Name PATH = new BasicName(Namespace.URI, "path");
    public static final Name MESSAGE = new BasicName(Namespace.URI, "message");

}
