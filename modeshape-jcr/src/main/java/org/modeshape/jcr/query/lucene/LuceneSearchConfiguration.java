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
import org.hibernate.search.impl.SimpleInitializer;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.ServiceProvider;

/**
 * The Hibernate Search {@link SearchConfiguration} implementation that specifies how Hibernate Search should be configured.
 */
public abstract class LuceneSearchConfiguration implements SearchConfiguration {

    protected static final String HIBERNATE_PROPERTY_PREFIX = "hibernate.search.";
    protected static final String DEFAULT_INDEX = "default.";

    private final Map<String, Class<?>> classes = new HashMap<String, Class<?>>();
    private final Map<String, Class<?>> unmodifiableClasses;
    private final Properties properties = new Properties();
    private final InstanceInitializer initializer = new SimpleInitializer();

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
        // return properties.toString();
    }
}
