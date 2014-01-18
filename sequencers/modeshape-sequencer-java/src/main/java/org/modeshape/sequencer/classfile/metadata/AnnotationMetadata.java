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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javassist.bytecode.annotation.Annotation;
import org.modeshape.common.annotation.Immutable;

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
