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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.Connector;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.TransactionSupport;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.ModeShapeEngine;

/**
 * JcrResourceAdapter
 * 
 * @author kulikov
 */
@Connector( reauthenticationSupport = false, transactionSupport = TransactionSupport.TransactionSupportLevel.XATransaction )
public class JcrResourceAdapter implements ResourceAdapter, java.io.Serializable {

    /**
     * The serial version UID
     */
    private static final long serialVersionUID = 1L;
    // XA
    private final XAResource[] xaResources = new XAResource[0];
    
    private static final Logger LOGGER = Logger.getLogger(JcrResourceAdapter.class);

    private ModeShapeEngine engine;

    /**
     * Default constructor
     */
    public JcrResourceAdapter() {
    }

    /**
     * This is called during the activation of a message endpoint.
     * 
     * @param endpointFactory A message endpoint factory instance.
     * @param spec An activation spec JavaBean instance.
     * @throws ResourceException generic exception
     */
    @Override
    public void endpointActivation( MessageEndpointFactory endpointFactory,
                                    ActivationSpec spec ) throws ResourceException {
    }

    /**
     * This is called when a message endpoint is deactivated.
     * 
     * @param endpointFactory A message endpoint factory instance.
     * @param spec An activation spec JavaBean instance.
     */
    @Override
    public void endpointDeactivation( MessageEndpointFactory endpointFactory,
                                      ActivationSpec spec ) {
    }

    /**
     * This is called when a resource adapter instance is bootstrapped.
     * 
     * @param ctx A bootstrap context containing references
     * @throws ResourceAdapterInternalException indicates bootstrap failure.
     */
    @Override
    public synchronized void start( BootstrapContext ctx ) throws ResourceAdapterInternalException {
        if (engine == null) {
            engine = new ModeShapeEngine();
            engine.start();
        }
    }

    /**
     * This is called when a resource adapter instance is undeployed or during application server shutdown.
     */
    @Override
    public synchronized void stop() {
        if (engine != null) {
            Future<Boolean> shutdown = engine.shutdown();
            try {
                LOGGER.debug("Shutting down engine to stop resource adapter");
                if ( ! shutdown.get(30, TimeUnit.SECONDS)) {
                    LOGGER.error(JcrI18n.errorWhileShuttingDownEngineInJndi, "");
                }
            } catch (InterruptedException e) {
                LOGGER.error(e, JcrI18n.errorWhileShuttingDownEngineInJndi, "");
            } catch (ExecutionException e) {
                LOGGER.error(e, JcrI18n.errorWhileShuttingDownEngineInJndi, "");
            } catch (TimeoutException e) {
                LOGGER.error(e, JcrI18n.timeoutWhileShuttingRepositoryDown);
            }
            engine = null;
        }
    }

    /**
     * This method is called by the application server during crash recovery.
     * 
     * @param specs An array of ActivationSpec JavaBeans
     * @throws ResourceException generic exception
     * @return An array of XAResource objects
     */
    @Override
    public XAResource[] getXAResources( ActivationSpec[] specs ) throws ResourceException {
        return xaResources;
    }

    /**
     * Indicates whether some other object is equal to this one.
     * 
     * @param other The reference object with which to compare.
     * @return true if this object is the same as the obj argument, false otherwise.
     */
    @Override
    public boolean equals( Object other ) {
        if (other == this) {
            return true;
        }
        if (other instanceof JcrManagedConnectionFactory) {
            return this == other;
        }
        return false;
    }

    /**
     * Calculates the hashcode for this object.
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public synchronized ModeShapeEngine getEngine() {
        return engine;
    }
}
