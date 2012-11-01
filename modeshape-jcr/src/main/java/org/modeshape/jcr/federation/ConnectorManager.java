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

package org.modeshape.jcr.federation;

import java.util.HashMap;
import java.util.Map;
import org.infinispan.schematic.internal.document.BasicDocument;
import org.modeshape.jcr.cache.NodeKey;

/**
 * Class which maintains (based on the configuration) the list of available connectors for a repository.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 * //TODO author=Horia Chiorean date=11/1/12 description=This should get the configuration from the running state and initialize the connectors
 */
public class ConnectorManager {

    private final Map<String, Connector> sourceKeyToConnectorMap;

    public ConnectorManager( Map<String, Connector> sourceKeyToConnectorMap ) {
        this.sourceKeyToConnectorMap = sourceKeyToConnectorMap;
    }

    public ConnectorManager() {
        this.sourceKeyToConnectorMap = new HashMap<String, Connector>();
        this.sourceKeyToConnectorMap.put(MockConnector.SOURCE_KEY, new MockConnector());
    }

    public void addConnector(String sourceName, Connector connector) {
        sourceKeyToConnectorMap.put(NodeKey.keyForSourceName(sourceName), connector);
    }

    public Connector getConnectorForSourceName( String sourceName ) {
        assert sourceName != null;
        return sourceKeyToConnectorMap.get(NodeKey.keyForSourceName(sourceName));
    }

    public Connector getConnectorForSourceKey( String sourceKey ) {
        return sourceKeyToConnectorMap.get(sourceKey);
    }

    public boolean hasConnectors() {
        return !sourceKeyToConnectorMap.isEmpty();
    }


    //TODO author=Horia Chiorean date=11/1/12 description=Should be removed after POC
    public static class MockConnector implements Connector {
        public static final String SOURCE_NAME = "mock-source";
        public static final String SOURCE_KEY = NodeKey.keyForSourceName(SOURCE_NAME);

        @Override
        public boolean containsKey( String key ) {
            return false;
        }

        @Override
        public String getSourceName() {
            return SOURCE_NAME;
        }

        @Override
        public Source getSource() {
            return null;
        }

        @Override
        public BasicDocument get( String key ) {
            return null;
        }

        @Override
        public BasicDocument put( String key,
                                  BasicDocument document,
                                  BasicDocument metadata ) {
            return null;
        }

        @Override
        public BasicDocument putIfAbsent( String key,
                                          BasicDocument document,
                                          BasicDocument metadata ) {
            return null;
        }

        @Override
        public BasicDocument remove( String key ) {
            return null;
        }
    }
}
