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
package org.modeshape.jboss.subsystem;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.MinMaxValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * An extension to ListAttributeDefinition that contains a mapped field.
 */
public class MappedListAttributeDefinition extends ListAttributeDefinition implements MappedAttributeDefinition {

    private final SimpleAttributeDefinition valueType;
    private SimpleListAttributeDefinition simpleList;
    private final List<String> pathToFieldInConfiguration;
    private final List<String> pathToContainerOfFieldInConfiguration;

    protected MappedListAttributeDefinition( SimpleListAttributeDefinition simpleList,
                                             SimpleAttributeDefinition valueType,
                                             List<String> pathToFieldInConfiguration ) {
        super(simpleList.getName(), simpleList.isAllowNull(), simpleList.getElementValidator(), (AttributeAccess.Flag[]) null);
        this.simpleList = simpleList;
        this.valueType = valueType;
        assert pathToFieldInConfiguration != null;
        assert pathToFieldInConfiguration.size() > 0;
        this.pathToFieldInConfiguration = pathToFieldInConfiguration;
        this.pathToContainerOfFieldInConfiguration = this.pathToFieldInConfiguration.size() > 1 ? this.pathToFieldInConfiguration.subList(0,
                                                                                                                                          this.pathToFieldInConfiguration.size() - 1) : Collections.<String>emptyList();
    }

    @Override
    public List<String> getPathToField() {
        return pathToFieldInConfiguration;
    }

    @Override
    public List<String> getPathToContainerOfField() {
        return pathToContainerOfFieldInConfiguration;
    }

    @Override
    public String getFieldName() {
        return pathToFieldInConfiguration.get(pathToFieldInConfiguration.size() - 1);
    }

    @Override
    public Object getTypedValue( ModelNode node ) throws OperationFailedException {
        return MappedSimpleAttributeDefinition.getTypedValue(node, this);
    }

    // ----------------------------------------------------------------------------------------
    // Delegate all other methods to the wrapped SimpleListAttributeDefinition
    // ----------------------------------------------------------------------------------------

    @Override
    public ModelNode addResourceAttributeDescription( ResourceBundle bundle,
                                                      String prefix,
                                                      ModelNode resourceDescription ) {
        return simpleList.addResourceAttributeDescription(bundle, prefix, resourceDescription);
    }

    @Override
    public ModelNode addOperationParameterDescription( ResourceBundle bundle,
                                                       String prefix,
                                                       ModelNode operationDescription ) {
        return simpleList.addOperationParameterDescription(bundle, prefix, operationDescription);
    }

    @Override
    public ParameterValidator getElementValidator() {
        return simpleList.getElementValidator();
    }

    @Override
    public ModelNode parse( String value,
                            XMLStreamReader reader ) throws XMLStreamException {
        return simpleList.parse(value, reader);
    }

    @Override
    public void marshallAsElement( ModelNode resourceModel,
                                   XMLStreamWriter writer ) throws XMLStreamException {
        simpleList.marshallAsElement(resourceModel, writer);
    }

    @Override
    public String getName() {
        return simpleList.getName();
    }

    @Override
    public String getXmlName() {
        return simpleList.getXmlName();
    }

    @Override
    public ModelType getType() {
        return simpleList.getType();
    }

    @Override
    public boolean isAllowNull() {
        return simpleList.isAllowNull();
    }

    @Override
    public boolean isAllowExpression() {
        return simpleList.isAllowExpression();
    }

    @Override
    public ModelNode getDefaultValue() {
        ModelNode listDefault = simpleList.getDefaultValue();
        if (listDefault != null) {
            return listDefault;
        }
        //attempt to resolve the default against the value type (not that this is a bit of hack because SimpleListAttributeDefinition
        //simply do not support default values (because of its builder)
        ModelNode valueTypeDefault = valueType.getDefaultValue();
        if (valueTypeDefault == null) {
            return null;
        }
        switch (valueTypeDefault.getType()) {
            case LIST: {
                return valueTypeDefault;
            }
            default: {
                ModelNode result = new ModelNode();
                String[] segments = valueTypeDefault.asString().split(" ");
                for (String segment : segments) {
                    result.add(segment);
                }
                return result;
            }
        }
    }

    @Override
    public MeasurementUnit getMeasurementUnit() {
        return simpleList.getMeasurementUnit();
    }

