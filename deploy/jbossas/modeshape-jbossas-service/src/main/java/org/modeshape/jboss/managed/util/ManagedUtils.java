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
package org.modeshape.jboss.managed.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.modeshape.common.util.Logger;
import org.modeshape.common.util.Reflection;
import org.modeshape.common.util.Logger.Level;
import org.modeshape.common.util.Reflection.Property;
import org.modeshape.graph.connector.RepositoryConnectionPool;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.jboss.managed.JBossManagedI18n;
import org.modeshape.jboss.managed.ManagedEngine.Component;
import org.modeshape.jboss.managed.ManagedEngine.ManagedProperty;
import org.modeshape.repository.sequencer.SequencerConfig;

/**
 * Class for common utility methods used for ModeShape Managed Objects
 */
public class ManagedUtils {

	private static final Logger LOGGER = Logger.getLogger(ManagedUtils.class);

	public static List<ManagedProperty> getProperties(Component objectType,
			Object object) {

		Reflection reflection = null;

		if (objectType.equals(Component.CONNECTOR)) {
			reflection = new Reflection(RepositorySource.class);
		} else if (objectType.equals(Component.CONNECTIONPOOL)) {
			reflection = new Reflection(RepositoryConnectionPool.class);
		} else if (objectType.equals(Component.SEQUENCER)) {
			reflection = new Reflection(SequencerConfig.class);
		}

		List<Property> props = null;
		List<ManagedProperty> managedProps = new ArrayList<ManagedProperty>();
		try {
			if (reflection != null){
				props = reflection.getAllPropertiesOn(object);
			}
		} catch (SecurityException e) {
			LOGGER.log(Level.ERROR, e,
					JBossManagedI18n.errorGettingPropertiesFromManagedObject,
					objectType);
		} catch (IllegalArgumentException e) {
			{
				LOGGER
						.log(
								Level.ERROR,
								e,
								JBossManagedI18n.errorGettingPropertiesFromManagedObject,
								objectType);
			}
		} catch (NoSuchMethodException e) {
			{
				LOGGER
						.log(
								Level.ERROR,
								e,
								JBossManagedI18n.errorGettingPropertiesFromManagedObject,
								objectType);
			}
		} catch (IllegalAccessException e) {
			{
				LOGGER
						.log(
								Level.ERROR,
								e,
								JBossManagedI18n.errorGettingPropertiesFromManagedObject,
								objectType);
			}
		} catch (InvocationTargetException e) {
			{
				LOGGER
						.log(
								Level.ERROR,
								e,
								JBossManagedI18n.errorGettingPropertiesFromManagedObject,
								objectType);
			}
		}

		if (props != null) {
			for (Property prop : props) {
				if (prop.getType().isPrimitive()
						|| prop.getType().toString().contains(
								"java.lang.String")) {
					if (prop.getValue().getClass().isArray()) {
						StringBuffer sb = new StringBuffer();
						String[] stringArray = (String[]) prop.getValue();
						for (String cell : stringArray) {
							sb.append(cell).append(" ");
						}
						prop.setValue(sb.toString());
					}
					managedProps.add(new ManagedProperty(prop));
				}
			}
		}

		return managedProps;
	}

}
