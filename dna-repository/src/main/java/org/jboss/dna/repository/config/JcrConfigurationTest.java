/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.repository.config;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.net.URL;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.junit.Test;

/**
 * 
 */
public class JcrConfigurationTest {

    @Test
    public void shoulConfigureDnaFromFileUsingPath() throws Exception {
        DnaConfiguration config = new DnaConfiguration();
        config.loadFrom("some path");

        DnaEngine engine = config.build();
        assertThat(engine, is(notNullValue()));
    }

    @Test
    public void shoulConfigureDnaFromFile() throws Exception {
        DnaConfiguration config = new DnaConfiguration();
        config.loadFrom(new File("file:some path"));

        DnaEngine engine = config.build();
        assertThat(engine, is(notNullValue()));
    }

    @Test
    public void shoulConfigureDnaFromFileUsingURL() throws Exception {
        DnaConfiguration config = new DnaConfiguration();
        config.loadFrom(new URL("file:some path"));

        DnaEngine engine = config.build();
        assertThat(engine, is(notNullValue()));
    }

    @Test
    public void shoulConfigureDnaFromRepositorySource() throws Exception {
        InMemoryRepositorySource source = new InMemoryRepositorySource();
        DnaConfiguration config = new DnaConfiguration();
        config.loadFrom(source);

        DnaEngine engine = config.build();
        assertThat(engine, is(notNullValue()));
    }

    @Test
    public void shouldBeConfigurableAsDnaConfiguration() throws Exception {
        DnaConfiguration config = new DnaConfiguration();
        assertThat(config.loadFrom("some path"), is(sameInstance(config)));
        assertThat(config.addMimeTypeDetector("something").and(), is(sameInstance(config)));

        DnaEngine engine = config.build();
        assertThat(engine, is(notNullValue()));
    }

    @Test
    public void shouldBeConfigurableAsJcrConfiguration() throws Exception {
        JcrConfiguration config = new JcrConfiguration();
        config.loadFrom("some path").addRepository("some repository ID");
        assertThat(config.loadFrom("some path"), is(sameInstance(config)));
        assertThat(config.addMimeTypeDetector("something").and(), is(sameInstance(config)));

        JcrEngine engine = config.build();
        assertThat(engine, is(notNullValue()));
    }

}
