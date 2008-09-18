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
import org.eclipse.jdt.core.dom.ArrayType;
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
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.sequencer.java.metadata.ArrayTypeFieldMetadata;
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
import org.jboss.dna.sequencer.java.metadata.ParameterizedTypeFieldMetadata;
import org.jboss.dna.sequencer.java.metadata.PrimitiveFieldMetadata;
import org.jboss.dna.sequencer.java.metadata.SimpleTypeFieldMetadata;
import org.jboss.dna.sequencer.java.metadata.SingleImportMetadata;
import org.jboss.dna.sequencer.java.metadata.SingleMemberAnnotationMetadata;
import org.jboss.dna.sequencer.java.metadata.TypeMetadata;
import org.jboss.dna.sequencer.java.metadata.Variable;

/**
 * Abstract definition of a <code>JavaMetadata<code>. This class exposes some useful methods, that can
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
     * Process modifiers of {@link TypeDeclaration}.
     * 
     * @param typeDeclaration - the type declaration.
     * @param classMetadata - class meta data.
     */
    @SuppressWarnings( "unchecked" )
    protected void processModifiersOfTypDeclaration( TypeDeclaration typeDeclaration,
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
    protected MethodMetadata getMethodMetadataFrom( MethodDeclaration methodDeclaration ) {
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
    protected MethodMetadata getMethodTypeMemberMetadataFrom( MethodDeclaration methodDeclaration ) {
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
     * Process return type of a {@link MethodDeclaration}.
     * 
     * @param methodDeclaration - the method declaration.
     * @param methodMetadata - the method meta data.
     */
    protected void processReturnTypeOfMethodDeclaration( MethodDeclaration methodDeclaration,
                                                         MethodMetadata methodMetadata ) {
        Type type = methodDeclaration.getReturnType2();
        if (type.isPrimitiveType()) {
            PrimitiveFieldMetadata primitive = new PrimitiveFieldMetadata();
            primitive.setType(((PrimitiveType)type).getPrimitiveTypeCode().toString());
            methodMetadata.setReturnType(primitive);
        }
        if (type.isSimpleType()) {
            SimpleTypeFieldMetadata simpleTypeFieldMetadata = new SimpleTypeFieldMetadata();
            simpleTypeFieldMetadata.setType(JavaMetadataUtil.getName(((SimpleType)type).getName()));
            methodMetadata.setReturnType(simpleTypeFieldMetadata);
        }
    }

    /**
     * Process parameters of a {@link MethodDeclaration}.
     * 
     * @param methodDeclaration - the method declaration.
     * @param methodMetadata - the method meta data.
     */
    @SuppressWarnings( "unchecked" )
    protected void processParametersOfMethodDeclaration( MethodDeclaration methodDeclaration,
                                                         MethodMetadata methodMetadata ) {
        for (SingleVariableDeclaration singleVariableDeclaration : (List<SingleVariableDeclaration>)methodDeclaration.parameters()) {
            Type type = singleVariableDeclaration.getType();
            if (type.isPrimitiveType()) {
                PrimitiveFieldMetadata primitiveFieldMetadata = (PrimitiveFieldMetadata)processVariableDeclaration(singleVariableDeclaration,
                                                                                                                   type);
                methodMetadata.getParameters().add(primitiveFieldMetadata);
            }
            if (type.isParameterizedType()) {
                ParameterizedTypeFieldMetadata parameterizedTypeFieldMetadata = (ParameterizedTypeFieldMetadata)processVariableDeclaration(singleVariableDeclaration,
                                                                                                                                           type);
                methodMetadata.getParameters().add(parameterizedTypeFieldMetadata);
            }
            if (type.isQualifiedType()) {
                // TODO
            }
            if (type.isSimpleType()) {
                SimpleTypeFieldMetadata simpleTypeFieldMetadata = (SimpleTypeFieldMetadata)processVariableDeclaration(singleVariableDeclaration, type);
                methodMetadata.getParameters().add(simpleTypeFieldMetadata);
            }
            if (type.isArrayType()) {
                ArrayTypeFieldMetadata arrayTypeFieldMetadata = (ArrayTypeFieldMetadata)processVariableDeclaration(singleVariableDeclaration, type);
                methodMetadata.getParameters().add(arrayTypeFieldMetadata);
            }
            if (type.isWildcardType()) {
                // TODO
            }
        }

    }

    /**
     * Process a {@link SingleVariableDeclaration} of a {@link MethodDeclaration}.
     * 
     * @param singleVariableDeclaration
     * @param type
     * @return a field meta data.
     */
    @SuppressWarnings( "unchecked" )
    private FieldMetadata processVariableDeclaration( SingleVariableDeclaration singleVariableDeclaration,
                                                      Type type ) {

        Variable variable;
        if (type.isPrimitiveType()) {
            PrimitiveFieldMetadata primitiveFieldMetadata = new PrimitiveFieldMetadata();
            primitiveFieldMetadata.setType(((PrimitiveType)type).getPrimitiveTypeCode().toString());
            variable = new Variable();
            variable.setName(JavaMetadataUtil.getName(singleVariableDeclaration.getName()));
            primitiveFieldMetadata.getVariables().add(variable);
            for (IExtendedModifier extendedModifier : (List<IExtendedModifier>)singleVariableDeclaration.modifiers()) {
                ModifierMetadata modifierMetadata = new ModifierMetadata();
                if (extendedModifier.isAnnotation()) {
                    // TODO
                } else {
                    Modifier modifier = (Modifier)extendedModifier;
                    modifierMetadata.setName(modifier.getKeyword().toString());
                    primitiveFieldMetadata.getModifiers().add(modifierMetadata);
                }
            }
            return primitiveFieldMetadata;
        }
        if(type.isSimpleType()) {
            SimpleType simpleType = (SimpleType)type;
            SimpleTypeFieldMetadata simpleTypeFieldMetadata = new SimpleTypeFieldMetadata();
            simpleTypeFieldMetadata.setType(JavaMetadataUtil.getName(simpleType.getName()));
            variable = new Variable();
            variable.setName(JavaMetadataUtil.getName(singleVariableDeclaration.getName()));
            simpleTypeFieldMetadata.getVariables().add(variable);
            for (IExtendedModifier simpleTypeExtendedModifier : (List<IExtendedModifier> )singleVariableDeclaration.modifiers()) {
                ModifierMetadata modifierMetadata = new ModifierMetadata();
                if (simpleTypeExtendedModifier.isAnnotation()) {
                    // TODO
                } else {
                    Modifier modifier = (Modifier)simpleTypeExtendedModifier;
                    modifierMetadata.setName(modifier.getKeyword().toString());
                    simpleTypeFieldMetadata.getModifiers().add(modifierMetadata);
                }
            }
            return simpleTypeFieldMetadata;
        }
        if (type.isParameterizedType()) {
            ParameterizedTypeFieldMetadata parameterizedTypeFieldMetadata = new ParameterizedTypeFieldMetadata();
            ParameterizedType parameterizedType = (ParameterizedType)type;
            parameterizedTypeFieldMetadata.setType(getTypeName(parameterizedType));
            variable = new Variable();
            variable.setName(JavaMetadataUtil.getName(singleVariableDeclaration.getName()));
            parameterizedTypeFieldMetadata.getVariables().add(variable);
            for (IExtendedModifier parameterizedExtendedModifier : (List<IExtendedModifier>)singleVariableDeclaration.modifiers()) {
                ModifierMetadata modifierMetadata = new ModifierMetadata();
                if(parameterizedExtendedModifier.isAnnotation()) {
                    // TODO
                } else {
                    Modifier modifier = (Modifier)parameterizedExtendedModifier;
                    modifierMetadata.setName(modifier.getKeyword().toString());
                    parameterizedTypeFieldMetadata.getModifiers().add(modifierMetadata); 
                }
            }
            return parameterizedTypeFieldMetadata;
        }
        if(type.isArrayType()) {
            ArrayTypeFieldMetadata arrayTypeFieldMetadata = new ArrayTypeFieldMetadata();
            ArrayType arrayType = (ArrayType)type;
            arrayTypeFieldMetadata.setType(getTypeName(arrayType));
            variable = new Variable();
            variable.setName(JavaMetadataUtil.getName(singleVariableDeclaration.getName()));
            arrayTypeFieldMetadata.getVariables().add(variable);
            
            for (IExtendedModifier arrayTypeExtendedModifier : (List<IExtendedModifier>)singleVariableDeclaration.modifiers()) {
                ModifierMetadata modifierMetadata = new ModifierMetadata();
                if(arrayTypeExtendedModifier.isAnnotation()) {
                    // TODO
                } else {
                    Modifier modifier = (Modifier)arrayTypeExtendedModifier;
                    modifierMetadata.setName(modifier.getKeyword().toString());
                    arrayTypeFieldMetadata.getModifiers().add(modifierMetadata); 
                }
            }
            return arrayTypeFieldMetadata;
        }
        return null;
    }

    /**
     * Extract the type name
     * 
     * @param type - the type to be processed. This can be primitive, simple, parameterized ...
     * @return the name of a type.
     * @throws IllegalArgumentException if type is null.
     */
    private String getTypeName( Type type ) {
        CheckArg.isNotNull(type, "type");
        if (type.isPrimitiveType()) {
            PrimitiveType primitiveType = (PrimitiveType)type;
            return primitiveType.getPrimitiveTypeCode().toString();
        }
        if (type.isSimpleType()) {
            SimpleType simpleType = (SimpleType)type;
            return JavaMetadataUtil.getName(simpleType.getName());
        }
        if(type.isArrayType()) {
            ArrayType arrayType = (ArrayType)type;
            // the element type is never an array type
            Type elementType = arrayType.getElementType();
            if (elementType.isPrimitiveType()) {
             return ((PrimitiveType)elementType).getPrimitiveTypeCode().toString();

            }
            // can't be an array type
            if (elementType.isSimpleType()) {
                return JavaMetadataUtil.getName(((SimpleType)elementType).getName());
            }
            
        }
        return null;
    }

    /**
     * Get {@link ConstructorMetadata}
     * 
     * @param methodDeclaration
     * @return constructorMetadata
     */
    protected MethodMetadata getConstructorMetadataFrom( MethodDeclaration methodDeclaration ) {
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
    protected FieldMetadata getFieldMetadataFrom( FieldDeclaration fieldDeclaration ) {
        if (fieldDeclaration != null && fieldDeclaration.getType() != null && (!fieldDeclaration.fragments().isEmpty())) {
            // type
            Type type = fieldDeclaration.getType();
            // Primitive type
            if (type.isPrimitiveType()) {
                PrimitiveFieldMetadata primitiveFieldMetadata = processPrimitiveType(fieldDeclaration);
                return primitiveFieldMetadata;
            }
            // ParameterizedType
            if (type.isParameterizedType()) {
                ParameterizedTypeFieldMetadata referenceFieldMetadata = processParameterizedType(fieldDeclaration);
                return referenceFieldMetadata;
            }
            // SimpleType
            if (type.isSimpleType()) {
                SimpleTypeFieldMetadata simpleTypeFieldMetadata = processSimpleType(fieldDeclaration);
                return simpleTypeFieldMetadata;
            }
            // ArrayType
            if (type.isArrayType()) {
                ArrayTypeFieldMetadata arrayFieldMetadata = processArrayTypeFrom(fieldDeclaration);
                return arrayFieldMetadata;
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
     * Process a {@link FieldDeclaration} to win information for an array type.
     * 
     * @param fieldDeclaration - field declaration
     * @return an ArrayTypeFieldMetadata, that contains information about an array type.
     */
    protected ArrayTypeFieldMetadata processArrayTypeFrom( FieldDeclaration fieldDeclaration ) {
        ArrayTypeFieldMetadata arrayTypeFieldMetadata = null;
        ArrayType arrayType = (ArrayType)fieldDeclaration.getType();
        // the element type is never an array type
        Type type = arrayType.getElementType();
        if (type.isPrimitiveType()) {
            PrimitiveType primitiveType = (PrimitiveType)type;
            arrayTypeFieldMetadata = new ArrayTypeFieldMetadata();
            arrayTypeFieldMetadata.setType(primitiveType.getPrimitiveTypeCode().toString());
            processModifiersAndVariablesOfFieldDeclaration(fieldDeclaration, arrayTypeFieldMetadata);
            return arrayTypeFieldMetadata;

        }
        // can't be an array type
        if (type.isSimpleType()) {
            SimpleType simpleType = (SimpleType)type;
            arrayTypeFieldMetadata = new ArrayTypeFieldMetadata();
            arrayTypeFieldMetadata.setType(JavaMetadataUtil.getName(simpleType.getName()));
            processModifiersAndVariablesOfFieldDeclaration(fieldDeclaration, arrayTypeFieldMetadata);
            return arrayTypeFieldMetadata;
        }

        return null;
    }

    /**
     * Process together modifiers and variables of a {@link FieldDeclaration}.
     * 
     * @param fieldDeclaration - the field declaration instance.
     * @param arrayTypeFieldMetadata - the meta data.
     */
    private void processModifiersAndVariablesOfFieldDeclaration( FieldDeclaration fieldDeclaration,
                                                                 ArrayTypeFieldMetadata arrayTypeFieldMetadata ) {
        processModifiersOfFieldDeclaration(fieldDeclaration, arrayTypeFieldMetadata);
        processVariablesOfVariableDeclarationFragment(fieldDeclaration, arrayTypeFieldMetadata);
    }

    /**
     * Process the simple type of a {@link FieldDeclaration}.
     * 
     * @param fieldDeclaration - the field declaration.
     * @return SimpleTypeFieldMetadata.
     */
    protected SimpleTypeFieldMetadata processSimpleType( FieldDeclaration fieldDeclaration ) {
        SimpleType simpleType = (SimpleType)fieldDeclaration.getType();
        SimpleTypeFieldMetadata simpleTypeFieldMetadata = new SimpleTypeFieldMetadata();
        simpleTypeFieldMetadata.setType(JavaMetadataUtil.getName(simpleType.getName()));
        // modifiers
        processModifiersOfFieldDeclaration(fieldDeclaration, simpleTypeFieldMetadata);
        processVariablesOfVariableDeclarationFragment(fieldDeclaration, simpleTypeFieldMetadata);
        return simpleTypeFieldMetadata;
    }

    /**
     * Process the parameterized type of a {@link FieldDeclaration}.
     * 
     * @param fieldDeclaration - the field declaration.
     * @return ParameterizedTypeFieldMetadata.
     */
    protected ParameterizedTypeFieldMetadata processParameterizedType( FieldDeclaration fieldDeclaration ) {
        ParameterizedType parameterizedType = (ParameterizedType)fieldDeclaration.getType();
        Type typeOfParameterizedType = parameterizedType.getType(); // type may be a simple type or a qualified type.
        ParameterizedTypeFieldMetadata referenceFieldMetadata = (ParameterizedTypeFieldMetadata)createParameterizedFieldMetadataFrom(typeOfParameterizedType);
        // modifiers
        processModifiersOfFieldDeclaration(fieldDeclaration, referenceFieldMetadata);
        // variables
        processVariablesOfVariableDeclarationFragment(fieldDeclaration, referenceFieldMetadata);
        return referenceFieldMetadata;
    }

    /**
     * Process the primitive type of a {@link FieldDeclaration}.
     * 
     * @param fieldDeclaration - the field declaration.
     * @return PrimitiveFieldMetadata.
     */
    protected PrimitiveFieldMetadata processPrimitiveType( FieldDeclaration fieldDeclaration ) {
        PrimitiveType primitiveType = (PrimitiveType)fieldDeclaration.getType();
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
    protected void processModifiersOfMethodDeclaration( MethodDeclaration methodDeclaration,
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
        ParameterizedTypeFieldMetadata parameterizedTypeFieldMetadata = null;
        if (type.isSimpleType()) {
            SimpleType simpleType = (SimpleType)type;
            parameterizedTypeFieldMetadata = new ParameterizedTypeFieldMetadata();
            parameterizedTypeFieldMetadata.setType(JavaMetadataUtil.getName(simpleType.getName()));
        }
        // TODO also process QualifiedType
        return parameterizedTypeFieldMetadata;
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