    @Override
    public ParameterValidator getValidator() {
        return simpleList.getValidator();
    }

    @Override
    public String[] getAlternatives() {
        return simpleList.getAlternatives();
    }

    @Override
    public String[] getRequires() {
        return simpleList.getRequires();
    }

    @Override
    public EnumSet<Flag> getFlags() {
        return simpleList.getFlags();
    }

    @Override
    public boolean isMarshallable( ModelNode resourceModel ) {
        return simpleList.isMarshallable(resourceModel);
    }

    @Override
    public void parseAndAddParameterElement( String value,
                                             ModelNode operation,
                                             XMLStreamReader reader ) throws XMLStreamException {
        simpleList.parseAndAddParameterElement(value, operation, reader);
    }

    @Override
    public boolean isMarshallable( ModelNode resourceModel,
                                   boolean marshallDefault ) {
        return simpleList.isMarshallable(resourceModel, marshallDefault);
    }

    @Override
    public ModelNode validateOperation( ModelNode operationObject ) throws OperationFailedException {
        return simpleList.validateOperation(operationObject);
    }

    @Override
    public ModelNode resolveModelAttribute( OperationContext context,
                                            ModelNode model ) throws OperationFailedException {
        return simpleList.resolveModelAttribute(context, model);
    }

    @Override
    public ModelNode addResourceAttributeDescription( ModelNode resourceDescription,
                                                      ResourceDescriptionResolver resolver,
                                                      Locale locale,
                                                      ResourceBundle bundle ) {
        return simpleList.addResourceAttributeDescription(resourceDescription, resolver, locale, bundle);
    }

    @Override
    public ModelNode addOperationParameterDescription( ModelNode resourceDescription,
                                                       String operationName,
                                                       ResourceDescriptionResolver resolver,
                                                       Locale locale,
                                                       ResourceBundle bundle ) {
        return simpleList.addOperationParameterDescription(resourceDescription, operationName, resolver, locale, bundle);
    }

    @Override
    public boolean isAllowed( ModelNode operationObject ) {
        return simpleList.isAllowed(operationObject);
    }

    @Override
    public boolean isRequired( ModelNode operationObject ) {
        return simpleList.isRequired(operationObject);
    }

    @Override
    public boolean hasAlternative( ModelNode operationObject ) {
        return simpleList.hasAlternative(operationObject);
    }

    @Override
    public String getAttributeTextDescription( ResourceBundle bundle,
                                               String prefix ) {
        return simpleList.getAttributeTextDescription(bundle, prefix);
    }

    @Override
    public ModelNode getNoTextDescription( boolean forOperation ) {
        return simpleList.getNoTextDescription(forOperation);
    }

    @Override
    protected void addValueTypeDescription( ModelNode node,
                                            ResourceBundle bundle ) {
        node.get(ModelDescriptionConstants.VALUE_TYPE, valueType.getName()).set(getValueTypeDescription(false));
    }

