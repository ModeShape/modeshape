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
package org.modeshape.example.repository;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.IsCollectionContaining.hasItems;
import static org.mockito.Mockito.when;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.security.auth.callback.CallbackHandler;
import org.jboss.security.config.IDTrustConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.graph.JaasSecurityContext;
import org.modeshape.graph.property.Binary;

/**
 * @author Randall Hauch
 */
public class RepositoryClientTest {

    private RepositoryClient client;
    private Map<String, Object[]> properties;
    private Map<String, Object> uuids;
    private List<String> children;
    @Mock
    private UserInterface userInterface;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        uuids = new HashMap<String, Object>();
        properties = new HashMap<String, Object[]>();
        children = new ArrayList<String>();
        client = new RepositoryClient();
        client.setUserInterface(userInterface);
        client.setApi(getApi());
        when(userInterface.getLocationOfRepositoryFiles()).thenReturn(new File("src/main/resources").getAbsolutePath());
        when(userInterface.getRepositoryConfiguration()).thenReturn(new File("src/main/resources/configRepository.xml"));

        // Set up the JAAS provider (IDTrust) and a policy file (which defines the "modeshape-jcr" login config name)
        IDTrustConfiguration idtrustConfig = new IDTrustConfiguration();
        try {
            idtrustConfig.config("security/jaas.conf.xml");
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        CallbackHandler handler = new JaasSecurityContext.UserPasswordCallbackHandler("jsmith", "secret".toCharArray());
        when(userInterface.getCallbackHandler()).thenReturn(handler);
    }

    @After
    public void afterEach() throws Exception {
        client.shutdown();
    }

    protected RepositoryClient.Api getApi() {
        return RepositoryClient.Api.ModeShape;
    }

    protected void assertProperty( String propertyName,
                                   Object... values ) {
        Object[] actualValues = properties.get(propertyName);
        assertThat(actualValues, is(values));
    }

    protected String string( Object rawValue ) {
        if (rawValue instanceof Binary) {
            return new String(((Binary)rawValue).getBytes());
        }

        return rawValue.toString();
    }

    protected void getNodeInfo( String source,
                                String path ) throws Throwable {
        properties.clear();
        children.clear();
        assertThat(client.getNodeInfo(source, path, properties, children), is(true));
        // Record the UUID of the node ...
        final Object uuid = properties.get("dna:uuid");
        final String key = source + "\n" + path;
        final Object existingUuid = uuids.get(key);
        if (existingUuid != null) {
            assertThat(uuid, is(existingUuid));
        } else {
            uuids.put(key, uuid);
        }
    }

    @Test
    public void shouldStartupWithoutError() throws Exception {
        client.startRepositories();
        assertThat(client.getNamesOfRepositories(), hasItems("Aircraft", "Cars", "Vehicles"));
    }

    @Test
    public void shouldStartupWithoutErrorMoreThanOnce() throws Exception {
        client.startRepositories();
        assertThat(client.getNamesOfRepositories(), hasItems("Aircraft", "Cars", "Vehicles"));
    }

