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
package org.modeshape.sequencer.classfile.metadata;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.modeshape.common.util.HashCode;
import org.modeshape.graph.Graph;
import org.modeshape.sequencer.classfile.ClassFileSequencer;
import org.modeshape.sequencer.classfile.MockEnum;
import org.junit.After;
import org.junit.Test;

public class ClassFileMetadataReaderTest {

    private InputStream input;

    @After
    public void afterEach() throws Exception {
        if (input != null) {
            try {
                input.close();
            } finally {
                input = null;
            }
        }
    }

    @Test
    public void shouldReadJavaLangObject() throws Exception {
        compareMetadataToClass(Object.class);
    }

    @Test
    public void shouldReadJavaLangString() throws Exception {
        compareMetadataToClass(String.class);
    }

    @Test
    public void shouldReadOrgJbossDnaGraph() throws Exception {
        compareMetadataToClass(Graph.class);
    }

    @Test
    public void shouldReadOrgJbossDnaSequencerClassfileClassFileSequencer() throws Exception {
        compareMetadataToClass(ClassFileSequencer.class);
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldReadEnum() throws Exception {
        String resourceName = "/" + MockEnum.class.getName().replace('.', '/') + ".class";
        input = getClass().getResourceAsStream(resourceName);

        ClassMetadata cmd = ClassFileMetadataReader.instance(input);

        assertThat(cmd, instanceOf(EnumMetadata.class));
        EnumMetadata emd = (EnumMetadata)cmd;

        List<String> enumValues = Arrays.asList(new String[] {"VALUE_A", "VALUE_B", "VALUE_C"});
        assertThat(emd.getValues(), is(enumValues));

        for (FieldMetadata fmd : emd.getFields()) {
            assertThat(fmd.getName(), not(anyOf(is("VALUE_A"), is("VALUE_B"), is("VALUE_C"))));
        }

    }

    @SuppressWarnings( "unchecked" )
    private void compareMetadataToClass( Class<?> clazz ) throws Exception {
        String resourceName = "/" + clazz.getName().replace('.', '/') + ".class";
        input = getClass().getResourceAsStream(resourceName);

        ClassMetadata cmd = ClassFileMetadataReader.instance(input);

        assertThat(cmd.getClassName(), is(clazz.getName()));

        if (clazz.getSuperclass() == null) {
            assertThat(cmd.getSuperclassName(), is(nullValue()));
        } else {
            assertThat(cmd.getSuperclassName(), is(clazz.getSuperclass().getName()));
        }

        String[] clazzInterfaces = new String[clazz.getInterfaces().length];
        for (int i = 0; i < clazz.getInterfaces().length; i++) {
            clazzInterfaces[i] = clazz.getInterfaces()[i].getName();
        }

        assertThat(cmd.getInterfaces(), is(clazzInterfaces));
        assertThat(cmd.isEnumeration(), is(clazz.isEnum()));
        assertThat(cmd.getVisibility(), is(visibilityFor(clazz.getModifiers())));
        assertThat(cmd.isInterface(), is(Modifier.isInterface(clazz.getModifiers())));
        assertThat(cmd.isAbstract(), is(Modifier.isAbstract(clazz.getModifiers())));

        assertThat(cmd.getAnnotations().size(), is(clazz.getDeclaredAnnotations().length));
        for (AnnotationMetadata amd : cmd.getAnnotations()) {
            Class<Annotation> annotationClass = (Class<Annotation>)Class.forName(amd.getAnnotationClassName());
            Annotation annotation = clazz.getAnnotation(annotationClass);
            assertThat(annotation, is(notNullValue()));
        }

        checkFields(cmd, clazz);
        checkMethods(cmd, clazz);
        checkConstructors(cmd, clazz);
    }

    @SuppressWarnings( {"unchecked", "synthetic-access"} )
    private void checkFields( ClassMetadata cmd,
                              Class<?> clazz ) throws Exception {
        Map<FieldKey, Field> clazzFields = new HashMap<FieldKey, Field>();
        Map<FieldKey, FieldMetadata> metaFields = new HashMap<FieldKey, FieldMetadata>();

        for (Field field : clazz.getDeclaredFields()) {
            clazzFields.put(new FieldKey(field), field);
        }

        for (FieldMetadata field : cmd.getFields()) {
            metaFields.put(new FieldKey(field), field);
        }

        assertThat(metaFields.size(), is(clazzFields.size()));
        assertThat(metaFields.keySet(), is(clazzFields.keySet()));

        for (Map.Entry<FieldKey, FieldMetadata> entry : metaFields.entrySet()) {
            Field clazzField = clazzFields.get(entry.getKey());
            FieldMetadata metaField = entry.getValue();
            assert clazzField != null;

            // getCanonicalName converts the $ to .
            String metaName = metaField.getTypeName().replace('$', '.');
            String clazzName = clazzField.getType().getCanonicalName();
            assertThat(metaName, is(clazzName));

            assertThat(metaField.getVisibility(), is(visibilityFor(clazzField.getModifiers())));
            assertThat(metaField.isFinal(), is(Modifier.isFinal(clazzField.getModifiers())));
            assertThat(metaField.isStatic(), is(Modifier.isStatic(clazzField.getModifiers())));

            assertThat(metaField.getAnnotations().size(), is(clazzField.getDeclaredAnnotations().length));
            for (AnnotationMetadata amd : metaField.getAnnotations()) {
                Class<Annotation> annotationClass = (Class<Annotation>)Class.forName(amd.getAnnotationClassName());
                Annotation annotation = clazz.getAnnotation(annotationClass);
                assertThat(annotation, is(notNullValue()));
            }
        }
    }

    @SuppressWarnings( "synthetic-access" )
    private void checkMethods( ClassMetadata cmd,
                               Class<?> clazz ) throws Exception {
        Map<MethodKey, Method> clazzMethods = new HashMap<MethodKey, Method>();
        Map<MethodKey, MethodMetadata> metaMethods = new HashMap<MethodKey, MethodMetadata>();

        for (Method field : clazz.getDeclaredMethods()) {
            clazzMethods.put(new MethodKey(field), field);
        }

        for (MethodMetadata field : cmd.getMethods()) {
            metaMethods.put(new MethodKey(field), field);
        }

        assertThat(metaMethods.size(), is(clazzMethods.size()));
        assertThat(metaMethods.keySet(), is(clazzMethods.keySet()));

        for (Map.Entry<MethodKey, MethodMetadata> entry : metaMethods.entrySet()) {

            /*
             * We already know that the parameter types and name are equal, otherwise this would fail
             */

            Method clazzMethod = clazzMethods.get(entry.getKey());
            MethodMetadata metaMethod = entry.getValue();
            assert clazzMethod != null;

            // getCanonicalName converts the $ to .
            String metaName = metaMethod.getReturnType().replace('$', '.');
            String clazzName = clazzMethod.getReturnType().getCanonicalName();
            assertThat(metaName, is(clazzName));

            assertThat(metaMethod.getVisibility(), is(visibilityFor(clazzMethod.getModifiers())));
            assertThat(metaMethod.isFinal(), is(Modifier.isFinal(clazzMethod.getModifiers())));
            assertThat(metaMethod.isStatic(), is(Modifier.isStatic(clazzMethod.getModifiers())));

            assertThat(metaMethod.getAnnotations().size(), is(clazzMethod.getDeclaredAnnotations().length));

            // Can't really check this since some annotations are not runtime annotations
            // for (AnnotationMetadata amd : metaMethod.getAnnotations()) {
            // Class<Annotation> annotationClass = (Class<Annotation>)Class.forName(amd.getAnnotationClassName());
            // Annotation annotation = clazz.getAnnotation(annotationClass);
            //                
            // assertThat(annotation, is(notNullValue()));
            // }
        }
    }

    @SuppressWarnings( {"unchecked", "synthetic-access"} )
    private void checkConstructors( ClassMetadata cmd,
                                    Class<?> clazz ) throws Exception {
        Map<MethodKey, Constructor> clazzCtors = new HashMap<MethodKey, Constructor>();
        Map<MethodKey, MethodMetadata> metaCtors = new HashMap<MethodKey, MethodMetadata>();

        for (Constructor field : clazz.getDeclaredConstructors()) {
            clazzCtors.put(new MethodKey(field), field);
        }

        for (MethodMetadata field : cmd.getConstructors()) {
            metaCtors.put(new MethodKey(field), field);
        }

        assertThat(metaCtors.size(), is(clazzCtors.size()));
        assertThat(metaCtors.keySet(), is(clazzCtors.keySet()));

        for (Map.Entry<MethodKey, MethodMetadata> entry : metaCtors.entrySet()) {

            /*
             * We already know that the parameter types and name are equal, otherwise this would fail
             */

            Constructor clazzCtor = clazzCtors.get(entry.getKey());
            MethodMetadata metaCtor = entry.getValue();
            assert clazzCtor != null;

            assertThat(metaCtor.getVisibility(), is(visibilityFor(clazzCtor.getModifiers())));
            assertThat(metaCtor.isFinal(), is(Modifier.isFinal(clazzCtor.getModifiers())));
            assertThat(metaCtor.isStatic(), is(Modifier.isStatic(clazzCtor.getModifiers())));

            assertThat(metaCtor.getAnnotations().size(), is(clazzCtor.getDeclaredAnnotations().length));

            // Can't really check this since some annotations are not runtime annotations
            // for (AnnotationMetadata amd : metaMethod.getAnnotations()) {
            // Class<Annotation> annotationClass = (Class<Annotation>)Class.forName(amd.getAnnotationClassName());
            // Annotation annotation = clazz.getAnnotation(annotationClass);
            //                
            // assertThat(annotation, is(notNullValue()));
            // }
        }
    }

    private Visibility visibilityFor( int modifier ) {
        if (Modifier.isPublic(modifier)) return Visibility.PUBLIC;
        if (Modifier.isProtected(modifier)) return Visibility.PROTECTED;
        if (Modifier.isPrivate(modifier)) return Visibility.PRIVATE;
        return Visibility.PACKAGE;
    }

    private class FieldKey {
        private final String name;

        private FieldKey( Field field ) {
            this.name = field.getName();
        }

        private FieldKey( FieldMetadata field ) {
            this.name = field.getName();
        }

        @Override
        public boolean equals( Object obj ) {
            if (!(obj instanceof FieldKey)) return false;
            return name.equals(((FieldKey)obj).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private class MethodKey {
        private final String name;
        private final List<String> parameters;

        private MethodKey( Method method ) {
            this.name = method.getName();
            this.parameters = new ArrayList<String>();
            for (Class<?> parameter : method.getParameterTypes()) {
                parameters.add(parameter.getCanonicalName());
            }
        }

        @SuppressWarnings( "unchecked" )
        private MethodKey( Constructor ctor ) {
            this.name = ctor.getName();
            this.parameters = new ArrayList<String>();
            for (Class<?> parameter : ctor.getParameterTypes()) {
                parameters.add(parameter.getCanonicalName());
            }
        }

        private MethodKey( MethodMetadata method ) {
            this.name = method.getName();
            this.parameters = method.getParameters();
        }

        @Override
        public boolean equals( Object obj ) {
            if (!(obj instanceof MethodKey)) return false;

            MethodKey other = (MethodKey)obj;
            return name.equals(other.name) && parameters.equals(other.parameters);
        }

        @Override
        public int hashCode() {
            return HashCode.compute(name, parameters);
        }

        @Override
        public String toString() {
            return name + "(" + parameters + ")";
        }

    }
}
