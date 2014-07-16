/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.federation;

import java.util.Collections;
import java.util.Map;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.spi.federation.Connector;
import org.modeshape.jcr.spi.federation.ExtraPropertiesStore;
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
        strings = connector.getContext().getValueFactories().getStringFactory();
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

    @Override
    public boolean contains( String id ) {
        return false;
    }
}
