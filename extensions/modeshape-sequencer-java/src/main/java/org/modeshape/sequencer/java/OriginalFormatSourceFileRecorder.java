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
package org.modeshape.sequencer.java;

import java.util.List;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.modeshape.sequencer.java.metadata.AnnotationMetadata;
import org.modeshape.sequencer.java.metadata.ArrayTypeFieldMetadata;
import org.modeshape.sequencer.java.metadata.ClassMetadata;
import org.modeshape.sequencer.java.metadata.ConstructorMetadata;
import org.modeshape.sequencer.java.metadata.FieldMetadata;
import org.modeshape.sequencer.java.metadata.ImportMetadata;
import org.modeshape.sequencer.java.metadata.ImportOnDemandMetadata;
import org.modeshape.sequencer.java.metadata.JavaMetadata;
import org.modeshape.sequencer.java.metadata.MarkerAnnotationMetadata;
import org.modeshape.sequencer.java.metadata.MethodMetadata;
import org.modeshape.sequencer.java.metadata.MethodTypeMemberMetadata;
import org.modeshape.sequencer.java.metadata.ModifierMetadata;
import org.modeshape.sequencer.java.metadata.NormalAnnotationMetadata;
import org.modeshape.sequencer.java.metadata.PackageMetadata;
import org.modeshape.sequencer.java.metadata.ParameterizedTypeFieldMetadata;
import org.modeshape.sequencer.java.metadata.PrimitiveFieldMetadata;
import org.modeshape.sequencer.java.metadata.QualifiedTypeFieldMetadata;
import org.modeshape.sequencer.java.metadata.SimpleTypeFieldMetadata;
import org.modeshape.sequencer.java.metadata.SingleImportMetadata;
import org.modeshape.sequencer.java.metadata.SingleMemberAnnotationMetadata;
import org.modeshape.sequencer.java.metadata.TypeMetadata;
import org.modeshape.sequencer.java.metadata.Variable;

/**
 * A source file recorder that writes the Java metadata from the source file to the repository.
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
 * </i>
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
 */
public class OriginalFormatSourceFileRecorder implements SourceFileRecorder {

