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
package org.modeshape.jcr.jca;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrRepositoryFactory;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.NoSuchRepositoryException;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.RepositoryFactory;

/**
 * JCA resource adaptor implementation.
 * 
 * @author kulikov
 */
public class JcrResourceAdapter implements ResourceAdapter, Serializable {

    private ModeShapeEngine engine;

    private final XAResource[] xaResources = new XAResource[0];
    private Map<String, JcrRepository> repositories = new HashMap();

    private String repositoryURI;

    @Override
    public void start(BootstrapContext bc) throws ResourceAdapterInternalException {
        System.out.println("----- START");
//        start engine
        engine = new ModeShapeEngine();
        engine.start();
    }

    @Override
    public void stop() {
        //shutdown repositories
        for (String name : repositories.keySet()) {
            try {
                engine.shutdownRepository(name);
            } catch (NoSuchRepositoryException e) {
            }
        }
        //shutdown engine
        engine.shutdown();
    }

    public void setRepositoryURI(String repositoryURI) {
        System.out.println("----- SET REPO URI");
        this.repositoryURI = repositoryURI;
    }

    public String getRepositoryURI() {
        return this.repositoryURI;
    }
    
    public Repository createRepository() throws RepositoryException {
/*         Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("org.modeshape.jcr.URL", repositoryURI);

        Repository repository = null;
        for (RepositoryFactory factory : ServiceLoader.load(RepositoryFactory.class)) {
            repository = factory.getRepository(parameters);
            if (repository != null) {
                break;
            }
        }

        return repository; 
*/
        RepositoryConfiguration config = null;
        try {
            config = RepositoryConfiguration.read(repositoryURI);
        } catch (Exception e) {
            throw new RepositoryException(e);
        }

        // Verify the configuration for the repository ...
        Problems problems = config.validate();
        if (problems.hasErrors()) {
            System.err.println("Problems starting the engine.");
            System.err.println(problems);
            throw new RepositoryException("Configuration validation failure");
        }
        
        Repository repository = engine.deploy(config);
        return repository;

    }

    @Override
    public void endpointActivation(MessageEndpointFactory mef, ActivationSpec as) throws ResourceException {
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory mef, ActivationSpec as) {
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] ass) throws ResourceException {
        return xaResources;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        if (!(other instanceof JcrResourceAdapter)) {
            return false;
        }

        return other == this;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        return hash;
    }
}
