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
package org.modeshape.web.server.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletContext;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.web.client.RemoteException;
import org.modeshape.web.server.Connector;
import org.modeshape.web.server.LRepository;
import org.modeshape.web.shared.RepositoryName;

/**
 *
 * @author kulikov
 */
public class JsonConfigConnectorImpl implements Connector {
    
    //this object will be serialized by user session but 
    //we do not want to serialize any data so mark everything as transient
    private static final long serialVersionUID = 1L;
    
    private transient HashMap<String, LRepository> repositories = new HashMap<>();
    
    //names of the available repositories
    private transient Collection<RepositoryName> repositoryNames;
    
    //user's credentials
    private transient Credentials credentials;
    private transient String userName;
    
    private transient ModeShapeEngine engine; 
    private final static Logger logger = Logger.getLogger(JsonConfigConnectorImpl.class);
    
    public JsonConfigConnectorImpl() {
    }
    
    @Override
    public void start(ServletContext context) throws RemoteException {
        String url = context.getInitParameter("config-url");
        
        repositoryNames = new ArrayList<>();
        
        engine = new ModeShapeEngine();
        engine.start();
        
        try {
            RepositoryConfiguration config = RepositoryConfiguration.read(context.getResource(url));
            engine.deploy(config);
            
            for (String name : engine.getRepositoryNames()) {                
                Repository repo = engine.getRepository(name);
                
                StringBuilder builder = new StringBuilder();
                builder.append("<b>Vendor: </b>");
                builder.append(repo.getDescriptor(Repository.REP_VENDOR_DESC));
                builder.append("</br>");

                builder.append("<b>Version: </b>");
                builder.append(repo.getDescriptor(Repository.REP_VERSION_DESC));
                builder.append("</br>");

                builder.append(repo.getDescriptor(Repository.REP_VENDOR_URL_DESC));
                builder.append("</br>");
                
                String descriptor = builder.toString();
                repositoryNames.add(new RepositoryName(name, descriptor));
            }
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }
    
    @Override
    public void login( String username, String password ) {        
        if (username == null) {
            credentials = null;
        }
        
        if (password == null) {
            credentials = new SimpleCredentials(username, null);
        } else {
            credentials = new SimpleCredentials(username, password.toCharArray());
        }
        this.userName = username;
    }

    @Override
    public void logout() {
        credentials = null;
        userName = null;
    }
    
    @Override
    public String userName() {
        return userName;
    }
    
    @Override
    public Collection<RepositoryName> getRepositories() {
        return repositoryNames;
    }

    @Override
    public LRepository find(String name) throws RemoteException {
        if (!repositories.containsKey(name)) {
            try {
                logger.debug("Starting repository: " + name);
                repositories.put(name, new LRepositoryImpl(engine.getRepository(name), credentials));
            } catch (Exception e) {
                logger.debug("Could not start repository " + name, e);
                throw new RemoteException(e.getMessage());
            }
        }
        return repositories.get(name);
    }

    @Override
    public Collection<RepositoryName> search(String name) {
        ArrayList<RepositoryName> list = new ArrayList<>();
        for (RepositoryName n : repositoryNames) {
            if (n.getName().contains(name) || n.getDescriptor().contains(name)) {
                list.add(n);
            }
        }
        return list;
    }
    
    
}
