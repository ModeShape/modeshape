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
package org.jboss.dna.web.jcr.rest.client;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.jboss.dna.web.jcr.rest.client.domain.Repository;
import org.jboss.dna.web.jcr.rest.client.domain.Server;
import org.junit.Before;
import org.junit.Test;

/**
 * The <code>ServerManagerTest</code> class is a test class for the {@link ServerManager server manager} object.
 */
public final class ServerManagerTest {

    // ===========================================================================================================================
    // Constants
    // ===========================================================================================================================

    private static final String URL1 = "file:/tmp/temp.txt"; //$NON-NLS-1$
    private static final String URL2 = "http:www.redhat.com"; //$NON-NLS-1$

    private static final String USER1 = "user1"; //$NON-NLS-1$
    private static final String USER2 = "user2"; //$NON-NLS-1$

    private static final String PSWD1 = "pwsd1"; //$NON-NLS-1$
    private static final String PSWD2 = "pwsd2"; //$NON-NLS-1$

    private static Server SERVER1 = new Server(URL1, USER1, PSWD1, false);
    private static Server SERVER1_UPDATE = new Server(SERVER1.getUrl(), SERVER1.getUser(), SERVER1.getPassword(),
                                                      SERVER1.isPasswordBeingPersisted());
    private static Server SERVER2 = new Server(URL2, USER2, PSWD2, !SERVER1.isPasswordBeingPersisted());

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    private ServerManager serverManager;

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    @Before
    public void beforeEach() {
        this.serverManager = new ServerManager(null, new MockRestClient());
    }

    // ===========================================================================================================================
    // Tests
    // ===========================================================================================================================

    @Test
    public void shouldBeRegisteredIfAdded() {
        this.serverManager.addServer(SERVER1);
        assertThat(this.serverManager.isRegistered(SERVER1), is(true));
        assertThat(this.serverManager.getServers().size(), is(1));
    }

    @Test
    public void shouldBeRegisteredIfServerWithSameKeyHasBeenAdded() {
        this.serverManager.addServer(SERVER1);
        assertThat(this.serverManager.isRegistered(new Server(SERVER1.getUrl(), SERVER1.getUser(), PSWD2,
                                                              !SERVER1.isPasswordBeingPersisted())), is(true));
        assertThat(this.serverManager.getServers().size(), is(1));
    }

    @Test
    public void shouldDecreaseRegistrySizeWhenServerRemoved() {
        this.serverManager.addServer(SERVER1);
        this.serverManager.addServer(SERVER2);

        this.serverManager.removeServer(SERVER1);
        assertThat(this.serverManager.getServers().size(), is(1));

        this.serverManager.removeServer(SERVER2);
        assertThat(this.serverManager.getServers().isEmpty(), is(true));
    }

    @Test
    public void shouldHaveOkStatusWhenAddingServerSuccessfully() {
        assertThat(this.serverManager.addServer(SERVER1).isOk(), is(true));
        assertThat(this.serverManager.getServers().size(), is(1));
    }

    @Test
    public void shouldHaveOkStatusWhenRemovingServerSuccessfully() {
        this.serverManager.addServer(SERVER1);
        assertThat(this.serverManager.removeServer(SERVER1).isOk(), is(true));
        assertThat(this.serverManager.getServers().isEmpty(), is(true));
    }

    @Test
    public void shouldHaveOkStatusWhenUpdateServerSuccessfully() {
        this.serverManager.addServer(SERVER1);
        assertThat(this.serverManager.updateServer(SERVER1, SERVER1_UPDATE).isOk(), is(true));
        assertThat(this.serverManager.getServers().size(), is(1));
    }

    @Test
    public void shouldIncreaseRegistrySizeWhenServerAdded() {
        this.serverManager.addServer(SERVER1);
        assertThat(this.serverManager.getServers().size(), is(1));

        this.serverManager.addServer(SERVER2);
        assertThat(this.serverManager.getServers().size(), is(2));
    }

    @Test
    public void shouldNotAddServerIfAlreadyAdded() {
        this.serverManager.addServer(SERVER1);
        assertThat(this.serverManager.addServer(SERVER1).isOk(), is(false));
        assertThat(this.serverManager.getServers().size(), is(1));
    }

    @Test
    public void shouldNotAddServerIfKeysMatch() {
        this.serverManager.addServer(SERVER1);
        Status status = this.serverManager.addServer(new Server(SERVER1.getUrl(), SERVER1.getUser(), PSWD2,
                                                                !SERVER1.isPasswordBeingPersisted()));
        assertThat(status.isOk(), is(false));
        assertThat(this.serverManager.getServers().size(), is(1));
    }

