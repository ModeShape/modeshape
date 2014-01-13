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

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * {@link SimpleResourceDefinition} which handles <cache-binary-storage/> elements.
 */
public class ModeShapeCacheBinaryStorageResource extends SimpleResourceDefinition {
    protected final static ModeShapeCacheBinaryStorageResource DEFAULT = new ModeShapeCacheBinaryStorageResource(
            PathElement.pathElement(ModelKeys.STORAGE_TYPE,
                                    ModelKeys.CACHE_BINARY_STORAGE));
    protected final static ModeShapeCacheBinaryStorageResource NESTED = new ModeShapeCacheBinaryStorageResource(
            PathElement.pathElement(ModelKeys.NESTED_STORAGE_TYPE_CACHE,
                                    PathElement.WILDCARD_VALUE));

    private ModeShapeCacheBinaryStorageResource(PathElement pathElement) {
        super(pathElement,
              ModeShapeExtension.getResourceDescriptionResolver(ModelKeys.REPOSITORY, ModelKeys.CACHE_BINARY_STORAGE),
              AddCacheBinaryStorage.INSTANCE, RemoveBinaryStorage.INSTANCE);
    }

    @Override
    public void registerAttributes( ManagementResourceRegistration resourceRegistration ) {
        super.registerAttributes(resourceRegistration);
        BinaryStorageWriteAttributeHandler.CACHE_BINARY_STORAGE_INSTANCE.registerAttributes(resourceRegistration);
    }
}
