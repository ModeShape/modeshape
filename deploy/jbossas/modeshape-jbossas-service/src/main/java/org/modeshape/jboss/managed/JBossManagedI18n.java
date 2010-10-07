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
package org.modeshape.jboss.managed;

import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.jboss.managed*</code> packages.
 */
public final class JBossManagedI18n {

    public static I18n repositoryEngineIsNotRunning;

    public static I18n errorDeterminingIfConnectionIsAlive;
    public static I18n errorDeterminingTotalInUseConnections;
    public static I18n errorDeterminingTotalActiveSessions;
    public static I18n errorGettingRepositoryFromEngine;
    public static I18n errorGettingPropertiesFromManagedObject;
    public static I18n errorBindingToJNDI;

    public static I18n logModeShapeBoundToJNDI;
    public static I18n logModeShapeUnBoundToJNDI;

    static {
        try {
            I18n.initialize(JBossManagedI18n.class);
        } catch (final Exception e) {
            System.err.println(e);
        }
    }

}
