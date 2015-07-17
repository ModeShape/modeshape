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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * 
 */
@Immutable
public class MappedSimpleAttributeDefinition extends SimpleAttributeDefinition implements MappedAttributeDefinition {

    private final List<String> pathToFieldInConfiguration;
    private final List<String> pathToContainerOfFieldInConfiguration;

    /**
     * @param builder the simple attribute definition builder
     * @param pathToFieldInConfiguration the path to the field within the {@link RepositoryConfiguration} document
     */
    @SuppressWarnings( "deprecation" )
    protected MappedSimpleAttributeDefinition( MappedAttributeDefinitionBuilder builder,
                                               List<String> pathToFieldInConfiguration ) {
        super(builder);
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
        return getTypedValue(node, this);
    }

    protected static Object getTypedValue( ModelNode node,
                                           AttributeDefinition defn ) throws OperationFailedException {
        ModelNode defaultValue = defn.getDefaultValue() != null && defn.getDefaultValue().isDefined() ? defn.getDefaultValue() : null;
        switch (defn.getType()) {
            case OBJECT:
            case STRING:
                return node.asString();
            case BIG_DECIMAL:
                return node.asBigDecimal();
            case BIG_INTEGER:
                return node.asBigInteger();
            case BOOLEAN:
                return node.asBoolean();
            case BYTES:
                return node.asBytes();
            case DOUBLE:
                return defaultValue != null ? node.asDouble(defaultValue.asDouble()) : node.asDouble();
            case EXPRESSION:
                return node.resolve().asString();
            case INT:
                return defaultValue != null ? node.asInt(defaultValue.asInt()) : node.asInt();
            case LIST:
                List<ModelNode> modelValues = node.asList();
                List<String> values = new ArrayList<String>(modelValues.size());
                for (ModelNode modelValue : modelValues) {
                    values.add(modelValue.asString());
                }
                return values;
            case LONG:
                return defaultValue != null ? node.asLong(defaultValue.asLong()) : node.asLong();
            case TYPE:
            case UNDEFINED:
            case PROPERTY:
                throw new OperationFailedException("Unexpected type " + defn.getType() + " for " + defn);

        }
        return null;
    }
}
