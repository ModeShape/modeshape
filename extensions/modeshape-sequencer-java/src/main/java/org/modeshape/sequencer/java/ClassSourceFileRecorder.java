package org.modeshape.sequencer.java;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.property.DateTimeFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.modeshape.sequencer.classfile.ClassFileSequencerLexicon;
import org.modeshape.sequencer.classfile.metadata.Visibility;
import org.modeshape.sequencer.java.metadata.AnnotationMetadata;
import org.modeshape.sequencer.java.metadata.EnumMetadata;
import org.modeshape.sequencer.java.metadata.FieldMetadata;
import org.modeshape.sequencer.java.metadata.InterfaceMetadata;
import org.modeshape.sequencer.java.metadata.JavaMetadata;
import org.modeshape.sequencer.java.metadata.MethodMetadata;
import org.modeshape.sequencer.java.metadata.TypeMetadata;

/**
 * A source file recorder that writes the Java metadata from the source file to the repository, using the same structure as the
 * default mode of the Java Class File sequencer.
 */
public class ClassSourceFileRecorder implements SourceFileRecorder {

    public void record( StreamSequencerContext context,
                        SequencerOutput output,
                        JavaMetadata javaMetadata ) {

        PathFactory pathFactory = pathFactoryFor(context);
        DateTimeFactory dateFactory = dateFactoryFor(context);

        writeJavaMetadata(output, pathFactory, dateFactory, javaMetadata);

    }

    private DateTimeFactory dateFactoryFor( StreamSequencerContext context ) {
        return context.getValueFactories().getDateFactory();
    }

    private PathFactory pathFactoryFor( StreamSequencerContext context ) {
        return context.getValueFactories().getPathFactory();
    }

    private Path pathFor( PathFactory pathFactory,
                          TypeMetadata tmd ) {
        List<Segment> segments = new LinkedList<Segment>();

        for (String segment : tmd.getName().split("\\.")) {
            segments.add(pathFactory.createSegment(segment));
        }

        return pathFactory.createRelativePath(segments);
    }

    private void writeJavaMetadata( SequencerOutput output,
                                    PathFactory pathFactory,
                                    DateTimeFactory dateFactory,
                                    JavaMetadata javaMetadata ) {

        for (TypeMetadata typeMetadata : javaMetadata.getTypeMetadata()) {
            writeClassMetadata(output, pathFactory, dateFactory, pathFor(pathFactory, typeMetadata), typeMetadata);
        }
    }

