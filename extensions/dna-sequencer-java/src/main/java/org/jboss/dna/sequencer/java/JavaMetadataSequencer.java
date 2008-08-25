/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.sequencer.java;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.jboss.dna.common.monitor.ProgressMonitor;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.sequencer.java.metadata.AnnotationMetadata;
import org.jboss.dna.sequencer.java.metadata.ClassMetadata;
import org.jboss.dna.sequencer.java.metadata.ConstructorMetadata;
import org.jboss.dna.sequencer.java.metadata.FieldMetadata;
import org.jboss.dna.sequencer.java.metadata.ImportMetadata;
import org.jboss.dna.sequencer.java.metadata.ImportOnDemandMetadata;
import org.jboss.dna.sequencer.java.metadata.JavaMetadata;
import org.jboss.dna.sequencer.java.metadata.MarkerAnnotationMetadata;
import org.jboss.dna.sequencer.java.metadata.MethodMetadata;
import org.jboss.dna.sequencer.java.metadata.MethodTypeMemberMetadata;
import org.jboss.dna.sequencer.java.metadata.ModifierMetadata;
import org.jboss.dna.sequencer.java.metadata.NormalAnnotationMetadata;
import org.jboss.dna.sequencer.java.metadata.PackageMetadata;
import org.jboss.dna.sequencer.java.metadata.PrimitiveFieldMetadata;
import org.jboss.dna.sequencer.java.metadata.ReferenceFieldMetadata;
import org.jboss.dna.sequencer.java.metadata.SingleImportMetadata;
import org.jboss.dna.sequencer.java.metadata.SingleMemberAnnotationMetadata;
import org.jboss.dna.sequencer.java.metadata.TypeMetadata;
import org.jboss.dna.sequencer.java.metadata.Variable;
import org.jboss.dna.spi.graph.NameFactory;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.sequencers.SequencerContext;
import org.jboss.dna.spi.sequencers.SequencerOutput;
import org.jboss.dna.spi.sequencers.StreamSequencer;

/**
 * A sequencer that processes a compilation unit, extracts the meta data for the compilation unit, and then writes these
 * informations to the repository.
 * <p>
 * The structural representation of the informations from the compilation unit looks like this:
 * <ul>
 * <li><strong>java:compilationUnit</strong> node of type <code>java:compilationUnit</code>
 * <ul>
 * <li> <strong>java:package</strong> - optional child node that represents the package child node of the compilation unit
 * <ul>
 * <li> <strong>java:packageDeclaration</strong> -
 * <ul>
 * <li><strong>java:packageName</strong></li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * <li> <strong>java:import</strong> - optional child node that represents the import declaration of the compilation unit
 * <ul>
 * <li> <strong>java:importDeclaration</strong> -
 * <ul>
 * <li> <strong>java:singleTypeImportDeclaration</strong>
 * <ul>
 * <li> <strong>java:packageDeclaration</strong> -
 * <ul>
 * <li><strong>java:typeName</strong></li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * <li> <strong>java:typeImportOnDemandDeclaration</strong>
 * <ul>
 * <li> <strong>java:packageDeclaration</strong> -
 * <ul>
 * <li><strong>java:typeName</strong></li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * <li><strong>java:unitType</strong> - optional child node that represents the top level type (class, interface, enum,
 * annotation) declaration of the compilation unit</li>
 * <ul>
 * <li> <strong>java:classDeclaration</strong> - optional child node that represents the class declaration of the compilation
 * unit
 * <ul>
 * <li> <strong>java:normalClass</strong> -
 * <ul>
 * <li> <strong>java:normalClassDeclaration</strong> -
 * <ul>
 * <li> <strong>java:modifier</strong> - </li>
 * <li> <strong>java:name</strong> - </li>
 * <li> <strong>java:field</strong> - </li>
 * <li> <strong>java:method</strong> - </li>
 * <li> <strong>java:constructor</strong> - </li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * </ul>
 * </p>
 * 
 * @author Serge Pagop
 */
