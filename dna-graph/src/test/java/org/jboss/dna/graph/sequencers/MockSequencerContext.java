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

import java.security.AccessControlContext;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.monitor.ProgressMonitor;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.properties.PropertyFactory;
import org.jboss.dna.graph.properties.ValueFactories;
import org.jboss.dna.graph.properties.basic.BasicNamespaceRegistry;
import org.jboss.dna.graph.properties.basic.StandardValueFactories;

/**
 * @author jverhaeg
 */
@Immutable
public class MockSequencerContext implements SequencerContext {

    private final ValueFactories factories;
    private final NamespaceRegistry registry = new BasicNamespaceRegistry();

    public MockSequencerContext() {
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
     * 
     * @see org.jboss.dna.common.component.ClassLoaderFactory#getClassLoader(java.lang.String[])
     */
    public ClassLoader getClassLoader( String... classpath ) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.sequencers.SequencerContext#getInputPath()
     */
    public Path getInputPath() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.sequencers.SequencerContext#getInputProperties()
     */
    public Set<Property> getInputProperties() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.sequencers.SequencerContext#getInputProperty(org.jboss.dna.graph.properties.Name)
     */
    public Property getInputProperty( Name name ) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.sequencers.SequencerContext#getMimeType()
     */
    public String getMimeType() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContext#getAccessControlContext()
     */
    public AccessControlContext getAccessControlContext() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContext#getLogger(java.lang.Class)
     */
    public Logger getLogger( Class<?> clazz ) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContext#getLogger(java.lang.String)
     */
    public Logger getLogger( String name ) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContext#getLoginContext()
     */
    public LoginContext getLoginContext() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContext#getNamespaceRegistry()
     */
    public NamespaceRegistry getNamespaceRegistry() {
        return registry;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContext#getProgressMonitor()
     */
    public ProgressMonitor getProgressMonitor() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContext#getPropertyFactory()
     */
    public PropertyFactory getPropertyFactory() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContext#getSubject()
     */
    public Subject getSubject() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContext#getValueFactories()
     */
    public ValueFactories getValueFactories() {
        return factories;
    }
}
