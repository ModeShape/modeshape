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
import org.jboss.dna.sequencer.java.metadata.AnnotationMetadata;
import org.jboss.dna.sequencer.java.metadata.ArrayTypeFieldMetadata;
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
import org.jboss.dna.sequencer.java.metadata.ParameterizedTypeFieldMetadata;
import org.jboss.dna.sequencer.java.metadata.PrimitiveFieldMetadata;
import org.jboss.dna.sequencer.java.metadata.QualifiedTypeFieldMetadata;
import org.jboss.dna.sequencer.java.metadata.SimpleTypeFieldMetadata;
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
 * A Java sequencer that processes a compilation unit, extracts the meta data for the compilation unit, and then writes these
 * informations to the repository.
 * <p>
 * The structural representation of the informations from the compilation unit looks like this:
 * <ul>
 * <li><strong>java:compilationUnit</strong> node of type <code>java:compilationUnit</code>
 * <ul>
 * <li> <strong>java:package</strong> - optional child node that represents the package child node of the compilation unit.
 * <ul>
 * <li> <strong>java:packageDeclaration</strong> - the package declaration.
 * <ul>
 * <li><strong>java:packageName</strong></li> - the package name.
 * </ul>
 * </li>
 * </ul>
 * </li>
 * <li> <strong>java:import</strong> - optional child node that represents the import declaration of the compilation unit
 * <ul>
 * <li> <strong>java:importDeclaration</strong> - the import declaration
 * <ul>
 * <li><strong>java:singleImport</strong>
 * <ul>
 * <li> <strong>java:singleTypeImportDeclaration</strong>
 * <ul>
 * <li> <strong>java:singleTypeImportkeyword</strong> - the keyword "import"
 * <li><strong>java:singleImportName</strong></li> - the name of a single import. </li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * <li><strong>java:importOnDemand</strong>
 * <li> <strong>java:typeImportOnDemandDeclaration</strong>
 * <ul>
 * <li> <strong>java:onDemandImportKeyword</strong> - the keyword "import"
 * <li><strong>java:onDemandImportName</strong></li> - the name of the on demand import. </li>
 * </ul>
 * </li>
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
 * <li> <strong>java:normalClass</strong> - the normal class.
 * <ul>
 * <li> <strong>java:normalClassDeclaration</strong> - the normal class declaration
 * <ul>
 * <li> <strong>java:modifier</strong> - modifier child node.
 * <ul>
 * <li><strong>java:modifierDeclaration</strong> - the modifier declaration.
 * <ul>
 * <li><strong>java:modifierName</strong> - modifier name.</li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * <li> <strong>java:normalClassName</strong> - class name.</li>
 * <li> <strong>java:field</strong> - field child node.
 * <ul>
 * <li><strong>java:fieldType</strong> - field type child node.
 * <ul>
 * <li><strong>java:type</strong> - type child node.
 * <ul>
 * <li>[java:primitiveType, java:simpleType, java:parameterizedType] - can be primitive type or simple type and or parameterized
 * type<.</li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * <li> <strong>java:constructor</strong> - the constructor child node
 * <ul>
 * <li><strong>java:constructorDeclaration</strong> - the constructor declaration.
 * <ul>
 * <li><strong>java:constructorName</strong> - constructor name. </li>
 * <li><strong>java:modifier </strong> - the modifier child node.</li> +
 * <li><strong>java:parameter </strong> - the parameter child node</li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * <li> <strong>java:method</strong> - method child node.
 * <ul>
 * <li></strong>java:methodDeclaration</strong> - method declaration.
 * <ul>
 * <li><strong>java:methodName </strong> - method name. </li>
 * <li><strong>java:modifier </strong> - the modifier child node.</li> +
 * <li><strong>java:resultType </strong> - the result type child node </li> +
 * <li><strong>java:parameter </strong> - the parameter child node</li>
 * </ul>
 * </li>
 * </ul>
 * </li>
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
public class JavaMetadataSequencer implements JavaSourceCndDefinition, StreamSequencer {

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
                    int simpleIndex = 1;
                    int parameterizedIndex = 1;
                    for (FieldMetadata fieldMetadata : classMetadata.getFields()) {
                        if (fieldMetadata instanceof PrimitiveFieldMetadata) {
                            // primitive type
                            PrimitiveFieldMetadata primitiveFieldMetadata = (PrimitiveFieldMetadata)fieldMetadata;
                            String primitiveFieldRootPath = JavaMetadataUtil.createPathWithIndex(JAVA_COMPILATION_UNIT_NODE
                                                                                                 + SLASH
                                                                                                 + JAVA_UNIT_TYPE_CHILD_NODE
                                                                                                 + SLASH
                                                                                                 + JAVA_CLASS_DECLARATION_CHILD_NODE
                                                                                                 + SLASH
                                                                                                 + JAVA_NORMAL_CLASS_CHILD_NODE
                                                                                                 + SLASH
                                                                                                 + JAVA_NORMAL_CLASS_DECLARATION_CHILD_NODE
                                                                                                 + SLASH + JAVA_FIELD_CHILD_NODE
                                                                                                 + SLASH
                                                                                                 + JAVA_FIELD_TYPE_CHILD_NODE
                                                                                                 + SLASH + JAVA_TYPE_CHILD_NODE
                                                                                                 + SLASH
                                                                                                 + JAVA_PRIMITIVE_TYPE_CHILD_NODE,
                                                                                                 primitiveIndex);
                            // type
                            Path primitiveTypeChildNode = pathFactory.create(primitiveFieldRootPath);
                            output.setProperty(primitiveTypeChildNode,
                                               nameFactory.create(JAVA_PRIMITIVE_TYPE_NAME),
                                               primitiveFieldMetadata.getType());
                            // modifiers
                            List<ModifierMetadata> modifiers = primitiveFieldMetadata.getModifiers();
                            int primitiveModifierIndex = 1;
                            for (ModifierMetadata modifierMetadata : modifiers) {
                                String modifierPath = JavaMetadataUtil.createPathWithIndex(primitiveFieldRootPath + SLASH
                                                                                           + JAVA_MODIFIER_CHILD_NODE + SLASH
                                                                                           + JAVA_MODIFIER_DECLARATION_CHILD_NODE,
                                                                                           primitiveModifierIndex);
                                Path modifierChildNode = pathFactory.create(modifierPath);
                                output.setProperty(modifierChildNode,
                                                   nameFactory.create(JAVA_MODIFIER_NAME),
                                                   modifierMetadata.getName());
                                primitiveModifierIndex++;
                            }
                            // variables
                            List<Variable> variables = primitiveFieldMetadata.getVariables();
                            int primitiveVariableIndex = 1;
                            for (Variable variable : variables) {
                                String variablePath = JavaMetadataUtil.createPathWithIndex(primitiveFieldRootPath + SLASH
                                                                                           + JAVA_PRIMITIVE_TYPE_VARIABLE + SLASH
                                                                                           + JAVA_VARIABLE,
                                                                                           primitiveVariableIndex);
                                Path primitiveChildNode = pathFactory.create(variablePath);
                                sequenceTheVariable(output, nameFactory, variable, primitiveChildNode);
                                primitiveVariableIndex++;
                            }
                            primitiveIndex++;
                        }

                        // Array type
                        if (fieldMetadata instanceof ArrayTypeFieldMetadata) {
                            @SuppressWarnings( "unused" )
                            ArrayTypeFieldMetadata arrayTypeFieldMetadata = (ArrayTypeFieldMetadata)fieldMetadata;
                        }

                        // Simple type
                        if (fieldMetadata instanceof SimpleTypeFieldMetadata) {
                            SimpleTypeFieldMetadata simpleTypeFieldMetadata = (SimpleTypeFieldMetadata)fieldMetadata;
                            String simpleTypeFieldRootPath = JavaMetadataUtil.createPathWithIndex(JAVA_COMPILATION_UNIT_NODE
                                                                                                  + SLASH
                                                                                                  + JAVA_UNIT_TYPE_CHILD_NODE
                                                                                                  + SLASH
                                                                                                  + JAVA_CLASS_DECLARATION_CHILD_NODE
                                                                                                  + SLASH
                                                                                                  + JAVA_NORMAL_CLASS_CHILD_NODE
                                                                                                  + SLASH
                                                                                                  + JAVA_NORMAL_CLASS_DECLARATION_CHILD_NODE
                                                                                                  + SLASH + JAVA_FIELD_CHILD_NODE
                                                                                                  + SLASH
                                                                                                  + JAVA_FIELD_TYPE_CHILD_NODE
                                                                                                  + SLASH + JAVA_TYPE_CHILD_NODE
                                                                                                  + SLASH
                                                                                                  + JAVA_SIMPLE_TYPE_CHILD_NODE,
                                                                                                  simpleIndex);
                            Path simpleTypeFieldChildNode = pathFactory.create(simpleTypeFieldRootPath);
                            output.setProperty(simpleTypeFieldChildNode,
                                               nameFactory.create(JAVA_SIMPLE_TYPE_NAME),
                                               simpleTypeFieldMetadata.getType());

                            // Simple type modifies
                            List<ModifierMetadata> simpleModifiers = simpleTypeFieldMetadata.getModifiers();
                            int simpleTypeModifierIndex = 1;
                            for (ModifierMetadata modifierMetadata : simpleModifiers) {
                                String simpleTypeModifierPath = JavaMetadataUtil.createPathWithIndex(simpleTypeFieldRootPath
                                                                                                     + SLASH
                                                                                                     + JAVA_SIMPLE_TYPE_MODIFIER_CHILD_NODE
                                                                                                     + SLASH
                                                                                                     + JAVA_MODIFIER_DECLARATION_CHILD_NODE,
                                                                                                     simpleTypeModifierIndex);
                                Path simpleTypeModifierChildNode = pathFactory.create(simpleTypeModifierPath);
                                output.setProperty(simpleTypeModifierChildNode,
                                                   nameFactory.create(JAVA_MODIFIER_NAME),
                                                   modifierMetadata.getName());
                                simpleTypeModifierIndex++;
                            }

                            // Simple type variables
                            List<Variable> variables = simpleTypeFieldMetadata.getVariables();
                            int simpleTypeVariableIndex = 1;
                            for (Variable variable : variables) {
                                String variablePath = JavaMetadataUtil.createPathWithIndex(simpleTypeFieldRootPath + SLASH
                                                                                           + JAVA_SIMPLE_TYPE_VARIABLE + SLASH
                                                                                           + JAVA_VARIABLE,
                                                                                           simpleTypeVariableIndex);
                                Path primitiveChildNode = pathFactory.create(variablePath);
                                sequenceTheVariable(output, nameFactory, variable, primitiveChildNode);
                                simpleTypeVariableIndex++;
                            }

                            simpleIndex++;
                        }

                        // Qualified type
                        if (fieldMetadata instanceof QualifiedTypeFieldMetadata) {
                            @SuppressWarnings( "unused" )
                            QualifiedTypeFieldMetadata qualifiedTypeFieldMetadata = (QualifiedTypeFieldMetadata)fieldMetadata;
                        }

                        // Parameterized type
                        if (fieldMetadata instanceof ParameterizedTypeFieldMetadata) {
                            ParameterizedTypeFieldMetadata parameterizedTypeFieldMetadata = (ParameterizedTypeFieldMetadata)fieldMetadata;
                            String parameterizedTypeFieldRootPath = ParameterizedTypeFieldMetadataSequencer.getParameterizedTypeFieldRootPath(parameterizedIndex);
                            ParameterizedTypeFieldMetadataSequencer.sequenceTheParameterizedTypeName(parameterizedTypeFieldMetadata,
                                                                                                     parameterizedTypeFieldRootPath,
                                                                                                     pathFactory,
                                                                                                     nameFactory,
                                                                                                     output);
                            // Parameterized type modifiers
                            List<ModifierMetadata> parameterizedTypeModifiers = parameterizedTypeFieldMetadata.getModifiers();
                            int parameterizedTypeModifierIndex = 1;
                            for (ModifierMetadata modifierMetadata : parameterizedTypeModifiers) {
                                String parameterizedTypeModifierPath = ParameterizedTypeFieldMetadataSequencer.getParameterizedTypeFieldRModifierPath(parameterizedTypeFieldRootPath,
                                                                                                                                                      parameterizedTypeModifierIndex);
                                ParameterizedTypeFieldMetadataSequencer.sequenceTheParameterizedTypeModifier(modifierMetadata,
                                                                                                             parameterizedTypeModifierPath,
                                                                                                             pathFactory,
                                                                                                             nameFactory,
                                                                                                             output);
                                parameterizedTypeModifierIndex++;
                            }
                            // Parameterized type variables
                            List<Variable> parameterizedTypeVariables = parameterizedTypeFieldMetadata.getVariables();
                            int parameterizedTypeVariableIndex = 1;
                            for (Variable variable : parameterizedTypeVariables) {

                                Path parameterizedTypeVariableChildNode = ParameterizedTypeFieldMetadataSequencer.getParameterizedTypeFieldVariablePath(pathFactory,
                                                                                                                                                        parameterizedTypeFieldRootPath,
                                                                                                                                                        parameterizedTypeVariableIndex);
                                sequenceTheVariable(output, nameFactory, variable, parameterizedTypeVariableChildNode);
                                parameterizedTypeVariableIndex++;
                            }

                            parameterizedIndex++;
                        }

                    }

