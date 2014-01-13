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

import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * A class which provides via CDI injection various test repositories.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class CDIRepositoryProvider {

    @Resource( mappedName = "/jcr/sample" )
    @Produces
    private Repository sampleRepository;

    @RequestScoped
    @Produces
    public Session getCurrentSession() throws RepositoryException {
        return sampleRepository.login();
    }

    public void logoutSession( @Disposes final Session session ) {
        session.logout();
    }
}
