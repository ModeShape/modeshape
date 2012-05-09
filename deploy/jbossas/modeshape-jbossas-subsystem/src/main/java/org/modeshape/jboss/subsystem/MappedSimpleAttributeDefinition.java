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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
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
     * @param defn the simple attribute definition
     * @param pathToFieldInConfiguration the path to the field within the {@link RepositoryConfiguration} document
     */
    protected MappedSimpleAttributeDefinition( SimpleAttributeDefinition defn,
                                               List<String> pathToFieldInConfiguration ) {
        super(defn.getName(), defn.getXmlName(), defn.getDefaultValue(), defn.getType(), defn.isAllowNull(),
              defn.isAllowExpression(), defn.getMeasurementUnit(), defn.getValidator(), defn.getAlternatives(),
              defn.getRequires(), defn.getFlags().toArray(new AttributeAccess.Flag[defn.getFlags().size()]));
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