                    // process methods of the unit.
                    List<MethodMetadata> methods = classMetadata.getMethods();
                    int methodIndex = 1;
                    int constructorIndex = 1;
                    for (MethodMetadata methodMetadata : methods) {
                        if (methodMetadata.isContructor()) {
                            // process constructor
                            ConstructorMetadata constructorMetadata = (ConstructorMetadata)methodMetadata;
                            String constructorRootPath = JavaMetadataUtil.createPathWithIndex(JAVA_COMPILATION_UNIT_NODE
                                                                                              + SLASH
                                                                                              + JAVA_UNIT_TYPE_CHILD_NODE
                                                                                              + SLASH
                                                                                              + JAVA_CLASS_DECLARATION_CHILD_NODE
                                                                                              + SLASH
                                                                                              + JAVA_NORMAL_CLASS_CHILD_NODE
                                                                                              + SLASH
                                                                                              + JAVA_NORMAL_CLASS_DECLARATION_CHILD_NODE
                                                                                              + SLASH
                                                                                              + JAVA_CONSTRUCTOR_CHILD_NODE
                                                                                              + SLASH
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
                                String contructorModifierPath = JavaMetadataUtil.createPathWithIndex(constructorRootPath
                                                                                                     + SLASH
                                                                                                     + JAVA_MODIFIER_CHILD_NODE
                                                                                                     + SLASH
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
                            for (FieldMetadata fieldMetadata : constructorMetadata.getParameters()) {

                                String constructorParameterRootPath = JavaMetadataUtil.createPathWithIndex(constructorRootPath
                                                                                                           + SLASH
                                                                                                           + JAVA_PARAMETER
                                                                                                           + SLASH
                                                                                                           + JAVA_FORMAL_PARAMETER,
                                                                                                           constructorParameterIndex);
                                // primitive type
                                if (fieldMetadata instanceof PrimitiveFieldMetadata) {

                                    PrimitiveFieldMetadata primitiveMetadata = (PrimitiveFieldMetadata)fieldMetadata;
                                    String constructPrimitiveFormalParamRootPath = MethodMetadataSequencer.createMethodParamRootPath(constructorParameterRootPath);
                                    // type
                                    Path constructorPrimitiveTypeParamChildNode = pathFactory.create(constructPrimitiveFormalParamRootPath);
                                    output.setProperty(constructorPrimitiveTypeParamChildNode,
                                                       nameFactory.create(JAVA_PRIMITIVE_TYPE_NAME),
                                                       primitiveMetadata.getType());

                                    Path constructorPrimitiveParamChildNode = MethodMetadataSequencer.createMethodParamPath(pathFactory,
                                                                                                                            constructPrimitiveFormalParamRootPath);
                                    // variables
                                    for (Variable variable : primitiveMetadata.getVariables()) {
                                        sequenceTheVariable(output, nameFactory, variable, constructorPrimitiveParamChildNode);
                                    }
                                }
                                // Simple type
                                if (fieldMetadata instanceof SimpleTypeFieldMetadata) {
                                    processMethodFormalParamOfSimpleType(output,
                                                                         nameFactory,
                                                                         pathFactory,
                                                                         fieldMetadata,
                                                                         constructorParameterRootPath);

                                }
                                // parameterized type
                                if (fieldMetadata instanceof ParameterizedTypeFieldMetadata) {
                                    @SuppressWarnings( "unused" )
                                    ParameterizedTypeFieldMetadata parameterizedTypeFieldMetadata = (ParameterizedTypeFieldMetadata)fieldMetadata;

                                }
                                // TODO support for more types

                                constructorParameterIndex++;
                            }

                            constructorIndex++;
                        } else {

                            // normal method
                            MethodTypeMemberMetadata methodTypeMemberMetadata = (MethodTypeMemberMetadata)methodMetadata;
                            String methodRootPath = JavaMetadataUtil.createPathWithIndex(JAVA_COMPILATION_UNIT_NODE
                                                                                         + SLASH
                                                                                         + JAVA_UNIT_TYPE_CHILD_NODE
                                                                                         + SLASH
                                                                                         + JAVA_CLASS_DECLARATION_CHILD_NODE
                                                                                         + SLASH
                                                                                         + JAVA_NORMAL_CLASS_CHILD_NODE
                                                                                         + SLASH
                                                                                         + JAVA_NORMAL_CLASS_DECLARATION_CHILD_NODE
                                                                                         + SLASH + JAVA_METHOD_CHILD_NODE + SLASH
                                                                                         + JAVA_METHOD_DECLARATION_CHILD_NODE,
                                                                                         methodIndex);
                            Path methodChildNode = pathFactory.create(methodRootPath);
                            output.setProperty(methodChildNode,
                                               nameFactory.create(JAVA_METHOD_NAME),
                                               methodTypeMemberMetadata.getName());

                            // method modifiers
                            int methodModierIndex = 1;
                            for (ModifierMetadata modifierMetadata : methodTypeMemberMetadata.getModifiers()) {
                                String methodModifierPath = JavaMetadataUtil.createPathWithIndex(methodRootPath
                                                                                                 + SLASH
                                                                                                 + JAVA_MODIFIER_CHILD_NODE
                                                                                                 + SLASH
                                                                                                 + JAVA_MODIFIER_DECLARATION_CHILD_NODE,
                                                                                                 methodModierIndex);
                                Path methodModifierChildNode = pathFactory.create(methodModifierPath);
                                output.setProperty(methodModifierChildNode,
                                                   nameFactory.create(JAVA_MODIFIER_NAME),
                                                   modifierMetadata.getName());
                                methodModierIndex++;
                            }

                            int methodParameterIndex = 1;
                            for (FieldMetadata fieldMetadata : methodMetadata.getParameters()) {

                                String methodParamRootPath = JavaMetadataUtil.createPathWithIndex(methodRootPath + SLASH
                                                                                                  + JAVA_PARAMETER + SLASH
                                                                                                  + JAVA_FORMAL_PARAMETER,
                                                                                                  methodParameterIndex);

                                if (fieldMetadata instanceof PrimitiveFieldMetadata) {

                                    PrimitiveFieldMetadata primitive = (PrimitiveFieldMetadata)fieldMetadata;

                                    String methodPrimitiveFormalParamRootPath = JavaMetadataUtil.createPath(methodParamRootPath
                                                                                                            + SLASH
                                                                                                            + JAVA_TYPE_CHILD_NODE
                                                                                                            + SLASH
                                                                                                            + JAVA_PRIMITIVE_TYPE_CHILD_NODE);

                                    Path methodParamChildNode = MethodMetadataSequencer.createMethodParamPath(pathFactory,
                                                                                                              methodPrimitiveFormalParamRootPath);
                                    // variables
                                    for (Variable variable : primitive.getVariables()) {
                                        sequenceTheVariable(output, nameFactory, variable, methodParamChildNode);
                                    }
                                    // type
                                    Path methodPrimitiveTypeParamChildNode = pathFactory.create(methodPrimitiveFormalParamRootPath);
                                    output.setProperty(methodPrimitiveTypeParamChildNode,
                                                       nameFactory.create(JAVA_PRIMITIVE_TYPE_NAME),
                                                       primitive.getType());

                                }

                                if (fieldMetadata instanceof SimpleTypeFieldMetadata) {
                                    processMethodFormalParamOfSimpleType(output,
                                                                         nameFactory,
                                                                         pathFactory,
                                                                         fieldMetadata,
                                                                         methodParamRootPath);
                                }

                                // TODO parameter reference types

                                methodParameterIndex++;
                            }

                            // method return type
                            FieldMetadata methodReturnType = methodTypeMemberMetadata.getReturnType();

                            if (methodReturnType instanceof PrimitiveFieldMetadata) {
                                PrimitiveFieldMetadata methodReturnPrimitiveType = (PrimitiveFieldMetadata)methodReturnType;
                                String methodReturnPrimitiveTypePath = JavaMetadataUtil.createPath(methodRootPath
                                                                                                   + SLASH
                                                                                                   + JAVA_RETURN_TYPE
                                                                                                   + SLASH
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
     * Process formal parameter of type simple type .
     * 
     * @param output - the {@link SequencerOutput}.
     * @param nameFactory - The {@link NameFactory}.
     * @param pathFactory - the {@link PathFactory}.
     * @param fieldMetadata - the meta data.
     * @param methodParamRootPath - base path of the parameter.
     */
    private void processMethodFormalParamOfSimpleType( SequencerOutput output,
                                                       NameFactory nameFactory,
                                                       PathFactory pathFactory,
                                                       FieldMetadata fieldMetadata,
                                                       String methodParamRootPath ) {
        SimpleTypeFieldMetadata simpleTypeFieldMetadata = (SimpleTypeFieldMetadata)fieldMetadata;
        String methodSimpleTypeFormalParamRootPath = SimpleTypeMetadataSequencer.createRootPath(methodParamRootPath);
        SimpleTypeMetadataSequencer.sequenceConstructorSimpleTypeName(simpleTypeFieldMetadata,
                                                                      methodSimpleTypeFormalParamRootPath,
                                                                      output,
                                                                      nameFactory,
                                                                      pathFactory);
        Path constructorSimpleTypeParamChildNode = SimpleTypeMetadataSequencer.createSimpleTypeParamPath(pathFactory,
                                                                                                         methodSimpleTypeFormalParamRootPath);
        for (Variable variable : simpleTypeFieldMetadata.getVariables()) {
            sequenceTheVariable(output, nameFactory, variable, constructorSimpleTypeParamChildNode);
        }
    }

    /**
     * Sequence a variable.
     * 
     * @param output - the {@link SequencerOutput}.
     * @param nameFactory - the {@link NameFactory}.
     * @param variable - the variable to be added in the tree.
     * @param path - the path
     */
    private void sequenceTheVariable( SequencerOutput output,
                                      NameFactory nameFactory,
                                      Variable variable,
                                      Path path ) {
        output.setProperty(path, nameFactory.create(JAVA_VARIABLE_NAME), variable.getName());
    }

}
