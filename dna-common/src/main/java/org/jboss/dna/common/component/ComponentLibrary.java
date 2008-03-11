/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.dna.common.component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.SystemFailureException;
import org.jboss.dna.common.util.StringUtil;

/**
 * Maintains the list of component instances for the system. This class does not actively update the component configurations, but
 * is designed to properly maintain the sequencer instances when those configurations are changed by other callers.
 * <p>
 * Therefore, this library does guarantee that the {@link #getInstances() instances} at the time they are
 * {@link #getInstances() obtained} are always reflected by the configurations.
 * </p>
 * @author Randall Hauch
 * @param <T> the type of the component
 */
@ThreadSafe
public class ComponentLibrary<T extends Component> {

    /**
     * Class loader factory instance that always returns the
     * {@link Thread#getContextClassLoader() current thread's context class loader} (if not null) or component library's class
     * loader.
     */
    public static final ClassLoaderFactory DEFAULT = new ClassLoaderFactory() {

        /**
         * {@inheritDoc}
         */
        public ClassLoader getClassLoader( String... classpath ) {
            ClassLoader result = Thread.currentThread().getContextClassLoader();
            if (result == null) result = this.getClass().getClassLoader();
            return result;
        }
    };

    /**
     * The class loader factory
     */
    private final AtomicReference<ClassLoaderFactory> classLoaderFactory = new AtomicReference<ClassLoaderFactory>(DEFAULT);

    /**
     * The list of component instances. The index of each component instance matches the corresponding configuration instance in
     * {@link #configurations}
     */
    @GuardedBy( value = "lock" )
    private final List<T> instances = new CopyOnWriteArrayList<T>();
    private final Lock lock = new ReentrantLock();

    /**
     * Create a new library of components.
     */
    public ComponentLibrary() {
    }

    /**
     * Get the class loader factory that should be used to load the component classes. Unless changed, the library uses the
     * {@link #DEFAULT default} class loader factory, which uses the
     * {@link Thread#getContextClassLoader() current thread's context class loader} if not null or the class loader that loaded
     * the library class.
     * @return the class loader factory; never null
     * @see #setClassLoaderFactory(ClassLoaderFactory)
     */
    public ClassLoaderFactory getClassLoaderFactory() {
        return this.classLoaderFactory.get();
    }

    /**
     * Set the Maven Repository that should be used to load the sequencer classes. Unless changed, the library uses the
     * {@link #DEFAULT default} class loader factory, which uses the
     * {@link Thread#getContextClassLoader() current thread's context class loader} if not null or the class loader that loaded
     * the library class.
     * @param classLoaderFactory the class loader factory reference
     * @see #getClassLoaderFactory()
     * @throws IllegalArgumentException if the reference is null
     */
    public void setClassLoaderFactory( ClassLoaderFactory classLoaderFactory ) {
        if (classLoaderFactory == null) throw new IllegalArgumentException("The class loader factory reference may not be null");
        this.classLoaderFactory.set(classLoaderFactory);
    }

    /**
     * Add the configuration for a sequencer, or update any existing one that represents the
     * {@link ComponentConfig#isSame(ComponentConfig) same configuration}
     * @param config the new configuration
     * @return this object for method chaining purposes
     * @throws IllegalArgumentException if <code>config</code> is null
     * @see #updateComponent(ComponentConfig)
     * @see #removeComponent(ComponentConfig)
     */
    public ComponentLibrary addComponent( ComponentConfig config ) {
        if (config == null) {
            throw new IllegalArgumentException("A non-null component configuration is required");
        }
        try {
            this.lock.lock();
            // Find an existing configuration that matches ...
            int index = findIndexOfMatchingConfiguration(config);
            if (index >= 0) {
                // See if the matching configuration has changed ...
                T existingInstance = this.instances.get(index);
                if (existingInstance.getConfiguration().hasChanged(config)) {
                    // It has changed, so we need to replace it ...
                    this.instances.set(index, newInstance(config));
                }
            } else {
                // Didn't find one, so add it ...
                this.instances.add(newInstance(config));
            }
        } finally {
            this.lock.unlock();
        }
        return this;
    }

    /**
     * Update the configuration for a sequencer, or add it if there is no
     * {@link ComponentConfig#isSame(ComponentConfig) matching configuration}.
     * @param config the updated (or new) configuration
     * @return this object for method chaining purposes
     * @throws IllegalArgumentException if <code>config</code> is null
     * @see #addComponent(ComponentConfig)
     * @see #removeComponent(ComponentConfig)
     */
    public ComponentLibrary updateComponent( ComponentConfig config ) {
        return addComponent(config);
    }

    /**
     * Remove the configuration for a sequencer.
     * @param config the configuration to be removed
     * @return this object for method chaining purposes
     * @throws IllegalArgumentException if <code>config</code> is null
     * @see #addComponent(ComponentConfig)
     * @see #updateComponent(ComponentConfig)
     */
    public ComponentLibrary removeComponent( ComponentConfig config ) {
        if (config == null) {
            throw new IllegalArgumentException("A non-null sequencer configuration is required");
        }
        try {
            this.lock.lock();
            // Find an existing configuration that matches ...
            int index = findIndexOfMatchingConfiguration(config);
            if (index >= 0) {
                // Remove the configuration and the sequencer instance ...
                this.instances.remove(index);
            }
        } finally {
            this.lock.unlock();
        }
        return this;
    }

    /**
     * Return the list of sequencers.
     * @return the unmodifiable list of sequencers; never null
     */
    public List<T> getInstances() {
        return Collections.unmodifiableList(this.instances);
    }

    /**
     * Instantiate, configure and return a new sequencer described by the supplied configuration. This method does not manage the
     * returned instance.
     * @param config the configuration describing the sequencer
     * @return the new sequencer, or null if the sequencer could not be successfully configured
     * @throws IllegalArgumentException if the sequencer could not be configured properly
     */
    @SuppressWarnings( "unchecked" )
    protected T newInstance( ComponentConfig config ) {
        String[] classpath = config.getComponentClasspathArray();
        final ClassLoader classLoader = this.getClassLoaderFactory().getClassLoader(classpath);
        assert classLoader != null;
        T newInstance = null;
        try {
            Class<?> componentClass = classLoader.loadClass(config.getComponentClassname());
            newInstance = (T)componentClass.newInstance();
            newInstance.setConfiguration(config);
        } catch (Throwable e) {
            throw new SystemFailureException(e);
        }
        if (newInstance.getConfiguration() == null) {
            String msg = "The component {1} was not configured and will not be used";
            msg = StringUtil.createString(msg, config.getName());
            throw new SystemFailureException(msg);
        }
        return newInstance;
    }

    /**
     * Find the index for the matching {@link #configurations configuration} and {@link #sequencerInstances sequencer}.
     * @param config the configuration; may not be null
     * @return the index, or -1 if not found
     */
    @GuardedBy( value = "lock" )
    protected int findIndexOfMatchingConfiguration( ComponentConfig config ) {
        // Iterate through the configurations and look for an existing one that matches
        for (int i = 0, length = this.instances.size(); i != length; i++) {
            ComponentConfig existingConfig = this.instances.get(i).getConfiguration();
            assert existingConfig != null;
            if (existingConfig.isSame(config)) {
                return i;
            }
        }
        return -1;
    }

}
