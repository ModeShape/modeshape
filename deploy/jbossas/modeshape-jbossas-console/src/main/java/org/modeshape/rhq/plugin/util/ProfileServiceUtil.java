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

package org.modeshape.rhq.plugin.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployers.spi.management.KnownDeploymentTypes;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedCommon;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedDeployment;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.metatype.api.types.MapCompositeMetaType;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.types.SimpleMetaType;
import org.jboss.metatype.api.values.EnumValue;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.SimpleValue;
import org.modeshape.jboss.managed.ManagedSequencingService;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;

public class ProfileServiceUtil {

	protected final static Log LOG = LogFactory
			.getLog(PluginConstants.DEFAULT_LOGGER_CATEGORY);


	/**
	 * Get the passed in {@link ManagedComponent}
	 * @param connection 
	 * 
	 * @param componentType
	 * @param componentName
	 * 
	 * @return {@link ManagedComponent}
	 * @throws NamingException
	 * @throws Exception
	 */
	public static ManagedComponent getManagedComponent( ProfileServiceConnection connection,
			ComponentType componentType, String componentName)
			throws NamingException, Exception {

		ManagedComponent mc = connection.getManagementView().getComponent(componentName, componentType);

		return mc;
	}
	
	/**
	 * Get the passed in {@link ManagedComponent}
	 * @param connection 
	 * 
	 * @return {@link ManagedComponent}
	 * @throws NamingException
	 * @throws Exception
	 */
	public static ManagedComponent getManagedEngine( ProfileServiceConnection connection)
			throws NamingException, Exception {

		ManagedComponent mc = ProfileServiceUtil
		.getManagedComponent(connection,
				new ComponentType(
						PluginConstants.ComponentType.Engine.MODESHAPE_TYPE,
						PluginConstants.ComponentType.Engine.MODESHAPE_SUB_TYPE),
				PluginConstants.ComponentType.Engine.MODESHAPE_ENGINE);

		return mc;
	}

	/**
	 * Get the {@link ManagedSequencingService}
	 * @param connection 
	 * 
	 * @return {@link ManagedSequencingService}
	 * @throws NamingException
	 * @throws Exception
	 */
	public static ManagedComponent getManagedSequencingService( ProfileServiceConnection connection)
			throws NamingException, Exception {

		ManagedComponent mc = ProfileServiceUtil
		.getManagedComponent(connection,
				new ComponentType(
						PluginConstants.ComponentType.SequencingService.MODESHAPE_TYPE,
						PluginConstants.ComponentType.SequencingService.MODESHAPE_SUB_TYPE),
				PluginConstants.ComponentType.SequencingService.NAME);

		return mc;
	}

	/**
	 * Get the {@link ManagedComponent} for the {@link ComponentType} and sub
	 * type.
	 * @param connection 
	 * @param componentType
	 * 
	 * @return Set of {@link ManagedComponent}s
	 * @throws NamingException
	 *             , Exception
	 * @throws Exception
	 */
	public static Set<ManagedComponent> getManagedComponents( ProfileServiceConnection connection,
			ComponentType componentType) throws NamingException, Exception {
		ManagementView mv = connection.getManagementView();
		Set<ManagedComponent> mcSet = mv.getComponentsForType(componentType);

		return mcSet;
	}

	/**
	 * @param connection 
	 * @return {@link ManagementView}
	 */
	public static ManagementView getManagementView(ProfileServiceConnection connection) {
		ManagementView mv = connection.getManagementView();
		return mv;
	}

	/**
	 * Get the {@link DeploymentManager} from the ProfileService
	 * @param connection 
	 * @return DeploymentManager
	 * @throws NamingException
	 * @throws Exception
	 */
	public static DeploymentManager getDeploymentManager(ProfileServiceConnection connection)
			throws NamingException, Exception {
		DeploymentManager deploymentManager = connection.getDeploymentManager();

		return deploymentManager;
	}

