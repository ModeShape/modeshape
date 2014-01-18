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
package org.modeshape.sequencer.cnd;

/**
 * Class which holds the string constants corresponding to the cnd sequencer's lexicon
 *
 * @author Horia Chiorean
 */
public final class CndSequencerLexicon {

    private CndSequencerLexicon() {
    }

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/sequencer/cnd/1.0";
        public static final String PREFIX = "cnd";
    }

    public static final String NODE_TYPE = "cnd:nodeType";
    public static final String NODE_TYPE_NAME = "cnd:nodeTypeName";
    public static final String SUPERTYPES = "cnd:supertypes";
    public static final String IS_ABSTRACT = "cnd:isAbstract";
    public static final String IS_MIXIN= "cnd:isMixin";
    public static final String IS_QUERYABLE = "cnd:isQueryable";
    public static final String PRIMARY_ITEM_NAME = "cnd:primaryItemName";
    public static final String HAS_ORDERABLE_CHILD_NODES = "cnd:hasOrderableChildNodes";

    public static final String PROPERTY_DEFINITION = "cnd:propertyDefinition";
    public static final String NAME = "cnd:name";
    public static final String AUTO_CREATED = "cnd:autoCreated";
    public static final String MANDATORY = "cnd:mandatory";
    public static final String IS_FULL_TEXT_SEARCHABLE = "cnd:isFullTextSearchable";
    public static final String ON_PARENT_VERSION = "cnd:onParentVersion";
    public static final String PROTECTED = "cnd:protected";
    public static final String REQUIRED_TYPE = "cnd:requiredType";
    public static final String VALUE_CONSTRAINTS = "cnd:valueConstraints";
    public static final String AVAILABLE_QUERY_OPERATORS = "cnd:availableQueryOperators";
    public static final String DEFAULT_VALUES = "cnd:defaultValues";
    public static final String MULTIPLE = "cnd:multiple";
    public static final String IS_QUERY_ORDERABLE = "cnd:isQueryOrderable";


    public static final String CHILD_NODE_DEFINITION = "cnd:childNodeDefinition";
    public static final String REQUIRED_PRIMARY_TYPES = "cnd:requiredPrimaryTypes";
    public static final String DEFAULT_PRIMARY_TYPE = "cnd:defaultPrimaryType";
    public static final String SAME_NAME_SIBLINGS = "cnd:sameNameSiblings";
}
