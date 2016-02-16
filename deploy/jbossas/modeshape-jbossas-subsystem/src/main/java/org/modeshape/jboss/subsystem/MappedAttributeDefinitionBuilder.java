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
import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *  An AS attribute definition builder which produces a {@link MappedAttributeDefinition} if a configuration path is given.
 */
public class MappedAttributeDefinitionBuilder extends AbstractAttributeDefinitionBuilder<MappedAttributeDefinitionBuilder, SimpleAttributeDefinition> {

    private List<String> configPath;

    protected MappedAttributeDefinitionBuilder( String attributeName,
                                                ModelType type,
                                                String...pathToField) {
        super(attributeName, type);
        configPath = Collections.unmodifiableList(Arrays.asList(pathToField));
    }

    @Override
    public MappedAttributeDefinitionBuilder setXmlName( String xmlName ) {
        return super.setXmlName(xmlName);
    }

    @Override
    public MappedAttributeDefinitionBuilder setAllowNull( boolean allowNull ) {
        return super.setAllowNull(allowNull);
    }

    @Override
    public MappedAttributeDefinitionBuilder setAllowExpression( boolean allowExpression ) {
        return super.setAllowExpression(allowExpression);
    }

    @Override
    public MappedAttributeDefinitionBuilder setDefaultValue( ModelNode defaultValue ) {
        return super.setDefaultValue(defaultValue);
    }

    @Override
    public MappedAttributeDefinitionBuilder setValidator( ParameterValidator validator ) {
        return super.setValidator(validator);
    }

    @Override
    public MappedAttributeDefinitionBuilder setFlags( Flag... flags ) {
        return super.setFlags(flags);
    }
    
    @Override
    public MappedAttributeDefinitionBuilder setAccessConstraints( AccessConstraintDefinition... constraints ) {
        return super.setAccessConstraints(constraints);
    }

    @Override
    public MappedAttributeDefinitionBuilder setMeasurementUnit( MeasurementUnit unit ) {
        return super.setMeasurementUnit(unit);
    }

    @Override
    public MappedSimpleAttributeDefinition build() {
        return new MappedSimpleAttributeDefinition(this, configPath);
    }
}
