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
package org.modeshape.sequencer.classfile.metadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javassist.bytecode.annotation.Annotation;
import net.jcip.annotations.Immutable;

@Immutable
public class AnnotationMetadata {

    private final String annotationClassName;
    private final Map<String, String> memberValues;

    @SuppressWarnings( "unchecked" )
    AnnotationMetadata( Annotation annotation ) {
        this.annotationClassName = annotation.getTypeName();

        Set<Object> memberNames = annotation.getMemberNames();
        if (memberNames != null) {
            Map<String, String> members = new HashMap<String, String>(memberNames.size());

            for (Object rawMemberName : memberNames) {
                String memberName = (String)rawMemberName;

                members.put(memberName, annotation.getMemberValue(memberName).toString());
            }

            this.memberValues = Collections.unmodifiableMap(members);
        } else {
            this.memberValues = Collections.emptyMap();
        }
    }

    public String getAnnotationClassName() {
        return annotationClassName;
    }

    public Map<String, String> getMemberValues() {
        return memberValues;
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder();

        buff.append('@').append(annotationClassName);

        if (!memberValues.isEmpty()) {
            buff.append('(');

            boolean first = true;
            for (Map.Entry<String, String> entry : memberValues.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    buff.append(", ");
                }
                buff.append(entry.getKey()).append('=').append(entry.getValue());
            }

            buff.append(')');
        }

        return buff.toString();
    }

}
