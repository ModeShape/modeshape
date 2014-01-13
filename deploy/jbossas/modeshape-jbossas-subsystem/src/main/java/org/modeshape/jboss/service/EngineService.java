/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
