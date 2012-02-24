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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Json;
import org.infinispan.schematic.document.ParsingException;
import org.infinispan.schematic.internal.document.JsonReader;
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
import org.modeshape.jboss.service.RepositoryService;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.RepositoryConfiguration;


public class RepositoryAdd extends AbstractAddStepHandler implements DescriptionProvider {

	private static Element[] attributes = {
		Element.REPOSITORY_NAME_ATTRIBUTE,
		Element.REPOSITORY_JNDI_NAME_ATTRIBUTE,
		
		Element.SEQUENCER_NAME_ATTRIBUTE,
		Element.SEQUENCER_DESCRIPTION_ATTRIBUTE,
		Element.SEQUENCER_TYPE_ATTRIBUTE,
		Element.SEQUENCER_EXPRESSIONS_ATTRIBUTE
	};
	
	private static String jndiBaseName = "java:jcr/local/";   
	public static final RepositoryAdd INSTANCE = new RepositoryAdd();
	   
	private RepositoryAdd() {
	}
	
	@Override
	public ModelNode getModelDescription(Locale locale) {
		final ResourceBundle bundle = JBossSubsystemI18n.getResourceBundle(locale);
		
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set("repository.add");  //$NON-NLS-1$
        
        describeRepository(node, REQUEST_PROPERTIES, bundle);
        return node; 
	}
	
	static void describeRepository(ModelNode node, String type, ResourceBundle bundle) {
		repositoryDescribe(node, type, bundle);
	}

	static void repositoryDescribe(ModelNode node, String type, ResourceBundle bundle) {
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
	protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
            final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

    	ServiceTarget target = context.getServiceTarget();
    	
        final ModelNode address = operation.require(OP_ADDR);
        final PathAddress pathAddress = PathAddress.pathAddress(address);
    	final String repositoryName = pathAddress.getLastElement().getValue();
    	
    	StringBuilder jndiName = new StringBuilder(jndiBaseName); 
    	if (Element.REPOSITORY_JNDI_NAME_ATTRIBUTE.isDefined(model)) {
    		jndiName.append(Element.REPOSITORY_JNDI_NAME_ATTRIBUTE.asString(model));
    	}else{
    		jndiName.append(repositoryName);
    	}
    	
    	ArrayList<String> expressionList = new ArrayList<String>();
   		if (Element.SEQUENCER_EXPRESSIONS_ATTRIBUTE.isDefined(operation)) {
    		String domains = Element.SEQUENCER_EXPRESSIONS_ATTRIBUTE.asString(operation);
    		StringTokenizer st = new StringTokenizer(domains, ","); //$NON-NLS-1$
    		while(st.hasMoreTokens()) {
    			expressionList.add(st.nextToken());
    		}
    	}  
    	
    	String jsonString = model.toJSONString(true);
    	Document configDoc;
    	try {
			configDoc = Json.read(jsonString);
		} catch (ParsingException e) {
			throw new OperationFailedException(e, model);
		}
    	
    	
   		
//   		if (Element.AUTHENTICATION_MAX_SESSIONS_ALLOWED_ATTRIBUTE.isDefined(operation)) {
//   			transport.setSessionMaxLimit(Element.AUTHENTICATION_MAX_SESSIONS_ALLOWED_ATTRIBUTE.asLong(operation));
//   		}
//    	
//   		if (Element.AUTHENTICATION_SESSION_EXPIRATION_TIME_LIMIT_ATTRIBUTE.isDefined(operation)) {
//   			transport.setSessionExpirationTimeLimit(Element.AUTHENTICATION_SESSION_EXPIRATION_TIME_LIMIT_ATTRIBUTE.asLong(operation));
//   		}   		
//   		if (Element.AUTHENTICATION_KRB5_DOMAIN_ATTRIBUTE.isDefined(operation)) {
//   			transport.setAuthenticationType(AuthenticationType.GSS);
//   			transport.setKrb5Domain(Element.AUTHENTICATION_KRB5_DOMAIN_ATTRIBUTE.asString(operation));
//   		}
//   		else {
//   			transport.setAuthenticationType(AuthenticationType.CLEARTEXT);
//   		}
    	RepositoryService repositoryService = new RepositoryService(new RepositoryConfiguration(configDoc, repositoryName));
		
    	ServiceBuilder<JcrRepository> repositoryBuilder = target.addService(ModeShapeServiceNames.repositoryServiceName(repositoryName), repositoryService);
    	repositoryBuilder.addDependency(ModeShapeServiceNames.ENGINE, JcrEngine.class, repositoryService.getJcrEngineInjector());
    	repositoryBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        ServiceController<JcrRepository> controller = repositoryBuilder.install(); 
        newControllers.add(controller);
        
	}
	
}
