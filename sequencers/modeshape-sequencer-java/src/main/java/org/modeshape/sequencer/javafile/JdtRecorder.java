/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.sequencer.javafile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WildcardType;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.jcr.api.sequencer.Sequencer.Context;
import org.modeshape.sequencer.classfile.ClassFileSequencerLexicon;
import org.modeshape.sequencer.classfile.metadata.Visibility;

/**
 * An Eclipse JDT DOM to JCR node recorder.
 *
 * <pre>
 * CompilationUnit:
 *     [ PackageDeclaration ]
 *         { ImportDeclaration }
 *         { TypeDeclaration | EnumDeclaration | AnnotationTypeDeclaration | ; }
 *
 * PackageDeclaration:
 *     [ Javadoc ] { Annotation } package Name ;
 *
 * ImportDeclaration:
 *     import [ static ] Name [ . * ] ;
 *
 * TypeDeclaration:
 *     ClassDeclaration
 *     InterfaceDeclaration
 *
 * ClassDeclaration:
 *     [ Javadoc ] { ExtendedModifier } class Identifier
 *         [ < TypeParameter { , TypeParameter } > ]
 *         [ extends Type ]
 *         [ implements Type { , Type } ]
 *         { { ClassBodyDeclaration | ; } }
 *
 * InterfaceDeclaration:
 *     [ Javadoc ] { ExtendedModifier } interface Identifier
 *         [ < TypeParameter { , TypeParameter } > ]
 *         [ extends Type { , Type } ]
 *         { { InterfaceBodyDeclaration | ; } }
 * </pre>
 */
public class JdtRecorder implements SourceFileRecorder {

    private static final Logger LOGGER = Logger.getLogger(JdtRecorder.class);

    private CompilationUnit compilationUnit;
    private Sequencer.Context context;
    private String sourceCode;

    protected String getSourceCode( final int startPosition,
                                    final int length ) {
        if (StringUtil.isBlank(this.sourceCode)) {
            return null;
        }

        return this.sourceCode.substring(startPosition, (startPosition + length));
    }

    protected String getTypeName( final Type type ) {
        if (type.isPrimitiveType()) {
            final PrimitiveType primitiveType = (PrimitiveType)type;
            return primitiveType.getPrimitiveTypeCode().toString();
        }

        if (type.isSimpleType()) {
            final SimpleType simpleType = (SimpleType)type;
            return simpleType.getName().getFullyQualifiedName();
        }

        if (type.isQualifiedType()) {
            final QualifiedType qualifiedType = (QualifiedType)type;
            return qualifiedType.getName().getFullyQualifiedName();
        }

        if (type.isParameterizedType()) {
            final ParameterizedType parameterizedType = (ParameterizedType)type;
            final StringBuilder result = new StringBuilder(getTypeName(parameterizedType.getType()));
            result.append('<');

            if (!parameterizedType.typeArguments().isEmpty()) {
                @SuppressWarnings( "unchecked" )
                final List<ParameterizedType> paramTypes = parameterizedType.typeArguments();
                boolean firstTime = true;

                for (final Type paramType : paramTypes) {
                    if (firstTime) {
                        firstTime = false;
                    } else {
                        result.append(", ");
                    }

                    result.append(getTypeName(paramType));
                }
            }

            result.append('>');
            return result.toString();
        }

        if (type.isArrayType()) {
            final ArrayType arrayType = (ArrayType)type;
            final Type elementType = arrayType.getElementType(); // the element type is never an array type

            if (elementType.isPrimitiveType()) {
                return ((PrimitiveType)elementType).getPrimitiveTypeCode().toString();

            }

            // can't be an array type
            if (elementType.isSimpleType()) {
                return ((SimpleType)elementType).getName().getFullyQualifiedName();
            }
        }

        if (type.isWildcardType()) {
            return "?";
        }

        return null;
    }

    protected String getVisibility( final int modifiers ) {
        if ((modifiers & Modifier.PUBLIC) != 0) {
            return Visibility.PUBLIC.getDescription();
        }

        if ((modifiers & Modifier.PROTECTED) != 0) {
            return Visibility.PROTECTED.getDescription();
        }

        if ((modifiers & Modifier.PRIVATE) != 0) {
            return Visibility.PRIVATE.getDescription();
        }

        return Visibility.PACKAGE.getDescription();
    }

    /**
     * <pre>
     * Annotation:
     *     NormalAnnotation
     *     MarkerAnnotation
     *     SingleMemberAnnotation
     *
     * NormalAnnotation:
     *     \@ TypeName ( [ MemberValuePair { , MemberValuePair } ] )
     *
     * MarkerAnnotation:
     *     \@ TypeName
     *
     * SingleMemberAnnotation:
     *     \@ TypeName ( Expression  )
     *
     * MemberValuePair:
     *     SimpleName = Expression
     *
     * </pre>
     *
     * @param annotation the {@link Annotation annotation} being recorded (cannot be <code>null</code>)
     * @param parentNode the parent {@link Node node} where the annotation will be created (cannot be <code>null</code>)
     * @throws Exception if there is a problem
     */
    protected void record( final Annotation annotation,
                           final Node parentNode ) throws Exception {
        final String name = annotation.getTypeName().getFullyQualifiedName();

        final Node annotationNode = parentNode.addNode(name, ClassFileSequencerLexicon.ANNOTATION);
        annotationNode.setProperty(ClassFileSequencerLexicon.NAME, name);

        if (annotation.isMarkerAnnotation()) {
            annotationNode.setProperty(ClassFileSequencerLexicon.ANNOTATION_TYPE,
                                       ClassFileSequencerLexicon.AnnotationType.MARKER.toString());
            LOGGER.debug("Marker annotation {0} created", name);
        } else if (annotation.isNormalAnnotation()) {
            annotationNode.setProperty(ClassFileSequencerLexicon.ANNOTATION_TYPE,
                                       ClassFileSequencerLexicon.AnnotationType.NORMAL.toString());
            @SuppressWarnings( "unchecked" )
            final List<MemberValuePair> entries = ((NormalAnnotation)annotation).values();

            if ((entries != null) && !entries.isEmpty()) {
                for (final MemberValuePair entry : entries) {
                    final String memberName = entry.getName().getFullyQualifiedName();
                    final Expression expression = entry.getValue();
                    recordAnnotationMember(memberName, expression, annotationNode);
                }
            }

            LOGGER.debug("Normal annotation {0} created", name);
        } else if (annotation.isSingleMemberAnnotation()) {
            annotationNode.setProperty(ClassFileSequencerLexicon.ANNOTATION_TYPE,
                                       ClassFileSequencerLexicon.AnnotationType.SINGLE_MEMBER.toString());
            final Expression expression = ((SingleMemberAnnotation)annotation).getValue();
            recordAnnotationMember(null, expression, annotationNode);
            LOGGER.debug("Single member annotation {0} created", name);
        } else {
            assert false;
            LOGGER.error(JavaFileI18n.unhandledAnnotationType, annotation.getClass().getName(), parentNode.getName());
        }

        recordSourceReference(annotation, annotationNode);
    }

