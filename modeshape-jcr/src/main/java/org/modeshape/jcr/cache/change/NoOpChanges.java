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
package org.modeshape.jcr.cache.change;

import java.util.Map;
import java.util.Set;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.Property;

/**
 * An immutable {@link Changes} implementation that does nothing.
 */
@Immutable
public final class NoOpChanges implements Changes {

    public static final NoOpChanges INSTANCE = new NoOpChanges();

    private NoOpChanges() {
    }

    @Override
    public void workspaceAdded( String workspaceName ) {
    }

    @Override
    public void workspaceRemoved( String workspaceName ) {
    }

    @Override
    public void repositoryMetadataChanged() {
    }

    @Override
    public void nodeCreated( NodeKey key,
                             NodeKey parentKey,
                             Path path,
                             Name primaryType,
                             Set<Name> mixinTypes,
                             Map<Name, Property> properties ) {
    }

    @Override
    public void nodeRemoved( NodeKey key,
                             NodeKey parentKey,
                             Path path,
                             Name primaryType,
                             Set<Name> mixinTypes,
                             Name parentPrimaryType,
                             Set<Name> parentMixinTypes ) {
    }

    @Override
    public void nodeRenamed( NodeKey key,
                             Path newPath,
                             Segment oldName,
                             Name primaryType,
                             Set<Name> mixinTypes ) {
    }

    @Override
    public void nodeMoved( NodeKey key,
                           Name primaryType,
                           Set<Name> mixinTypes,
                           NodeKey newParent,
                           NodeKey oldParent,
                           Path newPath,
                           Path oldPath ) {
    }

    @Override
    public void nodeReordered( NodeKey key,
                               Name primaryType,
                               Set<Name> mixinTypes,
                               NodeKey parent,
                               Path newPath,
                               Path oldPath,
                               Path reorderedBeforePath ) {
    }

    @Override
    public void nodeChanged( NodeKey key,
                             Path path,
                             Name primaryType,
                             Set<Name> mixinTypes ) {
    }

    @Override
    public void nodeSequenced( NodeKey sequencedNodeKey,
                               Path sequencedNodePath,
                               Name sequencedNodePrimaryType,
                               Set<Name> sequencedNodeMixinTypes,
                               NodeKey outputNodeKey,
                               Path outputNodePath,
                               String outputPath,
                               String userId,
                               String selectedPath,
                               String sequencerName ) {
    }

    @Override
    public void nodeSequencingFailure( NodeKey sequencedNodeKey,
                                       Path sequencedNodePath,
                                       Name sequencedNodePrimaryType,
                                       Set<Name> sequencedNodeMixinTypes,
                                       String outputPath,
                                       String userId,
                                       String selectedPath,
                                       String sequencerName,
                                       Throwable cause ) {
    }

    @Override
    public void propertyAdded( NodeKey key,
                               Name nodePrimaryType,
                               Set<Name> nodeMixinTypes,
                               Path nodePath,
                               Property property ) {
    }

    @Override
    public void propertyRemoved( NodeKey key,
                                 Name nodePrimaryType,
                                 Set<Name> nodeMixinTypes,
                                 Path nodePath,
                                 Property property ) {
    }

    @Override
    public void propertyChanged( NodeKey key,
                                 Name nodePrimaryType,
                                 Set<Name> nodeMixinTypes,
                                 Path nodePath,
                                 Property newProperty,
                                 Property oldProperty ) {
    }

    @Override
    public void binaryValueNoLongerUsed( BinaryKey key ) {
    }

    @Override
    public void binaryValueUsed( BinaryKey key ) {
    }
}
