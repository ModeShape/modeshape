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
import java.util.List;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;

public class EnumMetadata extends ClassMetadata {

    private final List<FieldMetadata> enumFields;
    private final List<String> values;

    public EnumMetadata( ClassFile clazz ) {
        super(clazz);

        assert (clazz.getAccessFlags() & AccessFlag.ENUM) == AccessFlag.ENUM;
        List<FieldMetadata> fieldsFromClass = super.getFields();

        /*
         * Each enum value is stored as a static field in the class file.  Filter those out and treat them separately.
         */
        List<FieldMetadata> enumFields = new ArrayList<FieldMetadata>(fieldsFromClass.size());
        List<String> values = new ArrayList<String>(fieldsFromClass.size());
        for (FieldMetadata fieldFromClass : fieldsFromClass) {
            if (fieldFromClass.getTypeName().equals(getClassName())) {
                values.add(fieldFromClass.getName());
            } else {
                enumFields.add(fieldFromClass);
            }
        }

        this.enumFields = Collections.unmodifiableList(enumFields);
        this.values = Collections.unmodifiableList(values);

    }

    public List<String> getValues() {
        return values;
    }

    @Override
    public List<FieldMetadata> getFields() {
        return enumFields;
    }

    @Override
    public boolean isEnumeration() {
        return true;
    }

}
