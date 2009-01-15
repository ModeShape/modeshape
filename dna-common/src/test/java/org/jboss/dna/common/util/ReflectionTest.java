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
package org.jboss.dna.common.util;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class ReflectionTest {

    private String string;
    private List<String> stringList;
    private Reflection stringReflection;
    private Reflection stringListReflection;

    @Before
    public void setUp() {
        string = "This is string #";
        stringList = new ArrayList<String>();
        for (int i = 0; i != 10; ++i) {
            stringList.add(string + (i + 1));
        }
        stringReflection = new Reflection(String.class);
        stringListReflection = new Reflection(List.class);
    }

    @Test
    public void shouldGetClassNameForClass() {
        assertThat(Reflection.getClassName(String.class), is(String.class.getName()));
        assertThat(Reflection.getClassName(ArrayList.class), is(ArrayList.class.getName()));
        assertThat(Reflection.getClassName(StringUtil.class), is(StringUtil.class.getName()));
    }

    @Test
    public void shouldGetClassNameOfInterface() {
        assertThat(Reflection.getClassName(CharSequence.class), is(CharSequence.class.getName()));
        assertThat(Reflection.getClassName(List.class), is(List.class.getName()));
    }

    @Test
    public void shouldGetClassNameWithPrimitive() {
        assertThat(Reflection.getClassName(Integer.TYPE), is("int"));
        assertThat(Reflection.getClassName(Boolean.TYPE), is("boolean"));
        assertThat(Reflection.getClassName(Long.TYPE), is("long"));
        assertThat(Reflection.getClassName(Short.TYPE), is("short"));
        assertThat(Reflection.getClassName(Float.TYPE), is("float"));
        assertThat(Reflection.getClassName(Double.TYPE), is("double"));
        assertThat(Reflection.getClassName(Character.TYPE), is("char"));
        assertThat(Reflection.getClassName(Byte.TYPE), is("byte"));
        assertThat(Reflection.getClassName(Void.TYPE), is("void"));
    }

    @Test
    public void shouldGetClassNameWith1DPrimitiveArray() {
        assertThat(Reflection.getClassName(new int[0].getClass()), is("int[]"));
        assertThat(Reflection.getClassName(new boolean[0].getClass()), is("boolean[]"));
        assertThat(Reflection.getClassName(new long[0].getClass()), is("long[]"));
        assertThat(Reflection.getClassName(new short[0].getClass()), is("short[]"));
        assertThat(Reflection.getClassName(new float[0].getClass()), is("float[]"));
        assertThat(Reflection.getClassName(new double[0].getClass()), is("double[]"));
        assertThat(Reflection.getClassName(new char[0].getClass()), is("char[]"));
        assertThat(Reflection.getClassName(new byte[0].getClass()), is("byte[]"));
    }

    @Test
    public void shouldGetClassNameWith2DPrimitiveArray() {
        assertThat(Reflection.getClassName(new int[0][0].getClass()), is("int[][]"));
        assertThat(Reflection.getClassName(new boolean[0][0].getClass()), is("boolean[][]"));
        assertThat(Reflection.getClassName(new long[0][0].getClass()), is("long[][]"));
        assertThat(Reflection.getClassName(new short[0][0].getClass()), is("short[][]"));
        assertThat(Reflection.getClassName(new float[0][0].getClass()), is("float[][]"));
        assertThat(Reflection.getClassName(new double[0][0].getClass()), is("double[][]"));
        assertThat(Reflection.getClassName(new char[0][0].getClass()), is("char[][]"));
        assertThat(Reflection.getClassName(new byte[0][0].getClass()), is("byte[][]"));
    }

    @Test
    public void shouldGetClassNameWith3DPrimitiveArray() {
        assertThat(Reflection.getClassName(new int[0][0][0].getClass()), is("int[][][]"));
        assertThat(Reflection.getClassName(new boolean[0][0][0].getClass()), is("boolean[][][]"));
        assertThat(Reflection.getClassName(new long[0][0][0].getClass()), is("long[][][]"));
        assertThat(Reflection.getClassName(new short[0][0][0].getClass()), is("short[][][]"));
        assertThat(Reflection.getClassName(new float[0][0][0].getClass()), is("float[][][]"));
        assertThat(Reflection.getClassName(new double[0][0][0].getClass()), is("double[][][]"));
        assertThat(Reflection.getClassName(new char[0][0][0].getClass()), is("char[][][]"));
        assertThat(Reflection.getClassName(new byte[0][0][0].getClass()), is("byte[][][]"));
    }

    @Test
    public void shouldGetClassNameWith8DPrimitiveArray() {
        assertThat(Reflection.getClassName(new int[0][0][0][0][0][0][0][0].getClass()), is("int[][][][][][][][]"));
        assertThat(Reflection.getClassName(new boolean[0][0][0][0][0][0][0][0].getClass()), is("boolean[][][][][][][][]"));
        assertThat(Reflection.getClassName(new long[0][0][0][0][0][0][0][0].getClass()), is("long[][][][][][][][]"));
        assertThat(Reflection.getClassName(new short[0][0][0][0][0][0][0][0].getClass()), is("short[][][][][][][][]"));
        assertThat(Reflection.getClassName(new float[0][0][0][0][0][0][0][0].getClass()), is("float[][][][][][][][]"));
        assertThat(Reflection.getClassName(new double[0][0][0][0][0][0][0][0].getClass()), is("double[][][][][][][][]"));
        assertThat(Reflection.getClassName(new char[0][0][0][0][0][0][0][0].getClass()), is("char[][][][][][][][]"));
        assertThat(Reflection.getClassName(new byte[0][0][0][0][0][0][0][0].getClass()), is("byte[][][][][][][][]"));
    }

    @Test
    public void shouldHaveTargetClass() {
        assertThat(stringReflection.getTargetClass() == String.class, is(true));
        assertThat(stringListReflection.getTargetClass() == List.class, is(true));
    }

    @Test
    public void shouldFindMethodsWithExactMatchingName() {
        Method[] stringMethods = stringReflection.findMethods("indexOf", true);
        assertThat(stringMethods.length, is(4));
        for (Method method : stringMethods) {
            assertThat(method.getName(), is("indexOf"));
        }
        stringMethods = stringReflection.findMethods("length", true);
        assertThat(stringMethods.length, is(1));
        for (Method method : stringMethods) {
            assertThat(method.getName(), is("length"));
        }
    }

    @Test
    public void shouldFindMethodsWithNameMatchingRegularExpression() {
        Method[] stringMethods = stringReflection.findMethods("indexO.", true);
        assertThat(stringMethods.length, is(4));
        for (Method method : stringMethods) {
            assertThat(method.getName(), is("indexOf"));
        }
        stringMethods = stringReflection.findMethods(".+gth", true);
        assertThat(stringMethods.length, is(1));
        for (Method method : stringMethods) {
            assertThat(method.getName(), is("length"));
        }
    }

    @Test
    public void shouldNotFindMethodsWhenThereAreNoMethodsWithThatName() {
        assertThat(stringReflection.findMethods("size", true).length, is(0));
        assertThat(stringListReflection.findMethods("argleBargle", true).length, is(0));
    }

}
