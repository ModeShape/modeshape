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
package org.jboss.dna.graph.sequencers;

import java.util.HashMap;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathFactory;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
@NotThreadSafe
public class MockSequencerOutput implements SequencerOutput {

    private final Map<Path, Object[]> properties;
    private final SequencerContext context;

    public MockSequencerOutput( SequencerContext context ) {
        this.context = context;
        this.properties = new HashMap<Path, Object[]>();
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty( Path nodePath,
                             Name propertyName,
                             Object... values ) {
        Path key = createKey(nodePath, propertyName);
        if (values == null || values.length == 0) {
            this.properties.remove(key);
        } else {
            this.properties.put(key, values);
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

    public Object[] getPropertyValues( String nodePath,
                                       String property ) {
        Path key = createKey(nodePath, property);
        return this.properties.get(key);
    }

    public boolean hasProperty( String nodePath,
                                String property ) {
        Path key = createKey(nodePath, property);
        return this.properties.containsKey(key);
    }

    public boolean hasProperties() {
        return this.properties.size() > 0;
    }

    protected Path createKey( String nodePath,
                              String propertyName ) {
        Path path = context.getValueFactories().getPathFactory().create(nodePath);
        Name name = context.getValueFactories().getNameFactory().create(propertyName);
        return createKey(path, name);
    }

    protected Path createKey( Path nodePath,
                              Name propertyName ) {
        return context.getValueFactories().getPathFactory().create(nodePath, propertyName);
    }

}
