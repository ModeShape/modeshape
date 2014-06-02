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
import java.util.List;
import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * 
 */
public class MappedAttributeDefinitionBuilder extends SimpleAttributeDefinitionBuilder {

    private List<String> configPath;

    /**
     * @param attributeName
     * @param type
     */
    public MappedAttributeDefinitionBuilder( String attributeName,
                                             ModelType type ) {
        super(attributeName, type);
    }

    @Override
    public MappedAttributeDefinitionBuilder setXmlName( String xmlName ) {
        return (MappedAttributeDefinitionBuilder)super.setXmlName(xmlName);
    }

    @Override
    public MappedAttributeDefinitionBuilder setAllowNull( boolean allowNull ) {
        return (MappedAttributeDefinitionBuilder)super.setAllowNull(allowNull);
    }

    @Override
    public MappedAttributeDefinitionBuilder setAllowExpression( boolean allowExpression ) {
        return (MappedAttributeDefinitionBuilder)super.setAllowExpression(allowExpression);
    }

    @Override
    public MappedAttributeDefinitionBuilder setDefaultValue( ModelNode defaultValue ) {
        return (MappedAttributeDefinitionBuilder)super.setDefaultValue(defaultValue);
    }

    @Override
    public MappedAttributeDefinitionBuilder setMeasurementUnit( MeasurementUnit unit ) {
        return (MappedAttributeDefinitionBuilder)super.setMeasurementUnit(unit);
    }

    @Override
    public MappedAttributeDefinitionBuilder setCorrector( ParameterCorrector corrector ) {
        return (MappedAttributeDefinitionBuilder)super.setCorrector(corrector);
    }

    @Override
    public MappedAttributeDefinitionBuilder setValidator( ParameterValidator validator ) {
        return (MappedAttributeDefinitionBuilder)super.setValidator(validator);
    }

    @Override
    public MappedAttributeDefinitionBuilder setAlternatives( String... alternatives ) {
        return (MappedAttributeDefinitionBuilder)super.setAlternatives(alternatives);
    }

    @Override
    public MappedAttributeDefinitionBuilder addAlternatives( String... alternatives ) {
        return (MappedAttributeDefinitionBuilder)super.addAlternatives(alternatives);
    }

    @Override
    public MappedAttributeDefinitionBuilder setRequires( String... requires ) {
        return (MappedAttributeDefinitionBuilder)super.setRequires(requires);
    }

    @Override
    public MappedAttributeDefinitionBuilder setFlags( Flag... flags ) {
        return (MappedAttributeDefinitionBuilder)super.setFlags(flags);
    }

    @Override
    public MappedAttributeDefinitionBuilder addFlag( Flag flag ) {
        return (MappedAttributeDefinitionBuilder)super.addFlag(flag);
    }

    @Override
    public MappedAttributeDefinitionBuilder removeFlag( Flag flag ) {
        return (MappedAttributeDefinitionBuilder)super.removeFlag(flag);
    }

    @Override
    public MappedAttributeDefinitionBuilder setStorageRuntime() {
        return (MappedAttributeDefinitionBuilder)super.setStorageRuntime();
    }

    @Override
    public MappedAttributeDefinitionBuilder setRestartAllServices() {
        return (MappedAttributeDefinitionBuilder)super.setRestartAllServices();
    }

    @Override
    public MappedAttributeDefinitionBuilder setRestartJVM() {
        return (MappedAttributeDefinitionBuilder)super.setRestartJVM();
    }

    public MappedAttributeDefinitionBuilder setFieldPathInRepositoryConfiguration( String... pathToField ) {
        configPath = Collections.unmodifiableList(Arrays.asList(pathToField));
        return this;
    }

    public MappedAttributeDefinitionBuilder setAccessConstraints(AccessConstraintDefinition...constraints) {
        super.setAccessConstraints(constraints);
        return this;
    }

    @Override
    public SimpleAttributeDefinition build() {
        SimpleAttributeDefinition simpleDefn = super.build();
        return configPath == null ? simpleDefn : new MappedSimpleAttributeDefinition(simpleDefn, configPath);
    }

}
