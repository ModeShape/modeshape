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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.infinispan.schematic.document.Document;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.value.Name;

/**
 * Value object which encapsulates a set of changes that occurred on a document.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class DocumentChanges {

    private final String documentId;
    private final Document document;

    private final PropertyChanges propertyChanges;
    private final MixinChanges mixinChanges;
    private final ChildrenChanges childrenChanges;
    private final ParentChanges parentChanges;
    private final ReferrerChanges referrerChanges;

    /**
     * Creates a new changes instance for the given document with the given id.
     *
     * @param documentId a {@code non-null} {@link String}
     * @param document {@code non-null} {@link Document}
     */
    public DocumentChanges( String documentId,
                            Document document ) {
        this.document = document;
        this.documentId = documentId;

        this.propertyChanges = new PropertyChanges();
        this.mixinChanges = new MixinChanges();
        this.childrenChanges = new ChildrenChanges();
        this.parentChanges = new ParentChanges();
        this.referrerChanges = new ReferrerChanges();
    }

    /**
     * Returns the changes to the children.
     *
     * @return a {@code non-null} {@link ChildrenChanges} instance
     */
    public ChildrenChanges getChildrenChanges() {
        return childrenChanges;
    }

    /**
     * Returns the document which contains all the changes.
     *
     * @return a {@code non-null} {@link Document}
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Returns the id of the document to which the changes apply.
     *
     * @return a {@code non-null} {@link String}
     */
    public String getDocumentId() {
        return documentId;
    }

    /**
     * Returns the changes to the mixins.
     *
     * @return a {@code non-null} {@link MixinChanges} instance
     */
    public MixinChanges getMixinChanges() {
        return mixinChanges;
    }

    /**
     * Returns the changes to the parents.
     *
     * @return a {@code non-null} {@link ParentChanges} instance
     */
    public ParentChanges getParentChanges() {
        return parentChanges;
    }

    /**
     * Returns the changes to the properties.
     *
     * @return a {@code non-null} {@link PropertyChanges} instance
     */
    public PropertyChanges getPropertyChanges() {
        return propertyChanges;
    }

    /**
     * Returns the changes to the referrers.
     *
     * @return a {@code non-null} {@link ReferrerChanges} instance
     */
    public ReferrerChanges getReferrerChanges() {
        return referrerChanges;
    }

    /**
     * Registers a set of property changes.
     *
     * @param sessionChangedProperties an optional {@link Set} of property names
     * @param sessionRemovedProperties an optional {@link Set} of property names
     * @return this instance
     */
    public DocumentChanges withPropertyChanges( Set<Name> sessionChangedProperties,
                                                Set<Name> sessionRemovedProperties ) {
        Set<Name> addedProperties = new HashSet<Name>();
        Set<Name> removedProperties = new HashSet<Name>(sessionRemovedProperties);
        Set<Name> changedProperties = new HashSet<Name>(sessionChangedProperties);

        //process the session properties to make the distinction between changed / added / removed
        for (Iterator<Name> changedPropertiesIterator = changedProperties.iterator(); changedPropertiesIterator.hasNext(); ) {
            Name changedPropertyName = changedPropertiesIterator.next();
            //check if it's an add or a change
            if (!sessionRemovedProperties.contains(changedPropertyName)) {
                addedProperties.add(changedPropertyName);
                changedPropertiesIterator.remove();
            } else  {
                //it's a changed property, so clean up the removals
                removedProperties.remove(changedPropertyName);
            }
        }

        propertyChanges.changed(changedProperties).removed(removedProperties).added(addedProperties);
        return this;
    }

    /**
     * Registers a set of mixin changes.
     *
     * @param addedMixins an optional {@link Set} of mixin names
     * @param removedMixins an optional {@link Set} of mixin names
     * @return this instance
     */
    public DocumentChanges withMixinChanges( Set<Name> addedMixins,
                                             Set<Name> removedMixins ) {
        mixinChanges.added(addedMixins).removed(removedMixins);
        return this;
    }

    /**
     * Registers a set of children changes.
     *
     * @param sessionAppendedChildren an optional map of (childId, childName) pairs
     * @param sessionRenamedChildren an optional map of (childId, newChildName) pairs
     * @param sessionRemovedChildren an optional set of the ids of removed children
     * @param sessionChildrenInsertedBeforeAnotherChild an optional map of (insertedBeforeChildId, (childId, childName)) pairs
     * @return this instance
     */
    public DocumentChanges withChildrenChanges( LinkedHashMap<String, Name> sessionAppendedChildren,
                                                Map<String, Name> sessionRenamedChildren,
                                                Set<String> sessionRemovedChildren,
                                                Map<String, LinkedHashMap<String, Name>> sessionChildrenInsertedBeforeAnotherChild ) {
        //the reordered children appear in the remove list as well, so we need to clean this up
        Set<String> removedChildren = new HashSet<String>(sessionRemovedChildren);
        for (String orderedBefore : sessionChildrenInsertedBeforeAnotherChild.keySet()) {
            LinkedHashMap<String, Name> childrenMap = sessionChildrenInsertedBeforeAnotherChild.get(orderedBefore);
            for (String childId : childrenMap.keySet()) {
                removedChildren.remove(childId);
            }
        }

        childrenChanges.appended(sessionAppendedChildren)
                       .renamed(sessionRenamedChildren)
                       .removed(removedChildren)
                       .insertedBeforeAnotherChild(sessionChildrenInsertedBeforeAnotherChild);
        return this;
    }

    /**
     * Registers a set of parent changes.
     *
     * @param addedParents an optional set of the ids of the added parents
     * @param removedParents an optional set of the ids of the removed parents
     * @param newPrimaryParent the optional id of the new primary parent
     * @return this instance
     */
    public DocumentChanges withParentChanges( Set<String> addedParents,
                                              Set<String> removedParents,
                                              String newPrimaryParent ) {
        parentChanges.added(addedParents).removed(removedParents).newPrimaryParent(newPrimaryParent);
        return this;
    }

    /**
     * Registers a set of referrer changes.
     *
     * @param addedWeakReferrers an optional set of the ids of the added weak referrers
     * @param removedWeakReferrers an optional set of the ids of the removed weak referrers
     * @param addedStrongReferrers an optional set of the ids of the added strong referrers
     * @param removedStrongReferrers an optional set of the ids of the removed strong referrers
     * @return this instance
     */
    public DocumentChanges withReferrerChanges( Set<String> addedWeakReferrers,
                                                Set<String> removedWeakReferrers,
                                                Set<String> addedStrongReferrers,
                                                Set<String> removedStrongReferrers ) {
        referrerChanges.addedWeak(addedWeakReferrers).addedStrong(addedStrongReferrers).removedStrong(removedStrongReferrers)
                       .removedWeak(removedWeakReferrers);
        return this;
    }

    /**
     * Class which encapsulates property changes
     */
    public class PropertyChanges {
        private Set<Name> added = Collections.emptySet();
        private Set<Name> changed = Collections.emptySet();
        private Set<Name> removed = Collections.emptySet();

        private PropertyChanges() {
        }

        /**
         * Checks if there are any changes to the properties (removed/added/changed)
         *
         * @return {@code true} if there aren't any changes, {@code false} otherwise
         */
        public boolean isEmpty() {
            return changed.isEmpty() && removed.isEmpty();
        }

        /**
         * Returns the set of name of the changed properties (added + modified)
         *
         * @return a {@code non-null} {@link Set}
         */
        public Set<Name> getChanged() {
            return changed;
        }

        /**
         * Returns the set of names of the removed properties
         *
         * @return a {@code non-null} {@link Set}
         */
        public Set<Name> getRemoved() {
            return removed;
        }


        /**
         * Returns the set of names of the added properties
         *
         * @return a {@code non-null} {@link Set}
         */
        public Set<Name> getAdded() {
            return added;
        }

        private PropertyChanges changed( final Set<Name> changed ) {
            if (changed != null) {
                this.changed = changed;
            }
            return this;
        }

        private PropertyChanges removed( final Set<Name> removed ) {
            if (removed != null) {
                this.removed = removed;
            }
            return this;
        }


        private PropertyChanges added( final Set<Name> added ) {
            this.added = added;
            return this;
        }
    }

    /**
     * Class which encapsulates mixin changes
     */
    public class MixinChanges {
        private Set<Name> added = Collections.emptySet();
        private Set<Name> removed = Collections.emptySet();

        private MixinChanges() {
        }

        /**
         * Checks if there are any changes to the mixins (removed/added)
         *
         * @return {@code true} if there aren't  any changes, {@code false} otherwise
         */
        public boolean isEmpty() {
            return added.isEmpty() && removed.isEmpty();
        }

        /**
         * Returns the set of names of the mixins that have been added.
         *
         * @return {@code a non-null} {@link Set}
         */
        public Set<Name> getAdded() {
            return added;
        }

        /**
         * Returns the set of names of the mixins that have been removed.
         *
         * @return {@code a non-null} {@link Set}
         */
        public Set<Name> getRemoved() {
            return removed;
        }

        private MixinChanges added( final Set<Name> added ) {
            if (added != null) {
                this.added = added;
            }
            return this;
        }

        private MixinChanges removed( final Set<Name> removed ) {
            if (removed != null) {
                this.removed = removed;
            }
            return this;
        }
    }

    /**
     * Class which encapsulates children changes
     */
    public class ChildrenChanges {
        private LinkedHashMap<String, Name> appended = new LinkedHashMap<String, Name>();
        private Map<String, Name> renamed = Collections.emptyMap();
        private Map<String, LinkedHashMap<String, Name>> insertedBeforeAnotherChild = Collections.emptyMap();
        private Set<String> removed = Collections.emptySet();

        private ChildrenChanges() {
        }

        /**
         * Checks if there are any changes to the children (appended/renamed/removed/insertedBefore)
         *
         * @return {@code true} if there aren't  any changes, {@code false} otherwise
         */
        public boolean isEmpty() {
            return appended.isEmpty() && renamed.isEmpty() && insertedBeforeAnotherChild.isEmpty() && removed.isEmpty();
        }

        /**
         * Returns the (childId, childName) map of children that have been appended to underlying document.
         *
         * @return a {@code non-null} {@link Map}
         */
        public LinkedHashMap<String, Name> getAppended() {
            return appended;
        }

        /**
         * Returns the (childId, newChildName) map of children that have been renamed.
         *
         * @return a {@code non-null} {@link Map}
         */
        public Map<String, Name> getRenamed() {
            return renamed;
        }

        /**
         * Returns the (insertedBeforeChildId, (childId, childName)) map of the children that have been inserted before an existing
         * child due to a reordering operation.
         *
         * @return a {@code non-null} {@link Map}
         */
        public Map<String, LinkedHashMap<String, Name>> getInsertedBeforeAnotherChild() {
            return insertedBeforeAnotherChild;
        }

        /**
         * Returns the ids of the children that have been removed.
         *
         * @return {@code non-null} {@link Set}
         */
        public Set<String> getRemoved() {
            return removed;
        }

        private ChildrenChanges appended( final LinkedHashMap<String, Name> appended ) {
            if (appended != null) {
                this.appended = appended;
            }
            return this;
        }

        private ChildrenChanges renamed( final Map<String, Name> renamed ) {
            if (renamed != null) {
                this.renamed = renamed;
            }
            return this;
        }

        private ChildrenChanges insertedBeforeAnotherChild( final Map<String, LinkedHashMap<String, Name>> insertedBeforeAnotherChild ) {
            if (insertedBeforeAnotherChild != null) {
                this.insertedBeforeAnotherChild = insertedBeforeAnotherChild;
            }
            return this;
        }

        private ChildrenChanges removed( final Set<String> removed ) {
            if (removed != null) {
                this.removed = removed;
            }
            return this;
        }
    }

    /**
     * Class which encapsulates changes to parents.
     */
    public class ParentChanges {
        private Set<String> added = Collections.emptySet();
        private Set<String> removed = Collections.emptySet();
        private String newPrimaryParent = null;

        /**
         * Checks if there are any changes to the parent (appended/removed/primary parent changed)
         *
         * @return {@code true} if there aren't any changes, {@code false} otherwise
         */
        public boolean isEmpty() {
            return added.isEmpty() && removed.isEmpty() && StringUtil.isBlank(newPrimaryParent);
        }

        /**
         * Checks if there is a new primary parent.
         *
         * @return {@code true if there is a new primary parent, false otherwise}
         */
        public boolean hasNewPrimaryParent() {
            return !StringUtil.isBlank(newPrimaryParent);
        }

        /**
         * Returns a set of the ids of the parents that have been added.
         *
         * @return {@code a non-null} {@link Set}
         */
        public Set<String> getAdded() {
            return added;
        }

        /**
         * Returns a set of the ids of the parents that have been removed.
         *
         * @return {@code a non-null} {@link Set}
         */
        public Set<String> getRemoved() {
            return removed;
        }

        /**
         * Returns the id of the new primary parent if there is a new primary parent.
         *
         * @return either the id of the new primary parent, or {@code null}.
         */
        public String getNewPrimaryParent() {
            return newPrimaryParent;
        }

        private ParentChanges added( final Set<String> added ) {
            if (added != null) {
                this.added = added;
            }
            return this;
        }

        private ParentChanges removed( final Set<String> removed ) {
            if (removed != null) {
                this.removed = removed;
            }
            return this;
        }

        private ParentChanges newPrimaryParent( final String newPrimaryParent ) {
            this.newPrimaryParent = newPrimaryParent;
            return this;
        }
    }

    /**
     * Class which encapsulates changes to referrers.
     */
    public class ReferrerChanges {
        private Set<String> addedWeak = Collections.emptySet();
        private Set<String> removedWeak = Collections.emptySet();
        private Set<String> addedStrong = Collections.emptySet();
        private Set<String> removedStrong = Collections.emptySet();

        /**
         * Checks if there are any changes to the referrers (added/removed weak/strong)
         *
         * @return {@code true} if there aren't  any changes, {@code false} otherwise
         */
        public boolean isEmpty() {
            return addedWeak.isEmpty() && removedWeak.isEmpty() && addedStrong.isEmpty() && removedStrong.isEmpty();
        }

        /**
         * Returns the set with the document identifiers of the documents which have been added as strong referrers.
         *
         * @return a {@code non-null} {@link Set}
         */
        public Set<String> getAddedStrong() {
            return addedStrong;
        }

        /**
         * Returns the set with the document identifiers of the documents which have been added as weak referrers.
         *
         * @return a {@code non-null} {@link Set}
         */
        public Set<String> getAddedWeak() {
            return addedWeak;
        }

        /**
         * Returns the set with the document identifiers of the document which have been removed as strong referrers.
         *
         * @return a {@code non-null} {@link Set}
         */
        public Set<String> getRemovedStrong() {
            return removedStrong;
        }

        /**
         * Returns the set with the document identifiers of the document which have been removed as weak referrers.
         *
         * @return a {@code non-null} {@link Set}
         */
        public Set<String> getRemovedWeak() {
            return removedWeak;
        }

        private ReferrerChanges addedStrong( final Set<String> addedStrong ) {
            if (addedStrong != null) {
                this.addedStrong = addedStrong;
            }
            return this;
        }

        private ReferrerChanges addedWeak( final Set<String> addedWeak ) {
            if (addedWeak != null) {
                this.addedWeak = addedWeak;
            }
            return this;
        }

        private ReferrerChanges removedWeak( final Set<String> removedWeak ) {
            if (removedWeak != null) {
                this.removedWeak = removedWeak;
            }
            return this;
        }

        private ReferrerChanges removedStrong( final Set<String> removedStrong ) {
            if (removedStrong != null) {
                this.removedStrong = removedStrong;
            }
            return this;
        }
    }
}
