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
package org.modeshape.jboss.managed.temp;

import java.util.ArrayList;
import java.util.Collection;
import org.modeshape.jboss.managed.ManagedConnector;
import org.modeshape.jboss.managed.ManagedRepository;
import org.modeshape.jboss.managed.ManagedSequencingService;

/**
 * 
 */
public class TempEngine {

    private static final int COUNT = 5;
    public static final TempEngine INSTANCE = new TempEngine();
    public static int id = 0;

    public Collection<ManagedConnector> getConnectors() {
        Collection<ManagedConnector> connectors = new ArrayList<ManagedConnector>();

        for (int i = 0; i < COUNT; ++i) {
            connectors.add(new TempManagedConnector(++id));
        }

        return connectors;
    }

    public Collection<ManagedRepository> getRepositories() {
        Collection<ManagedRepository> repositories = new ArrayList<ManagedRepository>();

        for (int i = 0; i < COUNT; ++i) {
            repositories.add(new TempManagedRepository(++id));
        }

        return repositories;
    }

    public ManagedSequencingService getSequencingService() {
        return new TempManagedSequencingService();
    }

    /**
     * Don't allow construction
     */
    private TempEngine() {
        // nothing to do
    }

}
