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

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.Property;

/**
 * A thread-safe {@link Changes} implementation that records the changes and makes them available for iteration.
 */
@ThreadSafe
public class RecordingChanges implements Changes, ChangeSet {

    private static final long serialVersionUID = 1L;

    /**
     * A map used to make sure that Name instances are reused, i.e. the same name instance is used for the same type. The main
     * purpose of this is to reduce the serialized footprint of the change set.
     */
    private static final transient ConcurrentHashMap<Name, Name> NAME_INSTANCES_MAP = new ConcurrentHashMap<Name, Name>();

    private final String processKey;
    private final String repositoryKey;
    private final String workspaceName;
    private final String journalId;
    private final String sessionId;
    private final Queue<Change> events = new ConcurrentLinkedQueue<Change>();
    private final String uuid = UUID.randomUUID().toString();
    private Set<NodeKey> nodeKeys = Collections.emptySet();
    private Map<String, String> userData = Collections.emptyMap();
    private Set<BinaryKey> unusedBinaries = Collections.newSetFromMap(new ConcurrentHashMap<BinaryKey, Boolean>());
    private Set<BinaryKey> usedBinaries = Collections.newSetFromMap(new ConcurrentHashMap<BinaryKey, Boolean>());
    private String userId;
    private DateTime timestamp;

