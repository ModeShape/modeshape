/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
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
package org.infinispan.schematic.internal.delta;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;
import org.infinispan.marshall.SerializeWith;
import org.infinispan.schematic.document.Path;
import org.infinispan.schematic.internal.HashCode;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.document.MutableDocument;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.Util;

/**
 * An atomic put-if-absent operation for SchematicValueDelta.
 * 
 * @author (various)
 */
@SerializeWith( PutIfAbsentOperation.Externalizer.class )
public class PutIfAbsentOperation extends Operation {
    protected final String fieldName;
    protected final Object newValue;
    private transient boolean absent = false;

    public PutIfAbsentOperation( Path parentPath,
                                 String fieldName,
                                 Object newValue ) {
        super(parentPath, HashCode.compute(parentPath, fieldName, newValue));
        this.fieldName = fieldName;
        this.newValue = newValue;
    }

    @Override
    public PutIfAbsentOperation clone() {
        return new PutIfAbsentOperation(getParentPath(), fieldName, cloneValue(newValue));
    }

    public Object getNewValue() {
        return newValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isApplied() {
        return absent;
    }

    @Override
    public void rollback( MutableDocument delegate ) {
        MutableDocument parent = mutableParent(delegate);
        assert parent != null;
        if (absent) {
            parent.remove(fieldName);
        }
    }

    @Override
    public void replay( MutableDocument delegate ) {
        MutableDocument parent = mutableParent(delegate);
        assert parent != null;
        if (!parent.containsField(fieldName)) {
            parent.put(fieldName, newValue);
            absent = true;
        }
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof PutIfAbsentOperation) {
            PutIfAbsentOperation other = (PutIfAbsentOperation)obj;
            return equalsIfNotNull(fieldName, other.fieldName) && absent == other.absent
                   && equalsIfNotNull(newValue, other.newValue) && getParentPath().equals(other.getParentPath());

        }
        return false;
    }

    @Override
    public String toString() {
        return "Put-if-absent at '" + parentPath + "' the '" + fieldName + "' field value '" + newValue + "'";
    }

    public static final class Externalizer extends SchematicExternalizer<PutIfAbsentOperation> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 PutIfAbsentOperation put ) throws IOException {
            output.writeObject(put.parentPath);
            output.writeUTF(put.fieldName);
            output.writeObject(put.newValue);
        }

        @Override
        public PutIfAbsentOperation readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            Path path = (Path)input.readObject();
            String fieldName = input.readUTF();
            Object newValue = input.readObject();
            return new PutIfAbsentOperation(path, fieldName, newValue);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_PUT_IF_ABSENT_OPERATION;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends PutIfAbsentOperation>> getTypeClasses() {
            return Util.<Class<? extends PutIfAbsentOperation>>asSet(PutIfAbsentOperation.class);
        }
    }
}
