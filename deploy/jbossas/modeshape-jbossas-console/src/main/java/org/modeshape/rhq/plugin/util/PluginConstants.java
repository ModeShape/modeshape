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
 * These are properties required for connecting to the profile service and
 * getting a handle to a specific component related to ModeShape.
 * @since 2.1
 */
public interface PluginConstants {

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
			public final static String MODESHAPE_DISPLAYNAME = "ModeShape"; //$NON-NLS-1$
			public final static String MODESHAPE_ENGINE_DESC = "A ModeShape Engine instance"; //$NON-NLS-1$
			

			public static interface Operations {
				public final static String SHUTDOWN = "shutdown"; //$NON-NLS-1$
				public final static String RESTART = "restart"; //$NON-NLS-1$
				
			}

			public static interface Metrics {

			}
		}
		
		public interface Repository {

			public final static String NAME = "Repository"; //$NON-NLS-1$
			public final static String MODESHAPE_REPOSITORY_DESC = "An information store with hierarchical organization, versioning, events, search, query, and automated content extraction"; //$NON-NLS-1$

			public static interface Operations {
				
				public static interface Parameters {
					public final static String REPOSITORY_NAME = "repositoryName"; //$NON-NLS-1$
				}
			}
	
			public static interface Metrics {
				public final static String ACTIVESESSIONS = "getActiveSessions"; //$NON-NLS-1$
			}
		}
		
		public interface Connector {

			public final static String NAME = "Connector"; //$NON-NLS-1$
			public final static String DESCRIPTION = "A specification of a resource that can be used to access or store repository information"; //$NON-NLS-1$
			
			public static interface Operations {
				
				//Connector operations
				public final static String PING = "pingConnector"; //$NON-NLS-1$
				
				//Connection pool operations
				// public final static String FLUSH = "flush"; //$NON-NLS-1$
				
				public static interface Parameters {
					public final static String CONNECTOR_NAME = "connectorName"; //$NON-NLS-1$
				}
			}

			public static interface Metrics {
				public final static String INUSECONNECTIONS = "getInUseConnections"; //$NON-NLS-1$
			}
		}

		public interface SequencingService {

			public final static String NAME = "ModeShapeSequencingService"; //$NON-NLS-1$
			public final static String MODESHAPE_TYPE = "ModeShape"; //$NON-NLS-1$
			public final static String MODESHAPE_SUB_TYPE = "SequencingService"; //$NON-NLS-1$
			public final static String DISPLAY_NAME = "Sequencing Service"; //$NON-NLS-1$
			public final static String DESC = "A ModeShape sequencing service"; //$NON-NLS-1$
			
			public static interface Operations {

			}

			public static interface Metrics {
				public final static String NUM_NODES_SEQUENCED = "getNodesSequencedCount"; //$NON-NLS-1$
				public final static String NUM_NODES_SKIPPED = "getNodesSkippedCount"; //$NON-NLS-1$
			}
		}
		
		public interface SequencerConfig {

			public final static String NAME = "Sequencer"; //$NON-NLS-1$
			public final static String DISPLAY_NAME = "Sequencer"; //$NON-NLS-1$
			public final static String DESC = "A ModeShape sequencer"; //$NON-NLS-1$
			
			public static interface Operations {

			}

			public static interface Metrics {

			}
		}
	}
}
