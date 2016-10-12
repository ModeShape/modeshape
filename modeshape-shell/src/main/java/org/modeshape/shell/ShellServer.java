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
package org.modeshape.shell;

import java.io.IOException;
import java.util.Properties;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.ModeShapeEngine;

/**
 * Wraps shell console with secure shell server.
 *
 * @author kulikov
 */
public class ShellServer {

    private final SshServer sshd = SshServer.setUpDefaultServer();
    private final ModeShapeEngine engine;
    private final Properties config;
    private static final Logger LOGGER = Logger.getLogger(ShellServer.class);

    /**
     * Creates instance of the server.
     *
     * @param engine engine instance running this server.
     * @param config bootstrap parameters.
     */
    public ShellServer(ModeShapeEngine engine, Properties config) {
        this.engine = engine;
        this.config = config;
    }

    /**
     * Starts this server.
     *
     */
    public void start() {
        if (config == null) {
            LOGGER.warn(ShellI18n.sshUserNotSpecified);
            return;
        }
        int port = 2222;
        if (config.containsKey("ssh.port")) {
            port = Integer.parseInt(config.getProperty("ssh.port"));
        }
        sshd.setPort(port);

        String user = config.getProperty("ssh.user");
        String password = config.getProperty("ssh.password");

        sshd.setPasswordAuthenticator(new ShellAuthenticator(user, password));
        sshd.setShellFactory(new FactoryImpl());

        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        if (user == null || password == null) {
            LOGGER.warn(ShellI18n.sshUserNotSpecified);
            return;
        }

        try {
            sshd.start();
        } catch (IOException e) {
            LOGGER.warn(ShellI18n.sshCouldNotStartServer, e.getMessage());
        }
    }

    private class FactoryImpl implements Factory<Command> {
        @Override
        public Command create() {
            return new ShellCommandProcessor(new ShellSession(engine));
        }
    }

    /**
     * Stops this server immediately.
     */
    public void stop() {
        try {
            sshd.stop(true);
        } catch (InterruptedException e) {
        }
    }
}
