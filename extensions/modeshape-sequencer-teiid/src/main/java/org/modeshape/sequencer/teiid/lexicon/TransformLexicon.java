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
public class TransformLexicon {

    public static class Namespace {
        public static final String URI = "http://www.metamatrix.com/metamodels/Transformation";
        public static final String PREFIX = "transform";
    }

    public static final Name SELECT_SQL = new BasicName(Namespace.URI, "selectSql");
    public static final Name INSERT_SQL = new BasicName(Namespace.URI, "insertSql");
    public static final Name UPDATE_SQL = new BasicName(Namespace.URI, "updateSql");
    public static final Name DELETE_SQL = new BasicName(Namespace.URI, "deleteSql");
    public static final Name INSERT_ALLOWED = new BasicName(Namespace.URI, "insertAllowed");
    public static final Name UPDATE_ALLOWED = new BasicName(Namespace.URI, "updateAllowed");
    public static final Name DELETE_ALLOWED = new BasicName(Namespace.URI, "deleteAllowed");
    public static final Name OUTPUT_LOCKED = new BasicName(Namespace.URI, "outputLocked");
    public static final Name INSERT_SQL_DEFAULT = new BasicName(Namespace.URI, "insertSqlDefault");
    public static final Name UPDATE_SQL_DEFAULT = new BasicName(Namespace.URI, "updateSqlDefault");
    public static final Name DELETE_SQL_DEFAULT = new BasicName(Namespace.URI, "deleteSqlDefault");
    public static final Name ALIAS = new BasicName(Namespace.URI, "alias");
    public static final Name WITH_SQL = new BasicName(Namespace.URI, "withSql");
    public static final Name TRANSFORMED = new BasicName(Namespace.URI, "transformed");
    public static final Name INPUTS = new BasicName(Namespace.URI, "inputs");
}
