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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.sequencer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.modeshape.jcr.api.JcrConstants.JCR_PRIMARY_TYPE;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.ABSTRACT;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.ANNOTATIONS;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.CONSTRUCTORS;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.ENUM_VALUES;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.FIELD;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.FIELDS;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.FINAL;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.INTERFACE;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.INTERFACES;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.METHOD;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.METHODS;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.NAME;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.NATIVE;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.PARAMETERS;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.RETURN_TYPE_CLASS_NAME;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.STATIC;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.STRICT_FP;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.SUPER_CLASS_NAME;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.SYNCHRONIZED;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.TRANSIENT;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.TYPE_CLASS_NAME;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.VALUE;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.VISIBILITY;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.VOLATILE;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import org.modeshape.sequencer.classfile.ClassFileSequencerLexicon;
import org.modeshape.sequencer.testdata.MockClass;
import org.modeshape.sequencer.testdata.MockEnum;

/**
 * Helper class, used by both {@link org.modeshape.sequencer.classfile.ClassFileSequencerTest} and
 * {@link org.modeshape.sequencer.javafile.JavaFileSequencerTest} to assert the expected values from {@link MockClass} and
 * {@link MockEnum}
 * 
 * @author Horia Chiorean
 */
public abstract class JavaSequencerHelper {

    private static final String STATIC_VOLATILE_STRING_FIELD = "STATIC_VOLATILE_STRING_FIELD";

    @SuppressWarnings( "synthetic-access" )
    public static final JavaSequencerHelper CLASS_FILE_HELPER = new ClassFile();
    @SuppressWarnings( "synthetic-access" )
    public static final JavaSequencerHelper JAVA_FILE_HELPER = new JavaFile();

    protected JavaSequencerHelper() {
    }

    public void assertSequencedMockEnum( Node enumNode ) throws Exception {
        TreeSet<String> expectedEnumValues = new TreeSet<String>(Arrays.asList(MockEnum.VALUE_A.name(),
                                                                               MockEnum.VALUE_B.name(),
                                                                               MockEnum.VALUE_C.name()));

        assertNotNull(enumNode);
        assertEquals(ClassFileSequencerLexicon.ENUM, enumNode.getProperty(JCR_PRIMARY_TYPE).getString());
        assertEquals(expectedEnumValues, enumValuesToString(enumNode));
    }

    private Set<String> enumValuesToString( Node sequencedNode ) throws RepositoryException {
        Value[] enumValues = sequencedNode.getProperty(ENUM_VALUES).getValues();
        assertNotNull(enumValues);
        Set<String> enumValuesSet = new TreeSet<String>();
        for (Value value : enumValues) {
            enumValuesSet.add(value.getString());
        }
        return enumValuesSet;
    }

    public void assertSequencedMockClass( Node classNode ) throws Exception {
        assertClassMetaInfo(classNode);
        assertConstructors(classNode);
        assertMethods(classNode);
        assertFields(classNode);
    }

    private void assertClassMetaInfo( Node classNode ) throws RepositoryException {
        // class meta-info
        assertNotNull(classNode);
        assertEquals(ClassFileSequencerLexicon.CLASS, classNode.getProperty(JCR_PRIMARY_TYPE).getString());
        assertEquals(Object.class.getName(), classNode.getProperty(SUPER_CLASS_NAME).getString());
        assertEquals("public", classNode.getProperty(VISIBILITY).getString().toLowerCase());
        assertFalse(classNode.getProperty(ABSTRACT).getBoolean());
        assertFalse(classNode.getProperty(INTERFACE).getBoolean());
        assertTrue(classNode.getProperty(FINAL).getBoolean());
        assertFalse(classNode.getProperty(STRICT_FP).getBoolean());
        List<String> interfaces = valuesToStringList(classNode.getProperty(INTERFACES).getValues());
        assertEquals(Arrays.asList(getExpectedTypeName(Serializable.class)), interfaces);
        assertNoAnnotationsOnNode(classNode);
    }

