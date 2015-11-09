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
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.NoSuchRepositoryException;
import org.modeshape.web.shared.RemoteException;
import org.modeshape.web.shared.RepositoryName;

/**
 *
 * @author kulikov
 */
public class RepositoryList {
    private final ModeShapeEngine engine;
    
    public RepositoryList(ModeShapeEngine engine) {
        this.engine = engine;
    }

    @SuppressWarnings("unchecked")
    public Collection<RepositoryName> getRepositories(Credentials creds) throws RemoteException {
        ArrayList<RepositoryName> list = new ArrayList();
            for (String name : engine.getRepositoryNames()) { 
                Repository repo;
                try {
                    repo = engine.getRepository(name);
                    testLogin(repo, creds);
                } catch (NoSuchRepositoryException | LoginException e) {
                    continue;
                } catch (RepositoryException e) {
                    throw new RemoteException(e.getMessage());
                }
                
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
                list.add(new RepositoryName(name, descriptor));
            }
        
        return list;
    }
    
    private void testLogin(Repository repository, Credentials credentials) 
            throws LoginException, RepositoryException {
        //if repository has anonymous access then test is passed
        try {
            repository.login();
        } catch (LoginException e) {
            if (credentials == null) throw e;
            repository.login(credentials);
        }
    }
}
