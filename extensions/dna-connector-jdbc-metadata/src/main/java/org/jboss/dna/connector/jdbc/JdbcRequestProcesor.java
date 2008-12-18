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
package org.jboss.dna.connector.jdbc;

import java.sql.Connection;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.properties.DateTime;
import org.jboss.dna.graph.requests.CopyBranchRequest;
import org.jboss.dna.graph.requests.CreateNodeRequest;
import org.jboss.dna.graph.requests.DeleteBranchRequest;
import org.jboss.dna.graph.requests.MoveBranchRequest;
import org.jboss.dna.graph.requests.ReadAllChildrenRequest;
import org.jboss.dna.graph.requests.ReadAllPropertiesRequest;
import org.jboss.dna.graph.requests.UpdatePropertiesRequest;
import org.jboss.dna.graph.requests.processor.RequestProcessor;

/**
 * JDBC request processor
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 *
 */
public class JdbcRequestProcesor extends RequestProcessor {
    protected Connection connection;
    
    /**
     * Logging for this instance
     */
    protected Logger log = Logger.getLogger(getClass());

    /**
     * @param sourceName
     * @param context
     * @param connection 
     */
    public JdbcRequestProcesor( String sourceName,
                                ExecutionContext context,
                                Connection connection) {
        super(sourceName, context);
        this.connection = connection;
    }

    /**
     * @param sourceName
     * @param context
     * @param connection 
     * @param now
     */
    public JdbcRequestProcesor( String sourceName,
                                ExecutionContext context,
                                Connection connection,
                                DateTime now ) {
        super(sourceName, context, now);
        this.connection = connection;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.CopyBranchRequest)
     */
    @Override
    public void process( CopyBranchRequest request ) {
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.CreateNodeRequest)
     */
    @Override
    public void process( CreateNodeRequest request ) {
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.DeleteBranchRequest)
     */
    @Override
    public void process( DeleteBranchRequest request ) {
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.MoveBranchRequest)
     */
    @Override
    public void process( MoveBranchRequest request ) {
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.ReadAllChildrenRequest)
     */
    @Override
    public void process( ReadAllChildrenRequest request ) {
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.ReadAllPropertiesRequest)
     */
    @Override
    public void process( ReadAllPropertiesRequest request ) {
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.UpdatePropertiesRequest)
     */
    @Override
    public void process( UpdatePropertiesRequest request ) {
    }
}
