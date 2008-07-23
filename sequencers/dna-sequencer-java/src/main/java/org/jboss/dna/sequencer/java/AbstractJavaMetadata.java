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
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
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
import org.jboss.dna.sequencer.java.metadata.NormalAnnotationMetadata;
import org.jboss.dna.sequencer.java.metadata.PackageMetadata;
import org.jboss.dna.sequencer.java.metadata.ParameterizedFieldMetadata;
import org.jboss.dna.sequencer.java.metadata.PrimitiveFieldMetadata;
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
                    classMetadata.setName(JavaMetadataUtil.getName(typeDeclaration.getName()));
                    List modifiers = typeDeclaration.modifiers();
                    for (Object object : modifiers) {
                        if (object instanceof Modifier) {
                            Modifier modifier = (Modifier)object;
                            if (modifier.isPublic()) {
                                classMetadata.getModifiers().put(TypeMetadata.PUBLIC_MODIFIER, modifier.getKeyword().toString());
                            }
                        }
                        if (object instanceof MarkerAnnotation) {
                            MarkerAnnotation marker = (MarkerAnnotation)object;
                            MarkerAnnotationMetadata markerAnnotationMetadata = new MarkerAnnotationMetadata();
                            markerAnnotationMetadata.setName(JavaMetadataUtil.getName(marker.getTypeName()));
                            classMetadata.getAnnotations().add(markerAnnotationMetadata);
                        }
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
                        MethodMetadata methodMetadata = getFieldMetadataFrom(methodDeclaration);
                        classMetadata.getMethods().add(methodMetadata);
                    }
                    metadata.add(classMetadata);
                }
            }

            // process EnumDeclaration
            if (abstractTypeDeclaration instanceof EnumDeclaration) {
                EnumDeclaration enumDeclaration = (EnumDeclaration)abstractTypeDeclaration;
                // TODO get infos from enum declaration and create a enum meta data object.
            }

            // process annotationTypeDeclaration
            if (abstractTypeDeclaration instanceof AnnotationTypeDeclaration) {
                AnnotationTypeDeclaration annotationTypeDeclaration = (AnnotationTypeDeclaration)abstractTypeDeclaration;
                // TODO get infos from annotation type declaration and create a annotation meta data object.
            }
        }
        return metadata;
    }

    /**
     * Gets a method meta data from {@link MethodDeclaration}.
     * 
     * @param methodDeclaration - the MethodDeclaration.
     * @return methodMetadata - the method meta data.
     */
    private MethodMetadata getFieldMetadataFrom( MethodDeclaration methodDeclaration ) {
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
    private MethodMetadata getMethodTypeMemberMetadataFrom( MethodDeclaration methodDeclaration ) {
        MethodTypeMemberMetadata methodTypeMemberMetadata = new MethodTypeMemberMetadata();
        methodTypeMemberMetadata.setName(JavaMetadataUtil.getName(methodDeclaration.getName()));
        return methodTypeMemberMetadata;
    }

    /**
     * Get {@link ConstructorMetadata}
     * 
     * @param methodDeclaration
     * @return constructorMetadata
     */
    private MethodMetadata getConstructorMetadataFrom( MethodDeclaration methodDeclaration ) {
        ConstructorMetadata constructorMetadata = new ConstructorMetadata();
        constructorMetadata.setName(JavaMetadataUtil.getName(methodDeclaration.getName()));
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
            List<VariableDeclarationFragment> fragments = null;
            // type
            Type type = fieldDeclaration.getType();
            // Primitive type
            if (type instanceof PrimitiveType) {
                PrimitiveType primitiveType = (PrimitiveType)type;
                PrimitiveFieldMetadata primitiveFieldMetadata = new PrimitiveFieldMetadata();
                primitiveFieldMetadata.setCode(primitiveType.getPrimitiveTypeCode().toString());
                // variables
                fragments = fieldDeclaration.fragments();
                getAllVariablesOf(fragments, primitiveFieldMetadata);
                return primitiveFieldMetadata;
            }
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType)type;
                ParameterizedFieldMetadata parameterizedFieldMetadata = new ParameterizedFieldMetadata();
                Type typeOfParameterizedType = parameterizedType.getType(); // type may be a simple type or a qualified type.
                if (typeOfParameterizedType instanceof SimpleType) {
                    SimpleType simpleType = (SimpleType)typeOfParameterizedType;
                    parameterizedFieldMetadata.setName(JavaMetadataUtil.getName(simpleType.getName()));
                }
                if (typeOfParameterizedType instanceof QualifiedType) {
                    QualifiedType qualifiedType = (QualifiedType)typeOfParameterizedType;
                    parameterizedFieldMetadata.setName(JavaMetadataUtil.getName(qualifiedType.getName()));
                    Type qualifier = qualifiedType.getQualifier();
                    if (qualifier instanceof ParameterizedType) {
                        // TODO
                    }
                }
                // variables
                fragments = fieldDeclaration.fragments();
                getAllVariablesOf(fragments, parameterizedFieldMetadata);
                return parameterizedFieldMetadata;
            }
            if (type instanceof SimpleType) {
                SimpleType simpleType = (SimpleType)type;
                SimpleFieldMetadata simpleFieldMetadata = new SimpleFieldMetadata();
                simpleFieldMetadata.setName(JavaMetadataUtil.getName(simpleType.getName()));
                fragments = fieldDeclaration.fragments();
                getAllVariablesOf(fragments, simpleFieldMetadata);
                return simpleFieldMetadata;
            }
        }
        return null;
    }

    /**
     * Get all variables of a fragment.
     * 
     * @param fragments
     * @param fieldMetadata
     */
    private void getAllVariablesOf( List<VariableDeclarationFragment> fragments,
                                    FieldMetadata fieldMetadata ) {
        for (VariableDeclarationFragment fragment : fragments) {
            fieldMetadata.getVariables().add(new Variable(JavaMetadataUtil.getName(fragment.getName())));
        }
    }
}