    @Ignore
    @Test
    public void shouldHaveContentFromConfigurationRepository() throws Throwable {
        client.startRepositories();

        getNodeInfo("Configuration", "/jcr:system");
        // assertThat(children, hasItems("dna:sources", "dna:federatedRepositories"));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));
        // assertThat(properties.containsKey("dna:uuid"), is(true));
        assertThat(properties.size() >= 1, is(true));

        getNodeInfo("Configuration", "/jcr:system/dna:sources");
        assertThat(children, hasItems("SourceA", "SourceB", "SourceC", "SourceD"));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));
        // assertThat(properties.containsKey("dna:uuid"), is(true));
        assertThat(properties.size() >= 1, is(true));

        getNodeInfo("Configuration", "/jcr:system/dna:sources/SourceA");
        assertThat(children.size(), is(0));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));
        // assertThat(properties.containsKey("dna:uuid"), is(true));
        assertProperty("dna:classname", org.modeshape.graph.connector.inmemory.InMemoryRepositorySource.class.getName());
        assertProperty("dna:name", "Cars");
        assertProperty("dna:retryLimit", "3");
        assertProperty("defaultWorkspaceName", "default");
        assertThat(properties.size() >= 5, is(true));
    }

    @Test
    public void shouldHaveContentFromCarsRepository() throws Throwable {
        client.startRepositories();

        getNodeInfo("Cars", "/Cars");
        assertThat(children, hasItems("Hybrid", "Sports", "Luxury", "Utility"));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));
        switch (getApi()) {
            case ModeShape:
                assertThat(properties.containsKey("mode:uuid"), is(true));
                assertThat(properties.size(), is(2));
                break;
            case JCR:
                assertThat(properties.size(), is(1));
                break;
        }

        getNodeInfo("Cars", "/Cars/Hybrid");
        assertThat(children, hasItems("Toyota Prius", "Toyota Highlander", "Nissan Altima"));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));
        switch (getApi()) {
            case ModeShape:
                assertThat(properties.containsKey("mode:uuid"), is(true));
                assertThat(properties.size(), is(2));
                break;
            case JCR:
                assertThat(properties.size(), is(1));
                break;
        }

        getNodeInfo("Cars", "/Cars/Sports/Aston Martin DB9");
        assertThat(children.size(), is(0));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));
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
        switch (getApi()) {
            case ModeShape:
                assertThat(properties.containsKey("mode:uuid"), is(true));
                assertThat(properties.size(), is(12));
                break;
            case JCR:
                assertThat(properties.size(), is(11));
                break;
        }
    }

    @Test
    public void shouldHaveContentFromAircraftRepository() throws Throwable {
        client.startRepositories();

        getNodeInfo("Aircraft", "/Aircraft");
        assertThat(children, hasItems("Business", "Commercial", "Vintage", "Homebuilt"));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));
        // assertThat(properties.containsKey("mode:uuid"), is(true)); // not referenceable in JCR

        getNodeInfo("Aircraft", "/Aircraft/Commercial");
        assertThat(children, hasItems("Boeing 777",
                                      "Boeing 767",
                                      "Boeing 787",
                                      "Boeing 757",
                                      "Airbus A380",
                                      "Airbus A340",
                                      "Airbus A310",
                                      "Embraer RJ-175"));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));
        // assertThat(properties.containsKey("mode:uuid"), is(true)); // not referenceable in JCR

        getNodeInfo("Aircraft", "/Aircraft/Vintage/Wright Flyer");
        assertThat(children.size(), is(0));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));
        // assertThat(properties.containsKey("mode:uuid"), is(true));
        assertProperty("maker", "Wright Brothers");
        assertProperty("introduced", "1903");
        assertProperty("range", "852ft");
        assertProperty("maxSpeed", "30mph");
        assertProperty("emptyWeight", "605lb");
        assertProperty("crew", "1");
    }

    @Test
    public void shouldHaveContentFromUfosRepository() throws Throwable {
        client.startRepositories();

        getNodeInfo("UFOs", "/");
        assertThat(children, hasItems("martians", "venutians", "README.txt"));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));

        getNodeInfo("UFOs", "/martians/zzyzx.txt");
        assertThat(children, hasItems("jcr:content"));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));

        getNodeInfo("UFOs", "/martians/zzyzx.txt/jcr:content");
        assertThat(children.size(), is(0));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));

        assertThat(string(properties.get("jcr:data")[0]).startsWith("The Zzyzx is the fastest vehicle in the Martian fleet."),
                   is(true));

    }

    @Test
    public void shouldHaveContentFromVehiclesRepository() throws Throwable {
        client.startRepositories();

        getNodeInfo("Vehicles", "/");
        // The non-JCR configuration does not have a "jcr:system"
        // assertThat(children, hasItems("Vehicles", "jcr:system"));

        getNodeInfo("Vehicles", "/Vehicles");
        assertThat(children, hasItems("Cars", "Aircraft"));

        getNodeInfo("Vehicles", "/");
        // assertThat(children, hasItems("Vehicles", "jcr:system"));

        getNodeInfo("Vehicles", "/Vehicles/Cars/Hybrid");
        assertThat(children, hasItems("Toyota Prius", "Toyota Highlander", "Nissan Altima"));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));

        getNodeInfo("Vehicles", "/Vehicles/Cars/Sports/Aston Martin DB9");
        assertThat(children.size(), is(0));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));
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

        getNodeInfo("Vehicles", "/Vehicles/Aircraft");

        getNodeInfo("Vehicles", "/Vehicles/Aircraft/Vintage/Wright Flyer");
        assertThat(children.size(), is(0));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));
        assertProperty("maker", "Wright Brothers");
        assertProperty("introduced", "1903");
        assertProperty("range", "852ft");
        assertProperty("maxSpeed", "30mph");
        assertProperty("emptyWeight", "605lb");
        assertProperty("crew", "1");

        getNodeInfo("Vehicles", "/Vehicles/UFOs/martians/zzyzx.txt/jcr:content");
        assertThat(children.size(), is(0));
        assertThat(properties.containsKey("jcr:primaryType"), is(true));
        assertThat(string(properties.get("jcr:data")[0]).startsWith("The Zzyzx is the fastest vehicle in the Martian fleet."),
                   is(true));
    }

    @Test
    public void shouldReturnNullForNonExistantNode() throws Throwable {
        client.startRepositories();
        assertThat(client.getNodeInfo("Cars", "/Cars/Sports/Non Existant Car", properties, children), is(false));
    }

    @Ignore
    @Test
    public void shouldNotFreezeIfWrongNodeEntered() throws Throwable {
        client.startRepositories();

        Map<String, Object[]> properties = new HashMap<String, Object[]>();
        List<String> children = new ArrayList<String>();
        client.getNodeInfo("Vehicles", "/Vehicles/foo", properties, children);
    }

    @Test
    public void shouldBeAbleToExecuteTestsRepeatedly() throws Throwable {
        for (int i = 0; i != 5; ++i) {
            // shouldHaveContentFromConfigurationRepository();
            shouldHaveContentFromCarsRepository();
            shouldHaveContentFromAircraftRepository();
            shouldHaveContentFromUfosRepository();
            shouldHaveContentFromVehiclesRepository();
        }
    }
}
