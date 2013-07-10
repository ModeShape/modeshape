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
package org.modeshape.jcr.cache.document;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.infinispan.Cache;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.jcr.cache.NodeKey;

/**
 * A component that can optimize the document for a node.
 */
public class DocumentOptimizer implements DocumentConstants {

    private final Cache<String, SchematicEntry> store;
    private final DocumentStore documentStore;

    public DocumentOptimizer( DocumentStore documentStore ) {
        this.documentStore = documentStore;
        this.store = null;
        assert this.store != null || this.documentStore != null;
    }

    public DocumentOptimizer( Cache<String, SchematicEntry> store ) {
        this.documentStore = null;
        this.store = store;
        assert this.store != null || this.documentStore != null;
    }

    /**
     * Optimize the children in the supplied node document
     * <p>
     * Note that this method changes the underlying db as well as the given document, so *it must* be called either from a
     * transactional context or it must be followed by a session.save call, otherwise there might be inconsistencies between what
     * a session sees as "persisted" state and the reality.
     * </p>
     * 
     * @param key the key for the node
     * @param document the node's document representation that is to be optimized
     * @param targetCountPerBlock the target number of children per block
     * @param tolerance the allowed tolerance between the target and actual number of children per block
     * @return true if the document was changed, or false otherwise
     */
    public boolean optimizeChildrenBlocks( NodeKey key,
                                           EditableDocument document,
                                           int targetCountPerBlock,
                                           int tolerance ) {
        if (document == null) {
            SchematicEntry entry = lookup(key.toString());
            if (entry == null) {
                return false;
            }
            document = entry.editDocumentContent();
            if (document == null) {
                return false;
            }
        }
        EditableArray children = document.getArray(CHILDREN);
        if (children == null) {
            // There are no children to optimize
            return false;
        }

        // Get the children info
        EditableDocument info = document.getDocument(CHILDREN_INFO);
        boolean selfContained = true;
        if (info != null) {
            selfContained = !info.containsField(NEXT_BLOCK);
        }

        boolean changed = false;
        if (selfContained) {
            // This is a self-contained block; we only need to do something if the child count is larger than target +/- tolerance
            int total = children.size();
            if (total < targetCountPerBlock + tolerance) {
                // The number of children is small enough ...
                return false;
            }
            // Otherwise, there are more children than our target + tolerance, so we need to split the children ...
            splitChildren(key, document, children, targetCountPerBlock, tolerance, true, null);
            changed = true;
        } else {
            assert info != null;
            // This is not self-contained; there are already at least two blocks.
            // Go through each block, and either split it, merge it with the previous block, or leave it.
            EditableDocument doc = document;
            NodeKey docKey = key;
            while (doc != null) {
                EditableDocument docInfo = doc.getDocument(CHILDREN_INFO);
                String nextKey = docInfo != null ? docInfo.getString(NEXT_BLOCK) : null;
                children = doc.getArray(CHILDREN);
                int count = children.size();
                boolean isFirst = doc == document;
                if (count > (targetCountPerBlock + tolerance)) {
                    // This block is too big, so we should split it into multiple blocks...
                    splitChildren(docKey, doc, children, targetCountPerBlock, tolerance, isFirst, nextKey);
                    changed = true;
                } else if (count < (targetCountPerBlock - tolerance) && nextKey != null) {
                    // This block is too small, so always combine it with the next block, if there is one
                    // (even if that makes the next block too big, since it will be split in a later pass).
                    // Note that since we're only splitting if there is a next block, a last block that
                    // is too small will be left untouched. At this time, we think this is okay.
                    nextKey = mergeChildren(docKey, doc, children, isFirst, nextKey);
                    changed = true;

                    if (nextKey == null) {
                        // We merged the last block into this document, so we need to change the pointer in 'document'
                        // to be this doc ...
                        info.setString(LAST_BLOCK, docKey.toString());
                    }
                }
                // Otherwise, this block is just right

                // Find the next block ...
                if (nextKey != null) {
                    SchematicEntry nextEntry = lookup(nextKey);
                    doc = nextEntry.editDocumentContent();
                    docKey = new NodeKey(nextKey);
                } else {
                    doc = null;
                }
            }
        }
        return changed;
    }

