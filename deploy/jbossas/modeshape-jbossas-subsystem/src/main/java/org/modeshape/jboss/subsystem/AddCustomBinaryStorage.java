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
import java.util.List;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryConfiguration.FieldValue;
import org.modeshape.schematic.document.EditableDocument;

/**
 *  Hook called when the AS subsystem encounters a {@code <custom-binary-storage/>} element
 */
public class AddCustomBinaryStorage extends AbstractAddBinaryStorage {

    public static final AddCustomBinaryStorage INSTANCE = new AddCustomBinaryStorage();
    private static final List<String> COMMON_BINARY_ATTRIBUTES = Arrays.asList(ModelKeys.MIME_TYPE_DETECTION, 
                                                                               ModelKeys.MINIMUM_BINARY_SIZE,
                                                                               ModelKeys.MINIMUM_STRING_SIZE,
                                                                               ModelKeys.STORE_NAME);

    private AddCustomBinaryStorage() {
    }

    @Override
    protected void writeBinaryStorageConfiguration( String repositoryName,
                                                    OperationContext context,
                                                    ModelNode model,
                                                    EditableDocument binaries ) {
        binaries.set(FieldName.TYPE, FieldValue.BINARY_STORAGE_TYPE_CUSTOM);
        model.asPropertyList().forEach(property -> writeCustomProperty(binaries, property));
    }
    
    private void writeCustomProperty(EditableDocument binaries, Property property) {
        ModelNode value = property.getValue();
        if (!value.isDefined()) {
            return;
        }
        String name = property.getName();
        if (COMMON_BINARY_ATTRIBUTES.contains(name)) {
            // these are set via the base class...
            return;
        }
        if (name.equals(ModelKeys.CLASSNAME)) {
            name = FieldName.CLASSNAME;
        } else if (name.equals(ModelKeys.MODULE)) {
            name = FieldName.CLASSLOADER;
        }
        binaries.set(name, value.asString());
        logger.debugv("added custom binary property [{0} = {1}]", name, value.asString());
    }
    
    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model, ModelAttributes.CUSTOM_BINARY_STORAGE_ATTRIBUTES);
    }
}
