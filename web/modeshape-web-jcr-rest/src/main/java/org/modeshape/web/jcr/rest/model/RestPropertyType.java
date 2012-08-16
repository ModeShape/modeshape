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

package org.modeshape.web.jcr.rest.model;

import javax.jcr.PropertyType;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.util.StringUtil;

/**
 * A REST representation of a {@link PropertyDefinition}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class RestPropertyType implements JSONAble {

    private final String declaringNodeTypeName;
    private final boolean isAutoCreated;
    private final boolean isMandatory;
    private final boolean isProtected;
    private final String onParentVersion;
    private final String name;
    private final boolean isMultiple;
    private final boolean isFullTextSearchable;
    private final String requiredType;

    public RestPropertyType( PropertyDefinition definition ) {
        this.name = definition.getName();
        this.requiredType = PropertyType.nameFromValue(definition.getRequiredType());
        NodeType declaringNodeType = definition.getDeclaringNodeType();
        this.declaringNodeTypeName = declaringNodeType == null ? null : declaringNodeType.getName();
        this.isAutoCreated = definition.isAutoCreated();
        this.isMandatory = definition.isMandatory();
        this.isProtected = definition.isProtected();
        this.isFullTextSearchable = definition.isFullTextSearchable();
        this.onParentVersion = OnParentVersionAction.nameFromValue(definition.getOnParentVersion());
        this.isMultiple = definition.isMultiple();
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject content = new JSONObject();
        content.put("requiredType", this.requiredType);
        if (!StringUtil.isBlank(declaringNodeTypeName)) {
            content.put("declaringNodeTypeName", this.declaringNodeTypeName);
        }
        content.put("mandatory", isMandatory);
        content.put("multiple", isMultiple);
        content.put("autocreated", isAutoCreated);
        content.put("protected", isProtected);
        content.put("fullTextSearchable", isFullTextSearchable);
        content.put("onParentVersion", onParentVersion);

        JSONObject object = new JSONObject();
        object.put(name, content);
        return object;
    }
}
