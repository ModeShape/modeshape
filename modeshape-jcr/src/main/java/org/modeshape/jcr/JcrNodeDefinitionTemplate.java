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

import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * ModeShape implementation of the JCR 2 NodeDefinitionTemplate interface
 */
@NotThreadSafe
class JcrNodeDefinitionTemplate extends JcrItemDefinitionTemplate implements NodeDefinitionTemplate {

    private Name defaultPrimaryType;
    private Name[] requiredPrimaryTypes;
    private boolean allowSameNameSiblings;

    JcrNodeDefinitionTemplate( ExecutionContext context ) {
        super(context);
    }

    JcrNodeDefinitionTemplate( JcrNodeDefinitionTemplate original,
                               ExecutionContext context ) {
        super(original, context);
        this.defaultPrimaryType = original.defaultPrimaryType;
        this.requiredPrimaryTypes = original.requiredPrimaryTypes;
        this.allowSameNameSiblings = original.allowSameNameSiblings;
        JcrItemDefinitionTemplate.registerMissingNamespaces(original.getContext(), context, this.defaultPrimaryType);
        JcrItemDefinitionTemplate.registerMissingNamespaces(original.getContext(), context, this.requiredPrimaryTypes);
    }

    JcrNodeDefinitionTemplate with( ExecutionContext context ) {
        return context == super.getContext() ? this : new JcrNodeDefinitionTemplate(this, context);
    }

    @Override
    public void setDefaultPrimaryTypeName( String defaultPrimaryType ) throws ConstraintViolationException {
        if (defaultPrimaryType == null || defaultPrimaryType.trim().length() == 0) {
            this.defaultPrimaryType = null;
        } else {
            try {
                this.defaultPrimaryType = getContext().getValueFactories().getNameFactory().create(defaultPrimaryType);
            } catch (ValueFormatException vfe) {
                throw new ConstraintViolationException(vfe);
            }
        }
    }

    @Override
    public void setRequiredPrimaryTypeNames( String[] requiredPrimaryTypes ) throws ConstraintViolationException {
        if (requiredPrimaryTypes == null) {
            throw new ConstraintViolationException(JcrI18n.badNodeTypeName.text("requiredPrimaryTypes"));
        }

        NameFactory nameFactory = getContext().getValueFactories().getNameFactory();
        Name[] rpts = new Name[requiredPrimaryTypes.length];
        for (int i = 0; i < requiredPrimaryTypes.length; i++) {
            try {
                rpts[i] = nameFactory.create(requiredPrimaryTypes[i]);
            } catch (ValueFormatException vfe) {
                throw new ConstraintViolationException(vfe);
            }
        }

        this.requiredPrimaryTypes = rpts;
    }

    @Override
    public void setSameNameSiblings( boolean allowSameNameSiblings ) {
        this.allowSameNameSiblings = allowSameNameSiblings;
    }

    @Override
    public boolean allowsSameNameSiblings() {
        return allowSameNameSiblings;
    }

    @Override
    public NodeType getDefaultPrimaryType() {
        return null;
    }

    @Override
    public String getDefaultPrimaryTypeName() {
        if (defaultPrimaryType == null) return null;
        return defaultPrimaryType.getString(getContext().getNamespaceRegistry());
    }

    @Override
    public NodeType[] getRequiredPrimaryTypes() {
        // This method should return null since it's not attached to a registered node type ...
        return null;
    }

    @Override
    public String[] getRequiredPrimaryTypeNames() {
        if (requiredPrimaryTypes == null) return null;

        NamespaceRegistry registry = getContext().getNamespaceRegistry();
        String[] rpts = new String[requiredPrimaryTypes.length];
        for (int i = 0; i < requiredPrimaryTypes.length; i++) {
            rpts[i] = requiredPrimaryTypes[i].getString(registry);
        }
        return rpts;
    }

    @Override
    public void setName( String name ) throws ConstraintViolationException {
        super.setName(name);
    }

    @Override
    public void setAutoCreated( boolean autoCreated ) {
        super.setAutoCreated(autoCreated);
    }

    @Override
    public void setMandatory( boolean mandatory ) {
        super.setMandatory(mandatory);
    }

    @Override
    public void setOnParentVersion( int onParentVersion ) {
        super.setOnParentVersion(onParentVersion);
    }

    @Override
    public void setProtected( boolean isProtected ) {
        super.setProtected(isProtected);
    }
}
