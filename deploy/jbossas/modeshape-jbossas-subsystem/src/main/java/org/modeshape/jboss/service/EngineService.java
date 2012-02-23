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
package org.modeshape.jboss.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrEngine.State;

/**
 * An <code>EngineService</code> instance is a JBoss service object for a {@link JcrEngine}.
 */
public final class EngineService implements Service<JcrEngine>, Serializable {

    private static final long serialVersionUID = 1L;

    private JcrEngine engine = new JcrEngine();

    public EngineService() {
        this.engine = null;
    }

    /**
     * Set the engine instance for this service
     * 
     * @param engine the engine (never <code>null</code>)
     */
    public EngineService( JcrEngine engine ) {
        CheckArg.isNotNull(engine, "engine");
        this.engine = engine;
    }

    @Override
    public JcrEngine getValue() throws IllegalStateException, IllegalArgumentException {
        return this.getEngine();
    }

    @Override
    public void start( final StartContext context ) {

        this.engine.start();

    }

    @Override
    public void stop( StopContext arg0 ) {

        engine.shutdown();

    }

    /**
     * A utility method that must be used by all non-synchronized methods to access the engine. Subsequent calls to this method
     * may return different JcrEngine instances, so all non-synchronized methods should call this method once and use the returned
     * reference for all operations against the engine.
     * 
     * @return the engine at the moment this method is called; may be null if an engine could not be created
     */
    protected synchronized JcrEngine getEngine() {
        return this.engine;
    }

    /**
     * Indicates if the managed engine is running. This is a JBoss managed operation.
     * 
     * @return <code>true</code> if the engine is running
     */
    public synchronized boolean isRunning() {
        if (this.engine == null) return false;
        try {
            return this.engine.getState().equals(State.RUNNING);
        } catch (IllegalStateException e) {
            return false;
        }
    }


    /**
     * Obtains the repositories of this engine.
     * 
     * @return an unmodifiable collection of repositories (never <code>null</code>)
     */
    public Collection<String> getRepositories() {
        if (!isRunning()) return Collections.emptyList();

        // Get engine to use for the rest of the method (this is synchronized)
        // ...
        final JcrEngine engine = getEngine();
        assert engine != null;

        Collection<String> repositories = new ArrayList<String>();
        for (String repositoryName : engine.getRepositoryNames()) {
            repositories.add(repositoryName);
        }

        return repositories;
    }

}