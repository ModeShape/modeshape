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
package org.modeshape.rhq.plugin;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.metatype.api.types.EnumMetaType;
import org.jboss.metatype.api.values.CollectionValueSupport;
import org.jboss.metatype.api.values.CompositeValueSupport;
import org.jboss.metatype.api.values.EnumValueSupport;
import org.jboss.metatype.api.values.MetaValue;
import org.modeshape.jboss.managed.ManagedEngine;
import org.modeshape.rhq.plugin.util.ModeShapeManagementView;
import org.modeshape.rhq.plugin.util.PluginConstants;
import org.modeshape.rhq.plugin.util.ProfileServiceUtil;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * 
 */
public class SequencingServiceDiscoveryComponent implements
		ResourceDiscoveryComponent<SequencingServiceComponent> {

	private final Log log = LogFactory
			.getLog(PluginConstants.DEFAULT_LOGGER_CATEGORY);

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
	 */
	public Set<DiscoveredResourceDetails> discoverResources(
			ResourceDiscoveryContext discoveryContext)
			throws InvalidPluginConfigurationException, Exception {

		Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

		ManagedComponent mc = ProfileServiceUtil
				.getManagedComponent( ((EngineComponent) discoveryContext
						.getParentResourceComponent()).getConnection(),
						new ComponentType(
								PluginConstants.ComponentType.SequencingService.MODESHAPE_TYPE,
								PluginConstants.ComponentType.SequencingService.MODESHAPE_SUB_TYPE),
								PluginConstants.ComponentType.SequencingService.NAME);

		if (mc == null) {
			return discoveredResources;
		}

		/**
		 * 
		 * A discovered resource must have a unique key, that must stay the same
		 * when the resource is discovered the next time
		 */
		DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
				discoveryContext.getResourceType(), // ResourceType
				PluginConstants.ComponentType.SequencingService.NAME, // Resource Key
				PluginConstants.ComponentType.SequencingService.DISPLAY_NAME, // Resource name
				null, //version 
				PluginConstants.ComponentType.SequencingService.DESC, // Description
				discoveryContext.getDefaultPluginConfiguration(), // Plugin config
				null // Process info from a process scan
		);
		Configuration c = detail.getPluginConfiguration();
		
		//Load properties
		String operation = "getProperties";

		EnumValueSupport enumVs = new EnumValueSupport(new EnumMetaType(ManagedEngine.Component.values()), ManagedEngine.Component.SEQUENCINGSERVICE);
		MetaValue[] args = new MetaValue[] {
				null,
				enumVs };

		MetaValue properties = ModeShapeManagementView
				.executeManagedOperation(mc, operation, args);

		MetaValue[] propertyArray = ((CollectionValueSupport) properties)
				.getElements();

		PropertyList list = new PropertyList("propertyList");
		c.put(list);
		loadProperties(propertyArray, list);

		// Add to return values
		discoveredResources.add(detail);
		log.debug("Discovered ModeShape Sequencing Service: " + mc.getName());

		return discoveredResources;

	}
	
	/**
	 * @param propertyArray
	 * @param list
	 * @throws Exception
	 */
	private void loadProperties(MetaValue[] propertyArray, PropertyList list)
			throws Exception {
		PropertyMap propMap;
		for (MetaValue property : propertyArray) {

			CompositeValueSupport proCvs = (CompositeValueSupport) property;
			propMap = new PropertyMap("map");
			propMap.put(new PropertySimple("label", ProfileServiceUtil
					.stringValue(proCvs.get("label"))));
			propMap.put(new PropertySimple("value", ProfileServiceUtil
					.stringValue(proCvs.get("value"))));
			propMap.put(new PropertySimple("description", ProfileServiceUtil
					.stringValue(proCvs.get("description"))));
			list.add(propMap);
		}
	}
}