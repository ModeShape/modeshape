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
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.UUID;
import org.junit.Test;

/**
 * 
 */
public class PropertyTypeTest {

    @Test
    public void shouldDiscoverPropertyTypeGivenPrimitiveClass() throws Exception {
        assertThat(PropertyType.discoverType(Integer.TYPE), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(Long.TYPE), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(Short.TYPE), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(Boolean.TYPE), is(PropertyType.BOOLEAN));
        assertThat(PropertyType.discoverType(Double.TYPE), is(PropertyType.DOUBLE));
        assertThat(PropertyType.discoverType(Float.TYPE), is(PropertyType.DOUBLE));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenNumberClass() throws Exception {
        assertThat(PropertyType.discoverType(Integer.class), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(Long.class), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(Short.class), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(Boolean.class), is(PropertyType.BOOLEAN));
        assertThat(PropertyType.discoverType(Double.class), is(PropertyType.DOUBLE));
        assertThat(PropertyType.discoverType(Float.class), is(PropertyType.DOUBLE));
        assertThat(PropertyType.discoverType(BigDecimal.class), is(PropertyType.DECIMAL));
    }

    @Test
    public void shouldNotDiscoverPropertyTypeOfByteClass() {
        assertThat(PropertyType.discoverType(Byte.class), is(nullValue()));
    }

    @Test
    public void shouldNotDiscoverPropertyTypeOfBigIntegerClass() {
        assertThat(PropertyType.discoverType(BigInteger.class), is(nullValue()));
    }

    @Test
    public void shouldNotDiscoverPropertyTypeOfVoidClass() {
        assertThat(PropertyType.discoverType(Void.TYPE), is(nullValue()));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenPrimitiveArrayClass() throws Exception {
        assertThat(PropertyType.discoverType(new int[] {}.getClass()), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(new long[] {}.getClass()), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(new short[] {}.getClass()), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(new boolean[] {}.getClass()), is(PropertyType.BOOLEAN));
        assertThat(PropertyType.discoverType(new double[] {}.getClass()), is(PropertyType.DOUBLE));
        assertThat(PropertyType.discoverType(new float[] {}.getClass()), is(PropertyType.DOUBLE));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenArrayOfPrimitiveArrayClass() throws Exception {
        assertThat(PropertyType.discoverType(new int[][] {}.getClass()), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(new long[][] {}.getClass()), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(new short[][] {}.getClass()), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(new boolean[][] {}.getClass()), is(PropertyType.BOOLEAN));
        assertThat(PropertyType.discoverType(new double[][] {}.getClass()), is(PropertyType.DOUBLE));
        assertThat(PropertyType.discoverType(new float[][] {}.getClass()), is(PropertyType.DOUBLE));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenArrayOfArrayOfPrimitiveArrayClass() throws Exception {
        assertThat(PropertyType.discoverType(new int[][][] {}.getClass()), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(new long[][][] {}.getClass()), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(new short[][][] {}.getClass()), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(new boolean[][][] {}.getClass()), is(PropertyType.BOOLEAN));
        assertThat(PropertyType.discoverType(new double[][][] {}.getClass()), is(PropertyType.DOUBLE));
        assertThat(PropertyType.discoverType(new float[][][] {}.getClass()), is(PropertyType.DOUBLE));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenNumberArrayClass() throws Exception {
        assertThat(PropertyType.discoverType(new Integer[] {}.getClass()), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(new Long[] {}.getClass()), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(new Short[] {}.getClass()), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(new Boolean[] {}.getClass()), is(PropertyType.BOOLEAN));
        assertThat(PropertyType.discoverType(new Double[] {}.getClass()), is(PropertyType.DOUBLE));
        assertThat(PropertyType.discoverType(new Float[] {}.getClass()), is(PropertyType.DOUBLE));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenArrayOfNumberArrayClass() throws Exception {
        assertThat(PropertyType.discoverType(new Integer[][] {}.getClass()), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(new Long[][] {}.getClass()), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(new Short[][] {}.getClass()), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(new Boolean[][] {}.getClass()), is(PropertyType.BOOLEAN));
        assertThat(PropertyType.discoverType(new Double[][] {}.getClass()), is(PropertyType.DOUBLE));
        assertThat(PropertyType.discoverType(new Float[][] {}.getClass()), is(PropertyType.DOUBLE));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenArrayOfArrayOfNumberArrayClass() throws Exception {
        assertThat(PropertyType.discoverType(new Integer[][][] {}.getClass()), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(new Long[][][] {}.getClass()), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(new Short[][][] {}.getClass()), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(new Boolean[][][] {}.getClass()), is(PropertyType.BOOLEAN));
        assertThat(PropertyType.discoverType(new Double[][][] {}.getClass()), is(PropertyType.DOUBLE));
        assertThat(PropertyType.discoverType(new Float[][][] {}.getClass()), is(PropertyType.DOUBLE));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenStringClass() throws Exception {
        assertThat(PropertyType.discoverType(String.class), is(PropertyType.STRING));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenPathClass() throws Exception {
        assertThat(PropertyType.discoverType(Path.class), is(PropertyType.PATH));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenNameClass() throws Exception {
        assertThat(PropertyType.discoverType(Name.class), is(PropertyType.NAME));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenReferenceClass() throws Exception {
        assertThat(PropertyType.discoverType(Reference.class), is(PropertyType.REFERENCE));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenDateTimeClass() throws Exception {
        assertThat(PropertyType.discoverType(DateTime.class), is(PropertyType.DATE));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenUriClass() throws Exception {
        assertThat(PropertyType.discoverType(URI.class), is(PropertyType.URI));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenUuidClass() throws Exception {
        assertThat(PropertyType.discoverType(UUID.class), is(PropertyType.UUID));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenArraysOfStringClass() throws Exception {
        assertThat(PropertyType.discoverType(new String[] {}.getClass()), is(PropertyType.STRING));
        assertThat(PropertyType.discoverType(new String[][] {}.getClass()), is(PropertyType.STRING));
        assertThat(PropertyType.discoverType(new String[][][] {}.getClass()), is(PropertyType.STRING));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenArraysOfPathClass() throws Exception {
        assertThat(PropertyType.discoverType(new Path[] {}.getClass()), is(PropertyType.PATH));
        assertThat(PropertyType.discoverType(new Path[][] {}.getClass()), is(PropertyType.PATH));
        assertThat(PropertyType.discoverType(new Path[][][] {}.getClass()), is(PropertyType.PATH));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenArraysOfNameClass() throws Exception {
        assertThat(PropertyType.discoverType(new Name[] {}.getClass()), is(PropertyType.NAME));
        assertThat(PropertyType.discoverType(new Name[][] {}.getClass()), is(PropertyType.NAME));
        assertThat(PropertyType.discoverType(new Name[][][] {}.getClass()), is(PropertyType.NAME));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenArraysOfReferenceClass() throws Exception {
        assertThat(PropertyType.discoverType(new Reference[] {}.getClass()), is(PropertyType.REFERENCE));
        assertThat(PropertyType.discoverType(new Reference[][] {}.getClass()), is(PropertyType.REFERENCE));
        assertThat(PropertyType.discoverType(new Reference[][][] {}.getClass()), is(PropertyType.REFERENCE));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenArraysOfDateTimeClass() throws Exception {
        assertThat(PropertyType.discoverType(new DateTime[] {}.getClass()), is(PropertyType.DATE));
        assertThat(PropertyType.discoverType(new DateTime[][] {}.getClass()), is(PropertyType.DATE));
        assertThat(PropertyType.discoverType(new DateTime[][][] {}.getClass()), is(PropertyType.DATE));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenArraysOfUriClass() throws Exception {
        assertThat(PropertyType.discoverType(new URI[] {}.getClass()), is(PropertyType.URI));
        assertThat(PropertyType.discoverType(new URI[][] {}.getClass()), is(PropertyType.URI));
        assertThat(PropertyType.discoverType(new URI[][][] {}.getClass()), is(PropertyType.URI));
    }

    @Test
    public void shouldDiscoverPropertyTypeGivenArraysOfUuidClass() throws Exception {
        assertThat(PropertyType.discoverType(new UUID[] {}.getClass()), is(PropertyType.UUID));
        assertThat(PropertyType.discoverType(new UUID[][] {}.getClass()), is(PropertyType.UUID));
        assertThat(PropertyType.discoverType(new UUID[][][] {}.getClass()), is(PropertyType.UUID));
    }

}
