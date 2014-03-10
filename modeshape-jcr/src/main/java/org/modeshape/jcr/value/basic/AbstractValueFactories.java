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

import java.util.Iterator;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;

/**
 * Abstract implementation of {@link ValueFactories} that implements all the methods other than the <code>get*Factory()</code>
 * methods. Subclasses can simply implement these methods and inherit the {@link #iterator()}, {@link #getValueFactory(Object)}
 * and {@link #getValueFactory(PropertyType)} method implementations.
 */
@ThreadSafe
public abstract class AbstractValueFactories implements ValueFactories {

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * <p>
     * This implementation always iterates over the instances return by the <code>get*Factory()</code> methods.
     * </p>
     */
    @Override
    public Iterator<ValueFactory<?>> iterator() {
        return new ValueFactoryIterator();
    }

    @Override
    public ValueFactory<?> getValueFactory( PropertyType type ) {
        CheckArg.isNotNull(type, "type");
        switch (type) {
            case BINARY:
                return getBinaryFactory();
            case BOOLEAN:
                return getBooleanFactory();
            case DATE:
                return getDateFactory();
            case DECIMAL:
                return getDecimalFactory();
            case DOUBLE:
                return getDoubleFactory();
            case LONG:
                return getLongFactory();
            case NAME:
                return getNameFactory();
            case PATH:
                return getPathFactory();
            case REFERENCE:
                return getReferenceFactory();
            case WEAKREFERENCE:
                return getWeakReferenceFactory();
            case SIMPLEREFERENCE:
                return getSimpleReferenceFactory();
            case STRING:
                return getStringFactory();
            case URI:
                return getUriFactory();
            case OBJECT:
                return getObjectFactory();
        }
        return getObjectFactory();
    }

    @Override
    public ValueFactory<?> getValueFactory( Object prototype ) {
        CheckArg.isNotNull(prototype, "prototype");
        PropertyType inferredType = PropertyType.discoverType(prototype);
        assert inferredType != null;
        return getValueFactory(inferredType);
    }

    protected class ValueFactoryIterator implements Iterator<ValueFactory<?>> {
        private final Iterator<PropertyType> propertyTypeIter = PropertyType.iterator();

        protected ValueFactoryIterator() {
        }

        @Override
        public boolean hasNext() {
            return propertyTypeIter.hasNext();
        }

        @Override
        public ValueFactory<?> next() {
            PropertyType nextType = propertyTypeIter.next();
            return getValueFactory(nextType);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
