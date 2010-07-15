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
package org.modeshape.clustering;

import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.connector.filesystem*</code> packages.
 */
public final class ClusteringI18n {

    public static I18n errorWhileStartingJGroups;
    public static I18n clusterNameRequired;
    public static I18n unableToNotifyChangesBecauseClusterChannelHasClosed;
    public static I18n unableToNotifyChangesBecauseClusterChannelIsNotConnected;
    public static I18n errorSerializingChanges;
    public static I18n errorDeserializingChanges;
    public static I18n clusteringChannelIsRunningAndCannotBeChangedUnlessShutdown;
    public static I18n memberOfClusterIsSuspect;

    static {
        try {
            I18n.initialize(ClusteringI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }
}
