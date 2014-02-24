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

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;

/**
 * ModeShape implementation of the JCR 2 PropertyDefinitionTemplate interface.
 */
class JcrPropertyDefinitionTemplate extends JcrItemDefinitionTemplate implements PropertyDefinitionTemplate {

    private boolean multiple = false;
    private Value[] defaultValues = null;
    private int requiredType = PropertyType.STRING;
    private String[] valueConstraints = null;
    private boolean fullTextSearchable = true;
    private boolean queryOrderable = true;
    private String[] availableQueryOperators;

    JcrPropertyDefinitionTemplate( ExecutionContext context ) {
        super(context);
    }

    JcrPropertyDefinitionTemplate( JcrPropertyDefinitionTemplate original,
                                   ExecutionContext context ) {
        super(original, context);
        this.multiple = original.multiple;
        this.requiredType = original.requiredType;
        this.valueConstraints = original.valueConstraints;
        this.fullTextSearchable = original.fullTextSearchable;
        this.queryOrderable = original.queryOrderable;
        this.availableQueryOperators = original.availableQueryOperators;
        this.defaultValues = original.defaultValues != null ? new Value[original.defaultValues.length] : null;
        if (original.defaultValues != null) {
            for (int i = 0; i != original.defaultValues.length; ++i) {
                Value originalValue = original.defaultValues[i];
                assert originalValue instanceof JcrValue;
                JcrValue jcrValue = ((JcrValue)originalValue);
                this.defaultValues[i] = new JcrValue(jcrValue.factories(), jcrValue.getType(), jcrValue.value());
                switch (jcrValue.getType()) {
                    case PropertyType.NAME:
                        Name nameValue = original.getContext().getValueFactories().getNameFactory().create(jcrValue.value());
                        JcrItemDefinitionTemplate.registerMissingNamespaces(original.getContext(), context, nameValue);
                        break;
                    case PropertyType.PATH:
                        Path pathValue = original.getContext().getValueFactories().getPathFactory().create(jcrValue.value());
                        JcrItemDefinitionTemplate.registerMissingNamespaces(original.getContext(), context, pathValue);
                        break;
                }
            }
        }

        //check if there are constraints of type Name, for which additional namespaces may need registration
        boolean shouldCheckForNameConstraints = requiredType == PropertyType.NAME ||
                                                requiredType == PropertyType.REFERENCE ||
                                                requiredType == PropertyType.WEAKREFERENCE ||
                                                requiredType == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE;
        if (this.valueConstraints != null && this.valueConstraints.length > 0 && shouldCheckForNameConstraints) {
            for (String constraintValue : original.getValueConstraints()) {
                Name nameValue = original.getContext().getValueFactories().getNameFactory().create(constraintValue);
                JcrItemDefinitionTemplate.registerMissingNamespaces(original.getContext(), context, nameValue);
            }
        }
    }

    JcrPropertyDefinitionTemplate with( ExecutionContext context ) {
        return context == super.getContext() ? this : new JcrPropertyDefinitionTemplate(this, context);
    }

    @Override
    public void setDefaultValues( Value[] defaultValues ) {
        this.defaultValues = defaultValues;
    }

    @Override
    public void setMultiple( boolean multiple ) {
        this.multiple = multiple;
    }

    @Override
    public void setRequiredType( int requiredType ) {
        assert requiredType == PropertyType.BINARY || requiredType == PropertyType.BOOLEAN || requiredType == PropertyType.DATE
               || requiredType == PropertyType.DOUBLE || requiredType == PropertyType.DECIMAL
               || requiredType == PropertyType.LONG || requiredType == PropertyType.NAME || requiredType == PropertyType.PATH
               || requiredType == PropertyType.REFERENCE || requiredType == PropertyType.WEAKREFERENCE
               || requiredType == PropertyType.URI || requiredType == PropertyType.STRING
               || requiredType == PropertyType.UNDEFINED || requiredType == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE;
        this.requiredType = requiredType;
    }

    @Override
    public void setValueConstraints( String[] constraints ) {
        this.valueConstraints = constraints;
    }

    @Override
    public Value[] getDefaultValues() {
        return this.defaultValues;
    }

    @Override
    public int getRequiredType() {
        return requiredType;
    }

    @Override
    public String[] getValueConstraints() {
        return valueConstraints;
    }

    @Override
    public boolean isMultiple() {
        return multiple;
    }

    @Override
    public boolean isFullTextSearchable() {
        return this.fullTextSearchable;
    }

    @Override
    public void setFullTextSearchable( boolean fullTextSearchable ) {
        this.fullTextSearchable = fullTextSearchable;
    }

    @Override
    public String[] getAvailableQueryOperators() {
        return this.availableQueryOperators;
    }

    @Override
    public void setAvailableQueryOperators( String[] queryOperators ) {
        this.availableQueryOperators = queryOperators;
    }

    @Override
    public boolean isQueryOrderable() {
        return this.queryOrderable;
    }

    @Override
    public void setQueryOrderable( boolean queryOrderable ) {
        this.queryOrderable = queryOrderable;
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
