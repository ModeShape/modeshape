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
package org.modeshape.graph.property;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class NameValueComparatorTest extends AbstractValueComparatorsTest<Name> {

    private static final ValueFactory<Name> FACTORY;
    private static final Name NAME1;
    private static final Name NAME2;
    private static final Name NAME3;
    private static final Name NAME4;
    private static final Name NAME5;
    private static final Name NAME6;

    static {
        NAMESPACE_REGISTRY.register("dna", "http://www.modeshape.org/namespaces/Test");
        NAMESPACE_REGISTRY.register("acme", "http://www.acme.com/");
        NAMESPACE_REGISTRY.register("dna-b", "http://www.modeshape.org/namespaces/Test/b");
        NAMESPACE_REGISTRY.register("dna-c", "http://www.modeshape.org/namespaces/TEST");
        FACTORY = VALUE_FACTORIES.getNameFactory();
        NAME1 = FACTORY.create("dna:alpha");
        NAME2 = FACTORY.create("dna:beta");
        NAME3 = FACTORY.create("acme:beta");
        NAME4 = FACTORY.create("dna:beta");
        NAME5 = FACTORY.create("dna:ALPHA");
        NAME6 = FACTORY.create("dna-c:ALPHA");
    }

    public NameValueComparatorTest() {
        super(ValueComparators.NAME_COMPARATOR, NAME1, NAME2, NAME3, NAME4, NAME5, NAME6);
    }

    @Test
    public void shouldConsiderNamesWithSameNamespaceUriAndLocalPartToBeComparable() {
        // System.out.println(NAME2);
        // System.out.println(NAME4);
        assertThat(comparator.compare(NAME2, NAME4), is(0));
    }

    @Test
    public void shouldBeCaseSensitive() {
        assertThat(comparator.compare(NAME2, NAME4), is(0));
        assertThat(comparator.compare(NAME1, NAME5), is(not(0)));
        assertThat(comparator.compare(NAME6, NAME5), is(not(0)));
    }
}
