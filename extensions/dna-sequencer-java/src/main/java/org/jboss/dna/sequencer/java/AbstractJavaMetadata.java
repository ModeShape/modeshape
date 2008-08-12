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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.jboss.dna.sequencer.java.metadata.ClassMetadata;
import org.jboss.dna.sequencer.java.metadata.ConstructorMetadata;
import org.jboss.dna.sequencer.java.metadata.FieldMetadata;
import org.jboss.dna.sequencer.java.metadata.ImportMetadata;
import org.jboss.dna.sequencer.java.metadata.ImportOnDemandMetadata;
import org.jboss.dna.sequencer.java.metadata.InterfaceMetadata;
import org.jboss.dna.sequencer.java.metadata.MarkerAnnotationMetadata;
import org.jboss.dna.sequencer.java.metadata.MethodMetadata;
import org.jboss.dna.sequencer.java.metadata.MethodTypeMemberMetadata;
import org.jboss.dna.sequencer.java.metadata.ModifierMetadata;
import org.jboss.dna.sequencer.java.metadata.NormalAnnotationMetadata;
import org.jboss.dna.sequencer.java.metadata.PackageMetadata;
import org.jboss.dna.sequencer.java.metadata.ParameterizedFieldMetadata;
import org.jboss.dna.sequencer.java.metadata.PrimitiveFieldMetadata;
import org.jboss.dna.sequencer.java.metadata.ReturnType;
import org.jboss.dna.sequencer.java.metadata.SimpleFieldMetadata;
import org.jboss.dna.sequencer.java.metadata.SingleImportMetadata;
import org.jboss.dna.sequencer.java.metadata.SingleMemberAnnotationMetadata;
import org.jboss.dna.sequencer.java.metadata.TypeMetadata;
import org.jboss.dna.sequencer.java.metadata.Variable;

/**
 * Abstract definition of a <tt>JavaMetadata<tt>. This class exposes some useful methods, that can
 * be used to create meta data of a compilation unit. Methods can also separately be used.
 * 
 * @author Serge Pagop
 */
public abstract class AbstractJavaMetadata {

    /**
     * Create a set of <code>ImportMetadata</code> of a compilation unit.
     * 
     * @param unit - the compilation unit.
     * @return all static import declarations from the compilation unit.
     */
    @SuppressWarnings( "unchecked" )
    protected List<ImportMetadata> createImportMetadata( CompilationUnit unit ) {
        List<ImportMetadata> metadata = new ArrayList<ImportMetadata>();
        List<ImportDeclaration> imports = unit.imports();
        if (!imports.isEmpty()) {
            for (ImportDeclaration importDeclaration : imports) {
                if (importDeclaration.isOnDemand()) {
                    // typeImportOnDemand and staticImportOnDemand
                    ImportOnDemandMetadata onDemandMetadata = new ImportOnDemandMetadata();
                    onDemandMetadata.setName(JavaMetadataUtil.getName(importDeclaration.getName()));
                    metadata.add(onDemandMetadata);
                } else {
                    // singleTypeImport and singleStaticImport
                    SingleImportMetadata singleImportMetadata = new SingleImportMetadata();
                    singleImportMetadata.setName(JavaMetadataUtil.getName(importDeclaration.getName()));
                    metadata.add(singleImportMetadata);
                }

            }
        }
        return metadata;
    }

