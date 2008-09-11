/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.example.dna.repository;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.IsCollectionContaining.hasItems;
import static org.mockito.Mockito.stub;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.dna.common.util.StringUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class RepositoryClientTest {

    private RepositoryClient client;
    private Map<String, Object[]> properties;
    private List<String> children;
    @Mock
    private UserInterface userInterface;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        properties = new HashMap<String, Object[]>();
        children = new ArrayList<String>();
        client = new RepositoryClient();
        client.setUserInterface(userInterface);
        stub(userInterface.getLocationOfRepositoryFiles()).toReturn(new File("src/main/resources").getAbsolutePath());
    }

    @After
    public void afterEach() throws Exception {
        client.shutdown();
    }

    protected void assertProperty( String propertyName,
                                   Object... values ) {
        Object[] actualValues = properties.get(propertyName);
        assertThat(actualValues, is(values));
    }

    @Test
    public void shouldStartupWithoutError() throws Exception {
        client.startRepositories();
        assertThat(client.getNamesOfRepositories(), hasItems("Aircraft", "Cars", "Configuration", "Vehicles", "Cache"));
    }

    @Test
    public void shouldStartupWithoutErrorMoreThanOnce() throws Exception {
        client.startRepositories();
        assertThat(client.getNamesOfRepositories(), hasItems("Aircraft", "Cars", "Configuration", "Vehicles", "Cache"));
    }

    @Ignore
    @Test
    public void shouldHaveContentFromConfigurationRepository() throws Throwable {
        client.startRepositories();
        assertThat(client.getNodeInfo("Configuration", "/dna:system", properties, children), is(true));
        assertThat(children, hasItems("dna:sources", "dna:federatedRepositories"));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));
        assertThat(properties.containsKey("dna:uuid"), is(true));
        assertThat(properties.size(), is(2));

        properties.clear();
        children.clear();
        assertThat(client.getNodeInfo("Configuration", "/dna:system/dna:sources", properties, children), is(true));
        assertThat(children, hasItems("sourceA", "sourceB", "sourceC", "sourceD"));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));
        assertThat(properties.containsKey("dna:uuid"), is(true));
        assertThat(properties.size(), is(2));

        properties.clear();
        children.clear();
        assertThat(client.getNodeInfo("Configuration", "/dna:system/dna:sources/sourceA", properties, children), is(true));
        assertThat(children.size(), is(0));
        System.out.println(StringUtil.readableString(properties));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));
        assertThat(properties.containsKey("dna:uuid"), is(true));
        assertProperty("dna:classname", "org.jboss.dna.connector.inmemory.InMemoryRepositorySource");
        assertProperty("name", "Cars");
        assertProperty("retryLimit", "3");
        assertThat(properties.size(), is(4));
    }

    @Test
    public void shouldLoadCarsRepository() throws Throwable {
        client.startRepositories();
        assertThat(client.getNodeInfo("Cars", "/Cars", properties, children), is(true));
        assertThat(children, hasItems("Hybrid", "Sports", "Luxury", "Utility"));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));
        assertThat(properties.containsKey("dna:uuid"), is(true));
        assertThat(properties.size(), is(2));

        properties.clear();
        children.clear();
        assertThat(client.getNodeInfo("Cars", "/Cars/Hybrid", properties, children), is(true));
        assertThat(children, hasItems("Toyota Prius", "Toyota Highlander", "Nissan Altima"));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));
        assertThat(properties.containsKey("dna:uuid"), is(true));
        assertThat(properties.size(), is(2));

        properties.clear();
        children.clear();
        assertThat(client.getNodeInfo("Cars", "/Cars/Sports/Aston Martin DB9", properties, children), is(true));
        assertThat(children.size(), is(0));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));
        assertThat(properties.containsKey("dna:uuid"), is(true));
        assertProperty("maker", "Aston Martin");
        assertProperty("maker", "Aston Martin");
        assertProperty("model", "DB9");
        assertProperty("year", "2008");
        assertProperty("msrp", "$171,600");
        assertProperty("userRating", "5");
        assertProperty("mpgCity", "12");
        assertProperty("mpgHighway", "19");
        assertProperty("lengthInInches", "185.5");
        assertProperty("wheelbaseInInches", "108.0");
        assertProperty("engine", "5,935 cc 5.9 liters V 12");
        assertThat(properties.size(), is(12));
    }

    @Test
    public void shouldReturnNullForNonExistantNode() throws Throwable {
        client.startRepositories();
        assertThat(client.getNodeInfo("Cars", "/Cars/Sports/Non Existant Car", properties, children), is(false));
    }
}
