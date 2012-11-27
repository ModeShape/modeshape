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
     * Returns the changes to the children.
     *
     * @return a {@code non-null} {@link org.modeshape.jcr.federation.spi.DocumentChanges.ChildrenChanges} instance
     */
    ChildrenChanges getChildrenChanges();

    /**
     * Returns the document which contains all the changes.
     *
     * @return a {@code non-null} {@link org.infinispan.schematic.document.Document}
     */
    Document getDocument();

    /**
     * Returns the id of the document to which the changes apply.
     *
     * @return a {@code non-null} {@link String}
     */
    String getDocumentId();

    /**
     * Returns the changes to the mixins.
     *
     * @return a {@code non-null} {@link org.modeshape.jcr.federation.FederatedDocumentChanges.FederatedMixinChanges} instance
     */
    MixinChanges getMixinChanges();

    /**
     * Returns the changes to the parents.
     *
     * @return a {@code non-null} {@link org.modeshape.jcr.federation.FederatedDocumentChanges.FederatedParentChanges} instance
     */
    ParentChanges getParentChanges();

    /**
     * Returns the changes to the properties.
     *
     * @return a {@code non-null} {@link org.modeshape.jcr.federation.FederatedDocumentChanges.FederatedPropertyChanges} instance
     */
    PropertyChanges getPropertyChanges();

    /**
     * Returns the changes to the referrers.
     *
     * @return a {@code non-null} {@link org.modeshape.jcr.federation.FederatedDocumentChanges.FederatedReferrerChanges} instance
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
         * @return a {@code non-null} {@link java.util.Set}
         */
        Set<Name> getChanged();

        /**
         * Returns the set of names of the removed properties
         *
         * @return a {@code non-null} {@link java.util.Set}
         */
        Set<Name> getRemoved();

        /**
         * Returns the set of names of the added properties
         *
         * @return a {@code non-null} {@link java.util.Set}
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
         * @return {@code true} if there aren't  any changes, {@code false} otherwise
         */
        boolean isEmpty();

        /**
         * Returns the set of names of the mixins that have been added.
         *
         * @return {@code a non-null} {@link java.util.Set}
         */
        Set<Name> getAdded();

        /**
         * Returns the set of names of the mixins that have been removed.
         *
         * @return {@code a non-null} {@link java.util.Set}
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
         * @return {@code true} if there aren't  any changes, {@code false} otherwise
         */
        boolean isEmpty();

        /**
         * Returns the (childId, childName) map of children that have been appended to underlying document.
         *
         * @return a {@code non-null} {@link java.util.Map}
         */
        LinkedHashMap<String, Name> getAppended();

        /**
         * Returns the (childId, newChildName) map of children that have been renamed.
         *
         * @return a {@code non-null} {@link java.util.Map}
         */
        Map<String, Name> getRenamed();

        /**
         * Returns the (insertedBeforeChildId, (childId, childName)) map of the children that have been inserted before an existing
         * child due to a reordering operation.
         *
         * @return a {@code non-null} {@link java.util.Map}
         */
        Map<String, LinkedHashMap<String, Name>> getInsertedBeforeAnotherChild();

        /**
         * Returns the ids of the children that have been removed.
         *
         * @return {@code non-null} {@link java.util.Set}
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
         * @return {@code true if there is a new primary parent, false otherwise}
         */
        boolean hasNewPrimaryParent();

        /**
         * Returns a set of the ids of the parents that have been added.
         *
         * @return {@code a non-null} {@link java.util.Set}
         */
        Set<String> getAdded();

        /**
         * Returns a set of the ids of the parents that have been removed.
         *
         * @return {@code a non-null} {@link java.util.Set}
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
         * @return {@code true} if there aren't  any changes, {@code false} otherwise
         */
        boolean isEmpty();

        /**
         * Returns the set with the document identifiers of the documents which have been added as strong referrers.
         *
         * @return a {@code non-null} {@link java.util.Set}
         */
        Set<String> getAddedStrong();

        /**
         * Returns the set with the document identifiers of the documents which have been added as weak referrers.
         *
         * @return a {@code non-null} {@link java.util.Set}
         */
        Set<String> getAddedWeak();

        /**
         * Returns the set with the document identifiers of the document which have been removed as strong referrers.
         *
         * @return a {@code non-null} {@link java.util.Set}
         */
        Set<String> getRemovedStrong();

        /**
         * Returns the set with the document identifiers of the document which have been removed as weak referrers.
         *
         * @return a {@code non-null} {@link java.util.Set}
         */
        Set<String> getRemovedWeak();
    }
}