    /**
     * Create a <code>PackageMetadata</code> of a compilation unit.
     * 
     * @param unit - the compilation unit.
     * @return the package meta data of a compilation unit.
     */
    @SuppressWarnings( "unchecked" )
    protected PackageMetadata createPackageMetadata( CompilationUnit unit ) {
        PackageMetadata packageMetadata = null;
        List<Annotation> annotations = null;
        PackageDeclaration packageDeclaration = unit.getPackage();
        if (packageDeclaration != null) {
            annotations = packageDeclaration.annotations();
            packageMetadata = new PackageMetadata();
            packageMetadata.setName(JavaMetadataUtil.getName(unit.getPackage().getName()));
            if (!annotations.isEmpty()) {
                for (Object object : annotations) {

                    if (object instanceof NormalAnnotation) {
                        NormalAnnotation normalAnnotation = (NormalAnnotation)object;
                        NormalAnnotationMetadata normalAnnotationMetadata = new NormalAnnotationMetadata();
                        normalAnnotationMetadata.setName(JavaMetadataUtil.getName(normalAnnotation.getTypeName()));
                        normalAnnotationMetadata.setNormal(Boolean.TRUE);
                        packageMetadata.getAnnotationMetada().add(normalAnnotationMetadata);
                    }
                    if (object instanceof MarkerAnnotation) {
                        MarkerAnnotation markerAnnotation = (MarkerAnnotation)object;
                        MarkerAnnotationMetadata markerAnnotationMetadata = new MarkerAnnotationMetadata();
                        markerAnnotationMetadata.setName(JavaMetadataUtil.getName(markerAnnotation.getTypeName()));
                        markerAnnotationMetadata.setMarker(Boolean.TRUE);
                        packageMetadata.getAnnotationMetada().add(markerAnnotationMetadata);
                    }
                    if (object instanceof SingleMemberAnnotation) {
                        SingleMemberAnnotation singleMemberAnnotation = (SingleMemberAnnotation)object;
                        SingleMemberAnnotationMetadata singleMemberAnnotationMetadata = new SingleMemberAnnotationMetadata();
                        singleMemberAnnotationMetadata.setName(JavaMetadataUtil.getName(singleMemberAnnotation.getTypeName()));
                        singleMemberAnnotationMetadata.setSingle(Boolean.TRUE);
                        packageMetadata.getAnnotationMetada().add(singleMemberAnnotationMetadata);

                    }
                }
            }
        }
        return packageMetadata;
    }

    /**
     * Create a list with all top level types of a compilation unit.
     * 
     * @param unit - the compilation unit.
     * @return meta data for types in this compilation unit.
     */
    @SuppressWarnings( "unchecked" )
    protected List<TypeMetadata> createTypeMetadata( CompilationUnit unit ) {
        List<TypeMetadata> metadata = new ArrayList<TypeMetadata>();
        List<AbstractTypeDeclaration> topLevelType = unit.types();

        for (AbstractTypeDeclaration abstractTypeDeclaration : topLevelType) {
            // process TypeDeclaration (class, interface)
            if (abstractTypeDeclaration instanceof TypeDeclaration) {
                TypeDeclaration typeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
                if (typeDeclaration.isInterface()) {
                    // is an interface top level type
                    InterfaceMetadata interfaceMetadata = new InterfaceMetadata();
                    interfaceMetadata.setName(JavaMetadataUtil.getName(typeDeclaration.getName()));
                    metadata.add(interfaceMetadata);
                } else {
                    // is a class top level type
                    ClassMetadata classMetadata = new ClassMetadata();
                    processModifiersOfTypDeclaration(typeDeclaration, classMetadata);
                    classMetadata.setName(JavaMetadataUtil.getName(typeDeclaration.getName()));
                    // fields of the class top level type
                    FieldDeclaration[] fieldDeclarations = typeDeclaration.getFields();
                    for (FieldDeclaration fieldDeclaration : fieldDeclarations) {
                        FieldMetadata fieldMetadata = getFieldMetadataFrom(fieldDeclaration);
                        classMetadata.getFields().add(fieldMetadata);
                    }
                    // methods of the class top level type
                    MethodDeclaration[] methodDeclarations = typeDeclaration.getMethods();
                    for (MethodDeclaration methodDeclaration : methodDeclarations) {
                        MethodMetadata methodMetadata = getMethodMetadataFrom(methodDeclaration);
                        classMetadata.getMethods().add(methodMetadata);
                    }
                    metadata.add(classMetadata);
                }
            }

            // process EnumDeclaration
            if (abstractTypeDeclaration instanceof EnumDeclaration) {
                // EnumDeclaration enumDeclaration = (EnumDeclaration)abstractTypeDeclaration;
                // TODO get infos from enum declaration and create a enum meta data object.
            }

            // process annotationTypeDeclaration
            if (abstractTypeDeclaration instanceof AnnotationTypeDeclaration) {
                // AnnotationTypeDeclaration annotationTypeDeclaration = (AnnotationTypeDeclaration)abstractTypeDeclaration;
                // TODO get infos from annotation type declaration and create a annotation meta data object.
            }
        }
        return metadata;
    }