    private void writeClassMetadata( SequencerOutput output,
                                     PathFactory pathFactory,
                                     DateTimeFactory dateFactory,
                                     Path classPath,
                                     TypeMetadata cmd ) {

        /*
        - class:name (string) mandatory 
        - class:superClassName (string) 
        - class:visibility (string) mandatory < 'public', 'protected', 'package', 'private'
        - class:abstract (boolean) mandatory
        - class:interface (boolean) mandatory
        - class:final (boolean) mandatory
        - class:strictFp (boolean) mandatory
        - class:interfaces (string) multiple
        + class:annotations (class:annotations) = class:annotations
        + class:constructors (class:constructors) = class:constructors
        + class:methods (class:methods) = class:methods
        + class:fields (class:fields) = class:fields
         */

        int numberOfMethods = cmd.getMethods().size();
        List<MethodMetadata> methods = new ArrayList<MethodMetadata>(numberOfMethods);
        List<MethodMetadata> ctors = new ArrayList<MethodMetadata>(numberOfMethods);

        for (MethodMetadata method : cmd.getMethods()) {
            if (method.isContructor()) {
                ctors.add(method);
            } else {
                methods.add(method);
            }
        }

        output.setProperty(classPath, ClassFileSequencerLexicon.NAME, cmd.getName());
        output.setProperty(classPath, ClassFileSequencerLexicon.SEQUENCED_DATE, dateFactory.create());
        String superClassName = cmd.getSuperClassName();
        if (superClassName == null || superClassName.length() == 0) {
            superClassName = Object.class.getCanonicalName();
        }
        output.setProperty(classPath, ClassFileSequencerLexicon.SUPER_CLASS_NAME, superClassName);
        output.setProperty(classPath, ClassFileSequencerLexicon.VISIBILITY, visibilityFor(cmd).getDescription());
        output.setProperty(classPath, ClassFileSequencerLexicon.ABSTRACT, cmd.hasModifierNamed("abstract"));
        output.setProperty(classPath, ClassFileSequencerLexicon.INTERFACE, (cmd instanceof InterfaceMetadata));
        output.setProperty(classPath, ClassFileSequencerLexicon.FINAL, cmd.hasModifierNamed("final"));
        output.setProperty(classPath, ClassFileSequencerLexicon.STRICT_FP, cmd.hasModifierNamed("strictfp"));
        output.setProperty(classPath, ClassFileSequencerLexicon.INTERFACES, cmd.getInterfaceNames().toArray());

        Path constructorsPath = pathFactory.create(classPath, ClassFileSequencerLexicon.CONSTRUCTORS);
        output.setProperty(constructorsPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.CONSTRUCTORS);
        writeMethods(output, pathFactory, constructorsPath, ctors);

        Path methodsPath = pathFactory.create(classPath, ClassFileSequencerLexicon.METHODS);
        output.setProperty(methodsPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.METHODS);
        writeMethods(output, pathFactory, methodsPath, methods);

        writeFieldsNode(output, pathFactory, classPath, cmd.getFields());
        writeAnnotationsNode(output, pathFactory, classPath, cmd.getAnnotations());

        if (cmd instanceof EnumMetadata) {
            output.setProperty(classPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.ENUM);

            output.setProperty(classPath, ClassFileSequencerLexicon.ENUM_VALUES, ((EnumMetadata)cmd).getValues().toArray());
        } else {
            output.setProperty(classPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.CLASS);
        }
    }

    private Visibility visibilityFor( TypeMetadata cmd ) {
        if (cmd.hasModifierNamed("public")) return Visibility.PUBLIC;
        if (cmd.hasModifierNamed("protected")) return Visibility.PROTECTED;
        if (cmd.hasModifierNamed("private")) return Visibility.PRIVATE;

        return Visibility.PACKAGE;
    }

    private Visibility visibilityFor( FieldMetadata cmd ) {
        if (cmd.hasModifierNamed("public")) return Visibility.PUBLIC;
        if (cmd.hasModifierNamed("protected")) return Visibility.PROTECTED;
        if (cmd.hasModifierNamed("private")) return Visibility.PRIVATE;

        return Visibility.PACKAGE;
    }

    private Visibility visibilityFor( MethodMetadata cmd ) {
        if (cmd.hasModifierNamed("public")) return Visibility.PUBLIC;
        if (cmd.hasModifierNamed("protected")) return Visibility.PROTECTED;
        if (cmd.hasModifierNamed("private")) return Visibility.PRIVATE;

        return Visibility.PACKAGE;
    }

    private void writeAnnotationsNode( SequencerOutput output,
                                       PathFactory pathFactory,
                                       Path parentPath,
                                       List<AnnotationMetadata> annotations ) {

        /*
        [class:annotationMember]
        - class:name (string) mandatory
        - class:value (string) 
        
        [class:annotation]
        - class:name (string) mandatory
        + * (class:annotationMember) = class:annotationMember
        
        [class:annotations]
        + * (class:annotation) = class:annotation
         */

        Path annotationsPath = pathFactory.create(parentPath, ClassFileSequencerLexicon.ANNOTATIONS);
        output.setProperty(annotationsPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.ANNOTATIONS);

        for (AnnotationMetadata annotation : annotations) {
            Path annotationPath = pathFactory.create(annotationsPath, annotation.getName());
            output.setProperty(annotationPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.ANNOTATION);

            for (Map.Entry<String, String> entry : annotation.getMemberValues().entrySet()) {
                String key = entry.getKey();
                if (key == null) key = "default";

                Path annotationMemberPath = pathFactory.create(annotationPath, key);
                output.setProperty(annotationMemberPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.ANNOTATION_MEMBER);
                output.setProperty(annotationMemberPath, ClassFileSequencerLexicon.NAME, entry.getKey());
                output.setProperty(annotationMemberPath, ClassFileSequencerLexicon.VALUE, entry.getValue());

            }
        }
    }

