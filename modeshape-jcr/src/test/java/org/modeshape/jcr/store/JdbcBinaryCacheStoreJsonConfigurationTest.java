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
package org.modeshape.jcr.store;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.net.URL;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.infinispan.schematic.document.ParsingException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.ConfigurationException;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;

import static org.hamcrest.core.IsNull.notNullValue;


/**
 * @author evgeniy.shevchenko
 *
 */
public class JdbcBinaryCacheStoreJsonConfigurationTest  {
	ModeShapeEngine engine;
	RepositoryConfiguration config;
	
	@Before
    public void before() throws Exception {
		engine = new ModeShapeEngine();
		engine.start();
        URL url = getClass().getClassLoader().getResource("config/mode-1786-repository-config.json");
        config = RepositoryConfiguration.read(url);
        engine.deploy(config);
	}	
	
    
    @Test
    public void shouldWorkWithJsonConfiguration() throws Exception {
    	
    	
        Session session = null;
        JcrRepository repository = engine.getRepository(config.getName());
        try{
        	session = repository.login("default");
        }catch(RepositoryException e){
        	e.printStackTrace();
        }
        assertThat(session, is(notNullValue()));
    	session.logout();
        engine.shutdown().get();
    }
    
    


}
