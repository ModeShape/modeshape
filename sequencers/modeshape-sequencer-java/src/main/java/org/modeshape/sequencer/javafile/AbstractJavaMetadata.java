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
package org.modeshape.sequencer.javafile;

import org.eclipse.jdt.core.dom.*;
import org.modeshape.common.util.CheckArg;
import org.modeshape.sequencer.javafile.metadata.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract definition of a <code>JavaMetadata<code>. This class exposes some useful methods, that can
 * be used to create meta data of a compilation unit. Methods can also separately be used.
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
                    metadata.add(ImportMetadata.onDemand(JavaMetadataUtil.getName(importDeclaration.getName())));
                } else {
                    // singleTypeImport and singleStaticImport
                    metadata.add(ImportMetadata.single(JavaMetadataUtil.getName(importDeclaration.getName())));
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
            packageMetadata = new PackageMetadata(JavaMetadataUtil.getName(unit.getPackage().getName()));
            if (!annotations.isEmpty()) {
                for (Object object : annotations) {
                    packageMetadata.getAnnotations().add(createAnnotationMetadataFor((Annotation) object));
                }
            }
        }
        return packageMetadata;
    }

    @SuppressWarnings( "unchecked" )
    protected AnnotationMetadata createAnnotationMetadataFor(Annotation annotation) {
        if (annotation instanceof NormalAnnotation) {
            NormalAnnotation normalAnnotation = (NormalAnnotation)annotation;

            Map<String, String> memberValues = new LinkedHashMap<String, String>();
            List<MemberValuePair> values = normalAnnotation.values();
            for (MemberValuePair pair : values) {
                memberValues.put(pair.getName().getIdentifier(), JavaMetadataUtil.expressionString(pair.getValue()));
            }

            return AnnotationMetadata.normalAnnotation(JavaMetadataUtil.getName(normalAnnotation.getTypeName()), memberValues);
        }

        if (annotation instanceof MarkerAnnotation) {
            MarkerAnnotation markerAnnotation = (MarkerAnnotation)annotation;
            return AnnotationMetadata.markerAnnotation(JavaMetadataUtil.getName(markerAnnotation.getTypeName()));
        }

        if (annotation instanceof SingleMemberAnnotation) {
            SingleMemberAnnotation singleMemberAnnotation = (SingleMemberAnnotation)annotation;
            return AnnotationMetadata.singleMemberAnnotation(JavaMetadataUtil.getName(singleMemberAnnotation.getTypeName()),
                                                             JavaMetadataUtil.expressionString(singleMemberAnnotation.getValue()));
        }

        return null;
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
                    TypeMetadata interfaceMetadata =  TypeMetadata.interfaceType(JavaMetadataUtil.getName(typeDeclaration.getName()));

                    // detect the interfaces, if any
                    for (Type superInterfaceType : (List<Type>)typeDeclaration.superInterfaceTypes()) {
                        interfaceMetadata.getInterfaceNames().add(getTypeName(superInterfaceType));
                    }

                    metadata.add(interfaceMetadata);
                } else {
                    // is a class top level type
                    TypeMetadata classMetadata = TypeMetadata.classType(JavaMetadataUtil.getName(typeDeclaration.getName()));
                    processModifiersOfTypeDeclaration(typeDeclaration, classMetadata);

                    // set the superclass, if any
                    Type superClassType = typeDeclaration.getSuperclassType();
                    if (superClassType != null) {
                        classMetadata.setSuperTypeName(getTypeName(superClassType));
                    }

                    // detect the interfaces, if any
                    for (Type superInterfaceType : (List<Type>)typeDeclaration.superInterfaceTypes()) {
                        classMetadata.getInterfaceNames().add(getTypeName(superInterfaceType));
                    }

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
                EnumDeclaration enumDeclaration = (EnumDeclaration)abstractTypeDeclaration;

                // is a class top level type
                EnumMetadata enumMetadata = TypeMetadata.enumType(JavaMetadataUtil.getName(enumDeclaration.getName()));
                processModifiersOfTypeDeclaration(enumDeclaration, enumMetadata);

                // Store the enum values
                List<EnumConstantDeclaration> enumValues = enumDeclaration.enumConstants();
                for (EnumConstantDeclaration enumValue : enumValues) {
                    enumMetadata.getValues().add(enumValue.getName().getIdentifier());
                }
                
                // Enums don't have superclasses
                
                // detect the interfaces, if any
                for (Type superInterfaceType : (List<Type>)enumDeclaration.superInterfaceTypes()) {
                    enumMetadata.getInterfaceNames().add(getTypeName(superInterfaceType));
                }

                /*
                 * It would be nice to be able to reuse the convenience methods from AbstractTypeDeclaration,
                 * but they don't exist in EnumDeclaration.  So we improvise!
                 */

                List<BodyDeclaration> bodyDecls = enumDeclaration.bodyDeclarations();
                for (BodyDeclaration bodyDecl : bodyDecls) {
                    if (bodyDecl instanceof FieldDeclaration) {
                        // fields of the class top level type
                        FieldMetadata fieldMetadata = getFieldMetadataFrom((FieldDeclaration) bodyDecl);
                        enumMetadata.getFields().add(fieldMetadata);
                    }
                    else if (bodyDecl instanceof MethodDeclaration){
                        // methods of the class top level type
                        MethodMetadata methodMetadata = getMethodMetadataFrom((MethodDeclaration) bodyDecl);
                        enumMetadata.getMethods().add(methodMetadata);
                    }
                }

                metadata.add(enumMetadata);
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
    protected void processModifiersOfTypeDeclaration( AbstractTypeDeclaration typeDeclaration, TypeMetadata classMetadata ) {
        List<IExtendedModifier> modifiers = typeDeclaration.modifiers();

        for (IExtendedModifier extendedModifier : modifiers) {           
            if (extendedModifier.isAnnotation()) {
                if (extendedModifier instanceof MarkerAnnotation) {
                    MarkerAnnotation marker = (MarkerAnnotation)extendedModifier;
                    classMetadata.getAnnotations().add(AnnotationMetadata.markerAnnotation(JavaMetadataUtil.getName(marker.getTypeName())));
                }
            } else {
                Modifier modifier = (Modifier)extendedModifier;
                classMetadata.getModifiers().add(new ModifierMetadata(modifier.getKeyword().toString()));
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
     * Get {@link MethodMetadata}
     * 
     * @param methodDeclaration
     * @return methodTypeMemberMetadata
     */
    @SuppressWarnings( "unchecked" )
    protected MethodMetadata getMethodTypeMemberMetadataFrom( MethodDeclaration methodDeclaration ) {
        MethodMetadata methodTypeMemberMetadata = MethodMetadata.methodMemberType(JavaMetadataUtil.getName(methodDeclaration.getName()));
        processReturnTypeOfMethodDeclaration(methodDeclaration, methodTypeMemberMetadata);
        processModifiersOfMethodDeclaration(methodDeclaration, methodTypeMemberMetadata);
        processParametersOfMethodDeclaration(methodDeclaration, methodTypeMemberMetadata);
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
            methodMetadata.setReturnType(FieldMetadata.primitiveType(((PrimitiveType)type).getPrimitiveTypeCode().toString()));
        }
        if (type.isSimpleType()) {            
            methodMetadata.setReturnType(FieldMetadata.simpleType(JavaMetadataUtil.getName(((SimpleType)type).getName())));
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
                FieldMetadata primitiveFieldMetadata = processVariableDeclaration(singleVariableDeclaration,                                                                                                                   type);
                methodMetadata.getParameters().add(primitiveFieldMetadata);
            }
            if (type.isParameterizedType()) {
                FieldMetadata parameterizedTypeFieldMetadata = processVariableDeclaration(singleVariableDeclaration,                                                                                                                                           type);
                methodMetadata.getParameters().add(parameterizedTypeFieldMetadata);
            }
            if (type.isQualifiedType()) {
                // TODO
            }
            if (type.isSimpleType()) {
                FieldMetadata simpleTypeFieldMetadata = processVariableDeclaration(singleVariableDeclaration, type);
                methodMetadata.getParameters().add(simpleTypeFieldMetadata);
            }
            if (type.isArrayType()) {
                FieldMetadata arrayTypeFieldMetadata = processVariableDeclaration(singleVariableDeclaration, type);
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
    private FieldMetadata processVariableDeclaration( SingleVariableDeclaration singleVariableDeclaration, Type type ) {

        Variable variable;
        if (type.isPrimitiveType()) {
            FieldMetadata primitiveFieldMetadata = FieldMetadata.primitiveType(((PrimitiveType)type).getPrimitiveTypeCode().toString());
            variable = new Variable();
            variable.setName(JavaMetadataUtil.getName(singleVariableDeclaration.getName()));
            primitiveFieldMetadata.setName(variable.getName());
            
            primitiveFieldMetadata.getVariables().add(variable);
            for (IExtendedModifier extendedModifier : (List<IExtendedModifier>)singleVariableDeclaration.modifiers()) {
                if (extendedModifier.isAnnotation()) {
                    primitiveFieldMetadata.getAnnotations().add(createAnnotationMetadataFor((Annotation)extendedModifier));
                } else {
                    Modifier modifier = (Modifier)extendedModifier;
                    primitiveFieldMetadata.getModifiers().add(new ModifierMetadata(modifier.getKeyword().toString()));
                }
            }
            return primitiveFieldMetadata;
        }
        if (type.isSimpleType()) {
            SimpleType simpleType = (SimpleType)type;
            FieldMetadata simpleTypeFieldMetadata = FieldMetadata.simpleType(JavaMetadataUtil.getName(simpleType.getName()));
            variable = new Variable();
            variable.setName(JavaMetadataUtil.getName(singleVariableDeclaration.getName()));
            simpleTypeFieldMetadata.setName(variable.getName());
            simpleTypeFieldMetadata.getVariables().add(variable);
            for (IExtendedModifier simpleTypeExtendedModifier : (List<IExtendedModifier>)singleVariableDeclaration.modifiers()) {
                if (simpleTypeExtendedModifier.isAnnotation()) {
                    simpleTypeFieldMetadata.getAnnotations().add(createAnnotationMetadataFor((Annotation)simpleTypeExtendedModifier));
                } else {
                    Modifier modifier = (Modifier)simpleTypeExtendedModifier;
                    simpleTypeFieldMetadata.getModifiers().add(new ModifierMetadata(modifier.getKeyword().toString()));
                }
            }
            return simpleTypeFieldMetadata;
        }
        if (type.isParameterizedType()) {
            ParameterizedType parameterizedType = (ParameterizedType)type;
            FieldMetadata parameterizedTypeFieldMetadata = FieldMetadata.parametrizedType(getTypeName(parameterizedType));

            variable = new Variable();
            variable.setName(JavaMetadataUtil.getName(singleVariableDeclaration.getName()));
            parameterizedTypeFieldMetadata.setName(variable.getName());
            parameterizedTypeFieldMetadata.getVariables().add(variable);
            for (IExtendedModifier parameterizedExtendedModifier : (List<IExtendedModifier>)singleVariableDeclaration.modifiers()) {
                if (parameterizedExtendedModifier.isAnnotation()) {
                    parameterizedTypeFieldMetadata.getAnnotations().add(createAnnotationMetadataFor((Annotation)parameterizedExtendedModifier));
                } else {
                    Modifier modifier = (Modifier)parameterizedExtendedModifier;
                    parameterizedTypeFieldMetadata.getModifiers().add(new ModifierMetadata(modifier.getKeyword().toString()));
                }
            }
            return parameterizedTypeFieldMetadata;
        }
        if (type.isArrayType()) {
            ArrayType arrayType = (ArrayType)type;
            FieldMetadata arrayTypeFieldMetadata = FieldMetadata.arrayType(getTypeName(arrayType));

            variable = new Variable();
            variable.setName(JavaMetadataUtil.getName(singleVariableDeclaration.getName()));
            arrayTypeFieldMetadata.setName(variable.getName());
            arrayTypeFieldMetadata.getVariables().add(variable);

            for (IExtendedModifier arrayTypeExtendedModifier : (List<IExtendedModifier>)singleVariableDeclaration.modifiers()) {
                if (arrayTypeExtendedModifier.isAnnotation()) {
                    arrayTypeFieldMetadata.getAnnotations().add(createAnnotationMetadataFor((Annotation)arrayTypeExtendedModifier));
                } else {
                    Modifier modifier = (Modifier)arrayTypeExtendedModifier;
                    arrayTypeFieldMetadata.getModifiers().add(new ModifierMetadata(modifier.getKeyword().toString()));
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
        if (type.isParameterizedType()) {
            ParameterizedType parameterizedType = (ParameterizedType)type;
            return getTypeName(parameterizedType.getType());
        }
        if (type.isArrayType()) {
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
     * Get {@link org.modeshape.sequencer.javafile.metadata.MethodMetadata}
     * 
     * @param methodDeclaration
     * @return constructorMetadata
     */
    protected MethodMetadata getConstructorMetadataFrom( MethodDeclaration methodDeclaration ) {
        MethodMetadata constructorMetadata = MethodMetadata.constructorType(JavaMetadataUtil.getName(methodDeclaration.getName()));
        // modifiers
        processModifiersOfMethodDeclaration(methodDeclaration, constructorMetadata);
        processParametersOfMethodDeclaration(methodDeclaration, constructorMetadata);
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
                return processPrimitiveType(fieldDeclaration);
            }
            // ParameterizedType
            if (type.isParameterizedType()) {
                return processParameterizedType(fieldDeclaration);
            }
            // SimpleType
            if (type.isSimpleType()) {
                return processSimpleType(fieldDeclaration);
            }
            // ArrayType
            if (type.isArrayType()) {
                return processArrayTypeFrom(fieldDeclaration);
            }
            // QualifiedType
            if (type.isQualifiedType()) {
                // TODO

            }
            // WildcardType
            if (type.isWildcardType()) {
                // TODO
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
    protected FieldMetadata processArrayTypeFrom( FieldDeclaration fieldDeclaration ) {
        ArrayType arrayType = (ArrayType)fieldDeclaration.getType();
        FieldMetadata arrayTypeFieldMetadata = null;
        // the element type is never an array type
        Type type = arrayType.getElementType();
        if (type.isPrimitiveType()) {
            PrimitiveType primitiveType = (PrimitiveType)type;
            arrayTypeFieldMetadata = FieldMetadata.arrayType(primitiveType.getPrimitiveTypeCode().toString());
            processModifiersAndVariablesOfFieldDeclaration(fieldDeclaration, arrayTypeFieldMetadata);
            arrayTypeFieldMetadata.setName(getFieldName(fieldDeclaration));
            return arrayTypeFieldMetadata;

        }
        // can't be an array type
        if (type.isSimpleType()) {
            SimpleType simpleType = (SimpleType)type;
            arrayTypeFieldMetadata = FieldMetadata.arrayType(JavaMetadataUtil.getName(simpleType.getName()));
            processModifiersAndVariablesOfFieldDeclaration(fieldDeclaration, arrayTypeFieldMetadata);
            arrayTypeFieldMetadata.setName(getFieldName(fieldDeclaration));
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
    private void processModifiersAndVariablesOfFieldDeclaration( FieldDeclaration fieldDeclaration, FieldMetadata arrayTypeFieldMetadata ) {
        processModifiersOfFieldDeclaration(fieldDeclaration, arrayTypeFieldMetadata);
        processVariablesOfVariableDeclarationFragment(fieldDeclaration, arrayTypeFieldMetadata);
    }

    /**
     * Process the simple type of a {@link FieldDeclaration}.
     * 
     * @param fieldDeclaration - the field declaration.
     * @return SimpleTypeFieldMetadata.
     */
    protected FieldMetadata processSimpleType( FieldDeclaration fieldDeclaration ) {
        SimpleType simpleType = (SimpleType)fieldDeclaration.getType();
        FieldMetadata simpleTypeFieldMetadata = FieldMetadata.simpleType(JavaMetadataUtil.getName(simpleType.getName()));
        // modifiers
        processModifiersOfFieldDeclaration(fieldDeclaration, simpleTypeFieldMetadata);
        processVariablesOfVariableDeclarationFragment(fieldDeclaration, simpleTypeFieldMetadata);
        simpleTypeFieldMetadata.setName(getFieldName(fieldDeclaration));

        return simpleTypeFieldMetadata;
    }

    /**
     * Process the parameterized type of a {@link FieldDeclaration}.
     * 
     * @param fieldDeclaration - the field declaration.
     * @return ParameterizedTypeFieldMetadata.
     */
    protected FieldMetadata processParameterizedType( FieldDeclaration fieldDeclaration ) {
        ParameterizedType parameterizedType = (ParameterizedType)fieldDeclaration.getType();
        Type typeOfParameterizedType = parameterizedType.getType(); // type may be a simple type or a qualified type.
        FieldMetadata referenceFieldMetadata = createParameterizedFieldMetadataFrom(typeOfParameterizedType);
        // modifiers
        processModifiersOfFieldDeclaration(fieldDeclaration, referenceFieldMetadata);
        // variables

        referenceFieldMetadata.setName(getFieldName(fieldDeclaration));

        processVariablesOfVariableDeclarationFragment(fieldDeclaration, referenceFieldMetadata);
        return referenceFieldMetadata;
    }

    protected String getFieldName( FieldDeclaration fieldDeclaration ) {
        FieldVisitor visitor = new FieldVisitor();
        fieldDeclaration.accept(visitor);

        return visitor.name;
    }

    /**
     * Process the primitive type of a {@link FieldDeclaration}.
     * 
     * @param fieldDeclaration - the field declaration.
     * @return PrimitiveFieldMetadata.
     */
    protected FieldMetadata processPrimitiveType( FieldDeclaration fieldDeclaration ) {
        PrimitiveType primitiveType = (PrimitiveType)fieldDeclaration.getType();
        FieldMetadata primitiveFieldMetadata = FieldMetadata.primitiveType(primitiveType.getPrimitiveTypeCode().toString());
        primitiveFieldMetadata.setName(getFieldName(fieldDeclaration));

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
            if (extendedModifier.isAnnotation()) {
                Annotation annotation = (Annotation) extendedModifier;
                fieldMetadata.getAnnotations().add(createAnnotationMetadataFor(annotation));
                
            } else {
                Modifier modifier = (Modifier)extendedModifier;
                ModifierMetadata modifierMetadata = new ModifierMetadata(modifier.getKeyword().toString());
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
            if (extendedModifier.isAnnotation()) {
                Annotation annotation = (Annotation) extendedModifier;
                methodMetadata.getAnnotations().add(createAnnotationMetadataFor(annotation));
            } else {
                Modifier modifier = (Modifier)extendedModifier;
                methodMetadata.getModifiers().add(new ModifierMetadata(modifier.getKeyword().toString()));
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
        FieldMetadata parameterizedTypeFieldMetadata = null;
        if (type.isSimpleType()) {
            SimpleType simpleType = (SimpleType)type;
            parameterizedTypeFieldMetadata = FieldMetadata.parametrizedType(JavaMetadataUtil.getName(simpleType.getName()));
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

    class FieldVisitor extends ASTVisitor {
        String name;

        @Override
        public boolean visit( VariableDeclarationFragment node ) {
            this.name = node.getName().getFullyQualifiedName();

            return super.visit(node);
        }

    }

}