	/**
	 * @param connection 
	 * @return {@link File}
	 * @throws NamingException
	 * @throws Exception
	 */
	public static File getDeployDirectory(ProfileServiceConnection connection) throws NamingException, Exception {
		ManagementView mv = connection.getManagementView();
		Set<ManagedDeployment> warDeployments;
		try {
			warDeployments = mv
					.getDeploymentsForType(KnownDeploymentTypes.JavaEEWebApplication
							.getType());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		ManagedDeployment standaloneWarDeployment = null;
		for (ManagedDeployment warDeployment : warDeployments) {
			if (warDeployment.getParent() == null) {
				standaloneWarDeployment = warDeployment;
				break;
			}
		}
		if (standaloneWarDeployment == null)
			// This could happen if no standalone WARs, including the admin
			// console WAR, have been fully deployed yet.
			return null;
		URL warUrl;
		try {
			warUrl = new URL(standaloneWarDeployment.getName());
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		}
		File warFile = new File(warUrl.getPath());
		File deployDir = warFile.getParentFile();
		return deployDir;
	}

	public static String stringValue(MetaValue v1) throws Exception {
		if (v1 != null) {
			MetaType type = v1.getMetaType();
			if (type instanceof SimpleMetaType) {
				SimpleValue simple = (SimpleValue) v1;
				return simple.getValue().toString();
			}
			throw new Exception("Failed to convert value to string value");
		}
		return null;
	}

	public static Boolean booleanValue(MetaValue v1) throws Exception {
		if (v1 != null) {
			MetaType type = v1.getMetaType();
			if (type instanceof SimpleMetaType) {
				SimpleValue simple = (SimpleValue) v1;
				return Boolean.valueOf(simple.getValue().toString());
			}
			throw new Exception("Failed to convert value to boolean value");
		}
		return null;
	}

	public static <T> T getSimpleValue(ManagedComponent mc, String prop,
			Class<T> expectedType) {
		ManagedProperty mp = mc.getProperty(prop);
		if (mp != null) {
			MetaType metaType = mp.getMetaType();
			if (metaType.isSimple()) {
				SimpleValue simpleValue = (SimpleValue) mp.getValue();
				return expectedType.cast((simpleValue != null) ? simpleValue
						.getValue() : null);
			} else if (metaType.isEnum()) {
				EnumValue enumValue = (EnumValue) mp.getValue();
				return expectedType.cast((enumValue != null) ? enumValue
						.getValue() : null);
			}
			throw new IllegalStateException(prop + " is not a simple type");
		}
		return null;
	}

	public static <T> T getSimpleValue(ManagedCommon mc, String prop,
			Class<T> expectedType) {
		ManagedProperty mp = mc.getProperty(prop);
		if (mp != null) {
			MetaType metaType = mp.getMetaType();
			if (metaType.isSimple()) {
				SimpleValue simpleValue = (SimpleValue) mp.getValue();
				return expectedType.cast((simpleValue != null) ? simpleValue
						.getValue() : null);
			} else if (metaType.isEnum()) {
				EnumValue enumValue = (EnumValue) mp.getValue();
				return expectedType.cast((enumValue != null) ? enumValue
						.getValue() : null);
			}
			throw new IllegalArgumentException(prop + " is not a simple type"); //$NON-NLS-1$
		}
		return null;
	}

	public static Map<String, PropertySimple> getCustomProperties(
			Configuration pluginConfig) {
		Map<String, PropertySimple> customProperties = new LinkedHashMap<String, PropertySimple>();
		if (pluginConfig == null)
			return customProperties;
		PropertyMap customPropsMap = pluginConfig.getMap("custom-properties");
		if (customPropsMap != null) {
			Collection<Property> customProps = customPropsMap.getMap().values();
			for (Property customProp : customProps) {
				if (!(customProp instanceof PropertySimple)) {
					LOG
							.error("Custom property definitions in plugin configuration must be simple properties - property "
									+ customProp + " is not - ignoring...");
					continue;
				}
				customProperties.put(customProp.getName(),
						(PropertySimple) customProp);
			}
		}
		return customProperties;
	}

	public static Configuration convertManagedObjectToConfiguration(
			Map<String, ManagedProperty> managedProperties,
			Map<String, PropertySimple> customProps, ResourceType resourceType) {
		// Configuration config = new Configuration();
		// ConfigurationDefinition configDef = resourceType
		// .getResourceConfigurationDefinition();
		// Map<String, PropertyDefinition> propDefs = configDef
		// .getPropertyDefinitions();
		// Set<String> propNames = managedProperties.keySet();
		// for (String propName : propNames) {
		// PropertyDefinition propertyDefinition = propDefs.get(propName);
		// ManagedProperty managedProperty = managedProperties.get(propName);
		// if (propertyDefinition == null) {
		// if (!managedProperty.hasViewUse(ViewUse.STATISTIC))
		// LOG
		// .debug(resourceType
		// + " does not define a property corresponding to ManagedProperty '"
		// + propName + "'.");
		// continue;
		// }
		// if (managedProperty == null) {
		// // This should never happen, but don't let it blow us up.
		// LOG.error("ManagedProperty '" + propName
		// + "' has a null value in the ManagedProperties Map.");
		// continue;
		// }
		// MetaValue metaValue = managedProperty.getValue();
		// if (managedProperty.isRemoved() || metaValue == null) {
		// // Don't even add a Property to the Configuration if the
		// // ManagedProperty is flagged as removed or has a
		// // null value.
		// continue;
		// }
		// PropertySimple customProp = customProps.get(propName);
		// PropertyAdapter<Property, PropertyDefinition> propertyAdapter =
		// PropertyAdapterFactory
		// .getCustomPropertyAdapter(customProp);
		// if (propertyAdapter == null)
		// propertyAdapter = PropertyAdapterFactory
		// .getPropertyAdapter(metaValue);
		// if (propertyAdapter == null) {
		// LOG
		// .error("Unable to find a PropertyAdapter for ManagedProperty '"
		// + propName
		// + "' with MetaType ["
		// + metaValue.getMetaType()
		// + "] for ResourceType '"
		// + resourceType.getName() + "'.");
		// continue;
		// }
		// Property property = propertyAdapter.convertToProperty(metaValue,
		// propertyDefinition);
		// config.put(property);
		// }
		return null;
	}

	public static void convertConfigurationToManagedProperties(
			Map<String, ManagedProperty> managedProperties,
			Configuration configuration, ResourceType resourceType) {
		ConfigurationDefinition configDefinition = resourceType
				.getResourceConfigurationDefinition();
		for (ManagedProperty managedProperty : managedProperties.values()) {
			String propertyName = managedProperty.getName();
			PropertyDefinition propertyDefinition = configDefinition
					.get(propertyName);
			if (propertyDefinition == null) {
				// The managed property is not defined in the configuration
				continue;
			}
			populateManagedPropertyFromProperty(managedProperty,
					propertyDefinition, configuration);
		}
		return;
	}

	public static void populateManagedPropertyFromProperty(
			ManagedProperty managedProperty,
			PropertyDefinition propertyDefinition, Configuration configuration) {
		// If the ManagedProperty defines a default value, assume it's more
		// definitive than any default value that may
		// have been defined in the plugin descriptor, and update the
		// PropertyDefinition to use that as its default
		// value.
		// MetaValue defaultValue = managedProperty.getDefaultValue();
		// if (defaultValue != null)
		// updateDefaultValueOnPropertyDefinition(propertyDefinition,
		// defaultValue);
		// MetaValue metaValue = managedProperty.getValue();
		// PropertyAdapter propertyAdapter = null;
		// if (metaValue != null) {
		// LOG.trace("Populating existing MetaValue of type "
		// + metaValue.getMetaType() + " from Teiid property "
		// + propertyDefinition.getName() + " with definition " +
		// propertyDefinition
		// + "...");
		// propertyAdapter = PropertyAdapterFactory
		// .getPropertyAdapter(metaValue);
		//			
		// propertyAdapter.populateMetaValueFromProperty(configuration.getSimple(propertyDefinition.getName()),
		// metaValue,
		// propertyDefinition);
		// managedProperty.setValue(metaValue);
		// } else {
		// MetaType metaType = managedProperty.getMetaType();
		// if (propertyAdapter == null)
		// propertyAdapter = PropertyAdapterFactory
		// .getPropertyAdapter(metaType);
		// LOG.trace("Converting property " + propertyDefinition.getName() +
		// " with definition "
		// + propertyDefinition + " to MetaValue of type " + metaType
		// + "...");
		// metaValue =
		// propertyAdapter.convertToMetaValue(configuration.getSimple(propertyDefinition.getName()),
		// propertyDefinition, metaType);
		// managedProperty.setValue(metaValue);
		// }

	}

	public static MetaType convertPropertyDefinitionToMetaType(
			PropertyDefinition propDef) {
		MetaType memberMetaType;
		if (propDef instanceof PropertyDefinitionSimple) {
			PropertySimpleType propSimpleType = ((PropertyDefinitionSimple) propDef)
					.getType();
			memberMetaType = convertPropertySimpleTypeToSimpleMetaType(propSimpleType);
		} else if (propDef instanceof PropertyDefinitionList) {
			// TODO (very low priority, since lists of lists are not going to be
			// at all common)
			memberMetaType = null;
		} else if (propDef instanceof PropertyDefinitionMap) {
			Map<String, PropertyDefinition> memberPropDefs = ((PropertyDefinitionMap) propDef)
					.getPropertyDefinitions();
			if (memberPropDefs.isEmpty())
				throw new IllegalStateException(
						"PropertyDefinitionMap doesn't contain any member PropertyDefinitions."); //$NON-NLS-1$
			// NOTE: We assume member prop defs are all of the same type, since
			// for MapCompositeMetaTypes, they have to be.
			PropertyDefinition mapMemberPropDef = memberPropDefs.values()
					.iterator().next();
			MetaType mapMemberMetaType = convertPropertyDefinitionToMetaType(mapMemberPropDef);
			memberMetaType = new MapCompositeMetaType(mapMemberMetaType);
		} else {
			throw new IllegalStateException(
					"List member PropertyDefinition has unknown type: " //$NON-NLS-1$
							+ propDef.getClass().getName());
		}
		return memberMetaType;
	}

	private static MetaType convertPropertySimpleTypeToSimpleMetaType(
			PropertySimpleType memberSimpleType) {
		MetaType memberMetaType;
		Class memberClass;
		switch (memberSimpleType) {
		case BOOLEAN:
			memberClass = Boolean.class;
			break;
		case INTEGER:
			memberClass = Integer.class;
			break;
		case LONG:
			memberClass = Long.class;
			break;
		case FLOAT:
			memberClass = Float.class;
			break;
		case DOUBLE:
			memberClass = Double.class;
			break;
		default:
			memberClass = String.class;
			break;
		}
		memberMetaType = SimpleMetaType.resolve(memberClass.getName());
		return memberMetaType;
	}

}
