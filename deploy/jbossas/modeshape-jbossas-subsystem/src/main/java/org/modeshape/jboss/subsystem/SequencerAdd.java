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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;

import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.jboss.service.SequencerService;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrRepository;

public class SequencerAdd extends AbstractAddStepHandler implements
		DescriptionProvider {

	private static Element[] attributes = { Element.SEQUENCER_NAME_ATTRIBUTE,
			Element.SEQUENCER_DESCRIPTION_ATTRIBUTE,
			Element.SEQUENCER_EXPRESSIONS_ATTRIBUTE,
			Element.SEQUENCER_TYPE_ATTRIBUTE };

	
	public static final SequencerAdd INSTANCE = new SequencerAdd();

	private SequencerAdd() {
	}

	@Override
	public ModelNode getModelDescription(Locale locale) {
		final ResourceBundle bundle = JBossSubsystemI18n
				.getResourceBundle(locale);

		final ModelNode node = new ModelNode();
		node.get(OPERATION_NAME).set(ADD);
		node.get(DESCRIPTION).set("sequencer.add"); //$NON-NLS-1$

		describeSequencer(node, REQUEST_PROPERTIES, bundle);
		return node;
	}

	static void describeSequencer(ModelNode node, String type,
			ResourceBundle bundle) {
		sequencerDescribe(node, type, bundle);
	}

	static void sequencerDescribe(ModelNode node, String type,
			ResourceBundle bundle) {
		for (int i = 0; i < attributes.length; i++) {
			attributes[i].describe(node, type, bundle);
		}
	}

	@Override
	protected void populateModel(ModelNode operation, ModelNode model) {
		populate(operation, model);
	}

	public static void populate(ModelNode operation, ModelNode model) {
		for (int i = 0; i < attributes.length; i++) {
			attributes[i].populate(operation, model);
		}
	}

	@Override
	protected void performRuntime(final OperationContext context,
			final ModelNode operation, final ModelNode model,
			final ServiceVerificationHandler verificationHandler,
			final List<ServiceController<?>> newControllers)
			throws OperationFailedException {

		ServiceTarget target = context.getServiceTarget();

		Properties props = new Properties();

		final ModelNode address = operation.require(OP_ADDR);
		final PathAddress pathAddress = PathAddress.pathAddress(address);
		final String repositoryName = pathAddress.getElement(1).getValue();
		final String sequencerName = pathAddress.getLastElement().getValue();

		props.put(Element.SEQUENCER_NAME_ATTRIBUTE.getLocalName(),
				sequencerName);

		if (Element.SEQUENCER_TYPE_ATTRIBUTE.isDefined(operation)) {
			props.put(Element.SEQUENCER_TYPE_ATTRIBUTE.getLocalName(),
					(Element.SEQUENCER_TYPE_ATTRIBUTE.asString(operation)));
		}
		
		if (Element.SEQUENCER_EXPRESSIONS_ATTRIBUTE.isDefined(operation)) {
			props.put(Element.SEQUENCER_EXPRESSIONS_ATTRIBUTE.getLocalName(),
					(Element.SEQUENCER_EXPRESSIONS_ATTRIBUTE.asString(operation)));
		}
		
		SequencerService sequencerService = new SequencerService(
				repositoryName, props);

		ServiceBuilder<JcrRepository> sequencerBuilder = target.addService(
				ModeShapeServiceNames.sequencerServiceName(repositoryName
						+ sequencerName), sequencerService);
		sequencerBuilder.addDependency(ModeShapeServiceNames.ENGINE,
				JcrEngine.class, sequencerService.getJcrEngineInjector());
		sequencerBuilder.addDependency(ModeShapeServiceNames.repositoryServiceName(repositoryName),
				JcrRepository.class, sequencerService.getJcrRepositoryInjector());
		sequencerBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
		ServiceController<JcrRepository> controller = sequencerBuilder
				.install();
		newControllers.add(controller);
	}
}