public class JavaMetadataSequencer implements StreamSequencer {

    private static final String SLASH = "/";

    public static final String JAVA_COMPILATION_UNIT_NODE = "java:compilationUnit";
    public static final String JAVA_COMPILATION_UNIT_PRIMARY_TYPE = "jcr:primaryType";
    
    // package declaration
    public static final String JAVA_PACKAGE_CHILD_NODE = "java:package";
    public static final String JAVA_PACKAGE_DECLARATION_CHILD_NODE = "java:packageDeclaration";
    public static final String JAVA_PACKAGE_NAME = "java:packageName";
    
    // Annnotation declaration
    public static final String JAVA_ANNOTATION_CHILD_NODE = "java:annotation";
    public static final String JAVA_ANNOTATION_DECLARATION_CHILD_NODE = "java:annotationDeclaration";
    public static final String JAVA_ANNOTATION_TYPE_CHILD_NODE = "java:annotationType";
    
    // Marker annotation
    public static final String JAVA_MARKER_ANNOTATION_CHILD_NODE = "java:markerAnnotation";
    public static final String JAVA_MARKER_ANNOTATION_NAME ="java:markerAnnotationName ";
    
    // Normal annotation 
    public static final String JAVA_NORMAL_ANNOTATION_CHILD_NODE = "java:normalAnnotation";
    public static final String JAVA_NORMALANNOTATION_NAME ="java:normalAnnotationName";
    
    // Single element annotation
    public static final String JAVA_SINGLE_ELEMENT_ANNOTATION_CHILD_NODE = "java:singleElementAnnotation";
    public static final String JAVA_SINGLE_ANNOTATION_NAME ="java:singleElementAnnotationName";
    
    // Import declaration
    public static final String JAVA_IMPORT_CHILD_NODE = "java:import";
    public static final String JAVA_IMPORT_DECLARATION_CHILD_NODE = "java:importDeclaration";
    
    // Single import declaration 
    public static final String JAVA_SINGLE_IMPORT_CHILD_NODE = "java:singleImport";
    public static final String JAVA_SINGLE_IMPORT_TYPE_DECLARATION_CHILD_NODE = "java:singleTypeImportDeclaration";
    public static final String JAVA_SINGLE_IMPORT_NAME = "java:singleImportName ";
    
    
    // OnDemand import declaration
    public static final String JAVA_ON_DEMAND_IMPORT_CHILD_NODE = "java:importOnDemand";
    public static final String JAVA_ON_DEMAND_IMPORT_TYPE_DECLARATION_CHILD_NODE = "java:typeImportOnDemandDeclaration";
    public static final String JAVA_ON_DEMAND_IMPORT_NAME ="java:onDemandImportName";
   
    
    // Class declaration
    public static final String JAVA_UNIT_TYPE_CHILD_NODE = "java:unitType";
    public static final String JAVA_CLASS_DECLARATION_CHILD_NODE = "java:classDeclaration";
    
    // Normal class declaration
    public static final String JAVA_NORMAL_CLASS_CHILD_NODE = "java:normalClass";
    public static final String JAVA_NORMAL_CLASS_DECLARATION_CHILD_NODE = "java:normalClassDeclaration";
    public static final String JAVA_NORMAL_CLASS_NAME = "java:normalClassName";
    
    // Modifier declaration
    public static final String JAVA_MODIFIER_CHILD_NODE = "java:modifier";
    public static final String JAVA_MODIFIER_DECLARATION_CHILD_NODE = "java:modifierDeclaration";
    public static final String JAVA_MODIFIER_NAME = "java:modifierName";

    // Variable declaration
    public static final String JAVA_VARIABLE = "java:variable";
    public static final String JAVA_VARIABLE_NAME = "java:variableName";

