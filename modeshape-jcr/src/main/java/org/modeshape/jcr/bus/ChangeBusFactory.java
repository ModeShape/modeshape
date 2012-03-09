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

package org.modeshape.jcr.bus;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Class which creates {@link ChangeBus} instances
 *
 * @author Horia Chiorean
 */
public final class ChangeBusFactory {

    private static final List<String> REQUIRED_JGROUPS_CLASSES = Arrays.asList("org.jgroups.JChannel",
                                                                               "org.jgroups.ReceiverAdapter",
                                                                               "org.jgroups.ChannelListener");
    private static final String CLUSTER_NAME = "ModeShape-ChangeBus";

    private ChangeBusFactory() {
    }

    /**
     * Creates either a new {@link RepositoryChangeBus} or a new {@link ClusteredRepositoryChangeBus}
     * based on JGroups being present in the classpath and the clusteringEnabled param to true.
     *
     * @see RepositoryChangeBus#RepositoryChangeBus(java.util.concurrent.ExecutorService, String, boolean)
     */
    public static ChangeBus createBus( boolean clusteringEnabled,
                                       ExecutorService executor,
                                       String systemWorkspaceName,
                                       boolean separateThreadForSystemWorkspace ) {
        RepositoryChangeBus standaloneBus = new RepositoryChangeBus(executor, systemWorkspaceName,
                                                                    separateThreadForSystemWorkspace);
        return isJGroupsInClasspath() && clusteringEnabled ? new ClusteredRepositoryChangeBus(CLUSTER_NAME, standaloneBus) : standaloneBus;
    }


    private static boolean isJGroupsInClasspath() {
        try {
            for (String jgroupsClass : REQUIRED_JGROUPS_CLASSES) {
                Class.forName(jgroupsClass, false, ChangeBusFactory.class.getClassLoader());
            }
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