    @Override
    protected void addAttributeValueTypeDescription( ModelNode node,
                                                     ResourceDescriptionResolver resolver,
                                                     Locale locale,
                                                     ResourceBundle bundle ) {
        final ModelNode valueTypeDesc = getValueTypeDescription(false);
        valueTypeDesc.get(ModelDescriptionConstants.DESCRIPTION)
                     .set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, valueType.getName()));
        node.get(ModelDescriptionConstants.VALUE_TYPE, valueType.getName()).set(valueTypeDesc);
    }

    @Override
    protected void addOperationParameterValueTypeDescription( ModelNode node,
                                                              String operationName,
                                                              ResourceDescriptionResolver resolver,
                                                              Locale locale,
                                                              ResourceBundle bundle ) {
        final ModelNode valueTypeDesc = getValueTypeDescription(true);
        valueTypeDesc.get(ModelDescriptionConstants.DESCRIPTION)
                     .set(resolver.getOperationParameterValueTypeDescription(operationName,
                                                                             getName(),
                                                                             locale,
                                                                             bundle,
                                                                             valueType.getName()));
        node.get(ModelDescriptionConstants.VALUE_TYPE, valueType.getName()).set(valueTypeDesc);
    }

    private ModelNode getValueTypeDescription( boolean forOperation ) {
        final ModelNode result = new ModelNode();
        result.get(ModelDescriptionConstants.TYPE).set(valueType.getType());
        result.get(ModelDescriptionConstants.DESCRIPTION); // placeholder
        result.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(valueType.isAllowExpression());
        if (forOperation) {
            result.get(ModelDescriptionConstants.REQUIRED).set(!valueType.isAllowNull());
        }
        result.get(ModelDescriptionConstants.NILLABLE).set(isAllowNull());
        final ModelNode defaultValue = valueType.getDefaultValue();
        if (!forOperation && defaultValue != null && defaultValue.isDefined()) {
            result.get(ModelDescriptionConstants.DEFAULT).set(defaultValue);
        }
        MeasurementUnit measurementUnit = valueType.getMeasurementUnit();
        if (measurementUnit != null && measurementUnit != MeasurementUnit.NONE) {
            result.get(ModelDescriptionConstants.UNIT).set(measurementUnit.getName());
        }
        final String[] alternatives = valueType.getAlternatives();
        if (alternatives != null) {
            for (final String alternative : alternatives) {
                result.get(ModelDescriptionConstants.ALTERNATIVES).add(alternative);
            }
        }
        final String[] requires = valueType.getRequires();
        if (requires != null) {
            for (final String required : requires) {
                result.get(ModelDescriptionConstants.REQUIRES).add(required);
            }
        }
        final ParameterValidator validator = valueType.getValidator();
        if (validator instanceof MinMaxValidator) {
            MinMaxValidator minMax = (MinMaxValidator)validator;
            Long min = minMax.getMin();
            if (min != null) {
                switch (valueType.getType()) {
                    case STRING:
                    case LIST:
                    case OBJECT:
                        result.get(ModelDescriptionConstants.MIN_LENGTH).set(min);
                        break;
                    default:
                        result.get(ModelDescriptionConstants.MIN).set(min);
                }
            }
            Long max = minMax.getMax();
            if (max != null) {
                switch (valueType.getType()) {
                    case STRING:
                    case LIST:
                    case OBJECT:
                        result.get(ModelDescriptionConstants.MAX_LENGTH).set(max);
                        break;
                    default:
                        result.get(ModelDescriptionConstants.MAX).set(max);
                }
            }
        }
        if (validator instanceof AllowedValuesValidator) {
            AllowedValuesValidator avv = (AllowedValuesValidator)validator;
            List<ModelNode> allowed = avv.getAllowedValues();
            if (allowed != null) {
                for (ModelNode ok : allowed) {
                    result.get(ModelDescriptionConstants.ALLOWED).add(ok);
                }
            }
        }
        return result;
    }

    public static class Builder {
        private final SimpleAttributeDefinition valueType;
        private final SimpleListAttributeDefinition.Builder builder;
        private List<String> configPath;

        public Builder( final String name,
                        final SimpleAttributeDefinition valueType ) {
            this.valueType = valueType;
            builder = new SimpleListAttributeDefinition.Builder(name, valueType);
        }

        public static Builder of( final String name,
                                  final SimpleAttributeDefinition valueType ) {
            return new Builder(name, valueType);
        }

        public ListAttributeDefinition build() {
            SimpleListAttributeDefinition simpleList = builder.build();
            if (configPath == null) return simpleList;
            return new MappedListAttributeDefinition(simpleList, valueType, configPath);
        }

        public Builder setAllowNull( final boolean allowNull ) {
            builder.setAllowNull(allowNull);
            return this;
        }

        public Builder setFlags( final AttributeAccess.Flag... flags ) {
            builder.setFlags(flags);
            return this;
        }

        public Builder setMaxSize( final int maxSize ) {
            builder.setMaxSize(maxSize);
            return this;
        }

        public Builder setMinSize( final int minSize ) {
            builder.setMinSize(minSize);
            return this;
        }

        public Builder setRequires( final String... requires ) {
            builder.setRequires(requires);
            return this;
        }

        public Builder setXmlName( final String xmlName ) {
            builder.setXmlName(xmlName);
            return this;
        }

        public Builder setFieldPathInRepositoryConfiguration( String... pathToField ) {
            configPath = Collections.unmodifiableList(Arrays.asList(pathToField));
            return this;
        }

        public Builder setAccessConstraints(AccessConstraintDefinition...constraints) {
            builder.setAccessConstraints(constraints);
            return this;
        }
    }
}
