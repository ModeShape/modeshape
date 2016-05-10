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
package org.modeshape.test.integration;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * EJB which reads a repository from JNDI (also initializing it) and checks for the existence of some dummy node.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Singleton
@Startup
public class StartupRepositoryProvider1 extends RepositoryProvider {
    
    @PostConstruct
    @TransactionAttribute( TransactionAttributeType.REQUIRED )
    public void run() throws Exception {
        org.modeshape.jcr.api.Repository repository = (org.modeshape.jcr.api.Repository)getRepositoryFromJndi("java:/jcr/sample");
        javax.jcr.Session session = repository.login();
        session.nodeExists("/DUMMY");
    }
}
