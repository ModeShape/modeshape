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

package org.modeshape.web.jcr.rest.handler;

import javax.jcr.Repository;
import javax.jcr.Value;
import javax.servlet.http.HttpServletRequest;
import org.modeshape.web.jcr.RepositoryManager;
import org.modeshape.web.jcr.rest.RestHelper;
import org.modeshape.web.jcr.rest.model.RestRepositories;
import java.util.ArrayList;
import java.util.List;

/**
 * An extension of {@link RepositoryHandler} which returns POJO-based rest model instances.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class RestServerHandler extends AbstractHandler {

    /**
     * @see ServerHandler#getRepositories(javax.servlet.http.HttpServletRequest)
     */
    public RestRepositories getRepositories( HttpServletRequest request ) {
        RestRepositories repositories = new RestRepositories();
        for (String repositoryName : RepositoryManager.getJcrRepositoryNames()) {
            addRepository(request, repositories, repositoryName);
        }
        return repositories;
    }

    private void addRepository( HttpServletRequest request,
                                RestRepositories repositories,
                                String repositoryName ) {
        RestRepositories.Repository repository = repositories.addRepository(repositoryName, RestHelper.urlFrom(request,
                                                                                                               repositoryName));
        try {
            Repository jcrRepository = RepositoryManager.getRepository(repositoryName);
            for (String metadataKey : jcrRepository.getDescriptorKeys()) {
                Value[] descriptorValues = jcrRepository.getDescriptorValues(metadataKey);
                if (descriptorValues != null) {
                    List<String> values = new ArrayList<String>(descriptorValues.length);
                    for (Value descriptorValue : descriptorValues) {
                        values.add(descriptorValue.getString());
                    }
                    repository.addMetadata(metadataKey, values);
                }
            }
        } catch (Exception e) {
            logger.error(e, e.getMessage());
        }
    }

}
