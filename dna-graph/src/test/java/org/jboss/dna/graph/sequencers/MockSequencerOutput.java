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
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathFactory;
import org.jboss.dna.graph.properties.ValueFactories;
import org.jboss.dna.graph.properties.basic.BasicNamespaceRegistry;
import org.jboss.dna.graph.properties.basic.StandardValueFactories;
import org.jboss.dna.graph.sequencers.SequencerOutput;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
@NotThreadSafe
public class MockSequencerOutput implements SequencerOutput {

    private final Map<Path, Object[]> properties;
    private final ValueFactories factories;

    /**
	 */
    public MockSequencerOutput() {
        this.properties = new HashMap<Path, Object[]>();
        NamespaceRegistry registry = new BasicNamespaceRegistry();
        registry.register("jcr", "http://www.jcp.org/jcr/1.0");
        registry.register("mix", "http://www.jcp.org/jcr/mix/1.0");
        registry.register("nt", "http://www.jcp.org/jcr/nt/1.0");
        registry.register(DnaLexicon.Namespace.PREFIX, DnaLexicon.Namespace.URI);
        registry.register("dnadtd", "http://www.jboss.org/dna/dtd/1.0");
        registry.register("dnaxml", "http://www.jboss.org/dna/xml/1.0");
        factories = new StandardValueFactories(registry);
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactories getFactories() {
        return this.factories;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * 
     * @see org.jboss.dna.graph.sequencers.SequencerOutput#getNamespaceRegistry()
     */
    public NamespaceRegistry getNamespaceRegistry() {
        return factories.getNameFactory().getNamespaceRegistry();
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
        Path path = this.factories.getPathFactory().create(nodePath);
        Name name = this.factories.getNameFactory().create(propertyName);
        setProperty(path, name, values);
    }

    /**
     * {@inheritDoc}
     */
    public void setReference( String nodePath,
                              String propertyName,
                              String... paths ) {
        PathFactory pathFactory = this.factories.getPathFactory();
        Path path = pathFactory.create(nodePath);
        Name name = this.factories.getNameFactory().create(propertyName);
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
        Path path = this.factories.getPathFactory().create(nodePath);
        Name name = this.factories.getNameFactory().create(propertyName);
        return createKey(path, name);
    }

    protected Path createKey( Path nodePath,
                              Name propertyName ) {
        return this.factories.getPathFactory().create(nodePath, propertyName);
    }

}
