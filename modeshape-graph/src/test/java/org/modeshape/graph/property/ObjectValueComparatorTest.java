/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.modeshape.graph.ExecutionContext;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class ObjectValueComparatorTest {

    private ExecutionContext context;
    private ValueFactories factories;

    @Before
    public void beforeEach() {
        this.context = new ExecutionContext();
        this.factories = this.context.getValueFactories();
    }

    @Test
    public void shouldCompareBinaryAndStringValues() {
        String strValue = "Jack and Jill went up the hill";
        Binary binaryValue = factories.getBinaryFactory().create(strValue);
        Binary binarySpy = spy(binaryValue);

        // The values should compare to each other ...
        assertThat(ValueComparators.OBJECT_COMPARATOR.compare(strValue, binarySpy), is(0));
        assertThat(ValueComparators.OBJECT_COMPARATOR.compare(binarySpy, strValue), is(0));

        // Verify that the binary value was not obtained, but that we just used the hash
        verify(binarySpy, times(0)).getBytes();
        verify(binarySpy, times(0)).getStream();
        verify(binarySpy, atLeastOnce()).getHash();
    }

    @Test
    public void shouldCompareLongAndStringValues() {
        String strValue = "3";
        Long longValue = factories.getLongFactory().create(strValue);
        assertThat(ValueComparators.OBJECT_COMPARATOR.compare(strValue, longValue), is(0));
        assertThat(ValueComparators.OBJECT_COMPARATOR.compare(longValue, strValue), is(0));
    }

    public void shouldComparePathAndStringValues() {
        String strValue = "/a/b/c";
        Path pathValue = factories.getPathFactory().create(strValue);
        assertThat(ValueComparators.OBJECT_COMPARATOR.compare(strValue, pathValue), is(0));
        assertThat(ValueComparators.OBJECT_COMPARATOR.compare(pathValue, strValue), is(0));
    }

}
