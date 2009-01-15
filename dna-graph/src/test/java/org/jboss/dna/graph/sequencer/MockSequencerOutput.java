/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.graph.sequencer;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.sequencer.SequencerContext;
import org.jboss.dna.graph.sequencer.SequencerOutput;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
@NotThreadSafe
public class MockSequencerOutput implements SequencerOutput {

    private final Map<Path, Map<Name, Property>> propertiesByPath;
    private final SequencerContext context;
    private final LinkedList<Path> nodePathsInCreationOrder;

    public MockSequencerOutput( SequencerContext context ) {
        this(context, false);
    }

    public MockSequencerOutput( SequencerContext context,
                                boolean recordOrderOfNodeCreation ) {
        this.context = context;
        this.propertiesByPath = new HashMap<Path, Map<Name, Property>>();
        this.nodePathsInCreationOrder = recordOrderOfNodeCreation ? new LinkedList<Path>() : null;
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty( Path nodePath,
                             Name propertyName,
                             Object... values ) {
        Map<Name, Property> properties = propertiesByPath.get(nodePath);
        if (values == null || values.length == 0) {
            // remove the property ...
            if (properties != null) {
                properties.remove(propertyName);
                if (properties.isEmpty()) {
                    propertiesByPath.remove(nodePath);
                    if (nodePathsInCreationOrder != null) nodePathsInCreationOrder.remove(nodePath);
                }
            }
        } else {
            if (properties == null) {
                properties = new HashMap<Name, Property>();
                propertiesByPath.put(nodePath, properties);
                if (nodePathsInCreationOrder != null) nodePathsInCreationOrder.add(nodePath);
            }
            Property property = context.getPropertyFactory().create(propertyName, values);
            properties.put(propertyName, property);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty( String nodePath,
                             String propertyName,
                             Object... values ) {
        Path path = context.getValueFactories().getPathFactory().create(nodePath);
        Name name = context.getValueFactories().getNameFactory().create(propertyName);
        setProperty(path, name, values);
    }

    /**
     * {@inheritDoc}
     */
    public void setReference( String nodePath,
                              String propertyName,
                              String... paths ) {
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        Path path = pathFactory.create(nodePath);
        Name name = context.getValueFactories().getNameFactory().create(propertyName);
        Object[] values = null;
        if (paths != null && paths.length != 0) {
            values = new Path[paths.length];
            for (int i = 0, len = paths.length; i != len; ++i) {
                String pathValue = paths[i];
                values[i] = pathFactory.create(pathValue);
            }
        }
        setProperty(path, name, values);
    }

    public LinkedList<Path> getOrderOfCreation() {
        return nodePathsInCreationOrder;
    }

    public boolean exists( Path nodePath ) {
        return this.propertiesByPath.containsKey(nodePath);
    }

    public Map<Name, Property> getProperties( Path nodePath ) {
        Map<Name, Property> properties = this.propertiesByPath.get(nodePath);
        if (properties == null) return null;
        return Collections.unmodifiableMap(properties);
    }

    public Property getProperty( String nodePath,
                                 String propertyName ) {
        Path path = context.getValueFactories().getPathFactory().create(nodePath);
        Name name = context.getValueFactories().getNameFactory().create(propertyName);
        return getProperty(path, name);
    }

    public Property getProperty( Path nodePath,
                                 Name propertyName ) {
        Map<Name, Property> properties = this.propertiesByPath.get(nodePath);
        if (properties == null) return null;
        return properties.get(propertyName);
    }

    public Object[] getPropertyValues( String nodePath,
                                       String propertyName ) {
        Path path = context.getValueFactories().getPathFactory().create(nodePath);
        return getPropertyValues(path, propertyName);
    }

    public Object[] getPropertyValues( Path path,
                                       String propertyName ) {
        Name name = context.getValueFactories().getNameFactory().create(propertyName);
        Property prop = getProperty(path, name);
        if (prop != null) {
            return prop.getValuesAsArray();
        }
        return null;
    }

    public boolean hasProperty( String nodePath,
                                String property ) {
        return getProperty(nodePath, property) != null;
    }

    public boolean hasProperties() {
        return this.propertiesByPath.size() > 0;
    }

}
