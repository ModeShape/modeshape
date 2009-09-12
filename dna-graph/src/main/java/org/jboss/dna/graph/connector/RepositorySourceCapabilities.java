/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.connector;

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
     * The default support for creating workspaces is {@value} .
     */
    public static final boolean DEFAULT_SUPPORT_REFERENCES = true;

    private boolean sameNameSiblings;
    private boolean updates;
    private boolean events;
    private boolean creatingWorkspaces;
    private boolean references;

    /**
     * Create a capabilities object using the defaults, .
     */
    public RepositorySourceCapabilities() {
        this(DEFAULT_SUPPORT_SAME_NAME_SIBLINGS, DEFAULT_SUPPORT_UPDATES, DEFAULT_SUPPORT_EVENTS,
             DEFAULT_SUPPORT_CREATING_WORKSPACES, DEFAULT_SUPPORT_REFERENCES);
    }

    public RepositorySourceCapabilities( boolean supportsSameNameSiblings,
                                         boolean supportsUpdates ) {
        this(supportsSameNameSiblings, supportsUpdates, DEFAULT_SUPPORT_EVENTS, DEFAULT_SUPPORT_CREATING_WORKSPACES,
             DEFAULT_SUPPORT_REFERENCES);
    }

    public RepositorySourceCapabilities( boolean supportsSameNameSiblings,
                                         boolean supportsUpdates,
                                         boolean supportsEvents,
                                         boolean supportsCreatingWorkspaces,
                                         boolean supportsReferences ) {
        this.sameNameSiblings = supportsSameNameSiblings;
        this.updates = supportsUpdates;
        this.events = supportsEvents;
        this.creatingWorkspaces = supportsCreatingWorkspaces;
        this.references = supportsReferences;
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
}
