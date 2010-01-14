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
package org.modeshape.graph.connector;

import net.jcip.annotations.Immutable;

/**
 * The capabilities of a {@link RepositorySource}. This class can be used as is, or subclassed by a connector to define more
 * complex behavior.
 * 
 * @see RepositorySource#getCapabilities()
 */
@Immutable
public class RepositorySourceCapabilities {

    /**
     * The default support for same-name-siblings is {@value} .
     */
    public static final boolean DEFAULT_SUPPORT_SAME_NAME_SIBLINGS = true;

    /**
     * The default support for updates is {@value} .
     */
    public static final boolean DEFAULT_SUPPORT_UPDATES = false;

    /**
     * The default support for updates is {@value} .
     */
    public static final boolean DEFAULT_SUPPORT_EVENTS = false;

    /**
     * The default support for creating workspaces is {@value} .
     */
    public static final boolean DEFAULT_SUPPORT_CREATING_WORKSPACES = false;

    /**
     * The default support for references is {@value} .
     */
    public static final boolean DEFAULT_SUPPORT_REFERENCES = true;

    /**
     * The default support for querying workspaces is {@value} .
     */
    public static final boolean DEFAULT_SUPPORT_QUERIES = false;

    /**
     * The default support for searching workspaces is {@value} .
     */
    public static final boolean DEFAULT_SUPPORT_SEARCHES = false;

    /**
     * The default support for creating locks is {@value} .
     */
    public static final boolean DEFAULT_SUPPORT_LOCKS = false;

    private final boolean sameNameSiblings;
    private final boolean updates;
    private final boolean events;
    private final boolean creatingWorkspaces;
    private final boolean references;
    private final boolean locks;
    private final boolean queries;
    private final boolean searches;

    /**
     * Create a capabilities object using the defaults, .
     */
    public RepositorySourceCapabilities() {
        this(DEFAULT_SUPPORT_SAME_NAME_SIBLINGS, DEFAULT_SUPPORT_UPDATES, DEFAULT_SUPPORT_EVENTS,
             DEFAULT_SUPPORT_CREATING_WORKSPACES, DEFAULT_SUPPORT_REFERENCES, DEFAULT_SUPPORT_LOCKS, DEFAULT_SUPPORT_QUERIES,
             DEFAULT_SUPPORT_SEARCHES);
    }

    public RepositorySourceCapabilities( RepositorySourceCapabilities capabilities ) {
        this(capabilities.supportsSameNameSiblings(), capabilities.supportsUpdates(), capabilities.supportsEvents(),
             capabilities.supportsCreatingWorkspaces(), capabilities.supportsReferences(), capabilities.supportsLocks(),
             capabilities.supportsQueries(), capabilities.supportsSearches());
    }

    public RepositorySourceCapabilities( boolean supportsSameNameSiblings,
                                         boolean supportsUpdates ) {
        this(supportsSameNameSiblings, supportsUpdates, DEFAULT_SUPPORT_EVENTS, DEFAULT_SUPPORT_CREATING_WORKSPACES,
             DEFAULT_SUPPORT_REFERENCES, DEFAULT_SUPPORT_LOCKS, DEFAULT_SUPPORT_QUERIES, DEFAULT_SUPPORT_SEARCHES);
    }

    public RepositorySourceCapabilities( boolean supportsSameNameSiblings,
                                         boolean supportsUpdates,
                                         boolean supportsEvents,
                                         boolean supportsCreatingWorkspaces,
                                         boolean supportsReferences ) {
        this(supportsSameNameSiblings, supportsUpdates, supportsEvents, supportsCreatingWorkspaces, supportsReferences,
             DEFAULT_SUPPORT_LOCKS, DEFAULT_SUPPORT_QUERIES, DEFAULT_SUPPORT_SEARCHES);
    }

    public RepositorySourceCapabilities( boolean supportsSameNameSiblings,
                                         boolean supportsUpdates,
                                         boolean supportsEvents,
                                         boolean supportsCreatingWorkspaces,
                                         boolean supportsReferences,
                                         boolean supportsLocks,
                                         boolean supportsQueries,
                                         boolean supportsSearches ) {

        this.sameNameSiblings = supportsSameNameSiblings;
        this.updates = supportsUpdates;
        this.events = supportsEvents;
        this.creatingWorkspaces = supportsCreatingWorkspaces;
        this.references = supportsReferences;
        this.locks = supportsLocks;
        this.queries = supportsQueries;
        this.searches = supportsSearches;
    }

    /**
     * Return whether the source supports same name siblings. If not, then no two siblings may share the same name.
     * 
     * @return true if same name siblings are supported, or false otherwise
     */
    public boolean supportsSameNameSiblings() {
        return sameNameSiblings;
    }

    /**
     * Return whether the source supports updates. This may be true, even though a particular connection made on behalf of a user
     * may not have any update privileges. In other words, returning <code>false</code> implies that no connections would allow
     * updates to the content.
     * 
     * @return true if updates are supported, or false if the source only supports reads.
     */
    public boolean supportsUpdates() {
        return updates;
    }

    /**
     * Return whether the source supports references by identifiers.
     * 
     * @return true if references are supported, or false otherwise
     */
    public boolean supportsReferences() {
        return references;
    }

    /**
     * Return whether the source supports publishing change events.
     * 
     * @return true if events are supported, or false if the source is not capable of generating events
     */
    public boolean supportsEvents() {
        return events;
    }

    /**
     * Return whether the source supports creating workspaces through the connector.
     * 
     * @return true if creating workspaces is supported, or false if the source is not capable of creating workspaces
     */
    public boolean supportsCreatingWorkspaces() {
        return creatingWorkspaces;
    }

    /**
     * Return whether the source supports creating locks.
     * 
     * @return true if locks are supported, or false if the source is not capable of creating locks
     */
    public boolean supportsLocks() {
        return locks;
    }

    /**
     * Return whether the source supports queries.
     * 
     * @return true if queries are supported, or false if the source is not capable of querying content
     */
    public boolean supportsQueries() {
        return queries;
    }

    /**
     * Return whether the source supports full-text searches.
     * 
     * @return true if searches are supported, or false if the source is not capable of searching content
     */
    public boolean supportsSearches() {
        return searches;
    }
}
