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
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;

/**
 * An extension to ListAttributeDefinition that contains a mapped field.
 */
public class MappedListAttributeDefinition extends SimpleListAttributeDefinition implements MappedAttributeDefinition {

    private final List<String> pathToFieldInConfiguration;
    private final List<String> pathToContainerOfFieldInConfiguration;

    protected MappedListAttributeDefinition( SimpleListAttributeDefinition.Builder builder,
                                             SimpleAttributeDefinition valueType,
                                             List<String> pathToFieldInConfiguration ) {
        super(builder, valueType);
        assert pathToFieldInConfiguration != null;
        assert pathToFieldInConfiguration.size() > 0;
        this.pathToFieldInConfiguration = pathToFieldInConfiguration;
        this.pathToContainerOfFieldInConfiguration = this.pathToFieldInConfiguration.size() > 1 ? 
                                                     this.pathToFieldInConfiguration.subList(0, this.pathToFieldInConfiguration.size() - 1) 
                                                                                                : 
                                                     Collections.<String>emptyList();
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

    @Override
    public ModelNode getDefaultValue() {
        ModelNode listDefault = super.getDefaultValue();
        if (listDefault != null) {
            return listDefault;
        }
        //attempt to resolve the default against the value type (not that this is a bit of hack because SimpleListAttributeDefinition
        //simply do not support default values (because of its builder)
        ModelNode valueTypeDefault = getValueType().getDefaultValue();
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

    public static class Builder {
        private final SimpleAttributeDefinition valueType;
        private final SimpleListAttributeDefinition.Builder builder;
        private List<String> configPath;

        protected Builder( final String name,
                           final SimpleAttributeDefinition valueType ) {
            this.valueType = valueType;
            this.builder = new SimpleListAttributeDefinition.Builder(name, valueType);
        }

        protected static Builder of( final String name,
                                     final SimpleAttributeDefinition valueType ) {
            return new Builder(name, valueType);
        }

        protected ListAttributeDefinition build() {
            if (configPath == null) {
                return builder.build();
            } else {
                return new MappedListAttributeDefinition(builder, valueType, configPath);
            }
        }

        protected Builder setAllowNull( final boolean allowNull ) {
            builder.setAllowNull(allowNull);
            return this;
        }

        protected Builder setFlags( final AttributeAccess.Flag... flags ) {
            builder.setFlags(flags);
            return this;
        }

        protected Builder setMaxSize( final int maxSize ) {
            builder.setMaxSize(maxSize);
            return this;
        }

        protected Builder setMinSize( final int minSize ) {
            builder.setMinSize(minSize);
            return this;
        }

        protected Builder setAllowExpression( final boolean allowExpression ) {
            builder.setAllowExpression(allowExpression);
            return this;
        }

        protected Builder setFieldPathInRepositoryConfiguration( String... pathToField ) {
            configPath = Collections.unmodifiableList(Arrays.asList(pathToField));
            return this;
        }

        protected Builder setAccessConstraints(AccessConstraintDefinition...constraints) {
            builder.setAccessConstraints(constraints);
            return this;
        }
    }
}
