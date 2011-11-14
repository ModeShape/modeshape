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

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.DateTime;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.Property;

/**
 * A thread-safe {@link Changes} implementation that records the changes and makes them available for iteration.
 */
@ThreadSafe
public class RecordingChanges implements Changes, ChangeSet {

    private final String processKey;
    private final String repositoryKey;
    private final String workspaceKey;
    private final Queue<Change> events = new ConcurrentLinkedQueue<Change>();
    private Set<NodeKey> nodeKeys;
    private Map<String, String> userData;
    private String userId;
    private DateTime timestamp;

    public RecordingChanges( String processKey,
                             String repositoryKey ) {
        this(processKey, repositoryKey, null);
    }

    public RecordingChanges( String processKey,
                             String repositoryKey,
                             String workspaceKey ) {
        this.processKey = processKey;
        this.repositoryKey = repositoryKey;
        this.workspaceKey = workspaceKey;
        assert this.processKey != null;
        assert this.repositoryKey != null;
    }

    @Override
    public void workspaceAdded( String workspaceName ) {
        events.add(new WorkspaceAdded(workspaceName));
    }

    @Override
    public void workspaceRemoved( String workspaceName ) {
        events.add(new WorkspaceRemoved(workspaceName));
    }

    @Override
    public void nodeCreated( NodeKey key,
                             NodeKey parentKey,
                             Path path,
                             Map<Name, Property> properties ) {
        events.add(new NodeAdded(key, parentKey, path, properties));
    }

    @Override
    public void nodeRemoved( NodeKey key,
                             NodeKey parentKey,
                             Path path ) {
        events.add(new NodeRemoved(key, parentKey, path));
    }

    @Override
    public void nodeRenamed( NodeKey key,
                             Path newPath,
                             Segment oldName ) {
        events.add(new NodeRenamed(key, newPath, oldName));
    }

    @Override
    public void nodeMoved( NodeKey key,
                           NodeKey newParent,
                           NodeKey oldParent,
                           Path newPath,
                           Path oldPath ) {
        events.add(new NodeMoved(key, newParent, oldParent, newPath, oldPath));
    }

    @Override
    public void nodeChanged( NodeKey key,
                             Path path ) {
        events.add(new NodeChanged(key, path));
    }

    @Override
    public void propertyAdded( NodeKey key,
                               Path nodePath,
                               Property property ) {
        events.add(new PropertyAdded(key, nodePath, property));
    }

    @Override
    public void propertyRemoved( NodeKey key,
                                 Path nodePath,
                                 Property property ) {
        events.add(new PropertyRemoved(key, nodePath, property));
    }

    @Override
    public void propertyChanged( NodeKey key,
                                 Path nodePath,
                                 Property newProperty,
                                 Property oldProperty ) {
        events.add(new PropertyChanged(key, nodePath, newProperty, oldProperty));
    }

    @Override
    public int size() {
        return events.size();
    }

    /**
     * Returns an iterator over the elements in this queue in proper sequence. The returned iterator is a "weakly consistent"
     * iterator that will never throw {@link ConcurrentModificationException}, and guarantees to traverse elements as they existed
     * upon construction of the iterator, and may (but is not guaranteed to) reflect any modifications subsequent to construction.
     * 
     * @return an iterator over the elements in this queue in proper sequence; never null
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Change> iterator() {
        return events.iterator();
    }

    @Override
    public Set<NodeKey> changedNodes() {
        return nodeKeys;
    }

    public void setChangedNodes( Set<NodeKey> keys ) {
        this.nodeKeys = keys;
    }

    public void freeze( String userId,
                        Map<String, String> userData,
                        DateTime timestamp ) {
        this.userId = userId;
        this.userData = userData;
        this.timestamp = timestamp;
    }

    @Override
    public String getProcessKey() {
        return processKey;
    }

    @Override
    public String getRepositoryKey() {
        return repositoryKey;
    }

    @Override
    public String getWorkspaceKey() {
        return workspaceKey;
    }

    @Override
    public DateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public Map<String, String> getUserData() {
        return userData;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Save by ")
          .append(getUserId())
          .append(" at ")
          .append(getTimestamp())
          .append(" with user data = ")
          .append(userData)
          .append("\n");
        for (Change change : this) {
            sb.append("  ").append(change).append("\n");
        }
        return sb.toString();
    }
}
