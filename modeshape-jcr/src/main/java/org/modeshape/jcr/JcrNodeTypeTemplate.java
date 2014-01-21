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

import java.util.ArrayList;
import java.util.List;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * ModeShape implementation of the JCR NodeTypeTemplate interface
 */
@NotThreadSafe
public class JcrNodeTypeTemplate implements NodeTypeTemplate {

    private final ExecutionContext context;
    private final List<NodeDefinitionTemplate> nodeDefinitionTemplates = new ArrayList<NodeDefinitionTemplate>();
    private final List<PropertyDefinitionTemplate> propertyDefinitionTemplates = new ArrayList<PropertyDefinitionTemplate>();
    private final boolean createdFromExistingDefinition;
    private boolean isAbstract;
    private boolean queryable = true;
    private boolean mixin;
    private boolean orderableChildNodes;
    private Name[] declaredSupertypeNames;
    private Name name;
    private Name primaryItemName;

    JcrNodeTypeTemplate( ExecutionContext context ) {
        this(context, false);
    }

    JcrNodeTypeTemplate( ExecutionContext context,
                         boolean createdFromExistingDefinition ) {
        assert context != null;

        this.context = context;
        this.createdFromExistingDefinition = createdFromExistingDefinition;
    }

    JcrNodeTypeTemplate( JcrNodeTypeTemplate original,
                         ExecutionContext context ) {
        this.context = context;
        this.isAbstract = original.isAbstract;
        this.queryable = original.queryable;
        this.mixin = original.mixin;
        this.name = original.name;
        this.orderableChildNodes = original.orderableChildNodes;
        this.declaredSupertypeNames = original.declaredSupertypeNames;
        this.primaryItemName = original.primaryItemName;
        JcrItemDefinitionTemplate.registerMissingNamespaces(original.context, context, this.name);
        JcrItemDefinitionTemplate.registerMissingNamespaces(original.context, context, this.declaredSupertypeNames);
        JcrItemDefinitionTemplate.registerMissingNamespaces(original.context, context, this.primaryItemName);
        for (NodeDefinitionTemplate childDefn : original.nodeDefinitionTemplates) {
            this.nodeDefinitionTemplates.add(((JcrNodeDefinitionTemplate)childDefn).with(context));
        }
        for (PropertyDefinitionTemplate propDefn : original.propertyDefinitionTemplates) {
            this.propertyDefinitionTemplates.add(((JcrPropertyDefinitionTemplate)propDefn).with(context));
        }
        this.createdFromExistingDefinition = original.createdFromExistingDefinition;
    }

    JcrNodeTypeTemplate with( ExecutionContext context ) {
        return context == this.context ? this : new JcrNodeTypeTemplate(this, context);
    }

    ExecutionContext getExecutionContext() {
        return context;
    }

    private String string( Name name ) {
        if (name == null) return null;
        return name.getString(context.getNamespaceRegistry());
    }

    Name[] declaredSupertypeNames() {
        return this.declaredSupertypeNames;
    }

    @Override
    public List<NodeDefinitionTemplate> getNodeDefinitionTemplates() {
        return nodeDefinitionTemplates;
    }

    @Override
    public List<PropertyDefinitionTemplate> getPropertyDefinitionTemplates() {
        return propertyDefinitionTemplates;
    }

    @Override
    public void setAbstract( boolean isAbstract ) {
        this.isAbstract = isAbstract;
    }

    @Override
    public void setDeclaredSuperTypeNames( String[] names ) throws ConstraintViolationException {
        if (names == null) {
            throw new ConstraintViolationException(JcrI18n.badNodeTypeName.text("names"));
        }

        Name[] supertypeNames = new Name[names.length];

        for (int i = 0; i < names.length; i++) {
            CheckArg.isNotEmpty(names[i], "names[" + i + "");
            try {
                supertypeNames[i] = context.getValueFactories().getNameFactory().create(names[i]);
            } catch (ValueFormatException vfe) {
                throw new ConstraintViolationException(vfe);
            }
        }
        this.declaredSupertypeNames = supertypeNames;
    }

    @Override
    public void setMixin( boolean mixin ) {
        this.mixin = mixin;
    }

    @Override
    public void setName( String name ) throws ConstraintViolationException {
        CheckArg.isNotEmpty(name, "name");
        try {
            this.name = context.getValueFactories().getNameFactory().create(name);
        } catch (ValueFormatException vfe) {
            throw new ConstraintViolationException(vfe);
        }
    }

    @Override
    public void setOrderableChildNodes( boolean orderable ) {
        this.orderableChildNodes = orderable;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Passing a null or blank name is equivalent to "unsetting" (or removing) the primary item name.
     * </p>
     * 
     * @see javax.jcr.nodetype.NodeTypeDefinition#getPrimaryItemName() type.NodeTypeTemplate#setPrimaryItemName(java.lang.String)
     */
    @Override
    public void setPrimaryItemName( String name ) throws ConstraintViolationException {
        if (name == null || name.trim().length() == 0) {
            this.primaryItemName = null;
        } else {
            try {
                this.primaryItemName = context.getValueFactories().getNameFactory().create(name);
            } catch (ValueFormatException vfe) {
                throw new ConstraintViolationException(vfe);
            }
        }
    }

    @Override
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        if (!createdFromExistingDefinition && nodeDefinitionTemplates.isEmpty()) return null;

        return nodeDefinitionTemplates.toArray(new NodeDefinition[nodeDefinitionTemplates.size()]);
    }

    @Override
    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        if (!createdFromExistingDefinition && propertyDefinitionTemplates.isEmpty()) return null;

        return propertyDefinitionTemplates.toArray(new PropertyDefinition[propertyDefinitionTemplates.size()]);
    }

    @Override
    public String[] getDeclaredSupertypeNames() {
        if (declaredSupertypeNames == null) return new String[0];
        String[] names = new String[declaredSupertypeNames.length];

        for (int i = 0; i < declaredSupertypeNames.length; i++) {
            names[i] = declaredSupertypeNames[i].getString(context.getNamespaceRegistry());
        }
        return names;
    }

    @Override
    public String getName() {
        return string(name);
    }

    @Override
    public String getPrimaryItemName() {
        return string(primaryItemName);
    }

    @Override
    public boolean hasOrderableChildNodes() {
        return orderableChildNodes;
    }

    @Override
    public boolean isAbstract() {
        return isAbstract;
    }

    @Override
    public boolean isMixin() {
        return mixin;
    }

    @Override
    public boolean isQueryable() {
        return queryable;
    }

    @Override
    public void setQueryable( boolean queryable ) {
        this.queryable = queryable;
    }

    @Override
    public String toString() {
        return getName();
    }
}
