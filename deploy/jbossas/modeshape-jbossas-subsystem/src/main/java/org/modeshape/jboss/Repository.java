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
package org.modeshape.jboss;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.modeshape.jcr.JcrRepository;


public class Repository implements Service<JcrRepository> {

	@Override
	public JcrRepository getValue() throws IllegalStateException,
			IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void start(StartContext arg0) throws StartException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop(StopContext arg0) {
		// TODO Auto-generated method stub
		
	}
	
//	private final InjectedValue<SocketBinding> socketBindingInjector = new InjectedValue<SocketBinding>();
//	private final InjectedValue<VDBRepository> vdbRepositoryInjector = new InjectedValue<VDBRepository>();
//	private final InjectedValue<DQPCore> dqpInjector = new InjectedValue<DQPCore>();	
//	private final InjectedValue<BufferServiceImpl> bufferServiceInjector = new InjectedValue<BufferServiceImpl>();
	
	
	
}
