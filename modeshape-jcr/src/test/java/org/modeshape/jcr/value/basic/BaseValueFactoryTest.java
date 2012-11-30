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
package org.modeshape.jcr.value.basic;

import java.math.BigDecimal;
import org.junit.Before;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.value.BinaryFactory;
import org.modeshape.jcr.value.DateTimeFactory;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.ReferenceFactory;
import org.modeshape.jcr.value.StringFactory;
import org.modeshape.jcr.value.UriFactory;
import org.modeshape.jcr.value.UuidFactory;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;

public abstract class BaseValueFactoryTest {

    protected NamespaceRegistry registry;
    protected NamespaceRegistry.Holder registryHolder;
    protected StringValueFactory stringFactory;
    protected NameFactory nameFactory;
    protected ValueFactories valueFactories;
    protected TextEncoder encoder;
    protected TextDecoder decoder;

    @SuppressWarnings( "synthetic-access" )
    @Before
    public void beforeEach() {
        registry = new SimpleNamespaceRegistry();
        registryHolder = new NamespaceRegistry.Holder() {
            @Override
            public NamespaceRegistry getNamespaceRegistry() {
                return registry;
            }
        };
        encoder = Path.DEFAULT_ENCODER;
        decoder = Path.DEFAULT_DECODER;
        stringFactory = new StringValueFactory(registryHolder, Path.URL_DECODER, Path.DEFAULT_ENCODER);
        valueFactories = new TestValueFactories();
        this.nameFactory = new NameValueFactory(registryHolder, Path.DEFAULT_DECODER, valueFactories);
    }

    private class TestValueFactories extends AbstractValueFactories {

        @Override
        public ReferenceFactory getWeakReferenceFactory() {
            return null;
        }

        @Override
        public UuidFactory getUuidFactory() {
            return null;
        }

        @Override
        public UriFactory getUriFactory() {
            return null;
        }

        @Override
        public TypeSystem getTypeSystem() {
            return null;
        }

        @Override
        public StringFactory getStringFactory() {
            return stringFactory;
        }

        @Override
        public ReferenceFactory getReferenceFactory() {
            return null;
        }

        @Override
        public PathFactory getPathFactory() {
            return null;
        }

        @Override
        public ValueFactory<Object> getObjectFactory() {
            return null;
        }

        @Override
        public NameFactory getNameFactory() {
            return nameFactory;
        }

        @Override
        public ValueFactory<Long> getLongFactory() {
            return null;
        }

        @Override
        public ValueFactory<Double> getDoubleFactory() {
            return null;
        }

        @Override
        public ValueFactory<BigDecimal> getDecimalFactory() {
            return null;
        }

        @Override
        public DateTimeFactory getDateFactory() {
            return null;
        }

        @Override
        public ValueFactory<Boolean> getBooleanFactory() {
            return null;
        }

        @Override
        public BinaryFactory getBinaryFactory() {
            return null;
        }
    }
}
