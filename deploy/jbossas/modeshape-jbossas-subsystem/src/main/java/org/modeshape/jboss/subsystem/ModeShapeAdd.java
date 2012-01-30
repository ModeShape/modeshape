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

package org.modeshape.jboss.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.jboss.lifecycle.JBossLifeCycleListener;
import org.modeshape.jboss.service.EngineService;
import org.modeshape.jcr.JcrEngine;

class ModeShapeAdd extends AbstractAddStepHandler implements DescriptionProvider {

	private static Element[] attributes = {
	};
	
	final JBossLifeCycleListener shutdownListener = new JBossLifeCycleListener();
	
	@Override
	public ModelNode getModelDescription(Locale locale) {
        final ResourceBundle bundle = JBossSubsystemI18n.getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("modeshape.add")); //$NON-NLS-1$
        
        describeModeShape(node, REQUEST_PROPERTIES,  bundle); 
		
        return node;
	}

	static void describeModeShape(final ModelNode node, String type, final ResourceBundle bundle) {
		for (int i = 0; i < attributes.length; i++) {
			attributes[i].describe(node, type, bundle);
		}	
	}
	
	@Override
	protected void populateModel(ModelNode operation, ModelNode model)	throws OperationFailedException {
		populate(operation, model);
	}

	static void populate(ModelNode operation, ModelNode model) {
		for (int i = 0; i < attributes.length; i++) {
			attributes[i].populate(operation, model);
		}
	}
	

	@Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
            final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(Module.getCallerModule().getClassLoader());
			initializeModeShapeEngine(context, operation, newControllers);
		} finally {
			Thread.currentThread().setContextClassLoader(classloader);
		}
	}

	private void initializeModeShapeEngine(final OperationContext context, final ModelNode operation, final List<ServiceController<?>> newControllers)
			throws OperationFailedException {
		ServiceTarget target = context.getServiceTarget();
		
		final JBossLifeCycleListener shutdownListener = new JBossLifeCycleListener();
		    	
    	// Jcr Engine
    	final EngineService engine = buildModeShapeEngine(operation);
    	
        ServiceBuilder<JcrEngine> engineBuilder = target.addService(ModeShapeServiceNames.ENGINE, engine);      
        engineBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        ServiceController<JcrEngine> controller = engineBuilder.install(); 
        newControllers.add(controller);
        ServiceContainer container =  controller.getServiceContainer();
        container.addTerminateListener(shutdownListener);
     
  }
	
	private EngineService buildModeShapeEngine(ModelNode node) {
		EngineService engine = new EngineService(new JcrEngine());
		return engine;
	}    
}
