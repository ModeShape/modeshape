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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrRepository.Option;

/**
 *
 */
public class ManagedEngineTest {

    private static Set<String> REPO_NAMES = null;

    private ManagedEngine me;

    private JcrConfiguration configuration;
    private JcrEngine engine;

    @BeforeClass
    public static void beforeAll() throws Exception {
        REPO_NAMES = new HashSet<String>();
        REPO_NAMES.add("JCR Repository");

    }

    @Before
    public void beforeEach() throws Exception {
        configuration = new JcrConfiguration();

        configuration.repositorySource("Source2")
                     .usingClass(InMemoryRepositorySource.class.getName())
                     .loadedFromClasspath()
                     .setDescription("description")
                     .and()
                     .repository("JCR Repository")
                     .setSource("Source2")
                     .setOption(Option.JAAS_LOGIN_CONFIG_NAME, "test")
                     .and()
                     .save();

        // Start the engine ...
        engine = configuration.build();

        me = new ManagedEngine(engine);
        me.start();
    }

    @After
    public void afterEach() throws Exception {
        if (engine != null) {
            try {
                engine.shutdown();
                engine.awaitTermination(3, TimeUnit.SECONDS);
            } finally {
                engine = null;
            }
        }
    }

    @Test
    public void shouldBeRunning() throws Exception {
        assertThat(me.isRunning(), is(true));
    }

    @Test
    public void shouldGetRepository() throws Exception {
        assertThat(me.getRepository(REPO_NAMES.iterator().next()), is(notNullValue()));
    }

    @Test
    public void shouldGetSequencingService() throws Exception {
        assertThat(me.getSequencingService(), is(notNullValue()));
    }

    @Test
    public void shouldGetRepositories() throws Exception {
        assertThat(me.getRepositories().size(), is(REPO_NAMES.size()));

    }
}