    /**
     * <pre>
     * AnnotationTypeDeclaration:
     *     [ Javadoc ] { ExtendedModifier } @ interface Identifier
     *          { { AnnotationTypeBodyDeclaration | ; } }
     *
     * AnnotationTypeBodyDeclaration:
     *      AnnotationTypeMemberDeclaration
     *      FieldDeclaration
     *      TypeDeclaration
     *      EnumDeclaration
     *      AnnotationTypeDeclaration
     *
     * AnnotationTypeMemberDeclaration:
     *     [ Javadoc ] { ExtendedModifier }
     *         Type Identifier ( ) [ default Expression ] ;
     * </pre>
     *
     * @param annotationType the {@link AnnotationTypeDeclaration annotation type} being recorded (cannot be <code>null</code>)
     * @param outputNode the output node where the annotation type should be created (cannot be <code>null</code>)
     * @return the node representing the annotation type (never <code>null</code>)
     * @throws Exception if there is a problem
     */
    protected Node record( final AnnotationTypeDeclaration annotationType,
                           final Node outputNode ) throws Exception {
        final String name = annotationType.getName().getFullyQualifiedName();
        final Node annotationTypeNode = outputNode.addNode(name, ClassFileSequencerLexicon.ANNOTATION_TYPE);
        annotationTypeNode.setProperty(ClassFileSequencerLexicon.NAME, name);
        annotationTypeNode.setProperty(ClassFileSequencerLexicon.VISIBILITY, getVisibility(annotationType.getModifiers()));
        annotationTypeNode.setProperty(ClassFileSequencerLexicon.SEQUENCED_DATE, this.context.getTimestamp());

        { // javadocs
            final Javadoc javadoc = annotationType.getJavadoc();

            if (javadoc != null) {
                record(javadoc, annotationTypeNode);
            }
        }

        { // annotations
            @SuppressWarnings( "unchecked" )
            final List<IExtendedModifier> modifiers = annotationType.modifiers();
            recordAnnotations(modifiers, annotationTypeNode);
        }

        { // body
            @SuppressWarnings( "unchecked" )
            final List<BodyDeclaration> body = annotationType.bodyDeclarations();

            if ((body != null) && !body.isEmpty()) {
                Node fieldsNode = null;
                Node membersNode = null;
                Node nestedTypesNode = null;

                for (final BodyDeclaration declaration : body) {
                    if (declaration instanceof AnnotationTypeMemberDeclaration) {
                        if (membersNode == null) {
                            membersNode = annotationTypeNode.addNode(ClassFileSequencerLexicon.ANNOTATION_TYPE_MEMBERS,
                                                                     ClassFileSequencerLexicon.ANNOTATION_TYPE_MEMBERS);
                        }

                        record((AnnotationTypeMemberDeclaration)declaration, membersNode);
                    } else if (declaration instanceof FieldDeclaration) {
                        if (fieldsNode == null) {
                            fieldsNode = annotationTypeNode.addNode(ClassFileSequencerLexicon.FIELDS,
                                                                    ClassFileSequencerLexicon.FIELDS);
                        }

                        record((FieldDeclaration)declaration, fieldsNode);
                    } else if (declaration instanceof AbstractTypeDeclaration) {
                        if (nestedTypesNode == null) {
                            nestedTypesNode = annotationTypeNode.addNode(ClassFileSequencerLexicon.NESTED_TYPES,
                                                                         ClassFileSequencerLexicon.NESTED_TYPES);
                        }

                        if (declaration instanceof TypeDeclaration) {
                            record((TypeDeclaration)declaration, nestedTypesNode);
                        } else if (declaration instanceof EnumDeclaration) {
                            record((EnumDeclaration)declaration, nestedTypesNode);
                        } else if (declaration instanceof AnnotationTypeDeclaration) {
                            record((AnnotationTypeDeclaration)declaration, nestedTypesNode);
                        } else {
                            assert false;
                            LOGGER.error(JavaFileI18n.unhandledAnnotationTypeBodyDeclarationType,
                                         declaration.getClass().getName(),
                                         annotationType.getName());
                        }
                    } else {
                        assert false;
                        LOGGER.error(JavaFileI18n.unhandledAnnotationTypeBodyDeclarationType,
                                     declaration.getClass().getName(),
                                     annotationType.getName());
                    }
                }
            }
        }

        recordSourceReference(annotationType, annotationTypeNode);
        return annotationTypeNode;
    }

    /**
     * <pre>
     * AnnotationTypeMemberDeclaration:
     *     [ Javadoc ] { ExtendedModifier }
     *         Type Identifier ( ) [ default Expression ] ;
     * </pre>
     *
     * @param annotationTypeMember the {@link AnnotationTypeMemberDeclaration annotation type member} being recorded (cannot be
     *        <code>null</code>)
     * @param parentNode the parent {@link Node node} where the annotation type members will be added (cannot be <code>null</code>
     *        )
     * @throws Exception if there is a problem
     */
    protected void record( final AnnotationTypeMemberDeclaration annotationTypeMember,
                           final Node parentNode ) throws Exception {
        final Node memberNode = parentNode.addNode(ClassFileSequencerLexicon.ANNOTATION_TYPE_MEMBER,
                                                   ClassFileSequencerLexicon.ANNOTATION_TYPE_MEMBER);
        memberNode.setProperty(ClassFileSequencerLexicon.NAME, annotationTypeMember.getName().getFullyQualifiedName());

        { // modifiers
            final int modifiers = annotationTypeMember.getModifiers();
            memberNode.setProperty(ClassFileSequencerLexicon.ABSTRACT, (modifiers & Modifier.ABSTRACT) != 0);
            memberNode.setProperty(ClassFileSequencerLexicon.VISIBILITY, getVisibility(modifiers));
        }

        { // javadocs
            final Javadoc javadoc = annotationTypeMember.getJavadoc();

            if (javadoc != null) {
                record(javadoc, memberNode);
            }
        }

        { // annotations
            @SuppressWarnings( "unchecked" )
            final List<IExtendedModifier> modifiers = annotationTypeMember.modifiers();
            recordAnnotations(modifiers, memberNode);
        }

        { // type
            final Type type = annotationTypeMember.getType();
            record(type, ClassFileSequencerLexicon.TYPE, memberNode);
        }

        { // default expression
            final Expression expression = annotationTypeMember.getDefault();

            if (expression != null) {
                recordExpression(expression, ClassFileSequencerLexicon.DEFAULT, memberNode);
            }
        }
    }

    /**
     * <pre>
     * Block:
     *     { { Statement } }
     * </pre>
     *
     * @param block the {@link Block block} being recorded (cannot be <code>null</code>)
     * @param blockNode the parent {@link Node node} where the statements will be added (cannot be <code>null</code>)
     * @throws Exception if there is a problem
     */
    protected void record( final Block block,
                           final Node blockNode ) throws Exception {
        if (block != null) {
            @SuppressWarnings( "unchecked" )
            final List<Statement> statements = block.statements();

            if ((statements != null) && !statements.isEmpty()) {
                for (final Statement statement : statements) {
                    // TODO handle each type of statement
                    final Node stmtNode = blockNode.addNode(ClassFileSequencerLexicon.STATEMENT,
                                                            ClassFileSequencerLexicon.STATEMENT);
                    stmtNode.setProperty(ClassFileSequencerLexicon.CONTENT, statement.toString());
                    recordSourceReference(statement, stmtNode);
                }
            }
        }
    }