    /**
     * @param typeDeclaration
     * @param classMetadata
     */
    @SuppressWarnings( "unchecked" )
    private void processModifiersOfTypDeclaration( TypeDeclaration typeDeclaration,
                                                   ClassMetadata classMetadata ) {
        List<IExtendedModifier> modifiers = typeDeclaration.modifiers();

        for (IExtendedModifier extendedModifier : modifiers) {
            ModifierMetadata modifierMetadata = new ModifierMetadata();
            if (extendedModifier.isAnnotation()) {
                if (extendedModifier instanceof MarkerAnnotation) {
                    MarkerAnnotation marker = (MarkerAnnotation)extendedModifier;
                    MarkerAnnotationMetadata markerAnnotationMetadata = new MarkerAnnotationMetadata();
                    markerAnnotationMetadata.setName(JavaMetadataUtil.getName(marker.getTypeName()));
                    classMetadata.getAnnotations().add(markerAnnotationMetadata);
                }
            } else {

                Modifier modifier = (Modifier)extendedModifier;
                modifierMetadata.setName(modifier.getKeyword().toString());
                classMetadata.getModifiers().add(modifierMetadata);
            }
        }
    }

    /**
     * Gets a method meta data from {@link MethodDeclaration}.
     * 
     * @param methodDeclaration - the MethodDeclaration.
     * @return methodMetadata - the method meta data.
     */
    private MethodMetadata getMethodMetadataFrom( MethodDeclaration methodDeclaration ) {
        if (methodDeclaration != null) {
            if (methodDeclaration.isConstructor()) {
                return getConstructorMetadataFrom(methodDeclaration);
            }
            return getMethodTypeMemberMetadataFrom(methodDeclaration);

        }
        return null;
    }

    /**
     * Get {@link MethodTypeMemberMetadata}
     * 
     * @param methodDeclaration
     * @return methodTypeMemberMetadata
     */
    @SuppressWarnings( "unchecked" )
    private MethodMetadata getMethodTypeMemberMetadataFrom( MethodDeclaration methodDeclaration ) {
        MethodTypeMemberMetadata methodTypeMemberMetadata = new MethodTypeMemberMetadata();
        processReturnTypeOfMethodDeclaration(methodDeclaration, methodTypeMemberMetadata);
        processModifiersOfMethodDeclaration(methodDeclaration, methodTypeMemberMetadata);
        processParametersOfMethodDeclaration(methodDeclaration, methodTypeMemberMetadata);
        methodTypeMemberMetadata.setName(JavaMetadataUtil.getName(methodDeclaration.getName()));
        List<SingleVariableDeclaration> params = methodDeclaration.parameters();
        for (SingleVariableDeclaration singleVariableDeclaration : params) {
            singleVariableDeclaration.getName();
        }
        return methodTypeMemberMetadata;
    }

    /**
     * @param methodDeclaration
     * @param methodMetadata
     */
    private void processReturnTypeOfMethodDeclaration( MethodDeclaration methodDeclaration,
                                                       MethodMetadata methodMetadata ) {
        Type type = methodDeclaration.getReturnType2();
        ReturnType returnType = null;
        if(type.isPrimitiveType()) {
            returnType = new ReturnType();
            returnType.setName(((PrimitiveType)type).getPrimitiveTypeCode().toString());
            methodMetadata.setReturnType(returnType);
        }
        if(type.isSimpleType()) {
            returnType = new ReturnType();
            returnType.setName(JavaMetadataUtil.getName(((SimpleType)type).getName()));
            methodMetadata.setReturnType(returnType);
        }
    }

