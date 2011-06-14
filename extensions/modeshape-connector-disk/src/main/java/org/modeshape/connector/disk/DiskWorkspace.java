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
package org.modeshape.connector.disk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;
import org.modeshape.graph.connector.base.MapWorkspace;
import org.modeshape.graph.connector.base.cache.BaseCachePolicy;
import org.modeshape.graph.connector.base.cache.WorkspaceCache;


/**
 * Workspace implementation for disk connector
 */
public class DiskWorkspace extends MapWorkspace<DiskNode> {

    private File workspaceRoot;
    private WorkspaceCache<UUID, DiskNode> cache;
    private BaseCachePolicy<UUID, DiskNode> policy;

    /**
     * Create a new workspace instance.
     * 
     * @param name the name of the workspace
     * @param workspaceRoot a pointer to the root of the workspace on disk
     * @param rootNode the root node for the workspace
     * @param cachePolicy the caching policy; may not be null
     */
    public DiskWorkspace( String name,
                          File workspaceRoot,
                          DiskNode rootNode,
                          BaseCachePolicy<UUID, DiskNode> cachePolicy ) {
        super(name, rootNode);
        this.workspaceRoot = workspaceRoot;

        this.cache = cachePolicy.newCache();

        File rootNodeFile = fileFor(rootNode.getUuid());
        if (!rootNodeFile.exists()) {
            putNode(rootNode);
        }

    }

    /**
     * Create a new workspace instance.
     * 
     * @param name the name of the workspace
     * @param workspaceRoot a pointer to the root of the workspace on disk
     * @param originalToClone the workspace that is to be cloned
     * @param cachePolicy the caching policy; may not be null
     */
    public DiskWorkspace( String name,
                          File workspaceRoot,
                          DiskWorkspace originalToClone,
                          BaseCachePolicy<UUID, DiskNode> cachePolicy ) {
        super(name, originalToClone);
        this.workspaceRoot = workspaceRoot;

        cache = cachePolicy.newCache();
    }

    public void destroy() {
        this.workspaceRoot.delete();
    }

    /**
     * This method shuts down the workspace and makes it no longer usable. This method should also only be called once.
     */
    public void shutdown() {
        cache.close();
    }

    /**
     * Notifies this workspace that the cache policy has changed and the cache should be reset.
     * 
     * @param newCachePolicy the new cache policy; may not be null
     */
    void cachePolicyChanged( BaseCachePolicy<UUID, DiskNode> newCachePolicy ) {
        this.cache = newCachePolicy.newCache();
    }

    @Override
    public DiskNode getNode( UUID uuid ) {
        DiskNode node = cache.get(uuid);
        if (node != null) return node;

        File nodeFile = fileFor(uuid);
        if (!nodeFile.exists()) return null;

        node = nodeFor(nodeFile);

        if (node != null) cache.put(uuid, node);
        return node;
    }

    @Override
    public DiskNode putNode( DiskNode node ) {
        writeNode(node);
        cache.put(node.getUuid(), node);
        return node;
    }

    @Override
    public void removeAll() {
        this.workspaceRoot.delete();
        this.workspaceRoot.mkdir();

        cache = policy.newCache();
    }

    @Override
    public DiskNode removeNode( UUID uuid ) {
        File nodeFile = fileFor(uuid);
        if (!nodeFile.exists()) return null;

        DiskNode node = nodeFor(nodeFile);

        nodeFile.delete();
        cache.remove(uuid);
        return node;
    }

    private File fileFor( UUID uuid ) {
        String uuidAsString = uuid.toString();

        File firstLevel = new File(workspaceRoot, uuidAsString.substring(0, 2));
        File secondLevel = new File(firstLevel, uuidAsString.substring(2, 4));
        File file = new File(secondLevel, uuidAsString);

        if (!file.exists()) {
            if (!secondLevel.exists()) {
                if (!firstLevel.exists()) firstLevel.mkdir();

                secondLevel.mkdir();
            }
        }

        return file;
    }

    private DiskNode nodeFor( File nodeFile ) {
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(nodeFile));
            DiskNode newNode = (DiskNode)ois.readObject();

            return newNode;
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalStateException(cnfe);

        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        } finally {
            try {
                if (ois != null) ois.close();
            } catch (Exception ignore) {
            }
        }
    }

    private File writeNode( DiskNode node ) {
        ObjectOutputStream oos = null;
        try {
            File nodeFile = fileFor(node.getUuid());
            oos = new ObjectOutputStream(new FileOutputStream(nodeFile));

            oos.writeObject(node);

            return nodeFile;
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        } finally {
            try {
                if (oos != null) oos.close();
            } catch (Exception ignore) {
            }
        }
    }
}
