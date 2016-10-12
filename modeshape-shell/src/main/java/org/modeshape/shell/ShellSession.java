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

import javax.jcr.Session;
import org.apache.sshd.server.ExitCallback;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.shell.cmd.jcrsession.JcrSessionInterpreter;
import org.modeshape.shell.cmd.config.ConfigurationInterpreter;
import org.modeshape.shell.cmd.engine.EngineCommandInterpreter;

/**
 *
 * @author kulikov
 */
public class ShellSession {
    
    public final static Interpreter ENGINE_COMMAND_INTERPRETER = new EngineCommandInterpreter();
    public final static Interpreter CONFIGURATION_INTERPRETER = new ConfigurationInterpreter();
    public final static Interpreter JCR_SESSION_INTERPRETER = new JcrSessionInterpreter();
    
    private final ModeShapeEngine engine;
    private ExitCallback exitCallback;
    private Session session;
    private String path;
    private Interpreter interpreter = ENGINE_COMMAND_INTERPRETER;
    private JcrRepository repository;
    private RepositoryConfiguration configuration;
    
    public ShellSession(ModeShapeEngine engine) {
        this.engine = engine;
    }

    public boolean isAlive() {
        return session != null && session.isLive();
    }
    
    public String getId() {
        if (session == null) {
            return null;
        }
        
        String[] tokens = session.toString().split(" ");        
        return tokens[1];
    }
    
    public Session jcrSession() {
        return session;
    }
    
    public void setJcrSession(Session session) {
        this.session = session;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getPath() {
        return path;
    }
    
    public ModeShapeEngine engine() {
        return engine;
    }
    
    public Interpreter getInterpreter() {
        return interpreter;
    }
    
    public void setInterpreter(Interpreter interpreter) {
        this.interpreter = interpreter;
/*        if (factory != null) {
            factory.setPrompt(interpreter.prompt());
        }
        */ 
    }
 
    public JcrRepository getRepository() {
        return repository;
    }
    
    public void setRepository(JcrRepository repository) {
        this.repository = repository;
    }
    
    public void setRepositoryConfiguration(RepositoryConfiguration configuration) {
        this.configuration = configuration;
    }
    
    public RepositoryConfiguration getRepositoryConfiguration() {
        return configuration;
    }
    
    public void setExitCallback(ExitCallback exitCallback) {
        this.exitCallback = exitCallback;
    }
    
    public void exit() {
        exitCallback.onExit(0);
    }
}