    protected SchematicEntry lookup( String key ) {
        return documentStore != null ? documentStore.get(key) : store.get(key);
    }

    /**
     * Split the children in the given document (with the given key) into two or more blocks, based upon the specified number of
     * desired children per block and a tolerance. This method will create additional blocks and will modify the supplied document
     * (with the smaller number of children and the pointer to the next block).
     * <p>
     * Note this method returns very quickly if the method determines that there is no work to do.
     * </p>
     * <p>
     * Note that this method changes the underlying db as well as the given document, so *it must* be called either from a
     * transactional context or it must be followed by a session.save call, otherwise there might be inconsistencies between what
     * a session sees as "persisted" state and the reality.
     * </p>
     * 
     * @param key the key for the document whose children are to be split; may not be null
     * @param document the document whose children are to be split; may not be null
     * @param children the children that are to be split; may not be null
     * @param targetCountPerBlock the goal for the number of children in each block; must be positive
     * @param tolerance a tolerance that when added to and subtraced from the <code>targetCountPerBlock</code> gives an acceptable
     *        range for the number of children; must be positive but smaller than <code>targetCountPerBlock</code>
     * @param isFirst true if the supplied document is the first node document, or false if it is a block document
     * @param nextBlock the key for the next block of children; may be null if the supplied document is the last document and
     *        there is no next block
     * @return true if the children were split, or false if no changes were made
     */
    protected boolean splitChildren( NodeKey key,
                                     EditableDocument document,
                                     EditableArray children,
                                     int targetCountPerBlock,
                                     int tolerance,
                                     boolean isFirst,
                                     String nextBlock ) {
        assert 0 < targetCountPerBlock;
        assert 0 < tolerance;
        assert tolerance < targetCountPerBlock;
        // Calculate the number of blocks that we'll create and the size of the last block ...
        int total = children.size();
        int numFullBlocks = total / targetCountPerBlock;

        if (numFullBlocks == 0) {
            // This block doesn't need to be split ...
            return false;
        }

        int sizeOfLastBlock = total % targetCountPerBlock;
        if (sizeOfLastBlock < (targetCountPerBlock - tolerance)) {
            // The last block would be too small to be on its own ...
            if (numFullBlocks == 1) {
                // We would split into one full block and a second too-small block, so there's no point of splitting ...
                return false;
            }
            // We'll split it into multiple blocks, so we'll just include the children in the last too-small block
            // in the previous block ...
            sizeOfLastBlock = 0;
        }

        // The order we do things is important here. The best thing is to create and persist blocks 2...n immediately,
        // and then we can change the first document to have the smaller number of children and to point to the newly-created
        // block 2 (which points to block 3, etc.). This order means that anybody reading the input document never reads an
        // inconsistent set of children.
        int startIndex = targetCountPerBlock;
        int endIndex = 0;
        final String firstNewBlockKey = key.withRandomId().toString();
        String blockKey = firstNewBlockKey;
        for (int n = 1; n != numFullBlocks; ++n) {
            // Create the sublist of children that should be written to a new block ...
            boolean isLast = n == (numFullBlocks - 1);
            endIndex = isLast ? total : (startIndex + targetCountPerBlock);
            EditableArray blockChildren = Schematic.newArray(children.subList(startIndex, endIndex));

            // Create the new block, with a key that contains a UUID for the identifier ...
            String nextBlockKey = (isLast) ? nextBlockKey = nextBlock : key.withRandomId().toString();
            EditableDocument blockDoc = Schematic.newDocument();
            EditableDocument childInfo = blockDoc.setDocument(CHILDREN_INFO);
            childInfo.setNumber(BLOCK_SIZE, blockChildren.size());
            if (nextBlockKey != null) {
                childInfo.setString(NEXT_BLOCK, nextBlockKey);
            }

            // Write the children ...
            blockDoc.setArray(CHILDREN, blockChildren);

            // Now persist the new document ...
            documentStore.localStore().put(blockKey, blockDoc);

            // And get ready for the next block ...
            if (!isLast) {
                blockKey = nextBlockKey;
                startIndex = endIndex;
            }
        }

        // Now we can update the input document's children and nextBlock reference ...
        EditableArray newChildren = Schematic.newArray(children.subList(0, targetCountPerBlock));
        document.setArray(CHILDREN, newChildren);
        EditableDocument childInfo = document.getDocument(CHILDREN_INFO);
        if (childInfo == null) {
            childInfo = document.setDocument(CHILDREN_INFO);
        }
        childInfo.setNumber(BLOCK_SIZE, newChildren.size());
        childInfo.setString(NEXT_BLOCK, firstNewBlockKey);

        if (isFirst && nextBlock == null) {
            // We generated a new last block and we have to update the reference ...
            childInfo.setString(LAST_BLOCK, blockKey);
        }

        // Note we never changed the number of children, so we don't need to update 'count'.
        return true;
    }

