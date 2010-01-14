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
