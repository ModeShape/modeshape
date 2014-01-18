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
package org.modeshape.jcr.query.lucene;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import org.apache.lucene.util.Version;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.cfg.spi.SearchConfigurationBase;
import org.hibernate.search.impl.SimpleInitializer;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.ServiceProvider;
import org.modeshape.common.logging.Logger;

/**
 * The Hibernate Search {@link SearchConfiguration} implementation that specifies how Hibernate Search should be configured.
 */
public abstract class LuceneSearchConfiguration extends SearchConfigurationBase {

    protected static final String HIBERNATE_PROPERTY_PREFIX = "hibernate.search.";
    protected static final String DEFAULT_INDEX = "default.";

    protected Logger logger = Logger.getLogger(LuceneSearchConfiguration.class);

    private final Map<String, Class<?>> classes = new HashMap<String, Class<?>>();
    private final Map<String, Class<?>> unmodifiableClasses;
    private final Properties properties = new Properties();
    private final InstanceInitializer initializer = SimpleInitializer.INSTANCE;

    /**
     * @param annotatedAndBridgeClasses the annotated classes that will be submitted to the indexer
     */
    protected LuceneSearchConfiguration( Class<?>... annotatedAndBridgeClasses ) {
        for (Class<?> annotatedAndBridgeClass : annotatedAndBridgeClasses) {
            classes.put(annotatedAndBridgeClass.getName(), annotatedAndBridgeClass);
        }
        unmodifiableClasses = Collections.unmodifiableMap(classes);
    }

    protected void setProperty( String name,
                                String value ) {
        if (value != null) this.properties.setProperty(name, value);
    }

    protected void setProperty( String name,
                                String value,
                                String defaultValue) {
        if (value != null) {
            this.properties.setProperty(name, value);
        } else if (defaultValue != null) {
            this.properties.setProperty(name, defaultValue);
        }
    }

    @SuppressWarnings( "deprecation" )
    public Version getVersion() {
        Version version = Version.valueOf(this.properties.getProperty("hibernate.search.lucene_version"));
        return version != null ? version : Version.LUCENE_CURRENT;
    }

    @Override
    public Iterator<Class<?>> getClassMappings() {
        return unmodifiableClasses.values().iterator();
    }

    @Override
    public Class<?> getClassMapping( String name ) {
        return classes.get(name);
    }

    @Override
    public String getProperty( String propertyName ) {
        return properties.getProperty(propertyName);
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public ReflectionManager getReflectionManager() {
        return null;
    }

    @Override
    public SearchMapping getProgrammaticMapping() {
        return null;
    }

    @Override
    public Map<Class<? extends ServiceProvider<?>>, Object> getProvidedServices() {
        return Collections.emptyMap();
    }

    @Override
    public boolean isTransactionManagerExpected() {
        // See MODE-1420; we use a transaction manager when updating indexes with changes made by sessions,
        // but a transaction is not used when manually re-indexing
        return false;
    }

    @Override
    public InstanceInitializer getInstanceInitializer() {
        return initializer;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<Object, Object>> iter = properties.entrySet().iterator();
        if (iter.hasNext()) {
            Map.Entry<Object, Object> entry = iter.next();
            sb.append(entry.getKey() + " = " + entry.getValue());
            while (iter.hasNext()) {
                entry = iter.next();
                sb.append("\n");
                sb.append(entry.getKey() + " = " + entry.getValue());
            }
        }
        return sb.toString();
    }
}
