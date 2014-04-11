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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.ReadOnlyIterator;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Reference;

/**
 * An immutable version of a property that has 2 or more values. This is done for efficiency of the in-memory representation,
 * since many properties will have just a single value, while others will have multiple values.
 */
@Immutable
public class BasicMultiValueProperty extends BasicProperty {
    private static final long serialVersionUID = 1L;

    private final List<Object> values;

    /**
     * Create a property with 2 or more values. Note that the supplied list may be modifiable, as this object does not expose any
     * means for modifying the contents.
     * 
     * @param name the property name
     * @param values the property values
     * @throws IllegalArgumentException if the values is null or does not have at least 2 values
     */
    public BasicMultiValueProperty( Name name,
                                    List<Object> values ) {
        super(name);
        CheckArg.isNotNull(values, "values");
        this.values = values;
    }

    /**
     * Create a property with 2 or more values.
     * 
     * @param name the property name
     * @param values the property values
     * @throws IllegalArgumentException if the values is null or does not have at least 2 values
     */
    public BasicMultiValueProperty( Name name,
                                    Object... values ) {
        super(name);
        CheckArg.isNotNull(values, "values");
        CheckArg.hasSizeOfAtLeast(values, 2, "values");
        this.values = Arrays.asList(values);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean isMultiple() {
        return true;
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public boolean isReference() {
        Object firstValue = getFirstValue();
        if (firstValue instanceof NodeKeyReference) {
            return !((NodeKeyReference)firstValue).isSimple();
        }
        return firstValue instanceof Reference;
    }

    @Override
    public boolean isSimpleReference() {
        Object firstValue = getFirstValue();
        return firstValue instanceof Reference && ((Reference)firstValue).isSimple();
    }

    @Override
    public boolean isBinary() {
        return getFirstValue() instanceof Binary;
    }

    @Override
    public int size() {
        return values != null ? values.size() : 0;
    }

    @Override
    public Object getFirstValue() {
        return size() == 0 ? null : values.get(0);
    }

    @Override
    public Iterator<Object> iterator() {
        return ReadOnlyIterator.around(values.iterator());
    }

    @Override
    public Object getValue( int index ) throws IndexOutOfBoundsException {
        return values.get(index);
    }
}
