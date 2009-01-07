/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.connectors;

import net.jcip.annotations.Immutable;

/**
 * The capabilities of a {@link RepositorySource}. This class can be used as is, or subclassed by a connector to define more
 * complex behavior.
 * 
 * @see RepositorySource#getCapabilities()
 * @author Randall Hauch
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

    private boolean sameNameSiblings;
    private boolean updates;
    private boolean events;

    /**
     * Create a capabilities object using the defaults, .
     */
    public RepositorySourceCapabilities() {
        this(DEFAULT_SUPPORT_SAME_NAME_SIBLINGS, DEFAULT_SUPPORT_UPDATES, DEFAULT_SUPPORT_EVENTS);
    }

    public RepositorySourceCapabilities( boolean supportsSameNameSiblings,
                                         boolean supportsUpdates ) {
        this(supportsSameNameSiblings, supportsUpdates, DEFAULT_SUPPORT_EVENTS);
    }

    public RepositorySourceCapabilities( boolean supportsSameNameSiblings,
                                         boolean supportsUpdates,
                                         boolean supportsEvents ) {
        this.sameNameSiblings = supportsSameNameSiblings;
        this.updates = supportsUpdates;
        this.events = supportsEvents;
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
     * Return whether the source supports events through {@link RepositorySourceListener}s.
     * 
     * @return true if events are supported, or false if the source is not capable of generating events
     */
    public boolean supportsEvents() {
        return events;
    }
}
