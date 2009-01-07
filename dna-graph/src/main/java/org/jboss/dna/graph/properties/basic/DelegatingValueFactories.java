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
package org.jboss.dna.graph.properties.basic;

import java.math.BigDecimal;
import java.net.URI;
import org.jboss.dna.graph.properties.Binary;
import org.jboss.dna.graph.properties.DateTimeFactory;
import org.jboss.dna.graph.properties.NameFactory;
import org.jboss.dna.graph.properties.PathFactory;
import org.jboss.dna.graph.properties.Reference;
import org.jboss.dna.graph.properties.UuidFactory;
import org.jboss.dna.graph.properties.ValueFactories;
import org.jboss.dna.graph.properties.ValueFactory;

/**
 * @author Randall Hauch
 */
public class DelegatingValueFactories extends AbstractValueFactories {

    private final ValueFactories delegate;

    protected DelegatingValueFactories( ValueFactories delegate ) {
        assert delegate != null;
        this.delegate = delegate;
    }

    public ValueFactory<Binary> getBinaryFactory() {
        return delegate.getBinaryFactory();
    }

    public ValueFactory<Boolean> getBooleanFactory() {
        return delegate.getBooleanFactory();
    }

    public DateTimeFactory getDateFactory() {
        return delegate.getDateFactory();
    }

    public ValueFactory<BigDecimal> getDecimalFactory() {
        return delegate.getDecimalFactory();
    }

    public ValueFactory<Double> getDoubleFactory() {
        return delegate.getDoubleFactory();
    }

    public ValueFactory<Long> getLongFactory() {
        return delegate.getLongFactory();
    }

    public NameFactory getNameFactory() {
        return delegate.getNameFactory();
    }

    public ValueFactory<Object> getObjectFactory() {
        return delegate.getObjectFactory();
    }

    public PathFactory getPathFactory() {
        return delegate.getPathFactory();
    }

    public ValueFactory<Reference> getReferenceFactory() {
        return delegate.getReferenceFactory();
    }

    public ValueFactory<String> getStringFactory() {
        return delegate.getStringFactory();
    }

    public ValueFactory<URI> getUriFactory() {
        return delegate.getUriFactory();
    }

    public UuidFactory getUuidFactory() {
        return delegate.getUuidFactory();
    }

}
