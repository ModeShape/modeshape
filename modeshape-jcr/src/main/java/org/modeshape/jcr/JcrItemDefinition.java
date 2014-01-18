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
package org.modeshape.jcr;

import javax.jcr.nodetype.ItemDefinition;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.api.Namespaced;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;

/**
 * ModeShape implementation of the {@link ItemDefinition} interface. This implementation is immutable and has all fields
 * initialized through its constructor.
 */
@Immutable
abstract class JcrItemDefinition implements ItemDefinition, Namespaced {

    protected final ExecutionContext context;

    protected final JcrNodeType declaringNodeType;
    protected final Name name;
    private final int onParentVersion;
    private final boolean autoCreated;
    private final boolean mandatory;
    private final boolean protectedItem;

    JcrItemDefinition( ExecutionContext context,
                       JcrNodeType declaringNodeType,
                       Name name,
                       int onParentVersion,
                       boolean autoCreated,
                       boolean mandatory,
                       boolean protectedItem ) {
        super();
        this.context = context;
        this.declaringNodeType = declaringNodeType;
        this.name = name != null ? name : context.getValueFactories().getNameFactory().create(JcrNodeType.RESIDUAL_ITEM_NAME);
        this.onParentVersion = onParentVersion;
        this.autoCreated = autoCreated;
        this.mandatory = mandatory;
        this.protectedItem = protectedItem;
    }

    abstract NodeKey key();

    final Name getInternalName() {
        return name;
    }

    @Override
    public String getLocalName() {
        return name.getLocalName();
    }

    @Override
    public String getNamespaceURI() {
        return name.getNamespaceUri();
    }

    /**
     * Determine whether this is a residual item. Section 6.7.15 in the JSR 1.0 specification defines a residual item as one
     * having a name equal to "*".
     * 
     * @return true if this item is residual, or false otherwise
     */
    public boolean isResidual() {
        return name.getLocalName().equals("*");
    }

    @Override
    public JcrNodeType getDeclaringNodeType() {
        return declaringNodeType;
    }

    @Override
    public String getName() {
        if (name == null) {
            return JcrNodeType.RESIDUAL_ITEM_NAME;
        }

        return name.getString(context.getNamespaceRegistry());
    }

    @Override
    public int getOnParentVersion() {
        return onParentVersion;
    }

    @Override
    public boolean isAutoCreated() {
        return autoCreated;
    }

    @Override
    public boolean isMandatory() {
        return mandatory;
    }

    @Override
    public boolean isProtected() {
        return protectedItem;
    }

}
