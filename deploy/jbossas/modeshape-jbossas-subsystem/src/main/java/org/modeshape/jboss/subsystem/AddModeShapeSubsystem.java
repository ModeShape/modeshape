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

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.jboss.lifecycle.JBossLifeCycleListener;
import org.modeshape.jboss.service.EngineService;
import org.modeshape.jboss.service.ReferenceFactoryService;
import org.modeshape.jcr.JcrEngine;

class AddModeShapeSubsystem extends AbstractAddStepHandler {

    public static final AddModeShapeSubsystem INSTANCE = new AddModeShapeSubsystem();

    // Jcr Engine
    EngineService engine;

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attribute : ModelAttributes.SUBSYSTEM_ATTRIBUTES) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime( final OperationContext context,
                                   final ModelNode operation,
                                   final ModelNode model,
                                   final ServiceVerificationHandler verificationHandler,
                                   final List<ServiceController<?>> newControllers ) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(Module.getCallerModule().getClassLoader());
            initializeModeShapeEngine(context, operation, model, newControllers);
        } finally {
            Thread.currentThread().setContextClassLoader(classloader);
        }
    }

    private void initializeModeShapeEngine( final OperationContext context,
                                            final ModelNode operation,
                                            ModelNode model,
                                            final List<ServiceController<?>> newControllers ) {
        ServiceTarget target = context.getServiceTarget();

        final JBossLifeCycleListener shutdownListener = new JBossLifeCycleListener();  //what is right, this or one defined in top?

        engine = buildModeShapeEngine(model);

        // Engine service
        ServiceBuilder<JcrEngine> engineBuilder = target.addService(ModeShapeServiceNames.ENGINE, engine);
        engineBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        ServiceController<JcrEngine> controller = engineBuilder.install();
        controller.getServiceContainer().addTerminateListener(shutdownListener);
        newControllers.add(controller);

        // JNDI Binding
        final ReferenceFactoryService<JcrEngine> referenceFactoryService = new ReferenceFactoryService<JcrEngine>();
        final ServiceName referenceFactoryServiceName = ModeShapeServiceNames.ENGINE.append("reference-factory"); //$NON-NLS-1$
        final ServiceBuilder<?> referenceBuilder = target.addService(referenceFactoryServiceName, referenceFactoryService);
        referenceBuilder.addDependency(ModeShapeServiceNames.ENGINE, JcrEngine.class, referenceFactoryService.getInjector());
        referenceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(ModeShapeJndiNames.JNDI_BASE_NAME);
        final BinderService binderService = new BinderService(bindInfo.getBindName());
        final ServiceBuilder<?> binderBuilder = target.addService(bindInfo.getBinderServiceName(), binderService);
        binderBuilder.addDependency(ModeShapeServiceNames.ENGINE,
                                    JcrEngine.class,
                                    new ManagedReferenceInjector<JcrEngine>(binderService.getManagedObjectInjector()));
        binderBuilder.addDependency(bindInfo.getParentContextServiceName(),
                                    ServiceBasedNamingStore.class,
                                    binderService.getNamingStoreInjector());
        binderBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        newControllers.add(referenceBuilder.install());
        newControllers.add(binderBuilder.install());

    }

    private EngineService buildModeShapeEngine( ModelNode model ) {
        EngineService engine = new EngineService(new JcrEngine());
        return engine;
    }
}
