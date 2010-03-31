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
package org.modeshape.connector.jcr;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import javax.jcr.Repository;
import javax.naming.Context;
import org.modeshape.graph.Graph;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.test.WritableConnectorTest;
import org.modeshape.jcr.JcrEngine;

public class JcrConnectorWritableTest extends WritableConnectorTest /* extends AbstractConnectorTest */{

    public static final String ARBITRARY_PROPERTIES_NOT_SUPPORTED = "This connector does not support setting arbitrary properties";

    private static JcrEngine engine;
    private static Context jndiContext;

    @Override
    protected RepositorySource setUpSource() throws Exception {
        final String carRepositoryJndiName = "cars repository in jndi";
        final String aircraftRepositoryJndiName = "aircraft repository in jndi";

        if (engine == null) {
            // Set up the JCR engine that the connector will use ...
            engine = JcrConnectorTestUtil.loadEngine();
            Repository carsRepository = engine.getRepository(JcrConnectorTestUtil.CARS_REPOSITORY_NAME);
            Repository aircraftRepository = engine.getRepository(JcrConnectorTestUtil.AIRCRAFT_REPOSITORY_NAME);

            // Set up the mock JNDI context and 'register' the two JCR Repository objects ...
            jndiContext = mock(Context.class);
            when(jndiContext.lookup(carRepositoryJndiName)).thenReturn(carsRepository);
            when(jndiContext.lookup(aircraftRepositoryJndiName)).thenReturn(aircraftRepository);
        }

        // Now create the connector instance ...
        JcrRepositorySource source = new JcrRepositorySource();
        source.setName("Test Repository");
        source.setRepositoryJndiName(carRepositoryJndiName);
        source.setUsername("superuser");
        source.setPassword("superuser");
        // For our tests, use the mock JNDI context ...
        source.setContext(jndiContext);

        return source;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#shutdownRepository()
     */
    @Override
    public void shutdownRepository() throws Exception {
        super.shutdownRepository();
        // Now that the RepositorySource has been closed, shut down the engine ...
        try {
            engine.shutdown();
        } finally {
            engine = null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#cleanUpSourceResources()
     */
    @Override
    protected void cleanUpSourceResources() throws Exception {
        super.cleanUpSourceResources();
    }

    @Override
    protected void initializeContent( Graph graph ) {
        // No setup required
    }

    @Override
    public void afterEach() throws Exception {
        super.afterEach();
    }
}
