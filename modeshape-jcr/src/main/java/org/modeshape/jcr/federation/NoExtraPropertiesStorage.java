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

import java.util.Collections;
import java.util.Map;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.ValueFactory;

/**
 * An {@link ExtraPropertiesStore} implementation that throws an exception whenever trying to store or update extra properties.
 */
public class NoExtraPropertiesStorage implements ExtraPropertiesStore {

    protected static final Map<Name, Property> NO_PROPERTIES_MAP = Collections.emptyMap();

    private final ValueFactory<String> strings;
    private final String sourceName;

    public NoExtraPropertiesStorage( Connector connector ) {
        strings = connector.factories().getStringFactory();
        sourceName = connector.getSourceName();
    }

    @Override
    public Map<Name, Property> getProperties( String id ) {
        return NO_PROPERTIES_MAP;
    }

    @Override
    public boolean removeProperties( String id ) {
        return false;
    }

    @Override
    public void storeProperties( String id,
                                 Map<Name, Property> properties ) {
        String names = null;
        int count = 0;
        for (Map.Entry<Name, Property> entry : properties.entrySet()) {
            if (entry.getValue() != null) {
                String name = strings.create(entry.getKey());
                if (names == null) names = name;
                else names = ", " + name;
                ++count;
            }
        }
        if (count == 0) return;
        String msg = null;
        if (count == 1) {
            msg = JcrI18n.couldNotStoreProperty.text(sourceName, id, names);
        } else {
            msg = JcrI18n.couldNotStoreProperties.text(sourceName, id, names);
        }
        throw new DocumentStoreException(id, msg);
    }

    @Override
    public void updateProperties( String id,
                                  Map<Name, Property> properties ) {
        storeProperties(id, properties);
    }
}