    /**
     * Creates a new change set.
     * @param sessionId  the ID of the session in which the change set was created; may not be null;
     * @param processKey the UUID of the process which created the change set; may not be null
     * @param repositoryKey the key of the repository for which the changes set is created; may not be null.
     * @param workspaceName the name of the workspace in which the changes occurred; may be null.
     * @param journalId the ID of the journal where this change set will be saved; may be null
     */
    public RecordingChanges( String sessionId,
                             String processKey,
                             String repositoryKey,
                             String workspaceName,
                             String journalId ) {
        this.sessionId = sessionId;
        this.processKey = processKey;
        this.repositoryKey = repositoryKey;
        this.workspaceName = workspaceName;
        this.journalId = journalId;

        assert this.sessionId != null;
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
    public void repositoryMetadataChanged() {
        events.add(new RepositoryMetadataChanged());
    }

    @Override
    public void nodeCreated( NodeKey key,
                             NodeKey parentKey,
                             Path path,
                             Name primaryType,
                             Set<Name> mixinTypes,
                             Map<Name, Property> properties ) {
        events.add(new NodeAdded(key, parentKey, path, filterName(primaryType), filterNameSet(mixinTypes), properties));
    }

    @Override
    public void nodeRemoved( NodeKey key,
                             NodeKey parentKey,
                             Path path,
                             Name primaryType,
                             Set<Name> mixinTypes,
                             Name parentPrimaryType,
                             Set<Name> parentMixinTypes ) {
        events.add(new NodeRemoved(key, parentKey, path, filterName(primaryType), filterNameSet(mixinTypes), parentPrimaryType, 
                                   parentMixinTypes));
    }

    @Override
    public void nodeRenamed( NodeKey key,
                             Path newPath,
                             Segment oldName,
                             Name primaryType,
                             Set<Name> mixinTypes ) {
        events.add(new NodeRenamed(key, newPath, oldName, filterName(primaryType), filterNameSet(mixinTypes)));
    }

    @Override
    public void nodeMoved( NodeKey key,
                           Name primaryType,
                           Set<Name> mixinTypes,
                           NodeKey newParent,
                           NodeKey oldParent,
                           Path newPath,
                           Path oldPath ) {
        events.add(new NodeMoved(key, filterName(primaryType), filterNameSet(mixinTypes), newParent, oldParent, newPath, oldPath
        ));
    }

    @Override
    public void nodeReordered( NodeKey key,
                               Name primaryType,
                               Set<Name> mixinTypes,
                               NodeKey parent,
                               Path newPath,
                               Path oldPath,
                               Path reorderedBeforePath ) {
        events.add(new NodeReordered(key, filterName(primaryType), filterNameSet(mixinTypes), parent, newPath, oldPath,
                                     reorderedBeforePath));
    }

    @Override
    public void nodeChanged( NodeKey key,
                             Path path,
                             Name primaryType,
                             Set<Name> mixinTypes ) {
        events.add(new NodeChanged(key, path, filterName(primaryType), filterNameSet(mixinTypes)));
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
        events.add(new NodeSequenced(sequencedNodeKey, sequencedNodePath, filterName(sequencedNodePrimaryType),
                                     filterNameSet(sequencedNodeMixinTypes), outputNodeKey, outputNodePath, outputPath, userId,
                                     selectedPath, sequencerName));
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
        events.add(new NodeSequencingFailure(sequencedNodeKey, sequencedNodePath, filterName(sequencedNodePrimaryType),
                                             filterNameSet(sequencedNodeMixinTypes), outputPath, userId, selectedPath,
                                             sequencerName, cause));
    }

    @Override
    public void propertyAdded( NodeKey key,
                               Name nodePrimaryType,
                               Set<Name> nodeMixinTypes,
                               Path nodePath,
                               Property property ) {
        events.add(new PropertyAdded(key, filterName(nodePrimaryType), filterNameSet(nodeMixinTypes), nodePath, property
        ));
    }

    @Override
    public void propertyRemoved( NodeKey key,
                                 Name nodePrimaryType,
                                 Set<Name> nodeMixinTypes,
                                 Path nodePath,
                                 Property property ) {
        events.add(new PropertyRemoved(key, filterName(nodePrimaryType), filterNameSet(nodeMixinTypes), nodePath, property
        ));
    }

    @Override
    public void propertyChanged( NodeKey key,
                                 Name nodePrimaryType,
                                 Set<Name> nodeMixinTypes,
                                 Path nodePath,
                                 Property newProperty,
                                 Property oldProperty ) {
        events.add(new PropertyChanged(key, filterName(nodePrimaryType), filterNameSet(nodeMixinTypes), nodePath, newProperty,
                                       oldProperty));
    }

    @Override
    public void binaryValueNoLongerUsed( BinaryKey key ) {
        events.add(new BinaryValueUnused(key));
        unusedBinaries.add(key);
    }

    @Override
    public void binaryValueUsed( BinaryKey key ) {
        events.add(new BinaryValueUsed(key));
        usedBinaries.add(key);
    }

    @Override
    public int size() {
        return events.size();
    }

    @Override
    public boolean isEmpty() {
        return events.isEmpty() && nodeKeys.isEmpty(); // not all changed nodes cause events (e.g., shared nodes)
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

    @Override
    public Set<BinaryKey> unusedBinaries() {
        return unusedBinaries;
    }

    @Override
    public Set<BinaryKey> usedBinaries() {
        return usedBinaries;
    }

    @Override
    public boolean hasBinaryChanges() {
        return !usedBinaries.isEmpty() || !unusedBinaries.isEmpty();
    }

    /**
     * Sets the list of node keys involved in this change set.
     * 
     * @param keys a Set<NodeKey>; may not be null
     */
    public void setChangedNodes( Set<NodeKey> keys ) {
        if (keys != null) {
            this.nodeKeys = Collections.unmodifiableSet(new HashSet<NodeKey>(keys));
        }
    }

    /**
     * Marks this change set as frozen (aka. closed). This means it should not accept any more changes.
     * 
     * @param userId the username from the session which originated the changes; may not be null
     * @param userData a Map which can contains arbitrary information; may be null
     * @param timestamp a {@link DateTime} at which the changes set was created.
     */
    public void freeze( String userId,
                        Map<String, String> userData,
                        DateTime timestamp ) {
        this.userId = userId;
        if (userData != null) {
            this.userData = Collections.unmodifiableMap(userData);
        }
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
    public String getWorkspaceName() {
        return workspaceName;
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
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String getJournalId() {
        return journalId;
    }

    @Override
    public String getUUID() {
        return uuid;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Save by '").append(getUserId()).append("' at ").append(getTimestamp()).append(" with user data = ")
          .append(userData).append(" in repository with key '").append(repositoryKey).append("' and workspace '")
          .append(workspaceName);

        if (journalId != null) {
            sb.append(". Journal id=").append(journalId);
        }
        sb.append("'\n");

        for (Change change : this) {
            sb.append("  ").append(change).append("\n");
        }
        sb.append("changed ").append(nodeKeys.size()).append(" nodes:\n");
        for (NodeKey key : nodeKeys) {
            sb.append("  ").append(key).append("\n");
        }

        return sb.toString();
    }

    private Name filterName( Name input ) {
        if (input == null) {
            return null;
        }
        Name name = NAME_INSTANCES_MAP.putIfAbsent(input, input);
        return name != null ? name : input;
    }

    private Set<Name> filterNameSet( Set<Name> input ) {
        Set<Name> result = new HashSet<Name>(input.size());
        for (Name name : input) {
            result.add(filterName(name));
        }
        return result;
    }
}
