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

/**
 * These are the Constants that used in conjunction with using the
 * 
 * @since 5.5.3
 */
public interface PluginConstants {

	/**
	 * These are properties required for connecting to the profile service and
	 * getting a handle to a specific component related to ModeShape.
	 */

	// The system key is the value used to obtain a connection.
	// In embedded, its a predefined value
	// In enterprise, its the installation directory
	//        public final static String INSTALL_DIR = "install.dir"; //$NON-NLS-1$
	/**
	 * These are global properties used by all components
	 */
	public final static String PROFILE_SERVICE = "ProfileService"; //$NON-NLS-1$

	/**
	 * Log4j log category to use
	 */
	public final static String DEFAULT_LOGGER_CATEGORY = "org.modeshape"; //$NON-NLS-1$

	/**
	 * Use these component type names when calling Connection related methods
	 * that require the type.
	 * 
	 * @since 1.0
	 */
	public interface ComponentType {

		public interface Engine {

			public final static String NAME = "Engine"; //$NON-NLS-1$
			public final static String MODESHAPE_TYPE = "ModeShape"; //$NON-NLS-1$
			public final static String MODESHAPE_SUB_TYPE = "Engine"; //$NON-NLS-1$
			public final static String MODESHAPE_ENGINE = "ModeShapeEngine"; //$NON-NLS-1$

			public static interface Operations {

			}

			public static interface Metrics {

			}
		}

	}
}
