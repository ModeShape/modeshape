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
package org.modeshape.jdbc.rest;

import javax.jcr.version.OnParentVersionAction;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.annotation.Immutable;

/**
 * An {@link javax.jcr.nodetype.ItemDefinition} implementation for the ModeShape client.
 */
@Immutable
public abstract class ItemDefinition implements javax.jcr.nodetype.ItemDefinition {

    private final String declaringNodeTypeName;
    private final boolean isAutoCreated;
    private final boolean isMandatory;
    private final boolean isProtected;
    private final int onParentVersion;
    private final NodeTypes nodeTypes;

    protected ItemDefinition( String declaringNodeTypeName,
                              JSONObject json,
                              NodeTypes nodeTypes ) {
        this.declaringNodeTypeName = declaringNodeTypeName;
        this.nodeTypes = nodeTypes;
        this.isAutoCreated = JSONHelper.valueFrom(json, "jcr:autoCreated", false);
        this.isMandatory = JSONHelper.valueFrom(json, "jcr:mandatory", false);
        this.isProtected = JSONHelper.valueFrom(json, "jcr:protected", false);
        this.onParentVersion = OnParentVersionAction.valueFromName(JSONHelper.valueFrom(json, "jcr:onParentVersion"));
    }

    protected NodeTypes nodeTypes() {
        return nodeTypes;
    }

    @Override
    public NodeType getDeclaringNodeType() {
        return nodeTypes.getNodeType(declaringNodeTypeName);
    }

    @Override
    public int getOnParentVersion() {
        return onParentVersion;
    }

    @Override
    public boolean isAutoCreated() {
        return isAutoCreated;
    }

    @Override
    public boolean isMandatory() {
        return isMandatory;
    }

    @Override
    public boolean isProtected() {
        return isProtected;
    }

}