    /**
     * <pre>
     * Comment:
     *     LineComment
     *     BlockComment
     *     Javadoc
     * </pre>
     *
     * @param comment the comment being recorded (cannot be <code>null</code>)
     * @param parentNode the {@link Node parent node} (cannot be <code>null</code>)
     * @throws Exception if there is a problem
     */
    protected void record( final Comment comment,
                           final Node parentNode ) throws Exception {
        Node commentNode = null;
        String commentType = null;

        if (comment.isDocComment()) {
            commentNode = parentNode.addNode(ClassFileSequencerLexicon.JAVADOC, ClassFileSequencerLexicon.JAVADOC);
        } else {
            commentNode = parentNode.addNode(ClassFileSequencerLexicon.COMMENT, ClassFileSequencerLexicon.COMMENT);

            if (comment.isBlockComment()) {
                commentType = ClassFileSequencerLexicon.CommentType.BLOCK.toString();
            } else if (comment.isLineComment()) {
                commentType = ClassFileSequencerLexicon.CommentType.LINE.toString();
            } else {
                assert false;
                LOGGER.error(JavaFileI18n.unhandledCommentType, comment.getClass().getName());
            }

            commentNode.setProperty(ClassFileSequencerLexicon.COMMENT_TYPE, commentType);
        }

        final String code = getSourceCode(comment.getStartPosition(), comment.getLength());

        if (!StringUtil.isBlank(code)) {
            commentNode.setProperty(ClassFileSequencerLexicon.COMMENT, code);
        }

        recordSourceReference(comment, commentNode);
    }

    /**
     * <pre>
     * CompilationUnit:
     *     [ PackageDeclaration ]
     *          { ImportDeclaration }
     *          { TypeDeclaration | EnumDeclaration | AnnotationTypeDeclaration | ; }
     * </pre>
     *
     * @param unit the {@link CompilationUnit compilation unit} being recorded (cannot be <code>null</code>)
     * @param compilationUnitNode the output node associated with the compilation unit (cannot be <code>null</code>)
     * @throws Exception if there is a problem
     */
    protected void record( final CompilationUnit unit,
                           final Node compilationUnitNode ) throws Exception {
        LOGGER.debug("recording unit comments");
        recordComments(unit, compilationUnitNode);

        LOGGER.debug("recording unit import nodes");
        recordImports(unit, compilationUnitNode);

        LOGGER.debug("recording unit compiler messages");
        recordCompilerMessages(unit, compilationUnitNode);

        LOGGER.debug("recording unit package nodes");
        final Node pkgNode = recordPackage(unit, compilationUnitNode);

        LOGGER.debug("recording unit type nodes");
        recordTypes(unit, compilationUnitNode, pkgNode);

        LOGGER.debug("recording unit source reference");
        recordSourceReference(unit, compilationUnitNode);

        compilationUnitNode.setProperty(ClassFileSequencerLexicon.SEQUENCED_DATE, this.context.getTimestamp());
    }

    /**
     * {@inheritDoc}
     *
     * @see org.modeshape.sequencer.javafile.SourceFileRecorder#record(org.modeshape.jcr.api.sequencer.Sequencer.Context,
     *      java.io.InputStream, long, java.lang.String, javax.jcr.Node)
     */
    @Override
    public void record( final Context context,
                        final InputStream inputStream,
                        final long length,
                        final String encoding,
                        final Node outputNode ) throws Exception {
        final char[] sourceCode = JavaMetadataUtil.getJavaSourceFromTheInputStream(inputStream, length, encoding);
        record(context, sourceCode, outputNode);
    }

    /**
     * <pre>
     * EnumConstantDeclaration:
     *     [ Javadoc ] { ExtendedModifier } Identifier
     *         [ ( [ Expression { , Expression } ] ) ]
     *         [ AnonymousClassDeclaration ]
     * </pre>
     *
     * @param enumConstant the enum constant being processed (cannot be <code>null</code>)
     * @param parentNode the {@link Node node} where the enum constant node will be created (cannot be <code>null</code>)
     * @throws Exception if there is a problem
     */
    protected void record( final EnumConstantDeclaration enumConstant,
                           final Node parentNode ) throws Exception {
        final String name = enumConstant.getName().getIdentifier();
        final Node constantNode = parentNode.addNode(name, ClassFileSequencerLexicon.ENUM_CONSTANT);

        { // javadocs
            final Javadoc javadoc = enumConstant.getJavadoc();

            if (javadoc != null) {
                record(javadoc, constantNode);
            }
        }

        { // no modifiers but can have annotations
            @SuppressWarnings( "unchecked" )
            final List<IExtendedModifier> modifiers = enumConstant.modifiers();
            recordAnnotations(modifiers, constantNode);
        }

        { // args
            @SuppressWarnings( "unchecked" )
            final List<Expression> args = enumConstant.arguments();

            if ((args != null) && !args.isEmpty()) {
                final Node containerNode = constantNode.addNode(ClassFileSequencerLexicon.ARGUMENTS,
                                                                ClassFileSequencerLexicon.ARGUMENTS);

                for (final Expression arg : args) {
                    recordExpression(arg, ClassFileSequencerLexicon.ARGUMENT, containerNode);
                }
            }
        }

        // anonymous classes
        final AnonymousClassDeclaration acd = enumConstant.getAnonymousClassDeclaration();

        if (acd != null) {
            recordBodyDeclarations(acd, constantNode);
        }

        recordSourceReference(enumConstant, constantNode);
    }

    /**
     * <pre>
     * EnumDeclaration:
     * [ Javadoc ] { ExtendedModifier } enum Identifier
     *     [ implements Type { , Type } ]
     *     {
     *     [ EnumConstantDeclaration { , EnumConstantDeclaration } ] [ , ]
     *     [ ; { ClassBodyDeclaration | ; } ]
     *     }
     * </pre>
     *
     * @param enumType the {@link EnumDeclaration enum} being recorded (cannot be <code>null</code>)
     * @param parentNode the parent {@link Node node} (cannot be <code>null</code>)
     * @return the node representing the enum being recorded (never <code>null</code>)
     * @throws Exception if there is a problem
     */
    protected Node record( final EnumDeclaration enumType,
                           final Node parentNode ) throws Exception {
        final String name = enumType.getName().getFullyQualifiedName();
        final Node enumNode = parentNode.addNode(name, ClassFileSequencerLexicon.ENUM);
        enumNode.setProperty(ClassFileSequencerLexicon.NAME, name);
        enumNode.setProperty(ClassFileSequencerLexicon.SEQUENCED_DATE, this.context.getTimestamp());
        enumNode.setProperty(ClassFileSequencerLexicon.INTERFACE, false);

        { // javadocs
            final Javadoc javadoc = enumType.getJavadoc();

            if (javadoc != null) {
                record(javadoc, enumNode);
            }
        }

        { // modifiers
            final int modifiers = enumType.getModifiers();

            enumNode.setProperty(ClassFileSequencerLexicon.ABSTRACT, (modifiers & Modifier.ABSTRACT) != 0);
            enumNode.setProperty(ClassFileSequencerLexicon.FINAL, (modifiers & Modifier.FINAL) != 0);
            enumNode.setProperty(ClassFileSequencerLexicon.STATIC, (modifiers & Modifier.STATIC) != 0);
            enumNode.setProperty(ClassFileSequencerLexicon.STRICT_FP, (modifiers & Modifier.STRICTFP) != 0);
            enumNode.setProperty(ClassFileSequencerLexicon.VISIBILITY, getVisibility(modifiers));
        }

        { // annotations
            @SuppressWarnings( "unchecked" )
            final List<IExtendedModifier> modifiers = enumType.modifiers();
            recordAnnotations(modifiers, enumNode);
        }

        { // implements
            @SuppressWarnings( "unchecked" )
            final List<Type> interfaces = enumType.superInterfaceTypes();

            if ((interfaces != null) && !interfaces.isEmpty()) {
                final String[] interfaceNames = new String[interfaces.size()];
                final Node containerNode = enumNode.addNode(ClassFileSequencerLexicon.IMPLEMENTS, ClassFileSequencerLexicon.TYPES);
                int i = 0;

                for (final Type superInterfaceType : interfaces) {
                    interfaceNames[i] = getTypeName(superInterfaceType);
                    record(superInterfaceType, ClassFileSequencerLexicon.INTERFACE, containerNode);
                    ++i;
                }

                enumNode.setProperty(ClassFileSequencerLexicon.INTERFACES, interfaceNames);
            }
        }

        { // enum constants
            @SuppressWarnings( "unchecked" )
            final List<EnumConstantDeclaration> enumValues = enumType.enumConstants();

            if ((enumValues != null) && !enumValues.isEmpty()) {
                final String[] values = new String[enumValues.size()];
                final Node containerNode = enumNode.addNode(ClassFileSequencerLexicon.ENUM_CONSTANTS,
                                                            ClassFileSequencerLexicon.ENUM_CONSTANTS);
                int i = 0;

                for (final EnumConstantDeclaration enumConstant : enumValues) {
                    values[i++] = enumConstant.getName().getFullyQualifiedName();
                    record(enumConstant, containerNode);
                }

                enumNode.setProperty(ClassFileSequencerLexicon.ENUM_VALUES, values);
            }
        }

        recordBodyDeclarations(enumType, enumNode);
        recordSourceReference(enumType, enumNode);
        return enumNode;
    }

