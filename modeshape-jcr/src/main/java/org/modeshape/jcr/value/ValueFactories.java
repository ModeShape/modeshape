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
package org.modeshape.jcr.value;

import java.math.BigDecimal;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.query.model.TypeSystem;

/**
 * The set of standard {@link ValueFactory} instances.
 */
@ThreadSafe
public interface ValueFactories extends Iterable<ValueFactory<?>>, NameFactory.Holder {

    /**
     * Get the type system associated with these factories.
     * 
     * @return the type system; never null
     */
    TypeSystem getTypeSystem();

    /**
     * Get the value factory that creates values of the supplied {@link PropertyType type}.
     * 
     * @param type the type for the values
     * @return the factory; never null
     * @throws IllegalArgumentException if the property type is null
     */
    ValueFactory<?> getValueFactory( PropertyType type );

    /**
     * Get the value factory that is best able to create values with the most natural type given by the supplied value.
     * 
     * @param prototype the value that should be used to determine the best value factory
     * @return the factory; never null
     * @throws IllegalArgumentException if the prototype value is null
     */
    ValueFactory<?> getValueFactory( Object prototype );

    /**
     * Get the value factory for {@link PropertyType#STRING string} properties.
     * 
     * @return the factory; never null
     */
    StringFactory getStringFactory();

    /**
     * Get the value factory for {@link PropertyType#BINARY binary} properties.
     * 
     * @return the factory; never null
     */
    BinaryFactory getBinaryFactory();

    /**
     * Get the value factory for {@link PropertyType#LONG long} properties.
     * 
     * @return the factory; never null
     */
    ValueFactory<Long> getLongFactory();

    /**
     * Get the value factory for {@link PropertyType#DOUBLE double} properties.
     * 
     * @return the factory; never null
     */
    ValueFactory<Double> getDoubleFactory();

    /**
     * Get the value factory for {@link PropertyType#DECIMAL decimal} properties.
     * 
     * @return the factory; never null
     */
    ValueFactory<BigDecimal> getDecimalFactory();

    /**
     * Get the value factory for {@link PropertyType#DATE date} properties.
     * 
     * @return the factory; never null
     */
    DateTimeFactory getDateFactory();

    /**
     * Get the value factory for {@link PropertyType#BOOLEAN boolean} properties.
     * 
     * @return the factory; never null
     */
    ValueFactory<Boolean> getBooleanFactory();

    /**
     * Get the value factory for {@link PropertyType#NAME name} properties.
     * 
     * @return the factory; never null
     */
    @Override
    NameFactory getNameFactory();

    /**
     * Get the value factory for {@link PropertyType#REFERENCE reference} properties.
     * 
     * @return the factory; never null
     */
    ReferenceFactory getReferenceFactory();

    /**
     * Get the value factory for {@link PropertyType#WEAKREFERENCE reference} properties.
     * 
     * @return the factory; never null
     */
    ReferenceFactory getWeakReferenceFactory();

    /**
     * Get the value factory for {@link PropertyType#SIMPLEREFERENCE reference} properties.
     *
     * @return the factory; never null
     */
    ReferenceFactory getSimpleReferenceFactory();

    /**
     * Get the value factory for {@link PropertyType#PATH path} properties.
     * 
     * @return the factory; never null
     */
    PathFactory getPathFactory();

    /**
     * Get the value factory for {@link PropertyType#URI URI} properties.
     * 
     * @return the factory; never null
     */
    UriFactory getUriFactory();

    /**
     * Get the value factory for {@link PropertyType#OBJECT object} properties.
     * 
     * @return the factory; never null
     */
    ValueFactory<Object> getObjectFactory();

}
