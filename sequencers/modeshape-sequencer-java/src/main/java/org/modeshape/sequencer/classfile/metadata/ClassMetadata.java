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
package org.modeshape.sequencer.classfile.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.sequencer.javafile.metadata.ImportMetadata;

@Immutable
public class ClassMetadata {

    private static final String JAVA_LANG_PKG = "java.lang.";

    private final ClassFile clazz;

    private final List<AnnotationMetadata> annotations;
    private final List<FieldMetadata> fields;
    private final List<MethodMetadata> methods;
    private final List<MethodMetadata> constructors;
    private final List<ImportMetadata> imports;

    ClassMetadata( ClassFile clazz ) {
        this.clazz = clazz;
        this.annotations = annotationsFor(clazz);
        this.fields = fieldsFor(clazz);
        this.methods = methodsFor(clazz);
        this.constructors = constructorsFor(clazz);
        this.imports = importsFor(clazz);
    }

    private List<AnnotationMetadata> annotationsFor( ClassFile clazz ) {
        List<AnnotationMetadata> annotations = new LinkedList<AnnotationMetadata>();

        for (Object ob : clazz.getAttributes()) {
            AttributeInfo att = (AttributeInfo)ob;

            if (att instanceof AnnotationsAttribute) {
                for (Annotation ann : ((AnnotationsAttribute)att).getAnnotations()) {
                    annotations.add(new AnnotationMetadata(ann));
                }
            }
        }

        return Collections.unmodifiableList(annotations);
    }

    private List<FieldMetadata> fieldsFor( ClassFile clazz ) {
        List<FieldMetadata> fields = new LinkedList<FieldMetadata>();

        for (Object field : clazz.getFields()) {
            fields.add(new FieldMetadata((FieldInfo)field));
        }

        Collections.sort(fields);

        return Collections.unmodifiableList(fields);
    }

    private List<MethodMetadata> methodsFor( ClassFile clazz ) {
        List<MethodMetadata> methods = new LinkedList<MethodMetadata>();

        for (Object ob : clazz.getMethods()) {
            MethodInfo method = (MethodInfo)ob;

            if (!method.isStaticInitializer() && !method.isConstructor()) {
                methods.add(new MethodMetadata(clazz, method));
            }
        }

        Collections.sort(methods);

        return Collections.unmodifiableList(methods);
    }

    private List<MethodMetadata> constructorsFor( ClassFile clazz ) {
        List<MethodMetadata> ctors = new LinkedList<MethodMetadata>();

        for (Object ob : clazz.getMethods()) {
            MethodInfo method = (MethodInfo)ob;
            if (!method.isStaticInitializer() && method.isConstructor()) {
                ctors.add(new MethodMetadata(clazz, method));
            }
        }

        Collections.sort(ctors);

        return Collections.unmodifiableList(ctors);
    }

    private List<ImportMetadata> importsFor( final ClassFile clazz ) {
        final String clazzName = clazz.getName();
        String pkg = null;
        boolean clazzHasPackage = false;

        if (clazzName.lastIndexOf(".") != -1) {
            pkg = clazzName.substring(0, (clazzName.lastIndexOf(".") + 1));
            clazzHasPackage = true;
        }

        final ConstPool pool = clazz.getConstPool();
        @SuppressWarnings( "unchecked" )
        final Set<String> references = pool.getClassNames();

        if ((references == null) || references.isEmpty()) {
            return Collections.emptyList();
        }

        final List<ImportMetadata> imports = new ArrayList<ImportMetadata>();

        for (final String reference : references) {
            String refClassName = Descriptor.toJavaName(reference);

            // handle array references
            if (refClassName.startsWith("[L")) {
                refClassName = Descriptor.toClassName(refClassName);

                if (refClassName.endsWith("[]")) {
                    refClassName = refClassName.substring(0, (refClassName.length() - 2));
                }
            }

            // don't add import if reference is from java.lang package
            if (refClassName.startsWith(JAVA_LANG_PKG)) {
                continue;
            }

            // don't add import if reference is from same package
            if ((clazzHasPackage && refClassName.startsWith(pkg)) || (!clazzHasPackage && (reference.indexOf('.') == -1))) {
                continue;
            }

            // add import
            imports.add(ImportMetadata.single(refClassName));
        }

        return Collections.unmodifiableList(imports);
    }

    public boolean isEnumeration() {
        return false;
    }

    public String getClassName() {
        return clazz.getName();
    }

    public String getSuperclassName() {
        return clazz.getSuperclass();
    }

    public String[] getInterfaces() {
        return clazz.getInterfaces();
    }

    public boolean isAbstract() {
        return clazz.isAbstract();
    }

    public boolean isInterface() {
        return clazz.isInterface();
    }

    public boolean isStrictFp() {
        return AccessFlag.STRICT == (AccessFlag.STRICT & clazz.getAccessFlags());
    }

    public boolean isFinal() {
        return AccessFlag.FINAL == (AccessFlag.FINAL & clazz.getAccessFlags());
    }

    public Visibility getVisibility() {
        return Visibility.fromAccessFlags(clazz.getAccessFlags());
    }

    public List<AnnotationMetadata> getAnnotations() {
        return annotations;
    }

    public List<FieldMetadata> getFields() {
        return fields;
    }

    public List<MethodMetadata> getMethods() {
        return methods;
    }

    public List<MethodMetadata> getConstructors() {
        return constructors;
    }

    /**
     * @return the imports (never <code>null</code> but can be empty)
     */
    public List<ImportMetadata> getImports() {
        return this.imports;
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder();

        if (!getImports().isEmpty()) {
            for (final ImportMetadata imported : getImports()) {
                buff.append(imported).append('\n');
            }

            buff.append('\n');
        }

        for (AnnotationMetadata annotation : annotations) {
            buff.append(annotation).append('\n');
        }

        buff.append(getVisibility());
        if (getVisibility() != Visibility.PACKAGE) {
            buff.append(' ');
        }

        if (isAbstract()) {
            if (isInterface()) {
                buff.append("interface ");
            } else {
                buff.append("abstract class ");

            }
        } else {
            buff.append("class ");
        }

        if (getSuperclassName() != null && !Object.class.getName().equals(getSuperclassName())) {
            buff.append(" extends ").append(getSuperclassName()).append(" ");
        }

        if (getInterfaces().length > 0) {
            boolean first = true;
            buff.append(" implements ");

            for (String interfaceName : getInterfaces()) {
                if (first) {
                    first = false;
                } else {
                    buff.append(", ");
                }
                buff.append(interfaceName);
            }

            buff.append(' ');
        }

        buff.append(getClassName()).append(" {\n");

        for (FieldMetadata field : fields) {
            buff.append('\t').append(field).append('\n');
        }

        if (!methods.isEmpty()) {
            buff.append('\n');
        }

        for (MethodMetadata method : methods) {
            buff.append('\t').append(method).append('\n');
        }

        buff.append("}");

        return buff.toString();
    }
}