    /**
     * <pre>
     * FieldDeclaration:
     *      [Javadoc] { ExtendedModifier } Type VariableDeclarationFragment
     *           { , VariableDeclarationFragment } ;
     *
     * VariableDeclarationFragment:
     *     Identifier { [] } [ = Expression ]
     * </pre>
     *
     * A field container node will be created if one does not exist under <code>node</node>.
     *
     * @param field the {@link FieldDeclaration field} being recorded {cannot be <code>null</code>
     * @param parentNode the parent {@link Node node} (cannot be <code>null</code>)
     * @throws Exception if there is a problem
     */
    protected void record( final FieldDeclaration field,
                           final Node parentNode ) throws Exception {
        @SuppressWarnings( "unchecked" )
        final List<VariableDeclarationFragment> frags = field.fragments();
        final String name = frags.get(0).getName().getFullyQualifiedName();

        final Node fieldNode = parentNode.addNode(name, ClassFileSequencerLexicon.FIELD);
        fieldNode.setProperty(ClassFileSequencerLexicon.NAME, name);

        { // javadocs
            final Javadoc javadoc = field.getJavadoc();

            if (javadoc != null) {
                record(javadoc, fieldNode);
            }
        }

        { // type
            final Type type = field.getType();
            final String typeName = getTypeName(type);
            fieldNode.setProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME, typeName);
            record(type, ClassFileSequencerLexicon.TYPE, fieldNode);
        }

        { // modifiers
            final int modifiers = field.getModifiers();

            fieldNode.setProperty(ClassFileSequencerLexicon.FINAL, (modifiers & Modifier.FINAL) != 0);
            fieldNode.setProperty(ClassFileSequencerLexicon.STATIC, (modifiers & Modifier.STATIC) != 0);
            fieldNode.setProperty(ClassFileSequencerLexicon.TRANSIENT, (modifiers & Modifier.TRANSIENT) != 0);
            fieldNode.setProperty(ClassFileSequencerLexicon.VISIBILITY, getVisibility(modifiers));
            fieldNode.setProperty(ClassFileSequencerLexicon.VOLATILE, (modifiers & Modifier.VOLATILE) != 0);
        }

        { // annotations
            @SuppressWarnings( "unchecked" )
            final List<IExtendedModifier> modifiers = field.modifiers();
            recordAnnotations(modifiers, fieldNode);
        }

        { // fragments
            @SuppressWarnings( "unchecked" )
            final List<VariableDeclarationFragment> fragments = field.fragments();

            if ((fragments != null) && !fragments.isEmpty()) {
                for (final VariableDeclarationFragment var : fragments) {
                    final Expression initializer = var.getInitializer();

                    if (initializer != null) {
                        recordExpression(initializer, ClassFileSequencerLexicon.INITIALIZER, fieldNode);
                    }
                }
            }
        }

