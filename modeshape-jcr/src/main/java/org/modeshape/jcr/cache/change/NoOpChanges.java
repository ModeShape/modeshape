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
package org.modeshape.jcr.cache.change;

import java.util.Map;
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
    public void nodeCreated( NodeKey key,
                             NodeKey parentKey,
                             Path path,
                             Map<Name, Property> properties ) {
    }

    @Override
    public void nodeMoved( NodeKey key,
                           NodeKey newParent,
                           NodeKey oldParent,
                           Path newPath,
                           Path oldPath ) {
    }

    @Override
    public void nodeChanged( NodeKey key,
                             Path path ) {
    }

    @Override
    public void nodeRemoved( NodeKey key,
                             NodeKey parentKey,
                             Path path ) {
    }

    @Override
    public void nodeRenamed( NodeKey key,
                             Path path,
                             Segment oldName ) {
    }

    @Override
    public void propertyAdded( NodeKey key,
                               Path nodePath,
                               Property property ) {
    }

    @Override
    public void propertyRemoved( NodeKey key,
                                 Path nodePath,
                                 Property property ) {
    }

    @Override
    public void propertyChanged( NodeKey key,
                                 Path nodePath,
                                 Property newProperty,
                                 Property oldProperty ) {
    }

    @Override
    public void binaryValueNoLongerUsed( BinaryKey key ) {
    }

    @Override
    public void binaryValueNowUsed( BinaryKey key ) {
    }

    @Override
    public void nodeReordered( NodeKey key,
                               NodeKey parent,
                               Path newPath,
                               Path oldPath,
                               Path reorderedBeforePath ) {
    }

    @Override
    public void nodeSequenced( NodeKey sequencedNodeKey,
                               Path sequencedNodePath,
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
                                       String outputPath,
                                       String userId,
                                       String selectedPath,
                                       String sequencerName,
                                       Throwable cause ) {
    }
}
