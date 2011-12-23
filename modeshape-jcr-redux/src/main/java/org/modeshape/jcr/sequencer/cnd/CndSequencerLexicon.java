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
package org.modeshape.jcr.sequencer.cnd;

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
