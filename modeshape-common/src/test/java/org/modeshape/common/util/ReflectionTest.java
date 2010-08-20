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
package org.modeshape.common.util;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.CommonI18n;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.collection.Problem.Status;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.Reflection.Property;

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

    @Test
    public void shouldGetAllPropertiesOnJavaBean() throws Exception {
        Status status = Status.INFO;
        int code = 121;
        I18n msg = CommonI18n.argumentMayNotBeEmpty;
        Object[] params = new Object[] {"argName"};
        String resource = "The source";
        String location = "The place to be";
        Throwable throwable = null;
        Problem problem = new Problem(status, code, msg, params, resource, location, throwable);
        Reflection reflection = new Reflection(Problem.class);
        List<Property> props = reflection.getAllPropertiesOn(problem);
        Map<String, Property> propsByName = reflection.getAllPropertiesByNameOn(problem);

        assertThat(props.size(), is(8));
        assertThat(propsByName.size(), is(8));
        assertThat(props.containsAll(propsByName.values()), is(true));

        Property property = propsByName.remove("status");
        assertThat(property.getName(), is("status"));
        assertThat(property.getLabel(), is("Status"));
        assertThat(property.getType().equals(Status.class), is(true));
        assertThat(property.isReadOnly(), is(true));
        assertThat(property, is(findProperty(property.getName(), props)));
        assertValue(reflection, problem, property, status);

        property = propsByName.remove("code");
        assertThat(property.getName(), is("code"));
        assertThat(property.getLabel(), is("Code"));
        assertThat(property.getType().equals(Integer.TYPE), is(true));
        assertThat(property.isReadOnly(), is(true));
        assertValue(reflection, problem, property, code);

        property = propsByName.remove("message");
        assertThat(property.getName(), is("message"));
        assertThat(property.getLabel(), is("Message"));
        assertThat(property.getType().equals(I18n.class), is(true));
        assertThat(property.isReadOnly(), is(true));
        assertValue(reflection, problem, property, msg);

        property = propsByName.remove("messageString");
        assertThat(property.getName(), is("messageString"));
        assertThat(property.getLabel(), is("Message String"));
        assertThat(property.getType().equals(String.class), is(true));
        assertThat(property.isReadOnly(), is(true));
        assertValue(reflection, problem, property, msg.text(params));

        property = propsByName.remove("parameters");
        assertThat(property.getName(), is("parameters"));
        assertThat(property.getLabel(), is("Parameters"));
        assertThat(property.getType().equals(Object[].class), is(true));
        assertThat(property.isReadOnly(), is(true));
        assertValue(reflection, problem, property, params);

        property = propsByName.remove("resource");
        assertThat(property.getName(), is("resource"));
        assertThat(property.getLabel(), is("Resource"));
        assertThat(property.getType().equals(String.class), is(true));
        assertThat(property.isReadOnly(), is(true));
        assertValue(reflection, problem, property, resource);

        property = propsByName.remove("location");
        assertThat(property.getName(), is("location"));
        assertThat(property.getLabel(), is("Location"));
        assertThat(property.getType().equals(String.class), is(true));
        assertThat(property.isReadOnly(), is(true));
        assertValue(reflection, problem, property, location);

        property = propsByName.remove("throwable");
        assertThat(property.getName(), is("throwable"));
        assertThat(property.getLabel(), is("Throwable"));
        assertThat(property.getType().equals(Throwable.class), is(true));
        assertThat(property.isReadOnly(), is(true));
        assertValue(reflection, problem, property, throwable);

        assertThat(propsByName.isEmpty(), is(true));
    }

    @Test
    public void shouldUseAnnotationsOnClassFieldsForProperties() throws Exception {
        SomeStructure structure = new SomeStructure();
        structure.setCount(33);
        structure.setIdentifier("This is the identifier value");
        Reflection reflection = new Reflection(SomeStructure.class);
        List<Property> props = reflection.getAllPropertiesOn(structure);
        Map<String, Property> propsByName = reflection.getAllPropertiesByNameOn(structure);

        assertThat(props.size(), is(3));
        assertThat(propsByName.size(), is(3));

        Property property = propsByName.remove("identifier");
        assertThat(property.getName(), is("identifier"));
        assertThat(property.getLabel(), is(CommonI18n.noMoreContent.text()));
        assertThat(property.getDescription(), is(CommonI18n.nullActivityMonitorTaskName.text()));
        assertThat(property.getCategory(), is(CommonI18n.noMoreContent.text()));
        assertThat(property.getType().equals(String.class), is(true));
        assertThat(property.isReadOnly(), is(false));
        assertThat(property, is(findProperty(property.getName(), props)));
        assertValue(reflection, structure, property, structure.getIdentifier());

        property = propsByName.remove("count");
        assertThat(property.getName(), is("count"));
        assertThat(property.getLabel(), is("Count"));
        assertThat(property.getDescription(), is("This is the count"));
        assertThat(property.getCategory(), is(""));
        assertThat(property.getType().equals(Integer.TYPE), is(true));
        assertThat(property.isReadOnly(), is(false));
        assertValue(reflection, structure, property, structure.getCount());

        property = propsByName.remove("onFire");
        assertThat(property.getName(), is("onFire"));
        assertThat(property.getLabel(), is("On Fire"));
        assertThat(property.getDescription(), is(""));
        assertThat(property.getCategory(), is(""));
        assertThat(property.getType().equals(Boolean.TYPE), is(true));
        assertThat(property.isReadOnly(), is(true));
        assertValue(reflection, structure, property, structure.isOnFire());
    }

    protected void assertValue( Reflection reflection,
                                Object target,
                                Property property,
                                Object expectedValue ) throws Exception {
        Object actual = reflection.getProperty(target, property);
        assertThat(actual, is(expectedValue));
        assertThat(reflection.getPropertyAsString(target, property), is(notNullValue()));
    }

    protected Property findProperty( String propertyName,
                                     List<Property> properties ) {
        for (Property prop : properties) {
            if (prop.getName().equals(propertyName)) return prop;
        }
        return null;
    }

    protected static class SomeStructure {
        @Category( i18n = CommonI18n.class, value = "noMoreContent" )
        @Label( i18n = CommonI18n.class, value = "noMoreContent" )
        @Description( i18n = CommonI18n.class, value = "nullActivityMonitorTaskName" )
        private String identifier;
        @Description( value = "This is the count" )
        private int count;
        private boolean onFire;

        public int getCount() {
            return count;
        }

        public String getIdentifier() {
            return identifier;
        }

        public boolean isOnFire() {
            return onFire;
        }

        public void setCount( int count ) {
            this.count = count;
        }

        public void setIdentifier( String identifier ) {
            this.identifier = identifier;
        }

        // public void setOnFire( boolean onFire ) {
        // this.onFire = onFire;
        // }
    }
}
