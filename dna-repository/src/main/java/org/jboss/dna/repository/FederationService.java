/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.repository;

import java.util.concurrent.TimeUnit;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.repository.services.AbstractServiceAdministrator;
import org.jboss.dna.repository.services.AdministeredService;
import org.jboss.dna.repository.services.ServiceAdministrator;
import org.jboss.dna.spi.graph.connection.RepositorySource;

/**
 * @author Randall Hauch
 */
public class FederationService implements AdministeredService {

    /**
     * The administrative component for this service.
     * @author Randall Hauch
     */
    protected class Administrator extends AbstractServiceAdministrator {

        protected Administrator() {
            super(RepositoryI18n.federationServiceName, State.STARTED);
        }

        /**
         * {@inheritDoc}
         */
        public boolean awaitTermination( long timeout, TimeUnit unit ) {
            return true;
        }

    }

    private RepositorySource bootstrapSource;
    private final Administrator administrator = new Administrator();

    /**
     * Create a federation service instance
     * @param bootstrapSource the repository source that should be used to bootstrap the federation service
     * @throws IllegalArgumentException if the bootstrap source is null
     */
    public FederationService( RepositorySource bootstrapSource ) {
        ArgCheck.isNotNull(bootstrapSource, "bootstrapSource");
    }

    /**
     * {@inheritDoc}
     */
    public ServiceAdministrator getAdministrator() {
        return this.administrator;
    }

    /**
     * Get the repository source used to obtain connections to the repository containing the configuration information for this
     * federation service.
     * @return bootstrapSource
     */
    public RepositorySource getBootstrapSource() {
        return this.bootstrapSource;
    }

}
