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
import javax.naming.InitialContext;
import org.jboss.logging.Logger;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.web.client.RemoteException;
import org.modeshape.web.server.Connector;
import org.modeshape.web.server.LRepository;
import org.modeshape.web.shared.RepositoryName;

/**
 *
 * @author kulikov
 */
public class ConnectorImpl implements Connector {
    private HashMap<String, LRepository> repositories = new HashMap();
    
    //names of the available repositories
    private final Collection<RepositoryName> repositoryNames;
    
    //user's credentials
    private Credentials credentials;
    private String userName;
    private final static Logger logger = Logger.getLogger(ConnectorImpl.class);
    
    public ConnectorImpl() throws RemoteException {
        repositoryNames = new ArrayList();
        ModeShapeEngine engine;
        try {
            InitialContext ic = new InitialContext();
            engine = (ModeShapeEngine) ic.lookup("jcr");
            
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
    public void login(Credentials creds) {
        this.credentials = creds;
        this.userName = creds == null ? null : creds.toString();
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
                logger.info("Starting repository: " + name);
                InitialContext ic = new InitialContext();
                ModeShapeEngine engine = (ModeShapeEngine) ic.lookup("jcr");
                repositories.put(name, new LRepositoryImpl(engine.getRepository(name), credentials));
            } catch (Exception e) {
                logger.error("Could not start repository " + name, e);
                throw new RemoteException(e.getMessage());
            }
        }
        return repositories.get(name);
    }

    @Override
    public Collection<RepositoryName> search(String name) throws RemoteException {
        ArrayList<RepositoryName> list = new ArrayList();
        for (RepositoryName n : repositoryNames) {
            if (n.getName().contains(name) || n.getDescriptor().contains(name)) {
                list.add(n);
            }
        }
        return list;
    }
    
    
}
