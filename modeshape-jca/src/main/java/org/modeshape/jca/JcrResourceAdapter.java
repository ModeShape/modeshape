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
    private static final Logger LOGGER = Logger.getLogger(JcrResourceAdapter.class);

    //XA
    private final XAResource[] xaResources = new XAResource[0];
    

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
            final int SHUTDOWN_TIMEOUT = 30;
            try {
                LOGGER.debug("Shutting down engine to stop resource adapter");
                if ( ! shutdown.get(SHUTDOWN_TIMEOUT, TimeUnit.SECONDS)) {
                    // ModeShapeEngine somehow remained running, nothing to be done about it.
                    LOGGER.error(JcaI18n.unableToStopEngine);
                }
            } catch (TimeoutException e) {
                // Exception can be expected, but stack trace is logged to find where limit was defined
                LOGGER.error(e, JcaI18n.unableToStopEngineWithinTimeLimit, SHUTDOWN_TIMEOUT);
            } catch (InterruptedException e) {
                LOGGER.error(e, JcaI18n.interruptedWhileStoppingJcaAdapter,e.getMessage());
            } catch (ExecutionException e) {
                LOGGER.error(e, JcaI18n.errorWhileStoppingJcaAdapter,e.getMessage());
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
        if (engine == null) {
            ModeShapeEngine engine = new ModeShapeEngine();
            engine.start();
            this.engine = engine;
        }
        return engine;
    }
}
