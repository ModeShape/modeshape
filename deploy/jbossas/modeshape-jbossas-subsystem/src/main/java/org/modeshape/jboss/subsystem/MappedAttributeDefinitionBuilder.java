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
package org.modeshape.jboss.subsystem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
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
     * @param basis
     */
    public MappedAttributeDefinitionBuilder( SimpleAttributeDefinition basis ) {
        super(basis);
    }

    /**
     * @param attributeName
     * @param type
     */
    public MappedAttributeDefinitionBuilder( String attributeName,
                                             ModelType type ) {
        super(attributeName, type);
    }

    /**
     * @param attributeName
     * @param type
     * @param allowNull
     */
    public MappedAttributeDefinitionBuilder( String attributeName,
                                             ModelType type,
                                             boolean allowNull ) {
        super(attributeName, type, allowNull);
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

    @Override
    public SimpleAttributeDefinition build() {
        SimpleAttributeDefinition simpleDefn = super.build();
        return configPath == null ? simpleDefn : new MappedSimpleAttributeDefinition(simpleDefn, configPath);
    }

}
