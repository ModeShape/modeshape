/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.modeshape.jboss.service;

import javax.jcr.RepositoryException;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.modeshape.jcr.ConfigurationException;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * A <code>RepositoryService</code> instance is the service responsible 
 * for initializing a {@link JcrRepository} in the ModeShape engine using the
 * information from the configuration.
 */
public class RepositoryService implements Service<JcrRepository> {

	private final InjectedValue<JcrEngine> jcrEngineInjector = new InjectedValue<JcrEngine>();
	
	private final RepositoryConfiguration repositoryConfiguration;
	
	public RepositoryService(RepositoryConfiguration repositoryConfiguration) {
		this.repositoryConfiguration = repositoryConfiguration;
	}

	@Override
	public JcrRepository getValue() throws IllegalStateException,
			IllegalArgumentException {
		return null;
	}
	
	private JcrEngine getJcrEngine() {
		return jcrEngineInjector.getValue();
	}

	@Override
	public void start(StartContext arg0) throws StartException {
		JcrEngine engine = getJcrEngine();
		try {
			engine.deploy(repositoryConfiguration);
		} catch (ConfigurationException e) {
			throw new StartException(e);
		} catch (RepositoryException e) {
			throw new StartException(e);
		}
		
	}

	@Override
	public void stop(StopContext arg0) {
		
	}

	/**
	 * @return the jcrEngineInjector
	 */
	public InjectedValue<JcrEngine> getJcrEngineInjector() {
		return jcrEngineInjector;
	}
	
}
