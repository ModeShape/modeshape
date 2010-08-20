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

import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import org.jboss.managed.api.ManagedComponent;
import org.jboss.metatype.api.values.MetaValue;
import org.mc4j.ems.connection.EmsConnection;
import org.modeshape.rhq.plugin.util.ModeShapeManagementView;
import org.modeshape.rhq.plugin.util.ProfileServiceUtil;
import org.modeshape.rhq.plugin.util.PluginConstants.ComponentType;
import org.modeshape.rhq.plugin.util.PluginConstants.ComponentType.Engine;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.plugins.jbossas5.ApplicationServerComponent;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;

public class EngineComponent extends Facet {
	
	/**
	 * {@inheritDoc}
	 * 
	 * @see org.modeshape.rhq.plugin.Facet#getComponentType()
	 */
	@Override
	String getComponentType() {
		return ComponentType.Engine.MODESHAPE_ENGINE;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.modeshape.rhq.plugin.Facet#getAvailability()
	 */
	@Override
	public AvailabilityType getAvailability() {
		AvailabilityType isRunning = AvailabilityType.DOWN;

		try {
			ManagedComponent mc = ProfileServiceUtil.getManagedEngine(this
					.getConnection());
			MetaValue running = ModeShapeManagementView
					.executeManagedOperation(mc, "isRunning",
							new MetaValue[] { null });
			if (ProfileServiceUtil.booleanValue(running).equals(Boolean.TRUE)) {
				isRunning = AvailabilityType.UP;
			}
		} catch (NamingException e) {
			LOG.error("NamingException in getAvailability", e);
		} catch (Exception e) {
			LOG.error("Exception in getAvailability", e);
		}

		return isRunning;
	}
	
	
	/**
	 * {@inheritDoc}
	 *
	 * @see org.modeshape.rhq.plugin.Facet#setOperationArguments(java.lang.String, org.rhq.core.domain.configuration.Configuration, java.util.Map)
	 */
	@Override
	protected void setOperationArguments(String name,
			Configuration configuration, Map<String, Object> valueMap) {
		// Parameter logic for engine Operations
		if (name.equals(Engine.Operations.SHUTDOWN)) {
			//no parms
		} else if (name.equals(Engine.Operations.RESTART)) {
			//no parms
		} 
	}

	


	/**
	 * {@inheritDoc}
	 * 
	 * @see org.modeshape.rhq.plugin.Facet#getValues(org.rhq.core.domain.measurement.MeasurementReport,
	 *      java.util.Set)
	 */
	@Override
	public void getValues(MeasurementReport arg0,
			Set<MeasurementScheduleRequest> arg1) throws Exception {
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.rhq.core.pluginapi.inventory.CreateChildResourceFacet#createResource(org.rhq.core.pluginapi.inventory.CreateResourceReport)
	 */
	@Override
	public CreateResourceReport createResource(CreateResourceReport arg0) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.rhq.plugins.jbossas5.ProfileServiceComponent#getConnection()
	 */
	@Override
	public ProfileServiceConnection getConnection() {
		return ((ApplicationServerComponent) this.resourceContext
				.getParentResourceComponent()).getConnection();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.rhq.plugins.jmx.JMXComponent#getEmsConnection()
	 */
	@Override
	public EmsConnection getEmsConnection() {
		return null;
	}

}
