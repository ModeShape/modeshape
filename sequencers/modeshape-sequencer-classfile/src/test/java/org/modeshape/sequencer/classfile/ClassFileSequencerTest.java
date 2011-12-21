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
package org.modeshape.sequencer.classfile;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.modeshape.jcr.api.JcrConstants.JCR_PRIMARY_TYPE;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.*;
import org.modeshape.sequencer.classfile.testdata.MockClass;
import org.modeshape.sequencer.classfile.testdata.MockEnum;
import java.io.Serializable;
import java.util.*;

/**
 * Unit test for {@link ClassFileSequencer}
 *
 * @author Horia Chiorean
 */
public class ClassFileSequencerTest extends AbstractSequencerTest {

    @Test
    public void sequenceEnum() throws Exception {

        String filePath = MockEnum.class.getName().replaceAll("\\.", "/") + ".class";
        createNodeWithContentFromFile("enum.class", filePath);
        
        //expected by sequencer in the same location
        String expectedSequencedPathSameLocation = "enum.class/" + MockEnum.class.getName().replaceAll("\\.", "/");
        assertSequencedMockEnum(expectedSequencedPathSameLocation);

        //expected by sequencer in a different location
        String expectedSequencedPathNewLocation = "classes/enum.class";
        assertSequencedMockEnum(expectedSequencedPathNewLocation);
    }

    private void assertSequencedMockEnum( String enumPath ) throws Exception {
        TreeSet<String> expectedEnumValues = new TreeSet<String>(Arrays.asList(MockEnum.VALUE_A.name(), MockEnum.VALUE_B.name(), MockEnum.VALUE_C.name()));

        Node sequencedNodeSameLocation = getSequencedNode(rootNode, enumPath);
        assertNotNull(sequencedNodeSameLocation);
        assertEquals(ClassFileSequencerLexicon.ENUM, sequencedNodeSameLocation.getProperty(JCR_PRIMARY_TYPE).getString());
        assertEquals(expectedEnumValues, enumValuesToString(sequencedNodeSameLocation));
    }

    @Test
    public void sequenceClass() throws Exception {
        String filePath = MockClass.class.getName().replaceAll("\\.", "/") + ".class";
        createNodeWithContentFromFile("mockclass.class", filePath);

        //expected by sequencer in the same location
        String expectedSequencedPathSameLocation = "mockclass.class/" + MockClass.class.getName().replaceAll("\\.", "/");
        assertSequencedMockClass(expectedSequencedPathSameLocation);

        //expected by sequencer in a different location
        String expectedSequencedPathNewLocation = "classes/mockclass.class";
        assertSequencedMockClass(expectedSequencedPathNewLocation);
    }

