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

import static org.modeshape.sequencer.teiid.lexicon.TransformLexicon.Namespace.PREFIX;

/**
 * 
 */
public class TransformLexicon {

    public static class Namespace {
        public static final String URI = "http://www.metamatrix.com/metamodels/Transformation";
        public static final String PREFIX = "transform";
    }

    public interface ModelIds {
        String ALIAS = "alias";
        String ALIASED_OBJECT = "aliasedObject";
        String ALIASES = "aliases";
        String DELETE_ALLOWED = "deleteAllowed";
        String DELETE_SQL = "deleteSql";
        String DELETE_SQL_DEFAULT = "deleteSql";
        String HELPER = "helper";
        String HREF = "href";
        String INPUTS = "inputs";
        String INSERT_ALLOWED = "insertAllowed";
        String INSERT_SQL = "insertSql";
        String INSERT_SQL_DEFAULT = "insertSql";
        String NESTED = "nested";
        String OUTPUTS = "outputs";
        String SELECT_SQL = "selectSql";
        String TARGET = "target";
        String TRANSFORMATION_CONTAINER = "TransformationContainer";
        String TRANSFORMATION_MAPPINGS = "transformationMappings";
        String UPDATE_ALLOWED = "updateAllowed";
        String UPDATE_SQL = "updateSql";
        String UPDATE_SQL_DEFAULT = "updateSql";
    }

    public static final String SELECT_SQL = PREFIX + ":selectSql";
    public static final String INSERT_SQL = PREFIX + ":insertSql";
    public static final String UPDATE_SQL = PREFIX + ":updateSql";
    public static final String DELETE_SQL = PREFIX + ":deleteSql";
    public static final String INSERT_ALLOWED = PREFIX + ":insertAllowed";
    public static final String UPDATE_ALLOWED = PREFIX + ":updateAllowed";
    public static final String DELETE_ALLOWED = PREFIX + ":deleteAllowed";
    public static final String OUTPUT_LOCKED = PREFIX + ":outputLocked";
    public static final String INSERT_SQL_DEFAULT = PREFIX + ":insertSqlDefault";
    public static final String UPDATE_SQL_DEFAULT = PREFIX + ":updateSqlDefault";
    public static final String DELETE_SQL_DEFAULT = PREFIX + ":deleteSqlDefault";
    public static final String ALIAS = PREFIX + ":alias";
    public static final String WITH_SQL = PREFIX + ":withSql";
    public static final String TRANSFORMED = PREFIX + ":transformed";
    public static final String INPUTS = PREFIX + ":inputs";
}
