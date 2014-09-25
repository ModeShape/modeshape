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
package org.modeshape.web.server.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;
import javax.naming.InitialContext;
import org.modeshape.common.logging.Logger;
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
    //this object will be serialized by user session but 
    //we do not want to serialize any data so mark everything as transient
    private static final long serialVersionUID = 1L;
    
    private transient HashMap<String, LRepository> repositories = new HashMap<String, LRepository>();
    
    //names of the available repositories
    private transient final Collection<RepositoryName> repositoryNames;
    
    //user's credentials
    private transient Credentials credentials;
    private transient String userName;
    
    private final static Logger logger = Logger.getLogger(ConnectorImpl.class);
    
    public ConnectorImpl() throws RemoteException {
        repositoryNames = new ArrayList<RepositoryName>();
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
                InitialContext ic = new InitialContext();
                ModeShapeEngine engine = (ModeShapeEngine) ic.lookup("jcr");
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
        ArrayList<RepositoryName> list = new ArrayList<RepositoryName>();
        for (RepositoryName n : repositoryNames) {
            if (n.getName().contains(name) || n.getDescriptor().contains(name)) {
                list.add(n);
            }
        }
        return list;
    }
    
    
}
