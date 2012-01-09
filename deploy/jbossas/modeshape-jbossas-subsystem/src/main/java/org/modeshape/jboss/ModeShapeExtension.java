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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.modeshape.jboss.subsystem.JBossManagedI18n;

public class ModeShapeExtension implements Extension {
	
	public static final String MODESHAPE_SUBSYSTEM = "modeshape"; //$NON-NLS-1$
	
	/** The name space used for the {@code subsystem} element */
    public static final String NAMESPACE = "urn:jboss.domain.modeshape:3.0";
	
    private static ModeShapeSubsystemParser parser = new ModeShapeSubsystemParser();
	private static RepositoryAdd REPOSITORY_ADD = new RepositoryAdd();
	private static RepositoryRemove REPOSITORY_REMOVE = new RepositoryRemove();
	private static ModeShapeAdd MODESHAPE_BOOT_ADD = new ModeShapeAdd();
	private static ModeShapeSubsystemDescribe MODESHAPE_DESCRIBE = new ModeShapeSubsystemDescribe();
	
	@Override
	public void initialize(ExtensionContext context) {
		final SubsystemRegistration registration = context.registerSubsystem(MODESHAPE_SUBSYSTEM);
		
//		LogManager.setLogListener(new Log4jListener());
		
		registration.registerXMLElementWriter(parser);

		// ModeShape system, with children repositories.
		
		final ManagementResourceRegistration modeShapeSubsystem = registration.registerSubsystemModel(MODESHAPE_DESCRIBE);
		modeShapeSubsystem.registerOperationHandler(ADD, MODESHAPE_BOOT_ADD, MODESHAPE_BOOT_ADD, false);
		modeShapeSubsystem.registerOperationHandler(DESCRIBE, MODESHAPE_DESCRIBE, MODESHAPE_DESCRIBE, false);     
				
		// Repository Subsystem
        final ManagementResourceRegistration repositorySubsystem = modeShapeSubsystem.registerSubModel(PathElement.pathElement(Element.REPOSITORY_ELEMENT.getLocalName()), new DescriptionProvider() {
			@Override
			public ModelNode getModelDescription(Locale locale) {
				final ResourceBundle bundle = JBossManagedI18n.getResourceBundle(locale);

				final ModelNode node = new ModelNode();
	            node.get(DESCRIPTION).set(Element.REPOSITORY_ELEMENT.getDescription(bundle));
	            node.get(HEAD_COMMENT_ALLOWED).set(true);
	            RepositoryAdd.repositoryDescribe(node, ATTRIBUTES, bundle);
	            
	            return node;
			}
		});
       repositorySubsystem.registerOperationHandler(ADD, REPOSITORY_ADD, REPOSITORY_ADD, false);
       repositorySubsystem.registerOperationHandler(REMOVE, REPOSITORY_REMOVE, REPOSITORY_REMOVE, false);

        
//        modeShapeSubsystem.registerReadOnlyAttribute(RUNTIME_VERSION, new GetRuntimeVersion(RUNTIME_VERSION), Storage.RUNTIME); 
		
		// teiid level admin api operation handlers
//		new GetTranslator().register(teiidSubsystem);
//		new ListTranslators().register(teiidSubsystem);
//		new MergeVDBs().register(teiidSubsystem);
//		new ListVDBs().register(teiidSubsystem);
//		new GetVDB().register(teiidSubsystem);
//		new CacheTypes().register(teiidSubsystem);
//		new ClearCache().register(teiidSubsystem);
//		new CacheStatistics().register(teiidSubsystem);
//		new AddDataRole().register(teiidSubsystem);
//		new RemoveDataRole().register(teiidSubsystem);
//		new AddAnyAuthenticatedDataRole().register(teiidSubsystem);
//		new AssignDataSource().register(teiidSubsystem);
//		new ChangeVDBConnectionType().register(teiidSubsystem);
//		new RemoveAnyAuthenticatedDataRole().register(teiidSubsystem);
//		new ListRequests().register(teiidSubsystem);
//		new ListSessions().register(teiidSubsystem);
//		new RequestsPerSession().register(teiidSubsystem);
//		new RequestsPerVDB().register(teiidSubsystem);
//		new GetLongRunningQueries().register(teiidSubsystem);
//		new TerminateSession().register(teiidSubsystem);
//		new CancelRequest().register(teiidSubsystem);
//		new WorkerPoolStatistics().register(teiidSubsystem);
//		new ListTransactions().register(teiidSubsystem);
//		new TerminateTransaction().register(teiidSubsystem);
//		new ExecuteQuery().register(teiidSubsystem);
//		new MarkDataSourceAvailable().register(teiidSubsystem);
//		new ReadRARDescription().register(teiidSubsystem);
	}

	@Override
	public void initializeParsers(ExtensionParsingContext context) {
		context.setSubsystemXmlMapping(Namespace.CURRENT.getUri(), parser);
	}
	
}
