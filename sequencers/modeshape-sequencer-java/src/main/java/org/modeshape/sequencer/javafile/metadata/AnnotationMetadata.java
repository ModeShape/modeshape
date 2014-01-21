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
package org.modeshape.sequencer.javafile.metadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract class for annotations.
 * 
 * @author Horia Chiorean
 */
public class AnnotationMetadata {

    private String annotationClassName;
    private Map<String, String> memberValues = new HashMap<String, String>();
    private Type type;

    private AnnotationMetadata( Type type,
                                String annotationClassName,
                                Map<String, String> memberValues ) {
        this.type = type;
        this.annotationClassName = annotationClassName;
        this.memberValues = (memberValues != null) ? memberValues : Collections.<String, String>emptyMap();
    }

    public String getName() {
        return annotationClassName;
    }

    public Map<String, String> getMemberValues() {
        return Collections.unmodifiableMap(memberValues);
    }

    public Type getType() {
        return type;
    }

    public static AnnotationMetadata markerAnnotation( String annotationClassName ) {
        return new AnnotationMetadata(Type.MARKER, annotationClassName, null);
    }

    public static AnnotationMetadata normalAnnotation( String annotationClassName,
                                                       Map<String, String> memberValues ) {
        return new AnnotationMetadata(Type.NORMAL, annotationClassName, memberValues);
    }

    public static AnnotationMetadata singleMemberAnnotation( String annotationClassName,
                                                             String value ) {
        Map<String, String> memberValues = new HashMap<String, String>(1);
        memberValues.put(null, value);
        return new AnnotationMetadata(Type.SINGLE_MEMBER, annotationClassName, memberValues);
    }

    public static enum Type {
        MARKER,
        NORMAL,
        SINGLE_MEMBER
    }
}