    public void record( StreamSequencerContext context,
                        SequencerOutput output,
                        JavaMetadata javaMetadata ) {
        NameFactory nameFactory = context.getValueFactories().getNameFactory();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();

        if (javaMetadata != null) {
            Path javaCompilationUnitNode = pathFactory.create(JavaMetadataLexicon.COMPILATION_UNIT_NODE);
            output.setProperty(javaCompilationUnitNode, JcrLexicon.PRIMARY_TYPE, JavaMetadataLexicon.COMPILATION_UNIT_NODE);

            // sequence package declaration of a unit.
            PackageMetadata packageMetadata = javaMetadata.getPackageMetadata();
            if (packageMetadata != null) {
                String packageName = packageMetadata.getName();
                if (packageName != null && packageName.length() != 0) {

                    Path javaPackageDeclarationChildNode = pathFactory.createRelativePath(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                                          JavaMetadataLexicon.PACKAGE_CHILD_NODE,
                                                                                          JavaMetadataLexicon.PACKAGE_DECLARATION_CHILD_NODE);
                    output.setProperty(javaPackageDeclarationChildNode,
                                       JavaMetadataLexicon.PACKAGE_NAME,
                                       javaMetadata.getPackageMetadata().getName());
                }

                int markerAnnotationIndex = 1;
                int singleAnnatationIndex = 1;
                int normalAnnotationIndex = 1;
                for (AnnotationMetadata annotationMetadata : packageMetadata.getAnnotationMetada()) {
                    if (annotationMetadata instanceof MarkerAnnotationMetadata) {
                        MarkerAnnotationMetadata markerAnnotationMetadata = (MarkerAnnotationMetadata)annotationMetadata;
                        Path basePath = pathFactory.createRelativePath(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                       JavaMetadataLexicon.PACKAGE_CHILD_NODE,
                                                                       JavaMetadataLexicon.PACKAGE_DECLARATION_CHILD_NODE,
                                                                       JavaMetadataLexicon.ANNOTATION_CHILD_NODE,
                                                                       JavaMetadataLexicon.ANNOTATION_DECLARATION_CHILD_NODE,
                                                                       JavaMetadataLexicon.ANNOTATION_TYPE_CHILD_NODE);

                        Path markerAnnotationChildNode = pathFactory.create(basePath,
                                                                            pathFactory.createSegment(JavaMetadataLexicon.MARKER_ANNOTATION_CHILD_NODE,
                                                                                                      markerAnnotationIndex));
                        output.setProperty(markerAnnotationChildNode,
                                           JavaMetadataLexicon.MARKER_ANNOTATION_NAME,
                                           markerAnnotationMetadata.getName());
                        markerAnnotationIndex++;
                    }
                    if (annotationMetadata instanceof SingleMemberAnnotationMetadata) {
                        SingleMemberAnnotationMetadata singleMemberAnnotationMetadata = (SingleMemberAnnotationMetadata)annotationMetadata;

                        Path basePath = pathFactory.createRelativePath(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                       JavaMetadataLexicon.PACKAGE_CHILD_NODE,
                                                                       JavaMetadataLexicon.PACKAGE_DECLARATION_CHILD_NODE,
                                                                       JavaMetadataLexicon.ANNOTATION_CHILD_NODE,
                                                                       JavaMetadataLexicon.ANNOTATION_DECLARATION_CHILD_NODE,
                                                                       JavaMetadataLexicon.ANNOTATION_TYPE_CHILD_NODE);

                        Path singleMemberAnnotationChildNode = pathFactory.create(basePath,
                                                                                  pathFactory.createSegment(JavaMetadataLexicon.SINGLE_ELEMENT_ANNOTATION_CHILD_NODE,
                                                                                                            singleAnnatationIndex));
                        output.setProperty(singleMemberAnnotationChildNode,
                                           JavaMetadataLexicon.SINGLE_ANNOTATION_NAME,
                                           singleMemberAnnotationMetadata.getName());
                        singleAnnatationIndex++;
                    }
                    if (annotationMetadata instanceof NormalAnnotationMetadata) {
                        NormalAnnotationMetadata normalAnnotationMetadata = (NormalAnnotationMetadata)annotationMetadata;
                        Path basePath = pathFactory.createRelativePath(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                       JavaMetadataLexicon.PACKAGE_CHILD_NODE,
                                                                       JavaMetadataLexicon.PACKAGE_DECLARATION_CHILD_NODE,
                                                                       JavaMetadataLexicon.ANNOTATION_CHILD_NODE,
                                                                       JavaMetadataLexicon.ANNOTATION_DECLARATION_CHILD_NODE,
                                                                       JavaMetadataLexicon.ANNOTATION_TYPE_CHILD_NODE);

                        Path normalAnnotationChildNode = pathFactory.create(basePath,
                                                                            pathFactory.createSegment(JavaMetadataLexicon.NORMAL_ANNOTATION_CHILD_NODE,
                                                                                                      normalAnnotationIndex));

                        output.setProperty(normalAnnotationChildNode,
                                           JavaMetadataLexicon.NORMALANNOTATION_NAME,
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

                    Path basePath = pathFactory.createRelativePath(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                   JavaMetadataLexicon.IMPORT_CHILD_NODE,
                                                                   JavaMetadataLexicon.IMPORT_DECLARATION_CHILD_NODE,
                                                                   JavaMetadataLexicon.ON_DEMAND_IMPORT_CHILD_NODE);

                    Path importOnDemandChildNode = pathFactory.create(basePath,
                                                                      pathFactory.createSegment(JavaMetadataLexicon.ON_DEMAND_IMPORT_TYPE_DECLARATION_CHILD_NODE,
                                                                                                importOnDemandIndex));
                    output.setProperty(importOnDemandChildNode,
                                       JavaMetadataLexicon.ON_DEMAND_IMPORT_NAME,
                                       importOnDemandMetadata.getName());
                    importOnDemandIndex++;
                }
                if (importMetadata instanceof SingleImportMetadata) {
                    SingleImportMetadata singleImportMetadata = (SingleImportMetadata)importMetadata;

                    Path basePath = pathFactory.createRelativePath(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                   JavaMetadataLexicon.IMPORT_CHILD_NODE,
                                                                   JavaMetadataLexicon.IMPORT_DECLARATION_CHILD_NODE,
                                                                   JavaMetadataLexicon.SINGLE_IMPORT_CHILD_NODE);

                    Path singleImportChildNode = pathFactory.create(basePath,
                                                                    pathFactory.createSegment(JavaMetadataLexicon.SINGLE_IMPORT_TYPE_DECLARATION_CHILD_NODE,
                                                                                              importOnDemandIndex));
                    output.setProperty(singleImportChildNode,
                                       JavaMetadataLexicon.SINGLE_IMPORT_NAME,
                                       singleImportMetadata.getName());
                    singleImportIndex++;
                }
            }

            // sequence type declaration (class declaration) information
            for (TypeMetadata typeMetadata : javaMetadata.getTypeMetadata()) {
                // class declaration
                if (typeMetadata instanceof ClassMetadata) {

                    Path normalClassRootPath = pathFactory.createRelativePath(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                              JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                                              JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                                              JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                                              JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE);

                    ClassMetadata classMetadata = (ClassMetadata)typeMetadata;
                    output.setProperty(normalClassRootPath, JavaMetadataLexicon.NORMAL_CLASS_NAME, classMetadata.getName());

                    // process modifiers of the class declaration
                    List<ModifierMetadata> classModifiers = classMetadata.getModifiers();
                    int modifierIndex = 1;
                    for (ModifierMetadata modifierMetadata : classModifiers) {

                        Path basePath = pathFactory.create(normalClassRootPath, JavaMetadataLexicon.MODIFIER_CHILD_NODE);

                        Path classModifierChildNode = pathFactory.create(basePath,
                                                                         pathFactory.createSegment(JavaMetadataLexicon.MODIFIER_DECLARATION_CHILD_NODE,
                                                                                                   modifierIndex));

                        output.setProperty(classModifierChildNode, JavaMetadataLexicon.MODIFIER_NAME, modifierMetadata.getName());
                    }

                    // process fields of the class unit.
                    int primitiveIndex = 1;
                    int simpleIndex = 1;
                    int parameterizedIndex = 1;
                    int arrayIndex = 1;
                    for (FieldMetadata fieldMetadata : classMetadata.getFields()) {
                        Path fieldMemberDataRootPath = pathFactory.create(normalClassRootPath,
                                                                          JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                                          JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                                          JavaMetadataLexicon.TYPE_CHILD_NODE);
                        if (fieldMetadata instanceof PrimitiveFieldMetadata) {
                            // primitive type
                            PrimitiveFieldMetadata primitiveFieldMetadata = (PrimitiveFieldMetadata)fieldMetadata;
                            Path primitiveFieldRootPath = pathFactory.create(fieldMemberDataRootPath,
                                                                             pathFactory.createSegment(JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE,
                                                                                                       primitiveIndex));
                            // type
                            output.setProperty(primitiveFieldRootPath,
                                               JavaMetadataLexicon.PRIMITIVE_TYPE_NAME,
                                               primitiveFieldMetadata.getType());
                            // modifiers
                            List<ModifierMetadata> modifiers = primitiveFieldMetadata.getModifiers();
                            int primitiveModifierIndex = 1;
                            for (ModifierMetadata modifierMetadata : modifiers) {
                                Path modifierPath = pathFactory.create(pathFactory.create(primitiveFieldRootPath,
                                                                                          JavaMetadataLexicon.MODIFIER_CHILD_NODE),
                                                                       pathFactory.createSegment(JavaMetadataLexicon.MODIFIER_DECLARATION_CHILD_NODE,
                                                                                                 primitiveModifierIndex));
                                output.setProperty(modifierPath, JavaMetadataLexicon.MODIFIER_NAME, modifierMetadata.getName());
                                primitiveModifierIndex++;
                            }
                            // variables
                            List<Variable> variables = primitiveFieldMetadata.getVariables();
                            int primitiveVariableIndex = 1;
                            for (Variable variable : variables) {
                                Path variablePath = pathFactory.create(pathFactory.create(primitiveFieldRootPath,
                                                                                          JavaMetadataLexicon.PRIMITIVE_TYPE_VARIABLE),
                                                                       pathFactory.createSegment(JavaMetadataLexicon.VARIABLE,
                                                                                                 primitiveVariableIndex));
                                VariableSequencer.sequenceTheVariable(output, nameFactory, variable, variablePath);
                                primitiveVariableIndex++;
                            }
                            primitiveIndex++;
                        }

                        // Array type
                        if (fieldMetadata instanceof ArrayTypeFieldMetadata) {
                            ArrayTypeFieldMetadata arrayTypeFieldMetadata = (ArrayTypeFieldMetadata)fieldMetadata;
                            Path arrayTypeRootPath = pathFactory.create(fieldMemberDataRootPath,
                                                                        pathFactory.createSegment(JavaMetadataLexicon.ARRAY_TYPE_CHILD_NODE,
                                                                                                  arrayIndex));
                            ArrayTypeFieldMetadataSequencer.sequenceFieldMemberData(arrayTypeFieldMetadata,
                                                                                    pathFactory,
                                                                                    nameFactory,
                                                                                    output,
                                                                                    arrayTypeRootPath,
                                                                                    arrayIndex);
                            arrayIndex++;
                        }

                        // Simple type
                        if (fieldMetadata instanceof SimpleTypeFieldMetadata) {
                            SimpleTypeFieldMetadata simpleTypeFieldMetadata = (SimpleTypeFieldMetadata)fieldMetadata;
                            Path basePath = pathFactory.createRelativePath(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                                           JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                                           JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                                           JavaMetadataLexicon.TYPE_CHILD_NODE);

                            Path simpleTypeFieldRootPath = pathFactory.create(basePath,
                                                                              pathFactory.createSegment(JavaMetadataLexicon.SIMPLE_TYPE_CHILD_NODE,
                                                                                                        simpleIndex));
                            output.setProperty(simpleTypeFieldRootPath,
                                               JavaMetadataLexicon.SIMPLE_TYPE_NAME,
                                               simpleTypeFieldMetadata.getType());

                            // Simple type modifies
                            List<ModifierMetadata> simpleModifiers = simpleTypeFieldMetadata.getModifiers();
                            int simpleTypeModifierIndex = 1;
                            for (ModifierMetadata modifierMetadata : simpleModifiers) {
                                Path simpleTypeModifierPath = pathFactory.create(pathFactory.create(simpleTypeFieldRootPath,
                                                                                                    JavaMetadataLexicon.SIMPLE_TYPE_MODIFIER_CHILD_NODE),
                                                                                 pathFactory.createSegment(JavaMetadataLexicon.MODIFIER_DECLARATION_CHILD_NODE,
                                                                                                           simpleTypeModifierIndex));
                                output.setProperty(simpleTypeModifierPath,
                                                   JavaMetadataLexicon.MODIFIER_NAME,
                                                   modifierMetadata.getName());
                                simpleTypeModifierIndex++;
                            }

                            // Simple type variables
                            List<Variable> variables = simpleTypeFieldMetadata.getVariables();
                            int simpleTypeVariableIndex = 1;
                            for (Variable variable : variables) {
                                Path variablePath = pathFactory.create(pathFactory.create(simpleTypeFieldRootPath,
                                                                                          JavaMetadataLexicon.SIMPLE_TYPE_VARIABLE),
                                                                       pathFactory.createSegment(JavaMetadataLexicon.VARIABLE,
                                                                                                 simpleTypeVariableIndex));
                                VariableSequencer.sequenceTheVariable(output, nameFactory, variable, variablePath);
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
                            Path parameterizedTypeFieldRootPath = ParameterizedTypeFieldMetadataSequencer.getParameterizedTypeFieldRootPath(pathFactory,
                                                                                                                                            parameterizedIndex);
                            ParameterizedTypeFieldMetadataSequencer.sequenceTheParameterizedTypeName(parameterizedTypeFieldMetadata,
                                                                                                     parameterizedTypeFieldRootPath,
                                                                                                     pathFactory,
                                                                                                     nameFactory,
                                                                                                     output);

                            // Parameterized type modifiers
                            List<ModifierMetadata> parameterizedTypeModifiers = parameterizedTypeFieldMetadata.getModifiers();
                            int parameterizedTypeModifierIndex = 1;
                            for (ModifierMetadata modifierMetadata : parameterizedTypeModifiers) {
                                Path parameterizedTypeModifierPath = ParameterizedTypeFieldMetadataSequencer.getParameterizedTypeFieldRModifierPath(pathFactory,
                                                                                                                                                    parameterizedTypeFieldRootPath,
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
                                VariableSequencer.sequenceTheVariable(output,
                                                                      nameFactory,
                                                                      variable,
                                                                      parameterizedTypeVariableChildNode);
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
                            Path basePath = pathFactory.createRelativePath(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                                           JavaMetadataLexicon.CONSTRUCTOR_CHILD_NODE);
                            Path constructorRootPath = pathFactory.create(basePath,
                                                                          pathFactory.createSegment(JavaMetadataLexicon.CONSTRUCTOR_DECLARATION_CHILD_NODE,
                                                                                                    constructorIndex));
                            output.setProperty(constructorRootPath,
                                               JavaMetadataLexicon.CONSTRUCTOR_NAME,
                                               constructorMetadata.getName());
                            List<ModifierMetadata> modifiers = constructorMetadata.getModifiers();
                            // modifiers
                            int constructorModifierIndex = 1;
                            for (ModifierMetadata modifierMetadata : modifiers) {
                                Path contructorModifierPath = pathFactory.create(pathFactory.create(constructorRootPath,
                                                                                                    JavaMetadataLexicon.MODIFIER_CHILD_NODE),
                                                                                 pathFactory.createSegment(JavaMetadataLexicon.MODIFIER_DECLARATION_CHILD_NODE,
                                                                                                           constructorModifierIndex));

                                output.setProperty(contructorModifierPath,
                                                   JavaMetadataLexicon.MODIFIER_NAME,
                                                   modifierMetadata.getName());
                                constructorModifierIndex++;
                            }

                            // constructor parameters
                            int constructorParameterIndex = 1;
                            for (FieldMetadata fieldMetadata : constructorMetadata.getParameters()) {

                                Path constructorParameterRootPath = pathFactory.create(pathFactory.create(constructorRootPath,
                                                                                                          JavaMetadataLexicon.PARAMETER),
                                                                                       pathFactory.createSegment(JavaMetadataLexicon.FORMAL_PARAMETER,
                                                                                                                 constructorParameterIndex));
                                // primitive type
                                if (fieldMetadata instanceof PrimitiveFieldMetadata) {

                                    PrimitiveFieldMetadata primitiveMetadata = (PrimitiveFieldMetadata)fieldMetadata;
                                    Path constructPrimitiveFormalParamRootPath = MethodMetadataSequencer.createMethodParamRootPath(pathFactory,
                                                                                                                                   constructorParameterRootPath);
                                    // type
                                    output.setProperty(constructPrimitiveFormalParamRootPath,
                                                       JavaMetadataLexicon.PRIMITIVE_TYPE_NAME,
                                                       primitiveMetadata.getType());

                                    Path constructorPrimitiveParamChildNode = MethodMetadataSequencer.createMethodParamPath(pathFactory,
                                                                                                                            constructPrimitiveFormalParamRootPath);
                                    // variables
                                    for (Variable variable : primitiveMetadata.getVariables()) {
                                        VariableSequencer.sequenceTheVariable(output,
                                                                              nameFactory,
                                                                              variable,
                                                                              constructorPrimitiveParamChildNode);
                                    }
                                }
                                // Simple type
                                if (fieldMetadata instanceof SimpleTypeFieldMetadata) {
                                    SimpleTypeFieldMetadata simpleTypeFieldMetadata = (SimpleTypeFieldMetadata)fieldMetadata;
                                    SimpleTypeMetadataSequencer.sequenceMethodFormalParam(output,
                                                                                          nameFactory,
                                                                                          pathFactory,
                                                                                          simpleTypeFieldMetadata,
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
                            Path basePath = pathFactory.createRelativePath(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                                           JavaMetadataLexicon.METHOD_CHILD_NODE);

                            Path methodRootPath = pathFactory.create(basePath,
                                                                     pathFactory.createSegment(JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                                                               methodIndex));

                            output.setProperty(methodRootPath,
                                               JavaMetadataLexicon.METHOD_NAME,
                                               methodTypeMemberMetadata.getName());

                            // method modifiers
                            int methodModierIndex = 1;
                            for (ModifierMetadata modifierMetadata : methodTypeMemberMetadata.getModifiers()) {
                                Path methodModifierPath = pathFactory.create(pathFactory.create(methodRootPath,
                                                                                                JavaMetadataLexicon.MODIFIER_CHILD_NODE),
                                                                             pathFactory.createSegment(JavaMetadataLexicon.MODIFIER_DECLARATION_CHILD_NODE,
                                                                                                       methodModierIndex));
                                output.setProperty(methodModifierPath,
                                                   JavaMetadataLexicon.MODIFIER_NAME,
                                                   modifierMetadata.getName());
                                methodModierIndex++;
                            }

                            int methodParameterIndex = 1;
                            for (FieldMetadata fieldMetadata : methodMetadata.getParameters()) {

                                Path methodParamRootPath = pathFactory.create(pathFactory.create(methodRootPath,
                                                                                                 JavaMetadataLexicon.PARAMETER),
                                                                              pathFactory.createSegment(JavaMetadataLexicon.FORMAL_PARAMETER,
                                                                                                        methodParameterIndex));

                                if (fieldMetadata instanceof PrimitiveFieldMetadata) {

                                    PrimitiveFieldMetadata primitive = (PrimitiveFieldMetadata)fieldMetadata;

                                    Path methodPrimitiveFormalParamRootPath = pathFactory.create(methodParamRootPath,
                                                                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE);

                                    Path methodParamChildNode = MethodMetadataSequencer.createMethodParamPath(pathFactory,
                                                                                                              methodPrimitiveFormalParamRootPath);
                                    // variables
                                    for (Variable variable : primitive.getVariables()) {
                                        VariableSequencer.sequenceTheVariable(output, nameFactory, variable, methodParamChildNode);
                                    }
                                    // type
                                    Path methodPrimitiveTypeParamChildNode = pathFactory.create(methodPrimitiveFormalParamRootPath);
                                    output.setProperty(methodPrimitiveTypeParamChildNode,
                                                       JavaMetadataLexicon.PRIMITIVE_TYPE_NAME,
                                                       primitive.getType());

                                }

                                if (fieldMetadata instanceof SimpleTypeFieldMetadata) {
                                    SimpleTypeFieldMetadata simpleTypeFieldMetadata = (SimpleTypeFieldMetadata)fieldMetadata;
                                    SimpleTypeMetadataSequencer.sequenceMethodFormalParam(output,
                                                                                          nameFactory,
                                                                                          pathFactory,
                                                                                          simpleTypeFieldMetadata,
                                                                                          methodParamRootPath);
                                }
                                if (fieldMetadata instanceof ArrayTypeFieldMetadata) {
                                    ArrayTypeFieldMetadata arrayTypeFieldMetadata = (ArrayTypeFieldMetadata)fieldMetadata;

                                    ArrayTypeFieldMetadataSequencer.sequenceMethodFormalParam(output,
                                                                                              nameFactory,
                                                                                              pathFactory,
                                                                                              arrayTypeFieldMetadata,
                                                                                              methodParamRootPath);

                                }

                                // TODO parameter reference types

                                methodParameterIndex++;
                            }

                            // method return type
                            FieldMetadata methodReturnType = methodTypeMemberMetadata.getReturnType();

                            if (methodReturnType instanceof PrimitiveFieldMetadata) {
                                PrimitiveFieldMetadata methodReturnPrimitiveType = (PrimitiveFieldMetadata)methodReturnType;
                                Path methodReturnPrimitiveTypePath = pathFactory.create(pathFactory.create(methodRootPath,
                                                                                                           JavaMetadataLexicon.RETURN_TYPE),
                                                                                        JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE);
                                Path methodReturnPrimitiveTypeChildNode = pathFactory.create(methodReturnPrimitiveTypePath);
                                output.setProperty(methodReturnPrimitiveTypeChildNode,
                                                   JavaMetadataLexicon.PRIMITIVE_TYPE_NAME,
                                                   methodReturnPrimitiveType.getType());

                            }
                            if (methodReturnType instanceof SimpleTypeFieldMetadata) {
                                SimpleTypeFieldMetadata simpleTypeFieldMetadata = (SimpleTypeFieldMetadata)methodReturnType;
                                SimpleTypeMetadataSequencer.sequenceMethodReturnType(output,
                                                                                     nameFactory,
                                                                                     pathFactory,
                                                                                     simpleTypeFieldMetadata,
                                                                                     methodRootPath);
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

    }

}
