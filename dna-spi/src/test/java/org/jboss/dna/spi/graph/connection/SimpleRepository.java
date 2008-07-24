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
package org.jboss.dna.spi.graph.connection;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.NameFactory;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.PropertyFactory;
import org.jboss.dna.spi.graph.connection.ExecutionEnvironment;

/**
 * A very simple repository that maintains properties for nodes identified by a path, and computes the children based upon the set
 * of paths registered in the {@link #getData() data}.
 * <p>
 * Note that the repository does not automatically rename same-name siblings when nodes are
 * {@link #delete(ExecutionEnvironment, String) deleted} or {@link #create(ExecutionEnvironment, String) explicitly} or
 * {@link #setProperty(ExecutionEnvironment, String, String, Object...) implicitly} created.
 * </p>
 * 
 * @author Randall Hauch
 */
@ThreadSafe
public class SimpleRepository {

    private static final ConcurrentMap<String, SimpleRepository> repositoriesByName = new ConcurrentHashMap<String, SimpleRepository>();

    public static SimpleRepository get( String name ) {
        return repositoriesByName.get(name);
    }

    public static void shutdownAll() {
        for (SimpleRepository repository : repositoriesByName.values()) {
            repository.shutdown();
        }
    }

    private ConcurrentMap<Path, Map<Name, Property>> data = new ConcurrentHashMap<Path, Map<Name, Property>>();
    private final String repositoryName;
    private boolean shutdown = false;

    public SimpleRepository( String repositoryName ) {
        this.repositoryName = repositoryName;
        if (repositoriesByName.putIfAbsent(repositoryName, this) != null) {
            throw new IllegalArgumentException("Repository \"" + repositoryName + "\" already exists and may not be recreated");
        }
    }

    /**
     * @return repositoryName
     */
    public String getRepositoryName() {
        return repositoryName;
    }

    /**
     * Get the current modifiable map of property data
     * 
     * @return data
     */
    public ConcurrentMap<Path, Map<Name, Property>> getData() {
        return data;
    }

    /**
     * Utility method to help set the property on the node given by the supplied path. If the node does not exist, it will be
     * created.
     * 
     * @param env the environment; may not be null
     * @param path the path to the node; may not be null
     * @param propertyName the property name; may not be null
     * @param values the values of the property
     * @return this repository, for method chaining
     */
    public SimpleRepository setProperty( ExecutionEnvironment env,
                                         String path,
                                         String propertyName,
                                         Object... values ) {
        PathFactory pathFactory = env.getValueFactories().getPathFactory();
        NameFactory nameFactory = env.getValueFactories().getNameFactory();
        PropertyFactory propertyFactory = env.getPropertyFactory();
        Path pathObj = pathFactory.create(path);
        create(env, pathObj.getAncestor().getString(env.getNamespaceRegistry()));
        Property property = propertyFactory.create(nameFactory.create(propertyName), values);
        Map<Name, Property> properties = new HashMap<Name, Property>();
        Map<Name, Property> existingProperties = data.putIfAbsent(pathObj, properties);
        if (existingProperties == null) existingProperties = properties;
        existingProperties.put(property.getName(), property);
        return this;
    }

    /**
     * Create the node if it does not exist.
     * 
     * @param env the environment; may not be null
     * @param path the path to the node; may not be null
     * @return this repository, for method chaining
     */
    public SimpleRepository create( ExecutionEnvironment env,
                                    String path ) {
        PathFactory pathFactory = env.getValueFactories().getPathFactory();
        Path pathObj = pathFactory.create(path);
        Path ancestorPath = pathObj.getAncestor();
        while (!ancestorPath.isRoot()) {
            data.putIfAbsent(ancestorPath, new HashMap<Name, Property>());
            ancestorPath = ancestorPath.getAncestor();
        }
        data.putIfAbsent(pathObj, new HashMap<Name, Property>());
        return this;
    }

    /**
     * Delete the branch rooted at the supplied path, if it exists.
     * 
     * @param env the environment; may not be null
     * @param path the path to the branch's top node; may not be null
     * @return this repository, for method chaining
     */
    public SimpleRepository delete( ExecutionEnvironment env,
                                    String path ) {
        PathFactory pathFactory = env.getValueFactories().getPathFactory();
        Path pathObj = pathFactory.create(path);
        List<Path> pathsToRemove = new LinkedList<Path>();
        for (Path nodePath : data.keySet()) {
            if (nodePath.equals(pathObj) || nodePath.isDecendantOf(pathObj)) {
                pathsToRemove.add(nodePath);
            }
        }
        for (Path pathToRemove : pathsToRemove) {
            data.remove(pathToRemove);
        }
        return this;
    }

    /**
     * @param data new new map of property data
     */
    public void setData( ConcurrentMap<Path, Map<Name, Property>> data ) {
        this.data = data;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public void shutdown() {
        shutdown = true;
        repositoriesByName.remove(this.repositoryName);
    }

}
