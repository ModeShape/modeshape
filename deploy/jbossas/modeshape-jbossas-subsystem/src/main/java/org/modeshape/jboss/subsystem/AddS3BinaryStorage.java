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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryConfiguration.FieldValue;
import org.modeshape.schematic.document.EditableDocument;

public class AddS3BinaryStorage extends AbstractAddBinaryStorage {

    public static final AddS3BinaryStorage INSTANCE = new AddS3BinaryStorage();

    private AddS3BinaryStorage() {
    }

    @Override
    protected void writeBinaryStorageConfiguration( String repositoryName,
                                                    OperationContext context,
                                                    ModelNode model,
                                                    EditableDocument binaries ) throws OperationFailedException {
        binaries.set(FieldName.TYPE, FieldValue.BINARY_STORAGE_TYPE_S3);
        String username = ModelAttributes.S3_USERNAME.resolveModelAttribute(context, model).asString();
        binaries.setString(FieldName.USER_NAME, username);
        String password = ModelAttributes.S3_PASSWORD.resolveModelAttribute(context, model).asString();
        binaries.setString(FieldName.USER_PASSWORD, password);
        String bucketName = ModelAttributes.S3_BUCKET_NAME.resolveModelAttribute(context, model).asString();
        binaries.setString(FieldName.BUCKET_NAME, bucketName);
        ModelNode node = ModelAttributes.S3_ENDPOINT_URL.resolveModelAttribute(context, model);
        String endPoint = node.isDefined() ? node.asString() : null;  //check if the node exist before getting the value
        binaries.setString(FieldName.ENDPOINT_URL, endPoint);
    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model, ModelAttributes.S3_BINARY_STORAGE_ATTRIBUTES);
    }

}
