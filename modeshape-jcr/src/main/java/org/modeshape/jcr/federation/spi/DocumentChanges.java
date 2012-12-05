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

package org.modeshape.jcr.federation.spi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.value.Name;

/**
 * Interface which encapsulates the changes that occurred on a document during an update process.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface DocumentChanges {

    /**
     * Returns the document which contains all the changes.
     * 
     * @return the updated document; never null
     */
    Document getDocument();

    /**
     * Returns the id of the document to which the changes apply.
     * 
     * @return the identifier of the modified document; never null
     */
    String getDocumentId();

    /**
     * Returns the changes to the children.
     * 
     * @return a {@link org.modeshape.jcr.federation.spi.DocumentChanges.ChildrenChanges} instance; never null
     */
    ChildrenChanges getChildrenChanges();

    /**
     * Returns the changes to the mixins.
     * 
     * @return a {@link MixinChanges} instance; never null
     */
    MixinChanges getMixinChanges();

    /**
     * Returns the changes to the parents.
     * 
     * @return a {@link ParentChanges} instance; never null
     */
    ParentChanges getParentChanges();

    /**
     * Returns the changes to the properties.
     * 
     * @return a {@link PropertyChanges} instance; never null
     */
    PropertyChanges getPropertyChanges();

    /**
     * Returns the changes to the referrers.
     * 
     * @return a {@link ReferrerChanges} instance; never null
     */
    ReferrerChanges getReferrerChanges();

    /**
     * Interface which encapsulates the changes to a document's properties during an update operation.
     */
    public interface PropertyChanges {
        /**
         * Checks if there are any changes to the properties (removed/added/changed)
         * 
         * @return {@code true} if there aren't any changes, {@code false} otherwise
         */
        boolean isEmpty();

        /**
         * Returns the set of name of the changed properties (added + modified)
         * 
         * @return the set of names of the properties that were changed on this node; never null
         */
        Set<Name> getChanged();

        /**
         * Returns the set of names of the removed properties
         * 
         * @return the set of names of the properties that were removed from this node; never null
         */
        Set<Name> getRemoved();

        /**
         * Returns the set of names of the added properties
         * 
         * @return the set of names of the properties that were added to this node; never null
         */
        Set<Name> getAdded();
    }

    /**
     * Interface which encapsulates the changes to a document's mixins during an update operation.
     */
    public interface MixinChanges {
        /**
         * Checks if there are any changes to the mixins (removed/added)
         * 
         * @return {@code true} if there aren't any changes, {@code false} otherwise
         */
        boolean isEmpty();

        /**
         * Returns the set of names of the mixins that have been added.
         * 
         * @return the set of node type names that were added as a mixin on this node; never null
         */
        Set<Name> getAdded();

        /**
         * Returns the set of names of the mixins that have been removed.
         * 
         * @return the set of node type names that were removed as a mixin on this node; never null
         */
        Set<Name> getRemoved();
    }

    /**
     * Interface which encapsulates the changes to a document's children during an update operation. None of the changes in
     * children expose SNS changes directly, so a connector would have to be aware of that during processing.
     */
    public interface ChildrenChanges {
        /**
         * Checks if there are any changes to the children (appended/renamed/removed/insertedBefore)
         * 
         * @return {@code true} if there aren't any changes, {@code false} otherwise
         */
        boolean isEmpty();

        /**
         * Returns the (childId, childName) map of children that have been appended to underlying document.
         * 
         * @return the map containing the names (keyed by identifiers) of the nodes that were appended to the end of this node's
         *         children; never null
         */
        LinkedHashMap<String, Name> getAppended();

        /**
         * Returns the (childId, newChildName) map of children that have been renamed.
         * 
         * @return the map containing the new node names (keyed by identifiers) of the child nodes that were renamed; never null
         */
        Map<String, Name> getRenamed();

        /**
         * Returns the (insertedBeforeChildId, (childId, childName)) map of the children that have been inserted before an
         * existing child due to a reordering operation.
         * 
         * @return the map containing the list of identifiers and names of the nodes that were inserted before another child of
         *         this node; never null
         */
        Map<String, LinkedHashMap<String, Name>> getInsertedBeforeAnotherChild();

        /**
         * Returns the ids of the children that have been removed.
         * 
         * @return the set containing the identifiers of the nodes that are no longer children of this node; never null
         */
        Set<String> getRemoved();
    }

    /**
     * Interface which encapsulates the changes to a document's parents during an update operation.
     */
    public interface ParentChanges {
        /**
         * Checks if there are any changes to the parent (appended/removed/primary parent changed)
         * 
         * @return {@code true} if there aren't any changes, {@code false} otherwise
         */
        boolean isEmpty();

        /**
         * Checks if there is a new primary parent.
         * 
         * @return {@code true} if there is a new primary parent, or {@code false} otherwise
         */
        boolean hasNewPrimaryParent();

        /**
         * Returns a set of the ids of the parents that have been added.
         * 
         * @return a set containing the identifiers of the nodes that are now longer considered parents of this node; never null
         */
        Set<String> getAdded();

        /**
         * Returns a set of the ids of the parents that have been removed.
         * 
         * @return a set containing the identifiers of the nodes that are no longer considered parents of this node; never null
         */
        Set<String> getRemoved();

        /**
         * Returns the id of the new primary parent if there is a new primary parent.
         * 
         * @return either the id of the new primary parent, or {@code null}.
         */
        String getNewPrimaryParent();
    }

    /**
     * Interface which encapsulates the changes to a document's referrers during an update operation.
     */
    public interface ReferrerChanges {
        /**
         * Checks if there are any changes to the referrers (added/removed weak/strong)
         * 
         * @return {@code true} if there aren't any changes, {@code false} otherwise
         */
        boolean isEmpty();

        /**
         * Returns the set with the document identifiers of the documents which have been added as strong referrers.
         * 
         * @return a set containing the identifiers of the nodes to which strong references to this node were added; never null
         */
        Set<String> getAddedStrong();

        /**
         * Returns the set with the document identifiers of the documents which have been added as weak referrers.
         * 
         * @return a set containing the identifiers of the nodes to which weak references to this node were added; never null
         */
        Set<String> getAddedWeak();

        /**
         * Returns the set with the document identifiers of the document which have been removed as strong referrers.
         * 
         * @return a set containing the identifiers of the nodes from which strong references to this node were removed; never
         *         null
         */
        Set<String> getRemovedStrong();

        /**
         * Returns the set with the document identifiers of the document which have been removed as weak referrers.
         * 
         * @return a set containing the identifiers of the nodes from which weak references to this node were removed; never null
         */
        Set<String> getRemovedWeak();
    }
}
