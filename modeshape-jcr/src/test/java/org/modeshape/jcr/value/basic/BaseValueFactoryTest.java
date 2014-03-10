/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        public ReferenceFactory getSimpleReferenceFactory() {
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
