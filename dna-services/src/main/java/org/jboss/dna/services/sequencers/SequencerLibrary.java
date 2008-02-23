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

package org.jboss.dna.services.sequencers;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.SystemFailureException;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.maven.MavenId;
import org.jboss.dna.maven.MavenRepository;

/**
 * Maintains the list of {@link ISequencer sequencers} for the system. This class does not actively update the sequencer
 * configurations, but is designed to properly maintain the sequencer instances when those configurations are changed by other
 * callers.
 * <p>
 * Therefore, this library does guarantee that the {@link #getSequencers() sequencers} at the time they are
 * {@link #getSequencers() obtained} are always reflected by the configurations.
 * </p>
 * @author Randall Hauch
 */
@ThreadSafe
public class SequencerLibrary {

    /**
     * The parent class loader that will be used when loading {@link ISequencer} instances. This may never be null, and will be
     * initialized in the constructor to be the first non-null class loader from:
     * <ol>
     * <li><code>Thread.currentThread().getContextClassLoader()</code></li>
     * <li>this.getClass().getClassLoader()</code></li>
     * </ol>
     */
    private ClassLoader parentClassLoader;
    private MavenRepository mavenRepository;

    /**
     * The list of sequencer instances. The index of each sequencer instance matches the corresponding configuration instance in
     * {@link #configurations}
     */
    @GuardedBy( value = "lock" )
    private final List<ISequencer> sequencerInstances = new CopyOnWriteArrayList<ISequencer>();
    private final Lock lock = new ReentrantLock();

    /**
     * Create a new library of sequencers.
     */
    public SequencerLibrary() {
        this.setParentClassLoader(null);
    }

    /**
     * Get the Maven Repository that should be used to load the sequencer classes. If not provided, the sequencer classes will be
     * loaded using the {@link #getParentClassLoader() parent class loader}.
     * @return mavenRepository the Maven repository used to load the sequencer classes, or null if no Maven repository is used.
     * @see #setMavenRepository(MavenRepository)
     */
    public MavenRepository getMavenRepository() {
        return this.mavenRepository;
    }

    /**
     * Set the Maven Repository that should be used to load the sequencer classes. If not provided, the sequencer classes will be
     * loaded using the {@link #getParentClassLoader() parent class loader}.
     * @param mavenRepository the Maven repository reference, or null if no Maven repository is to be used
     * @see #getMavenRepository()
     */
    public void setMavenRepository( MavenRepository mavenRepository ) {
        this.mavenRepository = mavenRepository;
    }

    /**
     * Get the parent class loader that's used when obtaining the class loader for each sequencer.
     * @return the parent class loader; never null
     * @see #setParentClassLoader(ClassLoader)
     */
    public ClassLoader getParentClassLoader() {
        return this.parentClassLoader;
    }

    /**
     * Set the parent class loader that's used when obtaining the class loader for each sequencer. The parent class loader may
     * never be null after this method is called. If null is supplied as the method parameter, the parent class loader will be set
     * to the first non-null value from:
     * <ol>
     * <li><code>Thread.currentThread().getContextClassLoader()</code></li>
     * <li>this.getClass().getClassLoader()</code></li>
     * </ol>
     * @param parentClassLoader the parent class loader, or null if the class loader should be obtained from the
     * {@link Thread#getContextClassLoader() current thread's context class loader} or, if that's null, this class'
     * {@link Class#getClassLoader() class loader}.
     * @see #getParentClassLoader()
     */
    public void setParentClassLoader( ClassLoader parentClassLoader ) {
        if (parentClassLoader == null) parentClassLoader = Thread.currentThread().getContextClassLoader();
        if (parentClassLoader == null) parentClassLoader = this.getClass().getClassLoader();
        assert parentClassLoader != null;
        this.parentClassLoader = parentClassLoader;
    }

