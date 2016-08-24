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
package org.modeshape.jcr;

import java.io.IOException;
import java.io.InputStream;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Json;

/**
 * @author okulikov
 */
public class TestRepository {
    
    private final String configName = "config/test-repository.json";
    private RepositoryConfiguration config;
    private ModeShapeEngine engine;
    
    private boolean dropOnExit;
    private boolean createOnStart = true;
    private String cnd = "cnd/cars.cnd";
    
    private JcrRepository repository;
    
    public void start() throws Exception {
        System.setProperty("dropOnExit", Boolean.toString(dropOnExit));
        System.setProperty("createOnStart", Boolean.toString(createOnStart));
        System.setProperty("node.types", cnd);
        
        InputStream configStream = getClass().getClassLoader().getResourceAsStream(configName);
        Document configDoc = Json.read(configStream);
        
        config = new RepositoryConfiguration(configDoc, configName);
        engine = new ModeShapeEngine();
        engine.start();
        engine.deploy(config);
        repository = engine.startRepository(config.getName()).get();
    }
    
    public void setDropOnExit(boolean dropOnExit) {
        this.dropOnExit = dropOnExit;
    }
    
    public void setCreateOnStart(boolean createOnStart) {
        this.createOnStart = createOnStart;
    }
    
    public void setCnd(String cnd) {
        this.cnd = cnd;
    }
    
    public Session login() throws RepositoryException {
        return repository.login();
    }
    
    public Session login(String workspace) throws RepositoryException {
        return repository.login(workspace);
    }
    
    public Repository repository() {
        return repository;
    }
    
    public void shutdown() {
       TestingUtil.killRepository(repository);
    }
    
    public void restart() throws Exception {
        shutdown();
        start();
    }
    
    
    public void loadInitialContent(String resourceName, Session session) throws RepositoryException, IOException {
        InputStream stream = TestRepository.class.getClassLoader().getResourceAsStream(resourceName);
        Workspace workspace = session.getWorkspace();
        workspace.importXML("/", stream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
    }
}
