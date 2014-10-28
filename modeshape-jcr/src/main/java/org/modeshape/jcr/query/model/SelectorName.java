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
package org.modeshape.jcr.query.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.value.NameFactory;

/**
 * A representation of a qualified or expanded name.
 */
@Immutable
public class SelectorName implements Readable, Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final boolean expandedForm;

    public SelectorName( String name ) {
        CheckArg.isNotEmpty(name, "name");
        this.name = name;
        this.expandedForm = name.startsWith("{");
    }

    /**
     * The raw name of the selector.
     * 
     * @return the raw name; never null and never empty
     */
    public String name() {
        return name;
    }

    /**
     * Returns this selector in qualified form (see #3.2.5 of the JCR spec)
     *
     * @param nameFactory a {@link org.modeshape.jcr.value.NameFactory} instance; may not be null
     * @return a {@link org.modeshape.jcr.query.model.SelectorName} instance for which the name is always in qualified form; never null
     */
    public SelectorName qualifiedForm(NameFactory nameFactory) {
        return expandedForm ? new SelectorName(nameFactory.create(this.name).getString()) : this;
    }

    @Override
    public String getString() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof SelectorName) {
            SelectorName that = (SelectorName)obj;
            return this.name.equals(that.name());
        }
        return false;
    }

    /**
     * Create a set that contains the supplied SelectName object.
     * 
     * @param name the name to include in the set; may be null
     * @return the set; never null
     */
    public static Set<SelectorName> nameSetFrom( SelectorName name ) {
        if (name == null) return Collections.emptySet();
        return Collections.singleton(name);
    }

    /**
     * Create a set that contains the supplied SelectName object.
     * 
     * @param firstName the first name; may be null
     * @param names the remaining names; may be null
     * @return the set; never null
     */
    public static Set<SelectorName> nameSetFrom( SelectorName firstName,
                                                 SelectorName... names ) {
        if (firstName == null && (names == null || names.length == 0)) {
            return Collections.emptySet();
        }
        Set<SelectorName> result = new LinkedHashSet<SelectorName>();
        result.add(firstName);
        for (SelectorName name : names) {
            if (name != null) result.add(name);
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Create a set that contains the SelectName objects in the supplied sets.
     * 
     * @param firstSet the first set of names; may be null or empty
     * @param secondSet the second set of names; may be null or empty
     * @return the set; never null
     */
    public static Set<SelectorName> nameSetFrom( Set<SelectorName> firstSet,
                                                 Set<SelectorName> secondSet ) {
        if ((firstSet == null || firstSet.isEmpty()) && (secondSet == null || secondSet.isEmpty())) {
            return Collections.emptySet();
        }
        Set<SelectorName> result = new LinkedHashSet<SelectorName>();
        result.addAll(firstSet);
        if (secondSet != null) result.addAll(secondSet);
        return Collections.unmodifiableSet(result);
    }
}
