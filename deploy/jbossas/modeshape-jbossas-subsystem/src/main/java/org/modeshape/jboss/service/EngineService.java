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
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.ModeShapeEngine;

/**
 * An <code>EngineService</code> instance is a JBoss service object for a {@link ModeShapeEngine}.
 */
public final class EngineService implements Service<ModeShapeEngine>, Serializable {

    private static final long serialVersionUID = 1L;

    private final ModeShapeEngine engine;

    /**
     * Set the engine instance for this service
     * 
     * @param engine the engine (never <code>null</code>)
     */
    public EngineService( ModeShapeEngine engine ) {
        CheckArg.isNotNull(engine, "engine");
        this.engine = engine;
    }

    @Override
    public ModeShapeEngine getValue() throws IllegalStateException, IllegalArgumentException {
        return this.engine;
    }

    @Override
    public void start( final StartContext context ) {
        this.engine.start();
    }

    @Override
    public void stop( StopContext arg0 ) {
        engine.shutdown();
    }
}
