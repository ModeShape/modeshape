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
package org.modeshape.jcr.cache;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import org.infinispan.schematic.SchematicDb;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.JcrRepository;

/**
 * An immutable unique key for a node within the {@link JcrRepository repository}'s {@link SchematicDb database}.
 * <p>
 * A node key consists of three parts:
 * <ol>
 * <li>A multi-character key uniquely identifying the repository's storage source in which this node appears;</li>
 * <li>A multi-character key uniquely identifying the workspace in which the node appears; and</li>
 * <li>A multi-character key representing the JCR identifier of a node, which is usually a UUID.</li>
 * </ol>
 * </p>
 * <p>
 * This class contains methods for
 */
@Immutable
public final class NodeKey implements Serializable, Comparable<NodeKey> {

    private static final int UUID_LENGTH = UUID.randomUUID().toString().length();
    private static final long serialVersionUID = 1L;

    protected static final int SOURCE_LENGTH = 7;
    protected static final int WORKSPACE_LENGTH = 7;
    private static final int SOURCE_START_INDEX = 0;
    private static final int SOURCE_END_INDEX = SOURCE_START_INDEX + SOURCE_LENGTH;
    private static final int WORKSPACE_START_INDEX = SOURCE_LENGTH;
    private static final int WORKSPACE_END_INDEX = WORKSPACE_START_INDEX + WORKSPACE_LENGTH;
    private static final int IDENTIFIER_START_INDEX = WORKSPACE_END_INDEX;

    /**
     * Determine if the supplied string may be a valid identifier. This method returns 'false' only if the identifier is known to
     * be invalid (e.g., it is not of the correct format). This method may return true even if the identifier itself does not
     * reference an existing node.
     * 
     * @param key
     * @return true if the string is of the correct format for a node key, or false if it not the correct format
     */
    public static boolean isValidFormat( String key ) {
        if (StringUtil.isBlank(key) || key.length() <= IDENTIFIER_START_INDEX) return false;
        return true;
    }

    /**
     * Determine if the supplied string is known to be a valid {@link #withRandomId() random} node key identifier.
     * 
     * @param identifier the identifier
     * @return true if the string is of the correct format for a node key, or false if it not the correct format
     */
    public static boolean isValidRandomIdentifier( String identifier ) {
        if (identifier.length() == UUID_LENGTH) {
            // It's the right length, but see if it's a UUID ...
            try {
                UUID.fromString(identifier);
                return true;
            } catch (IllegalArgumentException e) {
                // Nope
                return false;
            }
        }
        return false;
    }

    private final String key;
    private transient String sourceKey;
    private transient String workspaceKey;
    private transient String identifier;

    /**
     * Reconstitute a node key from the supplied string.
     * 
     * @param key the string representation of the key; may not be null
     */
    public NodeKey( String key ) {
        assert key != null;
        assert key.length() > IDENTIFIER_START_INDEX;
        this.key = key;
    }

    /**
     * Reconstitute a node key from the supplied source key, workspace key, and node identifier.
     * 
     * @param sourceKey the source key; may not be null and must be 4 characters
     * @param workspaceKey the workspace key; may not be null and must be 4 characters
     * @param identifier the node identifier; may not be null and must be at least 1 character
     */
    public NodeKey( String sourceKey,
                    String workspaceKey,
                    String identifier ) {
        assert sourceKey != null;
        assert workspaceKey != null;
        assert identifier != null;
        assert sourceKey.length() == SOURCE_LENGTH;
        assert workspaceKey.length() == WORKSPACE_LENGTH;
        assert workspaceKey.length() > 0;
        this.key = sourceKey + workspaceKey + identifier;
    }

    /**
     * Get the multi-character key uniquely identifying the repository's storage source in which this node appears.
     * 
     * @return the source key; never null and always contains at least one character
     */
    public String getSourceKey() {
        if (sourceKey == null) {
            // Value is idempotent, so it's okay to do this without synchronizing ...
            sourceKey = key.substring(SOURCE_START_INDEX, SOURCE_END_INDEX);
        }
        return sourceKey;
    }

    /**
     * Get the multi-character key uniquely identifying the workspace in which the node appears.
     * 
     * @return the workspace key; never null and always contains at least one character
     */
    public String getWorkspaceKey() {
        if (workspaceKey == null) {
            // Value is idempotent, so it's okay to do this without synchronizing ...
            workspaceKey = key.substring(WORKSPACE_START_INDEX, WORKSPACE_END_INDEX);
        }
        return workspaceKey;
    }

    /**
     * Get the multi-character key representing the JCR identifier of a node, which is usually a UUID.
     * 
     * @return the JCR identifier for the node; never null and always contains at least one character
     */
    public String getIdentifier() {
        if (identifier == null) {
            // Value is idempotent, so it's okay to do this without synchronizing ...
            identifier = key.substring(IDENTIFIER_START_INDEX);
        }
        return identifier;
    }

    /**
     * Get the SHA-1 hash of the {@link #getIdentifier() identifier}.
     * 
     * @return the hexadecimal representation of the identifier's SHA-1 hash; never null
     */
    public String getIdentifierHash() {
        return sha1(getIdentifier());
    }

    @Override
    public int compareTo( NodeKey that ) {
        if (that == this) return 0;
        return this.key.compareTo(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof NodeKey) {
            NodeKey that = (NodeKey)obj;
            return this.key.equals(that.key);
        }
        return false;
    }

    @Override
    public String toString() {
        return key;
    }

    public NodeKey withRandomId() {
        return new NodeKey(getSourceKey(), getWorkspaceKey(), UUID.randomUUID().toString());
    }

    public NodeKey withRandomIdAndWorkspace( String workspaceKey ) {
        return new NodeKey(getSourceKey(), workspaceKey, UUID.randomUUID().toString());
    }

    public NodeKey withId( String identifier ) {
        return new NodeKey(getSourceKey(), getWorkspaceKey(), identifier);
    }

    public NodeKey withWorkspaceKey( String workspaceKey ) {
        return new NodeKey(getSourceKey(), workspaceKey, getIdentifier());
    }

    public NodeKey withWorkspaceKeyAndId( String workspaceKey,
                                          String identifier ) {
        return new NodeKey(getSourceKey(), workspaceKey, identifier);
    }

    public NodeKey withSourceKeyAndId( String sourceKey,
                                       String identifier ) {
        return new NodeKey(sourceKey, getWorkspaceKey(), identifier);
    }

    public static String keyForSourceName( String name ) {
        return sha1(name).substring(0, NodeKey.SOURCE_LENGTH);
    }

    public static String keyForWorkspaceName( String name ) {
        return sha1(name).substring(0, NodeKey.WORKSPACE_LENGTH);
    }

    public static String sourceKey( String key ) {
        return isValidFormat(key) ? key.substring(SOURCE_START_INDEX, SOURCE_END_INDEX) : null;
    }

    private static String sha1( String name ) {
        try {
            byte[] sha1 = SecureHash.getHash(SecureHash.Algorithm.SHA_1, name.getBytes());
            return SecureHash.asHexString(sha1);
        } catch (NoSuchAlgorithmException e) {
            throw new SystemFailureException(e);
        }
    }

}