    /**
     * @param methodDeclaration
     * @param methodMetadata
     */
    @SuppressWarnings( "unchecked" )
    private void processParametersOfMethodDeclaration( MethodDeclaration methodDeclaration,
                                                       MethodMetadata methodMetadata ) {
        List<SingleVariableDeclaration> params = methodDeclaration.parameters();
        for (SingleVariableDeclaration singleVariableDeclaration : params) {
            Type type = singleVariableDeclaration.getType();
            if (type.isPrimitiveType()) {
                PrimitiveFieldMetadata primitiveFieldMetadata = new PrimitiveFieldMetadata();
                primitiveFieldMetadata.setType(((PrimitiveType)type).getPrimitiveTypeCode().toString());
                Variable variable = new Variable();
                variable.setName(JavaMetadataUtil.getName(singleVariableDeclaration.getName()));
                primitiveFieldMetadata.getVariables().add(variable);
                List<IExtendedModifier> extendedModifiers = singleVariableDeclaration.modifiers();
                for (IExtendedModifier extendedModifier : extendedModifiers) {
                    ModifierMetadata modifierMetadata = new ModifierMetadata();
                    if (extendedModifier.isAnnotation()) {
                        // TODO
                    } else {
                        Modifier modifier = (Modifier)extendedModifier;
                        modifierMetadata.setName(modifier.getKeyword().toString());
                        primitiveFieldMetadata.getModifiers().add(modifierMetadata);
                    }
                }
                methodMetadata.getParameters().add(primitiveFieldMetadata);
            }
            if (type.isParameterizedType()) {
                // TODO
            }
            if (type.isQualifiedType()) {

            }
            if (type.isSimpleType()) {
                SimpleType simpleType = (SimpleType)type;
                SimpleFieldMetadata simpleFieldMetadata = new SimpleFieldMetadata();
                simpleFieldMetadata.setType(JavaMetadataUtil.getName(simpleType.getName()));
                Variable variable = new Variable();
                variable.setName(JavaMetadataUtil.getName(singleVariableDeclaration.getName()));
                simpleFieldMetadata.getVariables().add(variable);
                List<IExtendedModifier> extendedModifiers = singleVariableDeclaration.modifiers();
                for (IExtendedModifier extendedModifier2 : extendedModifiers) {
                    ModifierMetadata modifierMetadata = new ModifierMetadata();
                    if (extendedModifier2.isAnnotation()) {
                        // TODO
                    } else {
                        Modifier modifier = (Modifier)extendedModifier2;
                        modifierMetadata.setName(modifier.getKeyword().toString());
                        simpleFieldMetadata.getModifiers().add(modifierMetadata);
                    }
                }
                methodMetadata.getParameters().add(simpleFieldMetadata);
            }
            if (type.isArrayType()) {
                // TODO
            }
            if (type.isWildcardType()) {
                // TODO
            }
        }

    }

    /**
     * Get {@link ConstructorMetadata}
     * 
     * @param methodDeclaration
     * @return constructorMetadata
     */
    private MethodMetadata getConstructorMetadataFrom( MethodDeclaration methodDeclaration ) {
        ConstructorMetadata constructorMetadata = new ConstructorMetadata();
        // modifiers
        processModifiersOfMethodDeclaration(methodDeclaration, constructorMetadata);
        processParametersOfMethodDeclaration(methodDeclaration, constructorMetadata);
        constructorMetadata.setName(JavaMetadataUtil.getName(methodDeclaration.getName()));
        // arguments list
        return constructorMetadata;
    }

    /**
     * Gets a field meta data from {@link FieldDeclaration}.
     * 
     * @param fieldDeclaration - the declaration.
     * @return fieldMetadata - meta data.
     */
    @SuppressWarnings( "unchecked" )
    private FieldMetadata getFieldMetadataFrom( FieldDeclaration fieldDeclaration ) {
        if (fieldDeclaration != null && fieldDeclaration.getType() != null && (!fieldDeclaration.fragments().isEmpty())) {
            // type
            Type type = fieldDeclaration.getType();
            // Primitive type
            if (type.isPrimitiveType()) {
                PrimitiveFieldMetadata primitiveFieldMetadata = processPrimitiveType(fieldDeclaration, type);
                return primitiveFieldMetadata;
            }
            // ParameterizedType
            if (type.isParameterizedType()) {
                ParameterizedFieldMetadata referenceFieldMetadata = processParameterizedType(fieldDeclaration, type);
                return referenceFieldMetadata;
            }
            // SimpleType
            if (type.isSimpleType()) {
                SimpleFieldMetadata simpleFieldMetadata = processSimpleType(fieldDeclaration, type);
                return simpleFieldMetadata;
            }
            // ArrayType
            if (type.isArrayType()) {
                // TODO
            }
            // QualifiedType
            if (type.isQualifiedType()) {
                // TODO

            }
            // WildcardType
            if (type.isWildcardType()) {

            }
        }
        return null;
    }

    /**
     * @param fieldDeclaration
     * @param type
     * @return SimpleFieldMetadata
     */
    private SimpleFieldMetadata processSimpleType( FieldDeclaration fieldDeclaration,
                                                   Type type ) {
        SimpleType simpleType = (SimpleType)type;
        SimpleFieldMetadata simpleFieldMetadata = new SimpleFieldMetadata();
        simpleFieldMetadata.setType(JavaMetadataUtil.getName(simpleType.getName()));
        // modifiers
        processModifiersOfFieldDeclaration(fieldDeclaration, simpleFieldMetadata);
        processVariablesOfVariableDeclarationFragment(fieldDeclaration, simpleFieldMetadata);
        return simpleFieldMetadata;
    }

