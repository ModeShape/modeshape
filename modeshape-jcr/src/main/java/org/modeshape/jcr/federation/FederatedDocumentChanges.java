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

package org.modeshape.jcr.federation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.infinispan.schematic.document.Document;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.federation.spi.DocumentChanges;
import org.modeshape.jcr.value.Name;

/**
 * Default implementation of the {@link DocumentChanges} interface
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@SuppressWarnings( "synthetic-access" )
public class FederatedDocumentChanges implements DocumentChanges {

    private final String documentId;
    private final Document document;

    private final FederatedPropertyChanges propertyChanges;
    private final FederatedMixinChanges mixinChanges;
    private final FederatedChildrenChanges childrenChanges;
    private final FederatedParentChanges parentChanges;
    private final FederatedReferrerChanges referrerChanges;

    protected FederatedDocumentChanges( String documentId,
                                        Document document ) {
        this.document = document;
        this.documentId = documentId;

        this.propertyChanges = new FederatedPropertyChanges();
        this.mixinChanges = new FederatedMixinChanges();
        this.childrenChanges = new FederatedChildrenChanges();
        this.parentChanges = new FederatedParentChanges();
        this.referrerChanges = new FederatedReferrerChanges();
    }

    @Override
    public ChildrenChanges getChildrenChanges() {
        return childrenChanges;
    }

    @Override
    public Document getDocument() {
        return document;
    }

    @Override
    public String getDocumentId() {
        return documentId;
    }

    @Override
    public MixinChanges getMixinChanges() {
        return mixinChanges;
    }

    @Override
    public ParentChanges getParentChanges() {
        return parentChanges;
    }

    @Override
    public PropertyChanges getPropertyChanges() {
        return propertyChanges;
    }

    @Override
    public ReferrerChanges getReferrerChanges() {
        return referrerChanges;
    }

    protected void setPropertyChanges( Set<Name> sessionChangedProperties,
                                       Set<Name> sessionRemovedProperties ) {
        Set<Name> addedProperties = new HashSet<Name>();

        // process the session properties to make the distinction between changed / added / removed
        for (Iterator<Name> changedPropertiesIterator = sessionChangedProperties.iterator(); changedPropertiesIterator.hasNext();) {
            Name changedPropertyName = changedPropertiesIterator.next();
            // check if it's an add or a change
            if (!sessionRemovedProperties.contains(changedPropertyName)) {
                addedProperties.add(changedPropertyName);
                changedPropertiesIterator.remove();
            } else {
                // it's a changed property, so clean up the removals
                sessionRemovedProperties.remove(changedPropertyName);
            }
        }

        propertyChanges.changed(sessionChangedProperties).removed(sessionRemovedProperties).added(addedProperties);
    }

    protected void setMixinChanges( Set<Name> addedMixins,
                                    Set<Name> removedMixins ) {
        mixinChanges.added(addedMixins).removed(removedMixins);
    }

    protected void setChildrenChanges( LinkedHashMap<String, Name> sessionAppendedChildren,
                                       Map<String, Name> sessionRenamedChildren,
                                       Set<String> sessionRemovedChildren,
                                       Map<String, LinkedHashMap<String, Name>> sessionChildrenInsertedBeforeAnotherChild ) {
        // the reordered children appear in the remove list as well, so we need to clean this up
        for (String orderedBefore : sessionChildrenInsertedBeforeAnotherChild.keySet()) {
            LinkedHashMap<String, Name> childrenMap = sessionChildrenInsertedBeforeAnotherChild.get(orderedBefore);
            for (String childId : childrenMap.keySet()) {
                sessionRemovedChildren.remove(childId);
            }
        }

        childrenChanges.appended(sessionAppendedChildren)
                       .renamed(sessionRenamedChildren)
                       .removed(sessionRemovedChildren)
                       .insertedBeforeAnotherChild(sessionChildrenInsertedBeforeAnotherChild);
    }

    protected void setParentChanges( Set<String> addedParents,
                                     Set<String> removedParents,
                                     String newPrimaryParent ) {
        parentChanges.added(addedParents).removed(removedParents).newPrimaryParent(newPrimaryParent);
    }

    protected void setReferrerChanges( Set<String> addedWeakReferrers,
                                       Set<String> removedWeakReferrers,
                                       Set<String> addedStrongReferrers,
                                       Set<String> removedStrongReferrers ) {
        referrerChanges.addedWeak(addedWeakReferrers)
                       .addedStrong(addedStrongReferrers)
                       .removedStrong(removedStrongReferrers)
                       .removedWeak(removedWeakReferrers);
    }

    protected class FederatedPropertyChanges implements PropertyChanges {
        private Set<Name> added = Collections.emptySet();
        private Set<Name> changed = Collections.emptySet();
        private Set<Name> removed = Collections.emptySet();

        private FederatedPropertyChanges() {
        }

        @Override
        public boolean isEmpty() {
            return changed.isEmpty() && removed.isEmpty();
        }

        @Override
        public Set<Name> getChanged() {
            return changed;
        }

        @Override
        public Set<Name> getRemoved() {
            return removed;
        }

        @Override
        public Set<Name> getAdded() {
            return added;
        }

        private FederatedPropertyChanges changed( final Set<Name> changed ) {
            if (changed != null) {
                this.changed = changed;
            }
            return this;
        }

        private FederatedPropertyChanges removed( final Set<Name> removed ) {
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

    protected class FederatedMixinChanges implements MixinChanges {
        private Set<Name> added = Collections.emptySet();
        private Set<Name> removed = Collections.emptySet();

        private FederatedMixinChanges() {
        }

        @Override
        public boolean isEmpty() {
            return added.isEmpty() && removed.isEmpty();
        }

        @Override
        public Set<Name> getAdded() {
            return added;
        }

        @Override
        public Set<Name> getRemoved() {
            return removed;
        }

        private FederatedMixinChanges added( final Set<Name> added ) {
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

    protected class FederatedChildrenChanges implements ChildrenChanges {

        private LinkedHashMap<String, Name> appended = new LinkedHashMap<String, Name>();
        private Map<String, Name> renamed = Collections.emptyMap();
        private Map<String, LinkedHashMap<String, Name>> insertedBeforeAnotherChild = Collections.emptyMap();
        private Set<String> removed = Collections.emptySet();

        private FederatedChildrenChanges() {
        }

        @Override
        public boolean isEmpty() {
            return appended.isEmpty() && renamed.isEmpty() && insertedBeforeAnotherChild.isEmpty() && removed.isEmpty();
        }

        @Override
        public LinkedHashMap<String, Name> getAppended() {
            return appended;
        }

        @Override
        public Map<String, Name> getRenamed() {
            return renamed;
        }

        @Override
        public Map<String, LinkedHashMap<String, Name>> getInsertedBeforeAnotherChild() {
            return insertedBeforeAnotherChild;
        }

        @Override
        public Set<String> getRemoved() {
            return removed;
        }

        private FederatedChildrenChanges appended( final LinkedHashMap<String, Name> appended ) {
            if (appended != null) {
                this.appended = appended;
            }
            return this;
        }

        private FederatedChildrenChanges renamed( final Map<String, Name> renamed ) {
            if (renamed != null) {
                this.renamed = renamed;
            }
            return this;
        }

        private FederatedChildrenChanges insertedBeforeAnotherChild( final Map<String, LinkedHashMap<String, Name>> insertedBeforeAnotherChild ) {
            if (insertedBeforeAnotherChild != null) {
                this.insertedBeforeAnotherChild = insertedBeforeAnotherChild;
            }
            return this;
        }

        private FederatedChildrenChanges removed( final Set<String> removed ) {
            if (removed != null) {
                this.removed = removed;
            }
            return this;
        }
    }

    protected class FederatedParentChanges implements ParentChanges {
        private Set<String> added = Collections.emptySet();
        private Set<String> removed = Collections.emptySet();
        private String newPrimaryParent = null;

        @Override
        public boolean isEmpty() {
            return added.isEmpty() && removed.isEmpty() && StringUtil.isBlank(newPrimaryParent);
        }

        @Override
        public boolean hasNewPrimaryParent() {
            return !StringUtil.isBlank(newPrimaryParent);
        }

        @Override
        public Set<String> getAdded() {
            return added;
        }

        @Override
        public Set<String> getRemoved() {
            return removed;
        }

        @Override
        public String getNewPrimaryParent() {
            return newPrimaryParent;
        }

        private FederatedParentChanges added( final Set<String> added ) {
            if (added != null) {
                this.added = added;
            }
            return this;
        }

        private FederatedParentChanges removed( final Set<String> removed ) {
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

    protected class FederatedReferrerChanges implements ReferrerChanges {
        private Set<String> addedWeak = Collections.emptySet();
        private Set<String> removedWeak = Collections.emptySet();
        private Set<String> addedStrong = Collections.emptySet();
        private Set<String> removedStrong = Collections.emptySet();

        @Override
        public boolean isEmpty() {
            return addedWeak.isEmpty() && removedWeak.isEmpty() && addedStrong.isEmpty() && removedStrong.isEmpty();
        }

        @Override
        public Set<String> getAddedStrong() {
            return addedStrong;
        }

        @Override
        public Set<String> getAddedWeak() {
            return addedWeak;
        }

        @Override
        public Set<String> getRemovedStrong() {
            return removedStrong;
        }

        @Override
        public Set<String> getRemovedWeak() {
            return removedWeak;
        }

        private FederatedReferrerChanges addedStrong( final Set<String> addedStrong ) {
            if (addedStrong != null) {
                this.addedStrong = addedStrong;
            }
            return this;
        }

        private FederatedReferrerChanges addedWeak( final Set<String> addedWeak ) {
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

        private FederatedReferrerChanges removedStrong( final Set<String> removedStrong ) {
            if (removedStrong != null) {
                this.removedStrong = removedStrong;
            }
            return this;
        }
    }
}
