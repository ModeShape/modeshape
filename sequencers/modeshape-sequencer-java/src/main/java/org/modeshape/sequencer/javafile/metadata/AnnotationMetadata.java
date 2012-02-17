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

    private AnnotationMetadata( Type type, String annotationClassName, Map<String, String>  memberValues ) {
        this.type = type;
        this.annotationClassName = annotationClassName;
        this.memberValues = (memberValues != null) ? memberValues : Collections.<String,String>emptyMap();
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

    public static AnnotationMetadata markerAnnotation(String annotationClassName) {
        return new AnnotationMetadata(Type.MARKER, annotationClassName, null);
    }

    public static AnnotationMetadata normalAnnotation(String annotationClassName, Map<String, String> memberValues) {
        return new AnnotationMetadata(Type.NORMAL, annotationClassName, memberValues);
    }

    public static AnnotationMetadata singleMemberAnnotation(String annotationClassName, String value) {
        Map<String, String> memberValues = new HashMap<String, String>(1);
        memberValues.put(null, value);
        return new AnnotationMetadata(Type.SINGLE_MEMBER, annotationClassName, memberValues);
    }

    public static enum Type {
        MARKER, NORMAL, SINGLE_MEMBER
    }
}
