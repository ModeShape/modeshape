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
import org.modeshape.common.logging.Logger;
import org.modeshape.common.naming.SingletonInitialContextFactory;
import org.modeshape.jboss.lifecycle.JBossLifeCycleListener;
import org.modeshape.jboss.service.EngineService;
import org.modeshape.jboss.service.ReferenceFactoryService;
import org.modeshape.jcr.ModeShapeEngine;

class AddModeShapeSubsystem extends AbstractAddStepHandler {

    public static final AddModeShapeSubsystem INSTANCE = new AddModeShapeSubsystem();

    // Jcr Engine
    EngineService engine;

    SingletonInitialContextFactory scf = new SingletonInitialContextFactory();
    final JBossLifeCycleListener shutdownListener = new JBossLifeCycleListener();// todo why defined here and then again inside
                                                                                 // initializeModeShapeEngine

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
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

        final JBossLifeCycleListener shutdownListener = new JBossLifeCycleListener(); // what is right, this or one defined in
                                                                                      // top?

        engine = buildModeShapeEngine(model);

        // Engine service
        ServiceBuilder<ModeShapeEngine> engineBuilder = target.addService(ModeShapeServiceNames.ENGINE, engine);
        engineBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        ServiceController<ModeShapeEngine> controller = engineBuilder.install();
        controller.getServiceContainer().addTerminateListener(shutdownListener);
        newControllers.add(controller);

        // JNDI Binding
        final ReferenceFactoryService<ModeShapeEngine> referenceFactoryService = new ReferenceFactoryService<ModeShapeEngine>();
        final ServiceName referenceFactoryServiceName = ModeShapeServiceNames.ENGINE.append("reference-factory"); //$NON-NLS-1$
        final ServiceBuilder<?> referenceBuilder = target.addService(referenceFactoryServiceName, referenceFactoryService);
        referenceBuilder.addDependency(ModeShapeServiceNames.ENGINE, ModeShapeEngine.class, referenceFactoryService.getInjector());
        referenceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(ModeShapeJndiNames.JNDI_BASE_NAME);
        final BinderService binderService = new BinderService(bindInfo.getBindName());
        final ServiceBuilder<?> binderBuilder = target.addService(bindInfo.getBinderServiceName(), binderService);
        binderBuilder.addDependency(ModeShapeServiceNames.ENGINE,
                                    ModeShapeEngine.class,
                                    new ManagedReferenceInjector<ModeShapeEngine>(binderService.getManagedObjectInjector()));
        binderBuilder.addDependency(bindInfo.getParentContextServiceName(),
                                    ServiceBasedNamingStore.class,
                                    binderService.getNamingStoreInjector());
        binderBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        Logger.getLogger(getClass()).debug("Binding ModeShape to JNDI name '{0}'", bindInfo.getAbsoluteJndiName());

        newControllers.add(referenceBuilder.install());
        newControllers.add(binderBuilder.install());

    }

    private EngineService buildModeShapeEngine( ModelNode model ) {
        EngineService engine = new EngineService(new ModeShapeEngine());
        return engine;
    }
}