    private void writeFieldsNode( SequencerOutput output,
                                  PathFactory pathFactory,
                                  Path classPath,
                                  List<FieldMetadata> fields ) {

        /*
            [class:field]
            - class:name (string) mandatory 
            - class:typeClassName (string) mandatory 
            - class:visibility (string) mandatory < 'public', 'protected', 'package', 'private'
            - class:static (boolean) mandatory
            - class:final (boolean) mandatory
            - class:transient (boolean) mandatory
            - class:volatile (boolean) mandatory
            + class:annotations (class:annotations) = class:annotations
            
            [class:fields]
            + * (class:field) = class:field
         */

        Path fieldsPath = pathFactory.create(classPath, ClassFileSequencerLexicon.FIELDS);
        output.setProperty(fieldsPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.FIELDS);

        for (FieldMetadata field : fields) {
            Path fieldPath = pathFactory.create(fieldsPath, field.getName());

            output.setProperty(fieldPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.FIELD);
            output.setProperty(fieldPath, ClassFileSequencerLexicon.NAME, field.getName());
            output.setProperty(fieldPath, ClassFileSequencerLexicon.TYPE_CLASS_NAME, field.getType());
            output.setProperty(fieldPath, ClassFileSequencerLexicon.VISIBILITY, visibilityFor(field).getDescription());
            output.setProperty(classPath, ClassFileSequencerLexicon.STATIC, field.hasModifierNamed("static"));
            output.setProperty(classPath, ClassFileSequencerLexicon.FINAL, field.hasModifierNamed("final"));
            output.setProperty(classPath, ClassFileSequencerLexicon.TRANSIENT, field.hasModifierNamed("transient"));
            output.setProperty(classPath, ClassFileSequencerLexicon.VOLATILE, field.hasModifierNamed("volatile"));

            writeAnnotationsNode(output, pathFactory, fieldPath, field.getAnnotations());

        }
    }

    private void writeMethods( SequencerOutput output,
                               PathFactory pathFactory,
                               Path methodsPath,
                               List<MethodMetadata> methods ) {

        /*
            [class:method]
            - class:name (string) mandatory 
            - class:returnTypeClassName (string) mandatory 
            - class:visibility (string) mandatory < 'public', 'protected', 'package', 'private'
            - class:static (boolean) mandatory
            - class:final (boolean) mandatory
            - class:abstract (boolean) mandatory
            - class:strictFp (boolean) mandatory
            - class:native (boolean) mandatory
            - class:synchronized (boolean) mandatory
            - class:parameters (string) multiple
            + class:annotations (class:annotations) = class:annotations
         */

        for (MethodMetadata method : methods) {
            Path methodPath = pathFactory.create(methodsPath, method.getId());

            output.setProperty(methodPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.METHOD);
            output.setProperty(methodPath, ClassFileSequencerLexicon.NAME, method.getName());
            output.setProperty(methodPath, ClassFileSequencerLexicon.RETURN_TYPE_CLASS_NAME, method.getReturnTypeName());
            output.setProperty(methodPath, ClassFileSequencerLexicon.VISIBILITY, visibilityFor(method).getDescription());
            output.setProperty(methodPath, ClassFileSequencerLexicon.STATIC, method.hasModifierNamed("static"));
            output.setProperty(methodPath, ClassFileSequencerLexicon.FINAL, method.hasModifierNamed("final"));
            output.setProperty(methodPath, ClassFileSequencerLexicon.ABSTRACT, method.hasModifierNamed("abstract"));
            output.setProperty(methodPath, ClassFileSequencerLexicon.STRICT_FP, method.hasModifierNamed("strictfp"));
            output.setProperty(methodPath, ClassFileSequencerLexicon.NATIVE, method.hasModifierNamed("native"));
            output.setProperty(methodPath, ClassFileSequencerLexicon.SYNCHRONIZED, method.hasModifierNamed("synchronized"));
            output.setProperty(methodPath, ClassFileSequencerLexicon.PARAMETERS, method.getParameterTypes().toArray());

            writeAnnotationsNode(output, pathFactory, methodPath, method.getAnnotations());

        }

    }

}
