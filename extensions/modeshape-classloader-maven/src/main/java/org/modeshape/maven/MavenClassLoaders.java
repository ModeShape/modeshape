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
package org.modeshape.maven;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.modeshape.common.SystemFailureException;

/**
 * A managed collection of {@link ClassLoader class loaders} that access JARs in a JCR repository, processing dependencies
 * according to <a
 * href="http://maven.apache.org/guides/introduction/introduction-to-optional-and-excludes-dependencies.html">Maven 2 transitive
 * dependency rules</a>. Each {@link MavenRepository} instance owns an instance of this class, which provides a cached set of
 * class loaders and a facility for {@link MavenRepository#getClassLoader(ClassLoader, MavenId...) getting class loaders} based
 * upon a set of one or more versioned libraries.
 */
/* package */class MavenClassLoaders {

    private final MavenRepository repository;
    private final Lock lock = new ReentrantLock();

    /**
     * The class loaders, keyed by Maven project ID, that are responsible for the project's classpath. Basically, each project
     * class loader delegates first to the JAR file class loader, and if not found, delegates to the project class loaders for
     * each of the project's dependencies.
     * <p>
     * These class loaders are loaded lazily and are never removed from the map, but may be changed as dependencies of the project
     * are updated.
     * </p>
     */
    private final Map<MavenId, ProjectClassLoader> projectClassLoaders = new HashMap<MavenId, ProjectClassLoader>();

    /**
     * Create with a specified repository and optionally the parent class loader that should be consulted first and a default
     * class loader that should be consulted after all others.
     * 
     * @param repository the Maven repository; may not be null
     */
    /* package */MavenClassLoaders( MavenRepository repository ) {
        this.repository = repository;
    }

    protected ProjectClassLoader getProjectClassLoader( MavenId mavenId ) {
        ProjectClassLoader result = null;
        try {
            this.lock.lock();
            result = this.projectClassLoaders.get(mavenId);
            if (result == null) {
                // The project has not yet been loaded, so get URL to the JAR file and get the dependencies ...
                URL jarFileUrl = this.repository.getUrl(mavenId, ArtifactType.JAR, null);
                URLClassLoader jarFileLoader = new URLClassLoader(new URL[] {jarFileUrl}, null);

                List<MavenDependency> dependencies = this.repository.getDependencies(mavenId);
                result = new ProjectClassLoader(mavenId, jarFileLoader);
                result.setDependencies(dependencies);
                this.projectClassLoaders.put(mavenId, result);
            }
        } catch (MalformedURLException e) {
            // This really should never happen, but if it does ...
            throw new SystemFailureException(MavenI18n.errorGettingUrlForMavenProject.text(mavenId), e);
        } finally {
            this.lock.unlock();
        }
        return result;
    }

    public ProjectClassLoader getClassLoader( ClassLoader parent,
                                              MavenId... mavenIds ) {
        if (parent == null) parent = Thread.currentThread().getContextClassLoader();
        if (parent == null) parent = this.getClass().getClassLoader();
        ProjectClassLoader result = new ProjectClassLoader(parent);
        // Create a dependencies list for the desired projects ...
        List<MavenDependency> dependencies = new ArrayList<MavenDependency>();
        for (MavenId mavenId : mavenIds) {
            if (!dependencies.contains(mavenId)) {
                MavenDependency dependency = new MavenDependency(mavenId);
                dependencies.add(dependency);
            }
        }
        result.setDependencies(dependencies);
        return result;
    }

    public void notifyChangeInDependencies( MavenId mavenId ) {
        List<MavenDependency> dependencies = this.repository.getDependencies(mavenId);
        try {
            this.lock.lock();
            ProjectClassLoader existingLoader = this.projectClassLoaders.get(mavenId);
            if (existingLoader != null) {
                existingLoader.setDependencies(dependencies);
            }
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * A project class loader is responsible for loading all classes and resources for the project, including delegating to
     * dependent projects if required and adhearing to all stated exclusions.
     * 
     * @author Randall Hauch
     */
    protected class ProjectClassLoader extends ClassLoader {

        private final MavenId mavenId;
        private final URLClassLoader jarFileClassLoader;
        private final Map<MavenId, ProjectClassLoader> dependencies = new LinkedHashMap<MavenId, ProjectClassLoader>();
        private final Map<MavenId, Set<MavenId>> exclusions = new HashMap<MavenId, Set<MavenId>>();
        private final ReadWriteLock dependencyLock = new ReentrantReadWriteLock();

        /**
         * Create a class loader for the given project.
         * 
         * @param mavenId
         * @param jarFileClassLoader
         * @see MavenClassLoaders#getClassLoader(ClassLoader, MavenId...)
         */
        protected ProjectClassLoader( MavenId mavenId,
                                      URLClassLoader jarFileClassLoader ) {
            super(null);
            this.mavenId = mavenId;
            this.jarFileClassLoader = jarFileClassLoader;
        }

        /**
         * Create a class loader that doesn't reference a top level project, but instead references multiple projects.
         * 
         * @param parent
         * @see MavenClassLoaders#getClassLoader(ClassLoader, MavenId...)
         */
        protected ProjectClassLoader( ClassLoader parent ) {
            super(parent);
            this.mavenId = null;
            this.jarFileClassLoader = null;
        }

        protected void setDependencies( List<MavenDependency> dependencies ) {
            try {
                this.dependencyLock.writeLock().lock();
                this.dependencies.clear();
                if (dependencies != null) {
                    // Find all of the project class loaders for the dependent projects ...
                    for (MavenDependency dependency : dependencies) {
                        ProjectClassLoader dependencyClassLoader = MavenClassLoaders.this.getProjectClassLoader(dependency.getId());
                        if (dependencyClassLoader != null) {
                            MavenId dependencyId = dependency.getId();
                            this.dependencies.put(dependencyId, dependencyClassLoader);
                            this.exclusions.put(dependencyId, Collections.unmodifiableSet(dependency.getExclusions()));
                        }
                    }
                }
            } finally {
                this.dependencyLock.writeLock().unlock();
            }
        }

        /**
         * Finds the resource with the given name. This implementation first consults the class loader for this project's JAR
         * file, and failing that, consults all of the class loaders for the dependent projects.
         * 
         * @param name The resource name
         * @return A <tt>URL</tt> object for reading the resource, or <tt>null</tt> if the resource could not be found or the
         *         invoker doesn't have adequate privileges to get the resource.
         */
        @Override
        protected URL findResource( String name ) {
            // This method is called only by the top-level class loader handling a request.
            return findResource(name, null);
        }

        /**
         * This method is called by the top-level class loader {@link #findResource(String) when finding a resource}. <i>In fact,
         * this method should only be directly called by test methods; subclasses should call {@link #findResource(String)}.</i>
         * <p>
         * This method first looks in this project's JAR. If the resource is not found, then this method
         * {@link #findResource(String, Set, Set, List) searches the dependencies}. This method's signature allows for a list to
         * be supplied for reporting the list of projects that make up the searched classpath (excluding those that were never
         * searched because the resource was found).
         * </p>
         * 
         * @param name The resource name
         * @param debugSearchPath the list into which the IDs of the searched projects will be placed; may be null if this
         *        information is not needed
         * @return A <tt>URL</tt> object for reading the resource, or <tt>null</tt> if the resource could not be found or the
         *         invoker doesn't have adequate privileges to get the resource.
         */
        protected URL findResource( String name,
                                    List<MavenId> debugSearchPath ) {
            Set<MavenId> processed = new HashSet<MavenId>();
            // This method is called only by the top-level class loader handling a request.
            // Therefore, first look in this project's JAR file ...
            URL result = null;
            if (this.jarFileClassLoader != null) {
                result = this.jarFileClassLoader.getResource(name);
                processed.add(this.mavenId);
            }
            if (debugSearchPath != null && this.mavenId != null) debugSearchPath.add(this.mavenId);

            if (result == null) {
                // Look in the dependencies ...
                result = findResource(name, processed, null, debugSearchPath);
            }
            return result;
        }

        protected URL findResource( String name,
                                    Set<MavenId> processed,
                                    Set<MavenId> exclusions,
                                    List<MavenId> debugSearchPath ) {
            // If this project is to be excluded, then simply return ...
            if (exclusions != null && exclusions.contains(this.mavenId)) return null;

            // Check the class loaders for the dependencies.
            URL result = null;
            try {
                this.dependencyLock.readLock().lock();
                // First, look in the immediate dependencies ...
                for (Map.Entry<MavenId, ProjectClassLoader> entry : this.dependencies.entrySet()) {
                    ProjectClassLoader loader = entry.getValue();
                    MavenId id = loader.mavenId;
                    if (processed.contains(id)) continue;
                    if (exclusions != null && exclusions.contains(id)) continue;
                    result = loader.jarFileClassLoader.findResource(name);
                    processed.add(id);
                    if (debugSearchPath != null && id != null) debugSearchPath.add(id);
                    if (result != null) break;
                }
                if (result == null) {
                    // Still not found, so look in the dependencies of the immediate dependencies ...
                    for (Map.Entry<MavenId, ProjectClassLoader> entry : this.dependencies.entrySet()) {
                        MavenId dependency = entry.getKey();
                        ProjectClassLoader loader = entry.getValue();
                        // Get the exclusions for this dependency ...
                        Set<MavenId> dependencyExclusions = this.exclusions.get(dependency);
                        if (!dependencyExclusions.isEmpty()) {
                            // Create a new set of exclusions for this branch ...
                            if (exclusions == null) {
                                exclusions = new HashSet<MavenId>();
                            } else {
                                exclusions = new HashSet<MavenId>(exclusions);
                            }
                            // Then add this dependencies exclusion to the set ...
                            exclusions.addAll(dependencyExclusions);
                        }
                        result = loader.findResource(name, processed, exclusions, debugSearchPath);
                        if (result != null) break;
                    }
                }
            } finally {
                this.dependencyLock.readLock().unlock();
            }
            return super.findResource(name);
        }
    }
}