    /**
     * @param fieldDeclaration
     * @param type
     * @return ParameterizedFieldMetadata
     */
    private ParameterizedFieldMetadata processParameterizedType( FieldDeclaration fieldDeclaration,
                                                                 Type type ) {
        ParameterizedType parameterizedType = (ParameterizedType)type;
        Type typeOfParameterizedType = parameterizedType.getType(); // type may be a simple type or a qualified type.
        ParameterizedFieldMetadata referenceFieldMetadata = (ParameterizedFieldMetadata)createParameterizedFieldMetadataFrom(typeOfParameterizedType);
        // modifiers
        processModifiersOfFieldDeclaration(fieldDeclaration, referenceFieldMetadata);
        // variables
        processVariablesOfVariableDeclarationFragment(fieldDeclaration, referenceFieldMetadata);
        return referenceFieldMetadata;
    }

    /**
     * @param fieldDeclaration
     * @param type
     * @return PrimitiveFieldMetadata
     */
    private PrimitiveFieldMetadata processPrimitiveType( FieldDeclaration fieldDeclaration,
                                                         Type type ) {
        PrimitiveType primitiveType = (PrimitiveType)type;
        PrimitiveFieldMetadata primitiveFieldMetadata = new PrimitiveFieldMetadata();
        primitiveFieldMetadata.setType(primitiveType.getPrimitiveTypeCode().toString());
        // modifiers
        processModifiersOfFieldDeclaration(fieldDeclaration, primitiveFieldMetadata);
        // variables
        processVariablesOfVariableDeclarationFragment(fieldDeclaration, primitiveFieldMetadata);
        return primitiveFieldMetadata;
    }

    /**
     * Process modifiers of a {@link FieldDeclaration}
     * 
     * @param fieldDeclaration
     * @param fieldMetadata
     */
    @SuppressWarnings( "unchecked" )
    protected void processModifiersOfFieldDeclaration( FieldDeclaration fieldDeclaration,
                                                       FieldMetadata fieldMetadata ) {
        List<IExtendedModifier> extendedModifiers = fieldDeclaration.modifiers();
        for (IExtendedModifier extendedModifier : extendedModifiers) {
            ModifierMetadata modifierMetadata = new ModifierMetadata();
            if (extendedModifier.isAnnotation()) {
                // TODO annotation modifiers
            } else {
                Modifier modifier = (Modifier)extendedModifier;
                modifierMetadata.setName(modifier.getKeyword().toString());
                fieldMetadata.getModifiers().add(modifierMetadata);
            }
        }

    }

    /**
     * Process modifiers of a {@link MethodDeclaration}.
     * 
     * @param methodDeclaration
     * @param methodMetadata
     */
    @SuppressWarnings( "unchecked" )
    private void processModifiersOfMethodDeclaration( MethodDeclaration methodDeclaration,
                                                      MethodMetadata methodMetadata ) {
        List<IExtendedModifier> extendedModifiers = methodDeclaration.modifiers();
        for (IExtendedModifier extendedModifier : extendedModifiers) {
            ModifierMetadata modifierMetadata = new ModifierMetadata();
            if (extendedModifier.isAnnotation()) {
                // TODO
            } else {
                Modifier modifier = (Modifier)extendedModifier;
                modifierMetadata.setName(modifier.getKeyword().toString());
                methodMetadata.getModifiers().add(modifierMetadata);
            }
        }
    }

    /**
     * Create a <code>FieldMetadata</code> from a {@link Type} instance.
     * 
     * @param type - The {@link Type}
     * @return the specific type of <code>FieldMetadata</code>
     */
    protected FieldMetadata createParameterizedFieldMetadataFrom( Type type ) {
        ParameterizedFieldMetadata parameterizedFieldMetadata = null;
        if (type.isSimpleType()) {
            SimpleType simpleType = (SimpleType)type;
            parameterizedFieldMetadata = new ParameterizedFieldMetadata();
            parameterizedFieldMetadata.setType(JavaMetadataUtil.getName(simpleType.getName()));
        }
        // TODO also process QualifiedType
        return parameterizedFieldMetadata;
    }

    /**
     * Process variables of a {@link VariableDeclarationFragment}.
     * 
     * @param fieldDeclaration - the {@link FieldDeclaration}
     * @param fieldMetadata - where to transfer the meta data.
     */
    @SuppressWarnings( "unchecked" )
    protected void processVariablesOfVariableDeclarationFragment( FieldDeclaration fieldDeclaration,
                                                                  FieldMetadata fieldMetadata ) {
        List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
        for (VariableDeclarationFragment fragment : fragments) {
            fieldMetadata.getVariables().add(new Variable(JavaMetadataUtil.getName(fragment.getName())));
        }
    }
}
