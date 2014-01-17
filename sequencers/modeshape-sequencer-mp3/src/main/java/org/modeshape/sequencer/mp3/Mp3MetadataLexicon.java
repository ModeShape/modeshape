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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.sequencer.mp3;

import static org.modeshape.sequencer.mp3.Mp3MetadataLexicon.Namespace.PREFIX;
import org.modeshape.common.annotation.Immutable;


/**
 * A lexicon of names used within the mp3 sequencer.
 */
@Immutable
public class Mp3MetadataLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/mp3/1.0";
        public static final String PREFIX = "mp3";
    }

    public static final String METADATA_NODE = PREFIX + ":metadata";
    public static final String TITLE = PREFIX + ":title";
    public static final String AUTHOR = PREFIX + ":author";
    public static final String ALBUM = PREFIX + ":album";
    public static final String YEAR = PREFIX + ":year";
    public static final String COMMENT = PREFIX + ":comment";

}
