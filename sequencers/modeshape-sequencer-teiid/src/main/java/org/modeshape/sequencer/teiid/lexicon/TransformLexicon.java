/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.sequencer.teiid.lexicon;

import static org.modeshape.sequencer.teiid.lexicon.TransformLexicon.Namespace.PREFIX;

/**
 * Constants associated with the transformation namespace used in reading XMI models and writing JCR nodes.
 */
public interface TransformLexicon {

    /**
     * The URI and prefix constants of the transformaion namespace.
     */
    public interface Namespace {
        String PREFIX = "transform";
        String URI = "http://www.metamatrix.com/metamodels/Transformation";
    }

    /**
     * Constants associated with the transformation namespace that identify XMI model identifiers.
     */
    public interface ModelId {
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

    /**
     * JCR identifiers relating to the transformation namespace.
     */
    public interface JcrId {
        String ALIAS = PREFIX + ":alias";
        String DELETE_ALLOWED = PREFIX + ":deleteAllowed";
        String DELETE_SQL = PREFIX + ":deleteSql";
        String DELETE_SQL_DEFAULT = PREFIX + ":deleteSqlDefault";
        String INPUTS = PREFIX + ":inputs";
        String INSERT_ALLOWED = PREFIX + ":insertAllowed";
        String INSERT_SQL = PREFIX + ":insertSql";
        String INSERT_SQL_DEFAULT = PREFIX + ":insertSqlDefault";
        String OUTPUT_LOCKED = PREFIX + ":outputLocked";
        String SELECT_SQL = PREFIX + ":selectSql";
        String TRANSFORMED = PREFIX + ":transformed";
        String TRANSFORMED_FROM = PREFIX + ":transformedFrom";
        String TRANSFORMED_FROM_HREFS = PREFIX + ":transformedFromHrefs";
        String TRANSFORMED_FROM_XMI_UUIDS = PREFIX + ":transformedFromXmiUuids";
        String TRANSFORMED_FROM_NAMES = PREFIX + ":transformedFromNames";
        String UPDATE_ALLOWED = PREFIX + ":updateAllowed";
        String UPDATE_SQL = PREFIX + ":updateSql";
        String UPDATE_SQL_DEFAULT = PREFIX + ":updateSqlDefault";
        String WITH_SQL = PREFIX + ":withSql";
    }
}