    private void assertConstructors( Node classNode ) throws RepositoryException {
        // constructors
        Node constructors = classNode.getNode(CONSTRUCTORS);
        assertEquals(CONSTRUCTORS, constructors.getProperty(JCR_PRIMARY_TYPE).getString());
        NodeIterator constructorMethodsIt = constructors.getNodes();
        Node constructorMethod = constructorMethodsIt.nextNode();
        assertFalse(constructorMethodsIt.hasNext());
        assertMethod(constructorMethod,
                     getExpectedTypeName(MockClass.class),
                     "void",
                     "public",
                     false,
                     false,
                     false,
                     false,
                     false,
                     false,
                     Arrays.asList(getExpectedTypeName(Boolean.class)));
        assertNoAnnotationsOnNode(constructorMethod);
    }

    private void assertMethods( Node classNode ) throws RepositoryException {
        // methods
        Node methods = classNode.getNode(METHODS);
        assertEquals(METHODS, methods.getProperty(JCR_PRIMARY_TYPE).getString());
        NodeIterator methodsIterator = methods.getNodes();
        Node method = methodsIterator.nextNode();
        assertMethod(method, "voidMethod", "void", "package", false, false, false, false, false, true, new ArrayList<String>());

        assertNodeHasAnnotation(method, Deprecated.class);
        assertFalse(methodsIterator.hasNext());
    }

    protected Node assertNodeHasAnnotation( Node method,
                                            Class<?> annotationClass ) throws RepositoryException {
        Node annotations = method.getNode(ANNOTATIONS);
        assertEquals(ANNOTATIONS, annotations.getProperty(JCR_PRIMARY_TYPE).getString());

        NodeIterator annotationsIt = annotations.getNodes();
        assertEquals(1, annotationsIt.getSize());
        Node annotation = annotationsIt.nextNode();
        assertEquals(getExpectedTypeName(annotationClass), annotation.getProperty(NAME).getString());
        return annotation;
    }

    private void assertFields( Node classNode ) throws RepositoryException {
        // fields
        Node fields = classNode.getNode(FIELDS);
        assertEquals(FIELDS, fields.getProperty(JCR_PRIMARY_TYPE).getString());

        // not sure about the order, so we load them into a map
        Map<String, Node> fieldsMap = loadNodesByName(fields);

        Node booleanField = fieldsMap.remove("booleanField");
        assertField(booleanField, "booleanField", getExpectedTypeName(Boolean.class), "protected", false, false, false, false);

        Node staticFinalIntegerField = fieldsMap.remove("STATIC_FINAL_INTEGER_FIELD");
        assertField(staticFinalIntegerField,
                    "STATIC_FINAL_INTEGER_FIELD",
                    getExpectedTypeName(Integer.class),
                    "public",
                    true,
                    true,
                    false,
                    false);

        Node staticVolatileStringField = fieldsMap.remove(STATIC_VOLATILE_STRING_FIELD);
        assertField(staticVolatileStringField,
                    "STATIC_VOLATILE_STRING_FIELD",
                    getExpectedTypeName(String.class),
                    "private",
                    true,
                    false,
                    false,
                    true);

        Node serialVersionUIDField = fieldsMap.remove("serialVersionUID");
        assertField(serialVersionUIDField,
                    "serialVersionUID",
                    getExpectedTypeName(Long.TYPE),
                    "private",
                    true,
                    true,
                    false,
                    false);
    }

    private Map<String, Node> loadNodesByName( Node rootNode ) throws RepositoryException {
        Map<String, Node> nodesMap = new HashMap<String, Node>();
        NodeIterator nodesIt = rootNode.getNodes();
        while (nodesIt.hasNext()) {
            Node node = nodesIt.nextNode();
            nodesMap.put(node.getName(), node);
        }
        return nodesMap;
    }