        recordSourceReference(field, fieldNode);
    }

    /**
     * <pre>
     * Initializer:
     *     [ static ] Block
     *
     * Block:
     *     { { Statement } }
     * </pre>
     *
     * @param initializer the {@link Initializer initializer} being recorded (cannot be <code>null</code>)
     * @param nodeName the name of the node being created that represents the initializer (cannot be <code>null</code> or empty)
     * @param parentNode the parent {@link Node node} (cannot be <code>null</code>)
     * @throws Exception if there is a problem
     */
    protected void record( final Initializer initializer,
                           final String nodeName,
                           final Node parentNode ) throws Exception {
        final Block block = initializer.getBody();

        if (block != null) {
            @SuppressWarnings( "unchecked" )
            final List<Statement> statements = block.statements();

            if ((statements != null) && !statements.isEmpty()) {
                final Node initializerNode = parentNode.addNode(nodeName, ClassFileSequencerLexicon.STATEMENTS);
                record(block, initializerNode);
            }
        }
    }

    /**
     * <pre>
     * MethodDeclaration:
     *     [ Javadoc ] { ExtendedModifier }
     *          [ < TypeParameter { , TypeParameter } > ]
     *     ( Type | void ) Identifier (
     *     [ FormalParameter
     *          { , FormalParameter } ] ) {[ ] }
     *     [ throws TypeName { , TypeName } ] ( Block | ; )
     *
     * ConstructorDeclaration:
     *     [ Javadoc ] { ExtendedModifier }
     *          [ < TypeParameter { , TypeParameter } > ]
     *     Identifier (
     *         [ FormalParameter
     *             { , FormalParameter } ] )
     *     [throws TypeName { , TypeName } ] Block
     *
     * </pre>
     *
     * @param method the {@link MethodDeclaration method} being recorded (cannot be <code>null</code>)
     * @param parentNode the parent {@link Node node} (cannot be <code>null</code>)
     * @throws Exception if there is a problem
     */
    protected void record( final MethodDeclaration method,
                           final Node parentNode ) throws Exception {
        final String name = method.getName().getFullyQualifiedName();
        final Node methodNode = parentNode.addNode(name, ClassFileSequencerLexicon.METHOD);
        methodNode.setProperty(ClassFileSequencerLexicon.NAME, name);

        { // javadocs
            final Javadoc javadoc = method.getJavadoc();

            if (javadoc != null) {
                record(javadoc, methodNode);
            }
        }

        { // type parameters
            @SuppressWarnings( "unchecked" )
            final List<TypeParameter> typeParams = method.typeParameters();

            if ((typeParams != null) && !typeParams.isEmpty()) {
                final Node containerNode = methodNode.addNode(ClassFileSequencerLexicon.TYPE_PARAMETERS,
                                                              ClassFileSequencerLexicon.TYPE_PARAMETERS);

                for (final TypeParameter param : typeParams) {
                    record(param, containerNode);
                }
            }
        }

        { // modifiers
            final int modifiers = method.getModifiers();

            methodNode.setProperty(ClassFileSequencerLexicon.ABSTRACT, (modifiers & Modifier.ABSTRACT) != 0);
            methodNode.setProperty(ClassFileSequencerLexicon.FINAL, (modifiers & Modifier.FINAL) != 0);
            methodNode.setProperty(ClassFileSequencerLexicon.NATIVE, (modifiers & Modifier.NATIVE) != 0);
            methodNode.setProperty(ClassFileSequencerLexicon.STATIC, (modifiers & Modifier.STATIC) != 0);
            methodNode.setProperty(ClassFileSequencerLexicon.STRICT_FP, (modifiers & Modifier.STRICTFP) != 0);
            methodNode.setProperty(ClassFileSequencerLexicon.SYNCHRONIZED, (modifiers & Modifier.SYNCHRONIZED) != 0);
            methodNode.setProperty(ClassFileSequencerLexicon.VISIBILITY, getVisibility(modifiers));
        }

        { // annotations
            @SuppressWarnings( "unchecked" )
            final List<IExtendedModifier> modifiers = method.modifiers();
            recordAnnotations(modifiers, methodNode);
        }

        { // parameters
            @SuppressWarnings( "unchecked" )
            final List<SingleVariableDeclaration> params = method.parameters();

            if ((params != null) && !params.isEmpty()) {
                final Node containerNode = methodNode.addNode(ClassFileSequencerLexicon.METHOD_PARAMETERS,
                                                              ClassFileSequencerLexicon.PARAMETERS);

                for (final SingleVariableDeclaration param : params) {
                    record(param, containerNode);
                }
            }
        }

        { // return type
            if (method.isConstructor()) {
                methodNode.setProperty(ClassFileSequencerLexicon.RETURN_TYPE_CLASS_NAME, Void.TYPE.getCanonicalName());
            } else {
                final Type returnType = method.getReturnType2();
                methodNode.setProperty(ClassFileSequencerLexicon.RETURN_TYPE_CLASS_NAME, getTypeName(returnType));
                record(returnType, ClassFileSequencerLexicon.RETURN_TYPE, methodNode);
            }
        }

        { // thrown exceptions
            @SuppressWarnings( "unchecked" )
            final List<Name> errors = method.thrownExceptions();

            if ((errors != null) && !errors.isEmpty()) {
                final String[] errorNames = new String[errors.size()];
                int i = 0;

                for (final Name error : errors) {
                    errorNames[i++] = error.getFullyQualifiedName();
                }

                methodNode.setProperty(ClassFileSequencerLexicon.THROWN_EXCEPTIONS, errorNames);
            }
        }

        { // body
            final Block body = method.getBody();

            if ((body != null) && (body.statements() != null) && !body.statements().isEmpty()) {
                final Node bodyNode = methodNode.addNode(ClassFileSequencerLexicon.BODY, ClassFileSequencerLexicon.STATEMENTS);
                record(body, bodyNode);
            }
        }

        recordSourceReference(method, methodNode);
    }

    /**
     * Convert the compilation unit into JCR nodes.
     *
     * @param context the sequencer context
     * @param sourceCode the source code being recorded (can be <code>null</code> if there is no source code)
     * @param outputNode the {@link Node node} where the output will be saved (cannot be <code>null</code>)
     * @throws Exception if there is a problem
     */
    protected void record( final Sequencer.Context context,
                           final char[] sourceCode,
                           final Node outputNode ) throws Exception {
        if ((sourceCode == null) || (sourceCode.length == 0)) {
            LOGGER.debug("No source code was found for output node {0}", outputNode.getName());
            return;
        }

        this.context = context;
        this.sourceCode = new String(sourceCode);
        this.compilationUnit = (CompilationUnit)CompilationUnitParser.runJLS3Conversion(sourceCode, true);

        outputNode.addMixin(ClassFileSequencerLexicon.COMPILATION_UNIT);
        record(this.compilationUnit, outputNode);
    }

    /**
     * <pre>
     * SingleVariableDeclaration:
     *     { ExtendedModifier } Type [ ... ] Identifier { [] } [ = Expression ]
     * </pre>
     *
     * @param variable the {@link SingleVariableDeclaration variable} being recorded (cannot be <code>null</code>)
     * @param parentNode the parent {@link Node node} where the variable is being recorded (cannot be <code>null</code>)
     * @throws Exception if there is a problem
     */
    protected void record( final SingleVariableDeclaration variable,
                           final Node parentNode ) throws Exception {
        final String name = variable.getName().getFullyQualifiedName();
        final Node paramNode = parentNode.addNode(name, ClassFileSequencerLexicon.PARAMETER);
        paramNode.setProperty(ClassFileSequencerLexicon.NAME, name);
        paramNode.setProperty(ClassFileSequencerLexicon.FINAL, (variable.getModifiers() & Modifier.FINAL) != 0);
        paramNode.setProperty(ClassFileSequencerLexicon.VARARGS, variable.isVarargs());

        { // type
            final Type type = variable.getType();
            final String typeName = getTypeName(type);
            paramNode.setProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME, typeName);
            record(type, ClassFileSequencerLexicon.TYPE, paramNode);
        }

        { // initializer
            final Expression initializer = variable.getInitializer();

            if (initializer != null) {
                recordExpression(initializer, ClassFileSequencerLexicon.INITIALIZER, paramNode);
            }
        }

        { // annotations
            @SuppressWarnings( "unchecked" )
            final List<IExtendedModifier> modifiers = variable.modifiers();
            recordAnnotations(modifiers, paramNode);
        }

        recordSourceReference(variable, paramNode);
    }

    /**
     * <pre>
     * Type:
     *     PrimitiveType
     *     ArrayType
     *     SimpleType
     *     QualifiedType
     *     ParameterizedType
     *     WildcardType
     *
     * PrimitiveType:
     *     byte
     *     short
     *     char
     *     int
     *     long
     *     float
     *     double
     *     boolean
     *     void
     *
     * ArrayType:
     *     Type [ ]
     *
     * SimpleType:
     *     TypeName
     *
     * ParameterizedType:
     *     Type < Type { , Type } >
     *
     * QualifiedType:
     *     Type . SimpleName
     *
     * WildcardType:
     *     ? [ ( extends | super) Type ]
     * </pre>
     *
     * @param type the type {@link Type type} being recorded (cannot be <code>null</code>)
     * @param typeNodeName the name of the type node being recorded (cannot be <code>null</code> or empty)
     * @param parentNode the parent {@link Node node} where the type will be recorded (cannot be <code>null</code>)
     * @throws Exception if there is a problem
     */
    protected void record( final Type type,
                           final String typeNodeName,
                           final Node parentNode ) throws Exception {
        if (type.isSimpleType()) {
            final Node typeNode = parentNode.addNode(typeNodeName, ClassFileSequencerLexicon.SIMPLE_TYPE);
            typeNode.setProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME, getTypeName(type));
            LOGGER.debug("Simple type created at '{0}'", typeNode.getPath());
        } else if (type.isPrimitiveType()) {
            final Node typeNode = parentNode.addNode(typeNodeName, ClassFileSequencerLexicon.PRIMITIVE_TYPE);
            typeNode.setProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME, getTypeName(type));
            LOGGER.debug("Primitive type created at '{0}'", typeNode.getPath());
        } else if (type.isArrayType()) {
            final Node typeNode = parentNode.addNode(typeNodeName, ClassFileSequencerLexicon.ARRAY_TYPE);
            typeNode.setProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME, getTypeName(type));

            final ArrayType arrayType = ((ArrayType)type);
            typeNode.setProperty(ClassFileSequencerLexicon.DIMENSIONS, arrayType.getDimensions());

            final Type componentType = arrayType.getComponentType();
            record(componentType, ClassFileSequencerLexicon.COMPONENT_TYPE, typeNode);
            LOGGER.debug("Array type created at '{0}'", typeNode.getPath());
        } else if (type.isParameterizedType()) {
            final Node typeNode = parentNode.addNode(typeNodeName, ClassFileSequencerLexicon.PARAMETERIZED_TYPE);
            typeNode.setProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME, getTypeName(type));

            final ParameterizedType paramType = (ParameterizedType)type;
            final Type baseType = paramType.getType();
            record(baseType, ClassFileSequencerLexicon.BASE_TYPE, typeNode);

            @SuppressWarnings( "unchecked" )
            final List<Type> arguments = ((ParameterizedType)type).typeArguments();

            if ((arguments != null) && !arguments.isEmpty()) {
                final Node containerNode = typeNode.addNode(ClassFileSequencerLexicon.ARGUMENTS, ClassFileSequencerLexicon.TYPES);

                for (final Type arg : arguments) {
                    record(arg, ClassFileSequencerLexicon.ARGUMENT, containerNode);
                }
            }

            LOGGER.debug("Parameterized type created at '{0}'", typeNode.getPath());
        } else if (type.isQualifiedType()) {
            final Node typeNode = parentNode.addNode(typeNodeName, ClassFileSequencerLexicon.QUALIFIED_TYPE);
            typeNode.setProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME, getTypeName(type));

            final QualifiedType qualifiedType = (QualifiedType)type;
            record(qualifiedType.getQualifier(), ClassFileSequencerLexicon.QUALIFIER, typeNode);
            LOGGER.debug("Qualified type created at '{0}'", typeNode.getPath());
        } else if (type.isWildcardType()) {
            final Node typeNode = parentNode.addNode("?", ClassFileSequencerLexicon.WILDCARD_TYPE);
            typeNode.setProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME, getTypeName(type));

            final WildcardType wildcardType = (WildcardType)type;
            final String bound = wildcardType.isUpperBound() ? ClassFileSequencerLexicon.WildcardTypeBound.UPPER.toString() : ClassFileSequencerLexicon.WildcardTypeBound.LOWER.toString();
            typeNode.setProperty(ClassFileSequencerLexicon.BOUND_TYPE, bound);

            if (wildcardType.getBound() != null) {
                record(wildcardType.getBound(), ClassFileSequencerLexicon.BOUND, typeNode);
            }

            LOGGER.debug("Wildcard type created at '{0}'", typeNode.getPath());
        } else {
            assert false;
            LOGGER.error(JavaFileI18n.unhandledType, type.getClass().getName(), typeNodeName);
        }
    }

    /**
     * <pre>
     * TypeDeclaration:
     *     ClassDeclaration
     *     InterfaceDeclaration
     *
     * ClassDeclaration:
     *     [ Javadoc ] { ExtendedModifier } class Identifier
     *     [ < TypeParameter { , TypeParameter } > ]
     *     [ extends Type ]
     *     [ implements Type { , Type } ]
     *     { { ClassBodyDeclaration | ; } }
     *
     * InterfaceDeclaration:
     *     [ Javadoc ] { ExtendedModifier } interface Identifier
     *     [ < TypeParameter { , TypeParameter } > ]
     *     [ extends Type { , Type } ]
     *     { { InterfaceBodyDeclaration | ; } }
     * </pre>
     *
     * @param type the {@link TypeDeclaration type} being recorded (cannot be <code>null</code>)
     * @param parentNode the parent {@link Node node} where the new type will be created (cannot be <code>null</code>)
     * @return the node representing the type being recorded (never <code>null</code>)
     * @throws Exception if there is a problem
     */
    protected Node record( final TypeDeclaration type,
                           final Node parentNode ) throws Exception {
        final String name = type.getName().getFullyQualifiedName();

        final Node typeNode = parentNode.addNode(name, ClassFileSequencerLexicon.CLASS);
        typeNode.setProperty(ClassFileSequencerLexicon.NAME, name);
        typeNode.setProperty(ClassFileSequencerLexicon.SEQUENCED_DATE, this.context.getTimestamp());

        final boolean isInterface = type.isInterface();
        typeNode.setProperty(ClassFileSequencerLexicon.INTERFACE, isInterface);

        // extends and implements
        @SuppressWarnings( "unchecked" )
        final List<Type> interfaces = type.superInterfaceTypes();

        { // extends
            if (isInterface) {
                if ((interfaces != null) && !interfaces.isEmpty()) {
                    final Node extendsNode = typeNode.addNode(ClassFileSequencerLexicon.EXTENDS, ClassFileSequencerLexicon.TYPES);

                    for (final Type interfaceType : interfaces) {
                        record(interfaceType, ClassFileSequencerLexicon.INTERFACE, extendsNode);
                    }
                }
            } else {
                final Type superType = type.getSuperclassType();
                String superTypeName = null;

                if (superType == null) {
                    superTypeName = Object.class.getCanonicalName();
                } else {
                    superTypeName = getTypeName(superType);
                }

                assert !StringUtil.isBlank(superTypeName);
                typeNode.setProperty(ClassFileSequencerLexicon.SUPER_CLASS_NAME, superTypeName);

                if (superType != null) {
                    final Node extendsNode = typeNode.addNode(ClassFileSequencerLexicon.EXTENDS, ClassFileSequencerLexicon.TYPES);
                    record(superType, getTypeName(superType), extendsNode);
                }
            }
        }

        { // implements
            if (!isInterface && (interfaces != null) && !interfaces.isEmpty()) {
                final Node implementsNode = typeNode.addNode(ClassFileSequencerLexicon.IMPLEMENTS,
                                                             ClassFileSequencerLexicon.TYPES);
                final String[] interfaceNames = new String[interfaces.size()];

                for (int i = 0, size = interfaces.size(); i < size; ++i) {
                    final Type interfaceType = interfaces.get(i);
                    interfaceNames[i] = getTypeName(interfaceType);
                    record(interfaceType, interfaceNames[i], implementsNode);
                }

                typeNode.setProperty(ClassFileSequencerLexicon.INTERFACES, interfaceNames);
            }
        }

        { // javadocs
            final Javadoc javadoc = type.getJavadoc();

            if (javadoc != null) {
                record(javadoc, typeNode);
            }
        }

        { // modifiers
            final int modifiers = type.getModifiers();

            typeNode.setProperty(ClassFileSequencerLexicon.ABSTRACT, (modifiers & Modifier.ABSTRACT) != 0);
            typeNode.setProperty(ClassFileSequencerLexicon.FINAL, (modifiers & Modifier.FINAL) != 0);
            typeNode.setProperty(ClassFileSequencerLexicon.STATIC, (modifiers & Modifier.STATIC) != 0);
            typeNode.setProperty(ClassFileSequencerLexicon.STRICT_FP, (modifiers & Modifier.STRICTFP) != 0);
            typeNode.setProperty(ClassFileSequencerLexicon.VISIBILITY, getVisibility(modifiers));
        }

        { // annotations
            @SuppressWarnings( "unchecked" )
            final List<IExtendedModifier> modifiers = type.modifiers();
            recordAnnotations(modifiers, typeNode);
        }

        { // type parameters
            @SuppressWarnings( "unchecked" )
            final List<TypeParameter> typeParams = type.typeParameters();

            if ((typeParams != null) && !typeParams.isEmpty()) {
                final Node containerNode = typeNode.addNode(ClassFileSequencerLexicon.TYPE_PARAMETERS,
                                                            ClassFileSequencerLexicon.TYPE_PARAMETERS);

                for (final TypeParameter param : typeParams) {
                    record(param, containerNode);
                }
            }
        }

        recordBodyDeclarations(type, typeNode);
        recordSourceReference(type, typeNode);
        return typeNode;
    }

    /**
     * <pre>
     * TypeParameter:
     *     TypeVariable [ extends Type { & Type } ]
     * </pre>
     *
     * @param param the {@link TypeParameter type parameter} being recorded (cannot be <code>null</code>)
     * @param parentNode the parent {@link Node node} where the type parameter will be recorded (cannot be <code>null</code>)
     * @throws Exception if there is a problem
     */
    protected void record( final TypeParameter param,
                           final Node parentNode ) throws Exception {
        final String paramName = param.getName().getFullyQualifiedName();
        final Node paramNode = parentNode.addNode(paramName, ClassFileSequencerLexicon.TYPE_PARAMETER);

        @SuppressWarnings( "unchecked" )
        final List<Type> bounds = param.typeBounds();

        if ((bounds != null) && !bounds.isEmpty()) {
            final Node containerNode = paramNode.addNode(ClassFileSequencerLexicon.BOUNDS, ClassFileSequencerLexicon.TYPES);

            for (final Type bound : bounds) {
                record(bound, getTypeName(bound), containerNode);
            }
        }

        recordSourceReference(param, paramNode);
    }

    protected void recordAnnotationMember( final String memberName,
                                           final Expression expression,
                                           final Node parentNode ) throws Exception {
        final String name = (StringUtil.isBlank(memberName) ? "default" : memberName);
        final Node node = parentNode.addNode(name, ClassFileSequencerLexicon.ANNOTATION_MEMBER);
        node.setProperty(ClassFileSequencerLexicon.NAME, name);

        String value = null;

        if (expression instanceof StringLiteral) {
            value = ((StringLiteral)expression).getLiteralValue();
        } else if (expression instanceof Name) {
            value = ((Name)expression).getFullyQualifiedName();
        } else if (expression instanceof BooleanLiteral) {
            value = Boolean.toString(((BooleanLiteral)expression).booleanValue());
        } else if (expression instanceof CharacterLiteral) {
            value = Character.toString(((CharacterLiteral)expression).charValue());
        } else {
            value = expression.toString();
        }

        node.setProperty(ClassFileSequencerLexicon.VALUE, value);
        recordExpression(expression, name, node);
    }

    protected void recordAnnotations( final List<IExtendedModifier> extendedModifiers,
                                      final Node node ) throws Exception {
        if ((extendedModifiers != null) && !extendedModifiers.isEmpty()) {
            Node containerNode = null;

            for (final IExtendedModifier modifier : extendedModifiers) {
                if (modifier.isAnnotation()) {
                    if (containerNode == null) {
                        containerNode = node.addNode(ClassFileSequencerLexicon.ANNOTATIONS, ClassFileSequencerLexicon.ANNOTATIONS);
                    }

                    record((Annotation)modifier, containerNode);
                }
            }
        }
    }

    protected void recordBodyDeclarations( final AbstractTypeDeclaration type,
                                           final Node typeNode ) throws Exception {
        Node constructorsContainer = null;
        Node fieldsContainer = null;
        Node methodsContainer = null;
        Node nestedTypesContainer = null;

        for (final Object bodyDeclaration : type.bodyDeclarations()) {
            if (bodyDeclaration instanceof FieldDeclaration) {
                if (fieldsContainer == null) {
                    fieldsContainer = typeNode.addNode(ClassFileSequencerLexicon.FIELDS, ClassFileSequencerLexicon.FIELDS);
                }

                record((FieldDeclaration)bodyDeclaration, fieldsContainer);
            } else if (bodyDeclaration instanceof MethodDeclaration) {
                final MethodDeclaration method = (MethodDeclaration)bodyDeclaration;

                if (method.isConstructor()) {
                    if (constructorsContainer == null) {
                        constructorsContainer = typeNode.addNode(ClassFileSequencerLexicon.CONSTRUCTORS,
                                                                 ClassFileSequencerLexicon.CONSTRUCTORS);
                    }

                    record(method, constructorsContainer);
                } else {
                    if (methodsContainer == null) {
                        methodsContainer = typeNode.addNode(ClassFileSequencerLexicon.METHODS, ClassFileSequencerLexicon.METHODS);
                    }

                    record(method, methodsContainer);
                }
            } else if (bodyDeclaration instanceof TypeDeclaration) {
                if (nestedTypesContainer == null) {
                    nestedTypesContainer = typeNode.addNode(ClassFileSequencerLexicon.NESTED_TYPES,
                                                            ClassFileSequencerLexicon.NESTED_TYPES);
                }

                record((TypeDeclaration)bodyDeclaration, nestedTypesContainer);
            } else if (bodyDeclaration instanceof EnumDeclaration) {
                if (nestedTypesContainer == null) {
                    nestedTypesContainer = typeNode.addNode(ClassFileSequencerLexicon.NESTED_TYPES,
                                                            ClassFileSequencerLexicon.NESTED_TYPES);
                }

                record((EnumDeclaration)bodyDeclaration, nestedTypesContainer);
            } else if (bodyDeclaration instanceof Initializer) {
                record(((Initializer)bodyDeclaration), ClassFileSequencerLexicon.INITIALIZER, typeNode);
            } else {
                assert false;
                LOGGER.error(JavaFileI18n.unhandledBodyDeclarationType, bodyDeclaration.getClass().getName());
            }
        }
    }

    protected void recordBodyDeclarations( final AnonymousClassDeclaration anonClass,
                                           final Node enumConstantNode ) throws Exception {
        Node fieldsContainer = null;
        Node methodsContainer = null;
        Node nestedTypesContainer = null;

        for (final Object bodyDeclaration : anonClass.bodyDeclarations()) {
            if (bodyDeclaration instanceof FieldDeclaration) {
                if (fieldsContainer == null) {
                    fieldsContainer = enumConstantNode.addNode(ClassFileSequencerLexicon.FIELDS, ClassFileSequencerLexicon.FIELDS);
                }

                record((FieldDeclaration)bodyDeclaration, fieldsContainer);
            } else if (bodyDeclaration instanceof MethodDeclaration) {
                if (methodsContainer == null) {
                    methodsContainer = enumConstantNode.addNode(ClassFileSequencerLexicon.METHODS,
                                                                ClassFileSequencerLexicon.METHODS);
                }

                record((MethodDeclaration)bodyDeclaration, methodsContainer);
            } else if (bodyDeclaration instanceof TypeDeclaration) {
                if (nestedTypesContainer == null) {
                    nestedTypesContainer = enumConstantNode.addNode(ClassFileSequencerLexicon.NESTED_TYPES,
                                                                    ClassFileSequencerLexicon.NESTED_TYPES);
                }

                record((TypeDeclaration)bodyDeclaration, nestedTypesContainer);
            } else {
                assert false;
                LOGGER.error(JavaFileI18n.unhandledBodyDeclarationType, bodyDeclaration.getClass().getName());
            }
        }
    }

    /**
     * <pre>
     * Comment:
     *     LineComment
     *     BlockComment
     *     Javadoc
     * </pre>
     *
     * @param compilationUnit the {@link CompilationUnit compilation unit} being recorded (cannot be <code>null</code>)
     * @param outputNode the parent {@link Node node} (cannot be <code>null</code>)
     * @throws Exception if there is a problem
     */
    protected void recordComments( final CompilationUnit compilationUnit,
                                   final Node outputNode ) throws Exception {
        @SuppressWarnings( "unchecked" )
        final List<Comment> comments = compilationUnit.getCommentList();

        if ((comments != null) && !comments.isEmpty()) {
            final Node containerNode = outputNode.addNode(ClassFileSequencerLexicon.COMMENTS, ClassFileSequencerLexicon.COMMENTS);

            for (final Comment comment : comments) {
                // javadocs are stored with the object they pertain to
                if (!comment.isDocComment()) {
                    record(comment, containerNode);
                }
            }
        }
    }

    protected void recordCompilerMessages( final CompilationUnit unit,
                                           final Node parentNode ) throws Exception {
        final Message[] messages = unit.getMessages();

        if ((messages != null) && (messages.length != 0)) {
            final Node containerNode = parentNode.addNode(ClassFileSequencerLexicon.MESSAGES, ClassFileSequencerLexicon.MESSAGES);

            for (final Message message : messages) {
                final Node messageNode = containerNode.addNode(ClassFileSequencerLexicon.MESSAGE,
                                                               ClassFileSequencerLexicon.MESSAGE);
                messageNode.setProperty(ClassFileSequencerLexicon.MESSAGE, message.getMessage());
                messageNode.setProperty(ClassFileSequencerLexicon.START_POSITION, message.getStartPosition());
                messageNode.setProperty(ClassFileSequencerLexicon.LENGTH, message.getLength());
            }
        }
    }

    /**
     * <pre>
     * Expression:
     *
     * Annotation,
     * ArrayAccess,
     * ArrayCreation,
     * ArrayInitializer,
     * Assignment,
     * BooleanLiteral,
     * CastExpression,
     * CharacterLiteral,
     * ClassInstanceCreation,
     * ConditionalExpression,
     * FieldAccess,
     * InfixExpression,
     * InstanceofExpression,
     * MethodInvocation,
     * Name,
     * NullLiteral,
     * NumberLiteral,
     * ParenthesizedExpression,
     * PostfixExpression,
     * PrefixExpression,
     * StringLiteral,
     * SuperFieldAccess,
     * SuperMethodInvocation,
     * ThisExpression,
     * TypeLiteral,
     * VariableDeclarationExpression
     * </pre>
     *
     * @param expression the {@link Expression expression} being recorded (cannot be <code>null</code>)
     * @param nodeName the name of the expression node that is created (cannot be <code>null</code> or empty)
     * @param parentNode the parent {@link Node node} where the expression is being recorded (cannot be <code>null</code>)
     * @throws Exception if there is a problem
     */
    protected void recordExpression( final Expression expression,
                                     final String nodeName,
                                     final Node parentNode ) throws Exception {
        // TODO handle all the different types of expressions
        final Node expressionNode = parentNode.addNode(nodeName, ClassFileSequencerLexicon.EXPRESSION);
        expressionNode.setProperty(ClassFileSequencerLexicon.CONTENT, expression.toString());
        recordSourceReference(expression, expressionNode);
    }

    /**
     * <pre>
     * ImportDeclaration:
     *      import [ static ] Name [ . * ] ;
     * </pre>
     *
     * @param compilationUnit the {@link CompilationUnit compilation unit} being recorded (cannot be <code>null</code>)
     * @param outputNode the parent {@link Node node} (cannot be <code>null</code>)
     * @throws Exception if there is a problem
     */
    protected void recordImports( final CompilationUnit compilationUnit,
                                  final Node outputNode ) throws Exception {
        @SuppressWarnings( "unchecked" )
        final List<ImportDeclaration> imports = compilationUnit.imports();

        if ((imports != null) && !imports.isEmpty()) {
            final Node containerNode = outputNode.addNode(ClassFileSequencerLexicon.IMPORTS, ClassFileSequencerLexicon.IMPORTS);

            for (final ImportDeclaration mport : imports) {
                final Node importNode = containerNode.addNode(mport.getName().getFullyQualifiedName(),
                                                              ClassFileSequencerLexicon.IMPORT);
                importNode.setProperty(ClassFileSequencerLexicon.STATIC, mport.isStatic());
                importNode.setProperty(ClassFileSequencerLexicon.ON_DEMAND, mport.isOnDemand());

                recordSourceReference(mport, importNode);
            }
        }
    }

    /**
     * <pre>
     * PackageDeclaration:
     *     [ Javadoc ] { Annotation } package Name ;
     * </pre>
     *
     * @param compilationUnit the {@link CompilationUnit compilation unit} whose package is being recorded (cannot be
     *        <code>null</code>)
     * @param outputNode the output node (cannot be <code>null</code>)
     * @return the package {@link Node node} or the passed in <code>outputNode</code> if a package is not found
     * @throws Exception if there is a problem
     */
    protected Node recordPackage( final CompilationUnit compilationUnit,
                                  final Node outputNode ) throws Exception {
        final PackageDeclaration pkg = compilationUnit.getPackage();

        if (pkg == null) {
            return outputNode;
        }

        Node pkgNode = outputNode;

        { // create node for each segment of the package name
            final String pkgName = pkg.getName().getFullyQualifiedName();
            final String[] packagePath = pkgName.split("\\.");

            if (pkgName.length() > 0) {
                for (final String segment : packagePath) {
                    pkgNode = pkgNode.addNode(segment);
                    pkgNode.addMixin(ClassFileSequencerLexicon.PACKAGE);
                }
            }
        }

        { // Javadocs
            final Javadoc javadoc = pkg.getJavadoc();

            if (javadoc != null) {
                record(javadoc, pkgNode);
            }
        }

        { // annotations
            @SuppressWarnings( "unchecked" )
            final List<Annotation> annotations = pkg.annotations();

            if ((annotations != null) && !annotations.isEmpty()) {
                for (final Annotation annotation : annotations) {
                    record(annotation, pkgNode);
                }
            }
        }

        recordSourceReference(pkg, pkgNode);
        return pkgNode;
    }

    protected void recordSourceReference( final ASTNode astNode,
                                          final Node jcrNode ) throws Exception {
        jcrNode.setProperty(ClassFileSequencerLexicon.START_POSITION, astNode.getStartPosition());
        jcrNode.setProperty(ClassFileSequencerLexicon.LENGTH, astNode.getLength());
    }

    protected void recordTypes( final CompilationUnit unit,
                                final Node compilationUnitNode,
                                final Node pkgNode ) throws Exception {
        @SuppressWarnings( "unchecked" )
        final List<AbstractTypeDeclaration> topLevelTypes = unit.types();

        if ((topLevelTypes != null) && !topLevelTypes.isEmpty()) {
            final List<Node> types = new ArrayList<>(topLevelTypes.size());

            for (final AbstractTypeDeclaration type : topLevelTypes) {
                if (type instanceof TypeDeclaration) {
                    types.add(record((TypeDeclaration)type, pkgNode));
                } else if (type instanceof EnumDeclaration) {
                    types.add(record((EnumDeclaration)type, pkgNode));
                } else if (type instanceof AnnotationTypeDeclaration) {
                    types.add(record((AnnotationTypeDeclaration)type, pkgNode));
                } else {
                    assert false;
                    LOGGER.error(JavaFileI18n.unhandledTopLevelType, type.getName().getFullyQualifiedName());
                }
            }

            final ValueFactory factory = this.context.valueFactory();
            final Value[] refs = new Value[topLevelTypes.size()];
            int i = 0;

            for (final Node typeNode : types) {
                refs[i++] = factory.createValue(typeNode);
            }

            compilationUnitNode.setProperty(ClassFileSequencerLexicon.TYPES, refs);
        }

        recordSourceReference(compilationUnit, compilationUnitNode);
    }

}