    @Test
    public void shouldNotBeRegisteredIfNotAdded() {
        this.serverManager.addServer(SERVER1);
        assertThat(this.serverManager.isRegistered(SERVER2), is(false));
        assertThat(this.serverManager.getServers().size(), is(1));
    }

    @Test
    public void shouldNotBeRegisteredIfRemoved() {
        this.serverManager.addServer(SERVER1);
        this.serverManager.removeServer(SERVER1);
        assertThat(this.serverManager.isRegistered(SERVER1), is(false));
        assertThat(this.serverManager.getServers().isEmpty(), is(true));
    }

    @Test
    public void shouldNotReceiveNotificationIfListenerUnregistered() {
        RegistryListener listener = new RegistryListener();

        this.serverManager.addRegistryListener(listener);
        this.serverManager.removeRegistryListener(listener);
        this.serverManager.addServer(SERVER1);
        assertThat(listener.wasNotified(), is(false));
    }

    @Test
    public void shouldNotRemoveServerIfNotAdded() {
        assertThat(this.serverManager.removeServer(SERVER1).isOk(), is(false));
        assertThat(this.serverManager.getServers().isEmpty(), is(true));
    }

    @Test( expected = RuntimeException.class )
    public void shouldNotObtainRepositoriesForUnregisteredServer() throws Exception {
        this.serverManager.getRepositories(SERVER1);
    }

    @Test( expected = RuntimeException.class )
    public void shouldNotObtainWorkspacesForUnregisteredServer() throws Exception {
        Repository repository = new Repository("repo", SERVER1);//$NON-NLS-1$
        this.serverManager.getWorkspaces(repository);
    }

    @Test
    public void shouldReceiveOneEventIfRegisteredMoreThanOnce() {
        RegistryListener listener = new RegistryListener();
        this.serverManager.addRegistryListener(listener);
        this.serverManager.addRegistryListener(listener);

        this.serverManager.addServer(SERVER1);
        assertThat(listener.wasNotified(), is(true));
    }

    @Test
    public void shouldReceiveNotificationIfRegisteredListener() {
        RegistryListener listener = new RegistryListener();
        assertThat(this.serverManager.addRegistryListener(listener), is(true));

        this.serverManager.addServer(SERVER1);
        assertThat(listener.wasNotified(), is(true));
    }

    @Test
    public void shouldReceiveNewServerEvent() {
        RegistryListener listener = new RegistryListener();
        this.serverManager.addRegistryListener(listener);

        this.serverManager.addServer(SERVER1);
        assertThat(listener.getEvent().isNew(), is(true));
        assertThat(listener.getEvent().isRemove(), is(false));
        assertThat(listener.getEvent().isUpdate(), is(false));
    }

    @Test
    public void shouldReceiveRemoveServerEvent() {
        this.serverManager.addServer(SERVER1);

        RegistryListener listener = new RegistryListener();
        this.serverManager.addRegistryListener(listener);

        this.serverManager.removeServer(SERVER1);
        assertThat(listener.getEvent().isRemove(), is(true));
        assertThat(listener.getEvent().isNew(), is(false));
        assertThat(listener.getEvent().isUpdate(), is(false));
    }

    @Test
    public void shouldReceiveUpdateServerEvent() {
        this.serverManager.addServer(SERVER1);

        RegistryListener listener = new RegistryListener();
        this.serverManager.addRegistryListener(listener);

        this.serverManager.updateServer(SERVER1, new Server(SERVER1.getUrl(), SERVER1.getUser(), PSWD2,
                                                            !SERVER1.isPasswordBeingPersisted()));
        assertThat(listener.getEvent().isUpdate(), is(true));
        assertThat(listener.getEvent().isNew(), is(false));
        assertThat(listener.getEvent().isRemove(), is(false));
    }

    @Test
    public void shouldRemoveServerIfNotAddedButKeyMatches() {
        this.serverManager.addServer(SERVER1_UPDATE);
        assertThat(this.serverManager.removeServer(SERVER1).isOk(), is(true));
        assertThat(this.serverManager.getServers().isEmpty(), is(true));
    }

    // ===========================================================================================================================
    // RegistryListener Inner Class
    // ===========================================================================================================================

    class RegistryListener implements IServerRegistryListener {
        boolean[] notified = new boolean[] {false};
        ServerRegistryEvent event = null;

        public Exception[] serverRegistryChanged( ServerRegistryEvent event ) {
            notified[0] = !notified[0];
            this.event = event;
            return null;
        }

        public ServerRegistryEvent getEvent() {
            return this.event;
        }

        public boolean wasNotified() {
            return notified[0];
        }
    }

}