    private void assertSequencedMockClass( String nodePath ) throws Exception {
        Node classNode = getSequencedNode(rootNode, nodePath);

        //class meta-info
        assertNotNull(classNode);
        assertEquals(ClassFileSequencerLexicon.CLASS, classNode.getProperty(JCR_PRIMARY_TYPE).getString());
        assertEquals(Object.class.getName(), classNode.getProperty(SUPER_CLASS_NAME).getString());
        assertEquals("public", classNode.getProperty(VISIBILITY).getString().toLowerCase());
        assertFalse(classNode.getProperty(ABSTRACT).getBoolean());
        assertFalse(classNode.getProperty(INTERFACE).getBoolean());
        assertTrue(classNode.getProperty(FINAL).getBoolean());
        assertFalse(classNode.getProperty(STRICT_FP).getBoolean());
        List<String> interfaces = valuesToStringList(classNode.getProperty(INTERFACES).getValues());
        assertEquals(Arrays.asList(Serializable.class.getName()), interfaces);
        assertFalse(classNode.getNodes(ANNOTATIONS).hasNext());

        //constructors
        Node constructors = classNode.getNode(CONSTRUCTORS);
        assertEquals(CONSTRUCTORS, constructors.getProperty(JCR_PRIMARY_TYPE).getString());
        NodeIterator constructorMethodsIt = constructors.getNodes();
        Node constructorMethod = constructorMethodsIt.nextNode();
        assertFalse(constructorMethodsIt.hasNext());
        assertMethod(constructorMethod, MockClass.class.getName(), "void", "public",
                     false, false, false, false, false, false, Arrays.asList(Boolean.class.getName()));
        assertFalse(constructorMethod.getNodes(ANNOTATIONS).hasNext());

        //methods
        Node methods = classNode.getNode(METHODS);
        assertEquals(METHODS, methods.getProperty(JCR_PRIMARY_TYPE).getString());
        NodeIterator methodsIterator = methods.getNodes();
        Node method = methodsIterator.nextNode();
        assertMethod(method, "voidMethod", "void", "package", false, false, false, false, false, true,
                     new ArrayList<String>());

        Node annotations = method.getNode(ANNOTATIONS);
        assertEquals(ANNOTATIONS, annotations.getProperty(JCR_PRIMARY_TYPE).getString());
        NodeIterator annotationsIt = annotations.getNodes();
        Node annotation = annotationsIt.nextNode();
        assertFalse(annotationsIt.hasNext());
        assertEquals(java.lang.Deprecated.class.getName(), annotation.getProperty(NAME).getString());
        assertFalse(annotation.getNodes().hasNext());
        assertFalse(methodsIterator.hasNext());

        //fields
        Node fields = classNode.getNode(FIELDS);
        assertEquals(FIELDS, fields.getProperty(JCR_PRIMARY_TYPE).getString());
        NodeIterator fieldsIt = fields.getNodes();
        Node field = fieldsIt.nextNode();

        assertField(field, "booleanField", Boolean.class.getName(), "protected", false, false, false, false);
        field = fieldsIt.nextNode();

        assertField(field, "STATIC_FINAL_INTEGER_FIELD", Integer.class.getName(), "public", true, true, false, false);
        field = fieldsIt.nextNode();

        assertField(field, "STATIC_VOLATILE_STRING_FIELD", String.class.getName(), "private", true, false, false, true);
        assertFalse(fieldsIt.hasNext());
    }

    private void assertMethod( Node method, String name, String returnTypeClassName, String visibility, 
                               boolean isStatic, boolean isFinal, boolean isAbstract, boolean isStrictFP, boolean isNative,
                               boolean isSynchronized, List<String> expectedParameters) throws RepositoryException {
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

    private void assertField(Node field, String name, String typeClassName, String visibility,
                             boolean isStatic, boolean isFinal, boolean isTransient, boolean isVolatile) throws RepositoryException {
        assertEquals(FIELD, field.getProperty(JCR_PRIMARY_TYPE).getString());
        assertEquals(name, field.getProperty(NAME).getString());
        assertEquals(typeClassName, field.getProperty(TYPE_CLASS_NAME).getString());
        assertEquals(visibility, field.getProperty(VISIBILITY).getString().toLowerCase());
        assertEquals(isStatic, field.getProperty(STATIC).getBoolean());
        assertEquals(isFinal, field.getProperty(FINAL).getBoolean());
        assertEquals(isTransient, field.getProperty(TRANSIENT).getBoolean());
        assertEquals(isVolatile, field.getProperty(VOLATILE).getBoolean());
        assertFalse(field.getNodes(ANNOTATIONS).hasNext());
    }
    
    private List<String> valuesToStringList(Value[] values) throws RepositoryException {
        List<String> result = new ArrayList<String>();
        for (Value value : values) {
            result.add(value.getString());
        }
        return result;
    }
    
    private Set<String> enumValuesToString( Node sequencedNode ) throws RepositoryException {
        Value[] enumValues = sequencedNode.getProperty(ClassFileSequencerLexicon.ENUM_VALUES).getValues();
        assertNotNull(enumValues);
        Set<String> enumValuesSet = new TreeSet<String>();
        for (Value value : enumValues) {
            enumValuesSet.add(value.getString());
        }
        return enumValuesSet;
    }

}
