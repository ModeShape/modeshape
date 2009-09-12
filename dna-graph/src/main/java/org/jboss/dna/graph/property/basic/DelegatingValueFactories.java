/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.property.basic;

import java.math.BigDecimal;
import java.net.URI;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.property.BinaryFactory;
import org.jboss.dna.graph.property.DateTimeFactory;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Reference;
import org.jboss.dna.graph.property.UuidFactory;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.ValueFactory;

/**
 * A {@link ValueFactories} implementation that delegates to another instance, and that is often useful for subclassing.
 */
@Immutable
public class DelegatingValueFactories extends AbstractValueFactories {

    private final ValueFactories delegate;

    protected DelegatingValueFactories( ValueFactories delegate ) {
        assert delegate != null;
        this.delegate = delegate;
    }

    public BinaryFactory getBinaryFactory() {
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
