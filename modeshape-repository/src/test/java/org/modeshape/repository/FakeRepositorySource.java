/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.repository;

import java.util.UUID;
import javax.naming.Reference;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceCapabilities;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.path.PathRepositorySource;
import org.modeshape.graph.connector.path.cache.PathCachePolicy;

/**
 * A fake {@link RepositorySource} implementation that has a variety of parameters, used for testing whether
 * {@link RepositoryService} can properly configure different kinds of properties.
 */
public class FakeRepositorySource implements PathRepositorySource {

    /**
     */
    private static final long serialVersionUID = 1L;

    private String name;
    private int retryLimit;
    private int intParam;
    private short shortParam;
    private boolean booleanParam;
    private String stringParam;
    private int[] intArrayParam;
    private boolean[] booleanArrayParam;
    private Long[] longObjectArrayParam;
    private Boolean[] booleanObjectArrayParam;
    private String[] stringArrayParam;
    private PathCachePolicy cachePolicy;

    /**
     * 
     */
    public FakeRepositorySource() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getCapabilities()
     */
    public RepositorySourceCapabilities getCapabilities() {
        return new RepositorySourceCapabilities();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getConnection()
     */
    public RepositoryConnection getConnection() throws RepositorySourceException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#close()
     */
    public void close() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * @param name Sets name to the specified value.
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return retryLimit;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#setRetryLimit(int)
     */
    public void setRetryLimit( int limit ) {
        this.retryLimit = limit;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#initialize(org.modeshape.graph.connector.RepositoryContext)
     */
    public void initialize( RepositoryContext context ) throws RepositorySourceException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.naming.Referenceable#getReference()
     */
    public Reference getReference() {
        throw new UnsupportedOperationException();
    }

    public int getIntParam() {
        return intParam;
    }

    public void setIntParam( int intParam ) {
        this.intParam = intParam;
    }

    public short getShortParam() {
        return shortParam;
    }

    public void setShortParam( short shortParam ) {
        this.shortParam = shortParam;
    }

    public boolean isBooleanParam() {
        return booleanParam;
    }

    public void setBooleanParam( boolean booleanParam ) {
        this.booleanParam = booleanParam;
    }

    public String getStringParam() {
        return stringParam;
    }

    public void setStringParam( String stringParam ) {
        this.stringParam = stringParam;
    }

    public int[] getIntArrayParam() {
        return intArrayParam;
    }

    public void setIntArrayParam( int[] intArrayParam ) {
        this.intArrayParam = intArrayParam;
    }

    public boolean[] getBooleanArrayParam() {
        return booleanArrayParam;
    }

    public void setBooleanArrayParam( boolean[] booleanArrayParam ) {
        this.booleanArrayParam = booleanArrayParam;
    }

    public Long[] getLongObjectArrayParam() {
        return longObjectArrayParam;
    }

    public void setLongObjectArrayParam( Long[] longObjectArrayParam ) {
        this.longObjectArrayParam = longObjectArrayParam;
    }

    public Boolean[] getBooleanObjectArrayParam() {
        return booleanObjectArrayParam;
    }

    public void setBooleanObjectArrayParam( Boolean[] booleanObjectArrayParam ) {
        this.booleanObjectArrayParam = booleanObjectArrayParam;
    }

    public String[] getStringArrayParam() {
        return stringArrayParam;
    }

    public void setStringArrayParam( String[] stringArrayParam ) {
        this.stringArrayParam = stringArrayParam;
    }

    public boolean areUpdatesAllowed() {
        return false;
    }

    public void setCachePolicy( PathCachePolicy cachePolicy ) {
        this.cachePolicy = cachePolicy;
    }

    public PathCachePolicy getCachePolicy() {
        return this.cachePolicy;
    }

    public String getDefaultWorkspaceName() {
        return null;
    }

    public RepositoryContext getRepositoryContext() {
        return null;
    }

    public UUID getRootNodeUuid() {
        return null;
    }

    public void setUpdatesAllowed( boolean updatesAllowed ) {
        // NOP
    }

}