    // Primitive type
    public static final String JAVA_FIELD_CHILD_NODE = "java:field";
    public static final String JAVA_FIELD_TYPE_CHILD_NODE = "java:fieldType";
    public static final String JAVA_TYPE_CHILD_NODE = "java:type";
    public static final String JAVA_PRIMITIVE_TYPE_CHILD_NODE = "java:primitiveType";
    public static final String JAVA_PRIMITIVE_TYPE_NAME="java:primitiveTypeName";

    // Reference type
    public static final String JAVA_REFERENCE_TYPE_CHILD_NODE = "java:referenceType";

    // method declaration
    public static final String JAVA_METHOD_CHILD_NODE = "java:method";
    public static final String JAVA_METHOD_DECLARATION_CHILD_NODE = "java:methodDeclaration";
    public static final String JAVA_METHOD_NAME = "java:methodName";

    // constructor
    public static final String JAVA_CONSTRUCTOR_CHILD_NODE = "java:constructor";
    public static final String JAVA_CONSTRUCTOR_DECLARATION_CHILD_NODE = "java:constructorDeclaration";
    public static final String JAVA_CONSTRUCTOR_NAME = "java:constructorName";

    // parameter
    public static final String JAVA_PARAMETER = "java:parameter";
    public static final String JAVA_FORMAL_PARAMETER = "java:formalParameter";
    public static final String JAVA_PARAMETER_NAME = "java:parameterName";