    /**
     * Add the configuration for a sequencer, or update any existing one that represents the
     * {@link SequencerConfig#isSame(SequencerConfig) same configuration}
     * @param config the new configuration
     * @return this object for method chaining purposes
     * @throws IllegalArgumentException if <code>config</code> is null
     * @see #updateSequencer(SequencerConfig)
     * @see #removeSequencer(SequencerConfig)
     */
    public SequencerLibrary addSequencer( SequencerConfig config ) {
        if (config == null) {
            throw new IllegalArgumentException("A non-null sequencer configuration is required");
        }
        try {
            this.lock.lock();
            // Find an existing configuration that matches ...
            int index = findIndexOfMatchingConfiguration(config);
            if (index >= 0) {
                // See if the matching configuration has changed ...
                ISequencer existingSequencer = this.sequencerInstances.get(index);
                if (existingSequencer.getConfiguration().hasChanged(config)) {
                    // It has changed, so we need to replace it ...
                    this.sequencerInstances.set(index, newSequencer(config));
                }
            } else {
                // Didn't find one, so add it ...
                this.sequencerInstances.add(newSequencer(config));
            }
        } finally {
            this.lock.unlock();
        }
        return this;
    }

    /**
     * Update the configuration for a sequencer, or add it if there is no
     * {@link SequencerConfig#isSame(SequencerConfig) matching configuration}.
     * @param config the updated (or new) configuration
     * @return this object for method chaining purposes
     * @throws IllegalArgumentException if <code>config</code> is null
     * @see #addSequencer(SequencerConfig)
     * @see #removeSequencer(SequencerConfig)
     */
    public SequencerLibrary updateSequencer( SequencerConfig config ) {
        return addSequencer(config);
    }

    /**
     * Remove the configuration for a sequencer.
     * @param config the configuration to be removed
     * @return this object for method chaining purposes
     * @throws IllegalArgumentException if <code>config</code> is null
     * @see #addSequencer(SequencerConfig)
     * @see #updateSequencer(SequencerConfig)
     */
    public SequencerLibrary removeSequencer( SequencerConfig config ) {
        if (config == null) {
            throw new IllegalArgumentException("A non-null sequencer configuration is required");
        }
        try {
            this.lock.lock();
            // Find an existing configuration that matches ...
            int index = findIndexOfMatchingConfiguration(config);
            if (index >= 0) {
                // Remove the configuration and the sequencer instance ...
                this.sequencerInstances.remove(index);
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
    public List<ISequencer> getSequencers() {
        return Collections.unmodifiableList(this.sequencerInstances);
    }

    /**
     * Instantiate, configure and return a new sequencer described by the supplied configuration. This method does not manage the
     * returned instance.
     * @param config the configuration describing the sequencer
     * @return the new sequencer, or null if the sequencer could not be successfully configured
     * @throws IllegalArgumentException if the sequencer could not be configured properly
     */
    protected ISequencer newSequencer( SequencerConfig config ) {
        ClassLoader classLoader = this.getParentClassLoader();
        if (this.mavenRepository != null) {
            MavenId[] mavenIds = config.getSequencerClasspathArray();
            classLoader = this.mavenRepository.getClassLoader(classLoader, mavenIds);
        }
        assert classLoader != null;
        ISequencer newSequencer = null;
        try {
            Class<?> sequencerClass = classLoader.loadClass(config.getSequencerClassname());
            newSequencer = (ISequencer)sequencerClass.newInstance();
            newSequencer.setConfiguration(config);
        } catch (Throwable e) {
            throw new SystemFailureException(e);
        }
        if (newSequencer.getConfiguration() == null) {
            String msg = "The sequencer {1} was not configured and will not be used";
            msg = StringUtil.createString(msg, config.getName());
            throw new SystemFailureException(msg);
        }
        return newSequencer;
    }

    /**
     * Find the index for the matching {@link #configurations configuration} and {@link #sequencerInstances sequencer}.
     * @param config the configuration; may not be null
     * @return the index, or -1 if not found
     */
    @GuardedBy( value = "lock" )
    protected int findIndexOfMatchingConfiguration( SequencerConfig config ) {
        // Iterate through the configurations and look for an existing one that matches
        for (int i = 0, length = this.sequencerInstances.size(); i != length; i++) {
            SequencerConfig existingConfig = this.sequencerInstances.get(i).getConfiguration();
            assert existingConfig != null;
            if (existingConfig.isSame(config)) {
                return i;
            }
        }
        return -1;
    }

}
