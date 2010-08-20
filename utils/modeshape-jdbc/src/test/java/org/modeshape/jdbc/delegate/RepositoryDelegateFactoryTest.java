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
package org.modeshape.jdbc.delegate;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

import java.sql.SQLException;
import java.util.Properties;

import org.junit.Test;
import org.modeshape.jdbc.JcrDriver;

/**
 * 
 */
public class RepositoryDelegateFactoryTest {

    private static final String REPOSITORY_NAME = "repositoryName";
    
    private static final String USER_NAME="jsmith";
    private static final String PASSWORD="secret";
    private static final String WORKSPACE="MyWorkspace";
    private static final String JNDINAME="java:MyRepository";
    private static final String INVALID_URL =  "jdbc:metamatrix:jndi://";
    
    private static final String VALID_HTTP_URL =  JcrDriver.HTTP_URL_PREFIX + "server:host";
    
    private static final String VALID_JNDI_URL =  JcrDriver.JNDI_URL_PREFIX + JNDINAME;

    
    private static final String VALID_JNDI_URL_WITH_PARMS =  VALID_JNDI_URL + 
    			"?workspace=" + WORKSPACE + 
    			"&user=" + USER_NAME + 
    			"&password=" + PASSWORD  + 
    			"&" + JcrDriver.REPOSITORY_PROPERTY_NAME + "=" + REPOSITORY_NAME;
           

    
    @Test
    public void shouldCreateLocalRepositoryDelegate() throws SQLException  {
	RepositoryDelegate delegate = RepositoryDelegateFactory.createRepositoryDelegate(VALID_JNDI_URL_WITH_PARMS, new Properties(), null);
	assertThat(delegate, instanceOf(LocalRepositoryDelegate.class));
    }

    @Test
    public void shouldSupportCreatingHttpRepositoryDelegate() throws SQLException  {
	RepositoryDelegateFactory.createRepositoryDelegate(VALID_HTTP_URL, new Properties(), null);	
    }
        
    @Test(expected = SQLException.class)
    public void shouldNotSupportCreatingInvalidURL() throws SQLException  {
	RepositoryDelegateFactory.createRepositoryDelegate(INVALID_URL, new Properties(), null);	
    }
    
    @Test
    public void shouldAcceptValidURL()  {
	assertThat(RepositoryDelegateFactory.acceptUrl(VALID_JNDI_URL_WITH_PARMS), is(true));  	
	assertThat(RepositoryDelegateFactory.acceptUrl(VALID_JNDI_URL), is(true));  
    }
    
    public void shouldNotAcceptInvalidURL()  {	
	assertThat(RepositoryDelegateFactory.acceptUrl(INVALID_URL), is(false));  	
    }
    
    
    

}