    public static final String JAVA_RETURN_TYPE = "java:resultType";

    


    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.sequencers.StreamSequencer#sequence(java.io.InputStream,
     *      org.jboss.dna.spi.sequencers.SequencerOutput, org.jboss.dna.spi.sequencers.SequencerContext,
     *      org.jboss.dna.common.monitor.ProgressMonitor)
     */
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          SequencerContext context,
                          ProgressMonitor progressMonitor ) {
        progressMonitor.beginTask(10, JavaMetadataI18n.sequencerTaskName);

        JavaMetadata javaMetadata = null;
        NameFactory nameFactory = context.getFactories().getNameFactory();
        PathFactory pathFactory = context.getFactories().getPathFactory();

        try {
            javaMetadata = JavaMetadata.instance(stream, JavaMetadataUtil.length(stream), null, progressMonitor.createSubtask(10));
            if (progressMonitor.isCancelled()) return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (javaMetadata != null) {
            Path javaCompilationUnitNode = pathFactory.create(JAVA_COMPILATION_UNIT_NODE);
            output.setProperty(javaCompilationUnitNode,
                               nameFactory.create(JAVA_COMPILATION_UNIT_PRIMARY_TYPE),
                               "java:compilationUnit");

            // sequence package declaration of a unit.
            PackageMetadata packageMetadata = javaMetadata.getPackageMetadata();
            if (packageMetadata != null) {
                if (StringUtils.isNotEmpty(packageMetadata.getName())) {

                    Path javaPackageDeclarationChildNode = pathFactory.create(JAVA_COMPILATION_UNIT_NODE + SLASH
                                                                              + JAVA_PACKAGE_CHILD_NODE + SLASH
                                                                              + JAVA_PACKAGE_DECLARATION_CHILD_NODE);
                    output.setProperty(javaPackageDeclarationChildNode,
                                       nameFactory.create(JAVA_PACKAGE_NAME),
                                       javaMetadata.getPackageMetadata().getName());
                }

                int markerAnnotationIndex = 1;
                int singleAnnatationIndex = 1;
                int normalAnnotationIndex = 1;
                for (AnnotationMetadata annotationMetadata : packageMetadata.getAnnotationMetada()) {
                    if (annotationMetadata instanceof MarkerAnnotationMetadata) {
                        MarkerAnnotationMetadata markerAnnotationMetadata = (MarkerAnnotationMetadata)annotationMetadata;
                        Path markerAnnotationChildNode = pathFactory.create(JAVA_COMPILATION_UNIT_NODE + SLASH
                                                                            + JAVA_PACKAGE_CHILD_NODE + SLASH
                                                                            + JAVA_PACKAGE_DECLARATION_CHILD_NODE + SLASH
                                                                            + JAVA_ANNOTATION_CHILD_NODE + SLASH
                                                                            + JAVA_ANNOTATION_DECLARATION_CHILD_NODE + SLASH
                                                                            + JAVA_ANNOTATION_TYPE_CHILD_NODE + SLASH
                                                                            + JAVA_MARKER_ANNOTATION_CHILD_NODE + "["
                                                                            + markerAnnotationIndex + "]");
                        output.setProperty(markerAnnotationChildNode,
                                           nameFactory.create(JAVA_MARKER_ANNOTATION_NAME),
                                           markerAnnotationMetadata.getName());
                        markerAnnotationIndex++;
                    }
                    if (annotationMetadata instanceof SingleMemberAnnotationMetadata) {
                        SingleMemberAnnotationMetadata singleMemberAnnotationMetadata = (SingleMemberAnnotationMetadata)annotationMetadata;
                        Path singleMemberAnnotationChildNode = pathFactory.create(JAVA_COMPILATION_UNIT_NODE + SLASH
                                                                                  + JAVA_PACKAGE_CHILD_NODE + SLASH
                                                                                  + JAVA_PACKAGE_DECLARATION_CHILD_NODE + SLASH
                                                                                  + JAVA_ANNOTATION_CHILD_NODE + SLASH
                                                                                  + JAVA_ANNOTATION_DECLARATION_CHILD_NODE
                                                                                  + SLASH + JAVA_ANNOTATION_TYPE_CHILD_NODE
                                                                                  + SLASH
                                                                                  + JAVA_SINGLE_ELEMENT_ANNOTATION_CHILD_NODE
                                                                                  + "[" + singleAnnatationIndex + "]");
                        output.setProperty(singleMemberAnnotationChildNode,
                                           nameFactory.create(JAVA_SINGLE_ANNOTATION_NAME),
                                           singleMemberAnnotationMetadata.getName());
                        singleAnnatationIndex++;
                    }
                    if (annotationMetadata instanceof NormalAnnotationMetadata) {
                        NormalAnnotationMetadata normalAnnotationMetadata = (NormalAnnotationMetadata)annotationMetadata;
                        Path normalAnnotationChildNode = pathFactory.create(JAVA_COMPILATION_UNIT_NODE + SLASH
                                                                            + JAVA_PACKAGE_CHILD_NODE + SLASH
                                                                            + JAVA_PACKAGE_DECLARATION_CHILD_NODE + SLASH
                                                                            + JAVA_ANNOTATION_CHILD_NODE + SLASH
                                                                            + JAVA_ANNOTATION_DECLARATION_CHILD_NODE + SLASH
                                                                            + JAVA_ANNOTATION_TYPE_CHILD_NODE + SLASH
                                                                            + JAVA_NORMAL_ANNOTATION_CHILD_NODE + "["
                                                                            + normalAnnotationIndex + "]");

                        output.setProperty(normalAnnotationChildNode,
                                           nameFactory.create(JAVA_NORMALANNOTATION_NAME),
                                           normalAnnotationMetadata.getName());
                        normalAnnotationIndex++;
                    }
                }
            }

            // sequence import declarations of a unit
            int importOnDemandIndex = 1;
            int singleImportIndex = 1;
            for (ImportMetadata importMetadata : javaMetadata.getImports()) {
                if (importMetadata instanceof ImportOnDemandMetadata) {
                    ImportOnDemandMetadata importOnDemandMetadata = (ImportOnDemandMetadata)importMetadata;
                    Path importOnDemandChildNode = pathFactory.create(JAVA_COMPILATION_UNIT_NODE + SLASH + JAVA_IMPORT_CHILD_NODE
                                                                      + SLASH + JAVA_IMPORT_DECLARATION_CHILD_NODE + SLASH
                                                                      + JAVA_ON_DEMAND_IMPORT_CHILD_NODE + SLASH
                                                                      + JAVA_ON_DEMAND_IMPORT_TYPE_DECLARATION_CHILD_NODE + "["
                                                                      + importOnDemandIndex + "]");
                    output.setProperty(importOnDemandChildNode,
                                       nameFactory.create(JAVA_ON_DEMAND_IMPORT_NAME),
                                       importOnDemandMetadata.getName());
                    importOnDemandIndex++;
                }
                if (importMetadata instanceof SingleImportMetadata) {
                    SingleImportMetadata singleImportMetadata = (SingleImportMetadata)importMetadata;
                    Path singleImportChildNode = pathFactory.create(JAVA_COMPILATION_UNIT_NODE + SLASH + JAVA_IMPORT_CHILD_NODE
                                                                    + SLASH + JAVA_IMPORT_DECLARATION_CHILD_NODE + SLASH
                                                                    + JAVA_SINGLE_IMPORT_CHILD_NODE + SLASH
                                                                    + JAVA_SINGLE_IMPORT_TYPE_DECLARATION_CHILD_NODE + "["
                                                                    + singleImportIndex + "]");
                    output.setProperty(singleImportChildNode,
                                       nameFactory.create(JAVA_SINGLE_IMPORT_NAME),
                                       singleImportMetadata.getName());
                    singleImportIndex++;
                }
            }

            // sequence type declaration (class declaration) information
            for (TypeMetadata typeMetadata : javaMetadata.getTypeMetadata()) {
                // class declaration
                if (typeMetadata instanceof ClassMetadata) {
                    ClassMetadata classMetadata = (ClassMetadata)typeMetadata;
                    Path classChildNode = pathFactory.create(JAVA_COMPILATION_UNIT_NODE + SLASH + JAVA_UNIT_TYPE_CHILD_NODE
                                                             + SLASH + JAVA_CLASS_DECLARATION_CHILD_NODE + SLASH
                                                             + JAVA_NORMAL_CLASS_CHILD_NODE + SLASH
                                                             + JAVA_NORMAL_CLASS_DECLARATION_CHILD_NODE);
                    output.setProperty(classChildNode, nameFactory.create(JAVA_NORMAL_CLASS_NAME), classMetadata.getName());

                    // process modifiers of the class declaration
                    List<ModifierMetadata> classModifiers = classMetadata.getModifiers();
                    int modifierIndex = 1;
                    for (ModifierMetadata modifierMetadata : classModifiers) {

                        Path classModifierChildNode = pathFactory.create(JAVA_COMPILATION_UNIT_NODE + SLASH
                                                                         + JAVA_UNIT_TYPE_CHILD_NODE + SLASH
                                                                         + JAVA_CLASS_DECLARATION_CHILD_NODE + SLASH
                                                                         + JAVA_NORMAL_CLASS_CHILD_NODE + SLASH
                                                                         + JAVA_NORMAL_CLASS_DECLARATION_CHILD_NODE + SLASH
                                                                         + JAVA_MODIFIER_CHILD_NODE + SLASH
                                                                         + JAVA_MODIFIER_DECLARATION_CHILD_NODE + "["
                                                                         + modifierIndex + "]");

                        output.setProperty(classModifierChildNode,
                                           nameFactory.create(JAVA_MODIFIER_NAME),
                                           modifierMetadata.getName());
                    }

                    // process fields of the class unit.
                    int primitiveIndex = 1;
                    for (FieldMetadata fieldMetadata : classMetadata.getFields()) {
                        if (fieldMetadata instanceof PrimitiveFieldMetadata) {
                            PrimitiveFieldMetadata primitiveFieldMetadata = (PrimitiveFieldMetadata)fieldMetadata;
                            String primitiveFieldRootPath = createPathWithIndex(JAVA_COMPILATION_UNIT_NODE + SLASH
                                                                                + JAVA_UNIT_TYPE_CHILD_NODE + SLASH
                                                                                + JAVA_CLASS_DECLARATION_CHILD_NODE + SLASH
                                                                                + JAVA_NORMAL_CLASS_CHILD_NODE + SLASH
                                                                                + JAVA_NORMAL_CLASS_DECLARATION_CHILD_NODE
                                                                                + SLASH + JAVA_FIELD_CHILD_NODE + SLASH
                                                                                + JAVA_FIELD_TYPE_CHILD_NODE + SLASH
                                                                                + JAVA_TYPE_CHILD_NODE + SLASH
                                                                                + JAVA_PRIMITIVE_TYPE_CHILD_NODE, primitiveIndex);
                            // type
                            Path primitiveTypeChildNode = pathFactory.create(primitiveFieldRootPath);
                            output.setProperty(primitiveTypeChildNode,
                                               nameFactory.create(JAVA_PRIMITIVE_TYPE_NAME),
                                               primitiveFieldMetadata.getType());
                            // modifiers
                            List<ModifierMetadata> modifiers = primitiveFieldMetadata.getModifiers();
                            int primitiveModifierIndex = 1;
                            for (ModifierMetadata modifierMetadata2 : modifiers) {
                                String modifierPath = createPathWithIndex(primitiveFieldRootPath + SLASH
                                                                          + JAVA_MODIFIER_CHILD_NODE + SLASH
                                                                          + JAVA_MODIFIER_DECLARATION_CHILD_NODE,
                                                                          primitiveModifierIndex);
                                Path modifierChildNode = pathFactory.create(modifierPath);
                                output.setProperty(modifierChildNode,
                                                   nameFactory.create(JAVA_MODIFIER_NAME),
                                                   modifierMetadata2.getName());
                                primitiveModifierIndex++;
                            }
                            // variables
                            List<Variable> variables = primitiveFieldMetadata.getVariables();
                            int primitiveVariableIndex = 1;
                            for (Variable variable : variables) {
                                String variablePath = createPathWithIndex(primitiveFieldRootPath.toString() + SLASH
                                                                          + JAVA_VARIABLE,
                                                                          primitiveVariableIndex).toString();
                                Path primitiveChildNode = pathFactory.create(variablePath);
                                output.setProperty(primitiveChildNode, nameFactory.create(JAVA_VARIABLE_NAME), variable.getName());
                                primitiveVariableIndex++;
                            }
                            primitiveIndex++;
                        }
                        if (fieldMetadata instanceof ReferenceFieldMetadata) {
                            // ReferenceFieldMetadata parameterizedFieldMetadata = (ReferenceFieldMetadata)fieldMetadata;
                            // TODO
                        }
                    }

                    // process methods of the unit.
                    List<MethodMetadata> methods = classMetadata.getMethods();
                    int methodIndex = 1;
                    int constructorIndex = 1;
                    for (MethodMetadata methodMetadata : methods) {
                        if (methodMetadata.isContructor()) {
                            // process contructor
                            ConstructorMetadata constructorMetadata = (ConstructorMetadata)methodMetadata;
                            String constructorRootPath = createPathWithIndex(JAVA_COMPILATION_UNIT_NODE + SLASH
                                                                             + JAVA_UNIT_TYPE_CHILD_NODE + SLASH
                                                                             + JAVA_CLASS_DECLARATION_CHILD_NODE + SLASH
                                                                             + JAVA_NORMAL_CLASS_CHILD_NODE + SLASH
                                                                             + JAVA_NORMAL_CLASS_DECLARATION_CHILD_NODE + SLASH
                                                                             + JAVA_CONSTRUCTOR_CHILD_NODE + SLASH
                                                                             + JAVA_CONSTRUCTOR_DECLARATION_CHILD_NODE,
                                                                             constructorIndex);
                            Path constructorChildNode = pathFactory.create(constructorRootPath);
                            output.setProperty(constructorChildNode,
                                               nameFactory.create(JAVA_CONSTRUCTOR_NAME),
                                               constructorMetadata.getName());
                            List<ModifierMetadata> modifiers = constructorMetadata.getModifiers();
                            // modifiers
                            int constructorModifierIndex = 1;
                            for (ModifierMetadata modifierMetadata : modifiers) {
                                String contructorModifierPath = createPathWithIndex(constructorRootPath + SLASH
                                                                                    + JAVA_MODIFIER_CHILD_NODE + SLASH
                                                                                    + JAVA_MODIFIER_DECLARATION_CHILD_NODE,
                                                                                    constructorModifierIndex);
                                Path constructorModifierChildNode = pathFactory.create(contructorModifierPath);
                                output.setProperty(constructorModifierChildNode,
                                                   nameFactory.create(JAVA_MODIFIER_NAME),
                                                   modifierMetadata.getName());
                                constructorModifierIndex++;
                            }

                            // constructor parameters
                            int constructorParameterIndex = 1;
                            for (FieldMetadata fieldMetadata2 : constructorMetadata.getParameters()) {

                                String constructorParameterRootPath = createPathWithIndex(constructorRootPath + SLASH
                                                                                          + JAVA_PARAMETER + SLASH
                                                                                          + JAVA_FORMAL_PARAMETER,
                                                                                          constructorParameterIndex);

                                if (fieldMetadata2 instanceof PrimitiveFieldMetadata) {

                                    PrimitiveFieldMetadata primitive = (PrimitiveFieldMetadata)fieldMetadata2;
                                    String constructPrimitiveFormalParamRootPath = createPath(constructorParameterRootPath
                                                                                              + SLASH + JAVA_TYPE_CHILD_NODE
                                                                                              + SLASH
                                                                                              + JAVA_PRIMITIVE_TYPE_CHILD_NODE);
                                    String constructorPrimitiveParamVariablePath = createPath(constructPrimitiveFormalParamRootPath
                                                                                              + SLASH + JAVA_VARIABLE);
                                    Path constructorParamChildNode = pathFactory.create(constructorPrimitiveParamVariablePath);
                                    // variables
                                    for (Variable variable : primitive.getVariables()) {
                                        output.setProperty(constructorParamChildNode,
                                                           nameFactory.create(JAVA_VARIABLE_NAME),
                                                           variable.getName());
                                    }
                                    // type
                                    Path constructPrimitiveTypeParamChildNode = pathFactory.create(constructPrimitiveFormalParamRootPath);
                                    output.setProperty(constructPrimitiveTypeParamChildNode,
                                                       nameFactory.create(JAVA_PRIMITIVE_TYPE_NAME),
                                                       primitive.getType());
                                }

                                // TODO parameter reference types

                                constructorParameterIndex++;
                            }

                            constructorIndex++;
                        } else {

                            // normal method
                            MethodTypeMemberMetadata methodTypeMemberMetadata = (MethodTypeMemberMetadata)methodMetadata;
                            String methodRootPath = createPathWithIndex(JAVA_COMPILATION_UNIT_NODE + SLASH
                                                                        + JAVA_UNIT_TYPE_CHILD_NODE + SLASH
                                                                        + JAVA_CLASS_DECLARATION_CHILD_NODE + SLASH
                                                                        + JAVA_NORMAL_CLASS_CHILD_NODE + SLASH
                                                                        + JAVA_NORMAL_CLASS_DECLARATION_CHILD_NODE + SLASH
                                                                        + JAVA_METHOD_CHILD_NODE + SLASH
                                                                        + JAVA_METHOD_DECLARATION_CHILD_NODE, methodIndex);
                            Path methodChildNode = pathFactory.create(methodRootPath);
                            output.setProperty(methodChildNode,
                                               nameFactory.create(JAVA_METHOD_NAME),
                                               methodTypeMemberMetadata.getName());

                            // method modifiers
                            int methodModierIndex = 1;
                            for (ModifierMetadata modifierMetadata : methodTypeMemberMetadata.getModifiers()) {
                                String methodModifierPath = createPathWithIndex(methodRootPath + SLASH + JAVA_MODIFIER_CHILD_NODE
                                                                                + SLASH + JAVA_MODIFIER_DECLARATION_CHILD_NODE,
                                                                                methodModierIndex);
                                Path methodModifierChildNode = pathFactory.create(methodModifierPath);
                                output.setProperty(methodModifierChildNode,
                                                   nameFactory.create(JAVA_MODIFIER_NAME),
                                                   modifierMetadata.getName());
                                methodModierIndex++;
                            }

                            int methodParameterIndex = 1;
                            for (FieldMetadata fieldMetadata : methodMetadata.getParameters()) {

                                String methodPrimitiveParamRootPath = createPathWithIndex(methodRootPath + SLASH + JAVA_PARAMETER
                                                                                          + SLASH + JAVA_FORMAL_PARAMETER,
                                                                                          methodParameterIndex);

                                if (fieldMetadata instanceof PrimitiveFieldMetadata) {

                                    PrimitiveFieldMetadata primitive = (PrimitiveFieldMetadata)fieldMetadata;

                                    String methodPrimitiveFormalParamRootPath = createPath(methodPrimitiveParamRootPath + SLASH
                                                                                           + JAVA_TYPE_CHILD_NODE + SLASH
                                                                                           + JAVA_PRIMITIVE_TYPE_CHILD_NODE);

                                    String methodPrimitiveParamVariablePath = createPath(methodPrimitiveFormalParamRootPath
                                                                                         + SLASH + JAVA_VARIABLE);

                                    Path methodParamChildNode = pathFactory.create(methodPrimitiveParamVariablePath);
                                    // variables
                                    for (Variable variable : primitive.getVariables()) {
                                        output.setProperty(methodParamChildNode,
                                                           nameFactory.create(JAVA_VARIABLE_NAME),
                                                           variable.getName());
                                    }
                                    // type
                                    Path methodPrimitiveTypeParamChildNode = pathFactory.create(methodPrimitiveFormalParamRootPath);
                                    output.setProperty(methodPrimitiveTypeParamChildNode,
                                                       nameFactory.create(JAVA_PRIMITIVE_TYPE_NAME),
                                                       primitive.getType());

                                }

                                // TODO parameter reference types

                                methodParameterIndex++;
                            }

                            // method return type
                            FieldMetadata methodReturnType = methodTypeMemberMetadata.getReturnType();

                            if (methodReturnType instanceof PrimitiveFieldMetadata) {
                                PrimitiveFieldMetadata methodReturnPrimitiveType = (PrimitiveFieldMetadata)methodReturnType;
                                String methodReturnPrimitiveTypePath = createPath(methodRootPath.toString() + SLASH
                                                                                  + JAVA_RETURN_TYPE + SLASH
                                                                                  + JAVA_PRIMITIVE_TYPE_CHILD_NODE);
                                Path methodReturnPrimitiveTypeChildNode = pathFactory.create(methodReturnPrimitiveTypePath);
                                output.setProperty(methodReturnPrimitiveTypeChildNode,
                                                   nameFactory.create(JAVA_PRIMITIVE_TYPE_NAME),
                                                   methodReturnPrimitiveType.getType());

                            }

                            // TODO method return reference type

                            methodIndex++;
                        }
                    }
                }
                // interface declaration

                // enumeration declaration
            }
        }

        progressMonitor.done();
    }

    /**
     * Create a path for the tree with index.
     * 
     * @param path the path.
     * @param index the index begin with 1.
     * @return the string
     * @throws IllegalArgumentException if the path is null, blank or empty, or if the index is not a positive value
     */
    private String createPathWithIndex( String path,
                                        int index ) {
        ArgCheck.isNotEmpty(path, "path");
        ArgCheck.isPositive(index, "index");
        return path + "[" + index + "]";
    }

    /**
     * Create a path for the tree without index.
     * 
     * @param path - the path.
     * @return the string
     * @throws IllegalArgumentException if the path is null, blank or empty
     */
    private String createPath( String path ) {
        ArgCheck.isNotEmpty(path, "path");
        return path;
    }
}