    private void assertMethod( Node method,
                               String name,
                               String returnTypeClassName,
                               String visibility,
                               boolean isStatic,
                               boolean isFinal,
                               boolean isAbstract,
                               boolean isStrictFP,
                               boolean isNative,
                               boolean isSynchronized,
                               List<String> expectedParameters ) throws RepositoryException {
        assertEquals(METHOD, method.getProperty(JCR_PRIMARY_TYPE).getString());
        assertEquals(name, method.getProperty(NAME).getString());
        assertEquals(returnTypeClassName, method.getProperty(RETURN_TYPE_CLASS_NAME).getString());
        assertEquals(visibility, method.getProperty(VISIBILITY).getString());
        assertEquals(isStatic, method.getProperty(STATIC).getBoolean());
        assertEquals(isFinal, method.getProperty(FINAL).getBoolean());
        assertEquals(isAbstract, method.getProperty(ABSTRACT).getBoolean());
        assertEquals(isStrictFP, method.getProperty(STRICT_FP).getBoolean());
        assertEquals(isNative, method.getProperty(NATIVE).getBoolean());
        assertEquals(isSynchronized, method.getProperty(SYNCHRONIZED).getBoolean());
        List<String> parameters = valuesToStringList(method.getProperty(PARAMETERS).getValues());
        assertEquals(expectedParameters, parameters);
    }

    private void assertField( Node field,
                              String name,
                              String typeClassName,
                              String visibility,
                              boolean isStatic,
                              boolean isFinal,
                              boolean isTransient,
                              boolean isVolatile ) throws RepositoryException {
        assertEquals(FIELD, field.getProperty(JCR_PRIMARY_TYPE).getString());
        assertEquals(name, field.getProperty(NAME).getString());
        assertEquals(typeClassName, field.getProperty(TYPE_CLASS_NAME).getString());
        assertEquals(visibility, field.getProperty(VISIBILITY).getString().toLowerCase());
        assertEquals(isStatic, field.getProperty(STATIC).getBoolean());
        assertEquals(isFinal, field.getProperty(FINAL).getBoolean());
        assertEquals(isTransient, field.getProperty(TRANSIENT).getBoolean());
        assertEquals(isVolatile, field.getProperty(VOLATILE).getBoolean());
        assertNoAnnotationsOnNode(field);
    }

    protected void assertNoAnnotationsOnNode( Node node ) throws RepositoryException {
        assertFalse(node.getNodes(ANNOTATIONS).hasNext());
    }

    private List<String> valuesToStringList( Value[] values ) throws RepositoryException {
        List<String> result = new ArrayList<String>();
        for (Value value : values) {
            result.add(value.getString());
        }
        return result;
    }

    protected abstract String getExpectedTypeName( Class<?> expectedTypeClass );

    /**
     * Extension of the default helper, with the particularities for java files
     */
    private static class JavaFile extends JavaSequencerHelper {
        @Override
        protected String getExpectedTypeName( Class<?> expectedTypeClass ) {
            return expectedTypeClass.getSimpleName();
        }

        @Override
        protected void assertNoAnnotationsOnNode( Node node ) throws RepositoryException {
            // overcome the fact that source annotations are present when parsing java files
            if (STATIC_VOLATILE_STRING_FIELD.equals(node.getName())) {
                Node suppressWarnings = assertNodeHasAnnotation(node, SuppressWarnings.class);
                Node annotationMember = suppressWarnings.getNode("default");
                assertEquals("unused", annotationMember.getProperty(VALUE).getString());
            } else {
                super.assertNoAnnotationsOnNode(node);
            }
        }
    }

    /**
     * Extension of the default helper, with the particularities for class files
     */
    private static class ClassFile extends JavaSequencerHelper {
        @Override
        protected String getExpectedTypeName( Class<?> expectedTypeClass ) {
            return expectedTypeClass.getName();
        }
    }
}
