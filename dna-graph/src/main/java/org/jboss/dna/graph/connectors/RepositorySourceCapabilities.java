/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

/**
 * The capabilities of a {@link RepositorySource}.
 * 
 * @author Randall Hauch
 */
public interface RepositorySourceCapabilities {

    /**
     * Return whether the source supports same name siblings. If not, then no two siblings may share the same name.
     * 
     * @return true if same name siblings are supported, or false otherwise
     */
    boolean supportsSameNameSiblings();

    /**
     * Return whether the source supports updates.
     * 
     * @return true if updates are supported, or false if the source only supports reads.
     */
    boolean supportsUpdates();
}
