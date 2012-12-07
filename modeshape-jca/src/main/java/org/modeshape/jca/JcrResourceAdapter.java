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
package org.modeshape.jca;

import java.net.URL;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.Connector;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.TransactionSupport;
import javax.resource.spi.endpoint.MessageEndpointFactory;

import javax.transaction.xa.XAResource;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * JcrResourceAdapter
 *
 * @author kulikov
 */
@Connector(reauthenticationSupport = false,
transactionSupport = TransactionSupport.TransactionSupportLevel.XATransaction)
public class JcrResourceAdapter implements ResourceAdapter, java.io.Serializable {

    /**
     * The serial version UID
     */
    private static final long serialVersionUID = 1L;
    /**
     * repositoryURL
     */
    @ConfigProperty(defaultValue = "file:///home/kulikov/work/my-repository-config.json")
    private String repositoryURL;

    /**
     * Repository instance
     */
    private Repository repository;

    //XA
    private final XAResource[] xaResources = new XAResource[0];

    private ModeShapeEngine engine;

    /**
     * Default constructor
     */
    public JcrResourceAdapter() {
    }

    /**
     * Set repositoryURL
     *
     * @param repositoryURL The value
     */
    public void setRepositoryURL(String repositoryURL) {
        this.repositoryURL = repositoryURL;
    }

    /**
     * Get repositoryURL
     *
     * @return The value
     */
    public String getRepositoryURL() {
        return repositoryURL;
    }

    protected Repository getRepository() throws ResourceException {
        if (this.repository == null) {
            this.repository = deployRepository(repositoryURL);
        }
        return this.repository;
    }

    private boolean isAbsolutePath(String uri) {
        return !(uri.startsWith("jndi") | uri.startsWith("file"));
    }

    private Repository deployRepository(String uri) throws ResourceException {
        if (engine == null) {
            engine = new ModeShapeEngine();
            engine.start();
        }

        //load configuration
        RepositoryConfiguration config = null;
        try {
            URL url = isAbsolutePath(uri) ? getClass().getClassLoader().getResource(uri) : new URL(uri);
            config = RepositoryConfiguration.read(url);
        } catch (Exception e) {
            throw new ResourceException(e);
        }

        //check configuration
        Problems problems = config.validate();
        if (problems.hasErrors()) {
            throw new ResourceException(problems.toString());
        }

        try {
            return engine.deploy(config);
        } catch (RepositoryException e) {
            throw new ResourceException(e);
        }
    }

    /**
     * This is called during the activation of a message endpoint.
     *
     * @param endpointFactory A message endpoint factory instance.
     * @param spec An activation spec JavaBean instance.
     * @throws ResourceException generic exception
     */
    public void endpointActivation(MessageEndpointFactory endpointFactory,
            ActivationSpec spec) throws ResourceException {
    }

    /**
     * This is called when a message endpoint is deactivated.
     *
     * @param endpointFactory A message endpoint factory instance.
     * @param spec An activation spec JavaBean instance.
     */
    public void endpointDeactivation(MessageEndpointFactory endpointFactory,
            ActivationSpec spec) {
    }

    /**
     * This is called when a resource adapter instance is bootstrapped.
     *
     * @param ctx A bootstrap context containing references
     * @throws ResourceAdapterInternalException indicates bootstrap failure.
     */
    public void start(BootstrapContext ctx)
            throws ResourceAdapterInternalException {
        if (engine == null) {
            engine = new ModeShapeEngine();
            engine.start();
        }
    }

    /**
     * This is called when a resource adapter instance is undeployed or during
     * application server shutdown.
     */
    public void stop() {
        if (engine != null) {
            engine.shutdown();
        }
    }

    /**
     * This method is called by the application server during crash recovery.
     *
     * @param specs An array of ActivationSpec JavaBeans
     * @throws ResourceException generic exception
     * @return An array of XAResource objects
     */
    public XAResource[] getXAResources(ActivationSpec[] specs)
            throws ResourceException {
        return xaResources;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        return repositoryURL.hashCode();
    }

    /**
     * Indicates whether some other object is equal to this one.
     *
     * @param other The reference object with which to compare.
     * @return true if this object is the same as the obj argument, false
     * otherwise.
     */
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
}