    /**
     * Modify the supplied document (with the given key) to merge in all of the children from the next block. If the next block is
     * empty or contains no children, it will be deleted its next block merged. Note that this merging is performed, even if the
     * resulting number of children is considered 'too-large' (as such 'too-large' blocks will be optimized at a subsequent
     * optimization pass).
     * <p>
     * Note that this method changes the underlying db as well as the given document, so *it must* be called either from a
     * transactional context or it must be followed by a session.save call, otherwise there might be inconsistencies between what
     * a session sees as "persisted" state and the reality.
     * </p>
     * 
     * @param key the key for the document whose children are to be merged with the next block; may not be null
     * @param document the document to be modified with the next block's children; may not be null
     * @param children the children into which are to be merged the next block's children; may not be null
     * @param isFirst true if the supplied document is the first node document, or false if it is a block document
     * @param nextBlock the key for the next block of children; may be null if the supplied document is the last document and
     *        there is no next block
     * @return the key for the block of children that is after blocks that are removed; may be null if the supplied document is
     *         the last block
     */
    protected String mergeChildren( NodeKey key,
                                    EditableDocument document,
                                    EditableArray children,
                                    boolean isFirst,
                                    String nextBlock ) {
        // The children in the next block should be added to the children in this block, even if the size would be too large
        // as any too-large blocks will eventually be optimized later ...
        EditableDocument info = document.getDocument(CHILDREN_INFO);
        if (info == null) {
            info = document.setDocument(CHILDREN_INFO);
        }

        // First, find the next block that we can use ...
        Set<String> toBeDeleted = new HashSet<String>();
        SchematicEntry nextEntry = null;
        String nextBlocksNext = null;
        while (nextBlock != null) {
            nextEntry = documentStore.get(nextBlock);
            Document nextDoc = nextEntry.getContentAsDocument();
            List<?> nextChildren = nextDoc.getArray(CHILDREN);
            Document nextInfo = nextDoc.getDocument(CHILDREN_INFO);

            if (nextChildren == null || nextChildren.isEmpty()) {
                // Delete this empty block ...
                toBeDeleted.add(nextBlock);
                nextEntry = null;

                // And figure out the next block ...
                nextBlock = nextInfo != null ? nextInfo.getString(NEXT_BLOCK) : null;
            } else {
                // We can use this block, so copy the children into it ...
                children.addAll(nextChildren);

                // Figure out the key for the next block ...
                nextBlocksNext = nextInfo != null ? nextInfo.getString(NEXT_BLOCK) : null;

                if (isFirst && nextBlocksNext == null) {
                    // This is the first block and there is no more, so set the count and remove the block-related fields ...
                    info.setNumber(COUNT, children.size());
                    info.remove(NEXT_BLOCK);
                    info.remove(LAST_BLOCK);
                } else {
                    // Just update the block size and the next block ...
                    info.setNumber(BLOCK_SIZE, children.size());
                    info.setString(NEXT_BLOCK, nextBlocksNext);
                }

                // And then mark it for deletion ...
                toBeDeleted.add(nextBlock);
                nextBlock = null;
            }
        }

        // Now that we've updated the input document, delete any entries that are no longer needed ...
        for (String deleteKey : toBeDeleted) {
            documentStore.remove(deleteKey);
        }

        return nextBlocksNext;
    }

}
