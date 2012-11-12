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
import org.infinispan.schematic.internal.document.MutableArray;
import org.infinispan.schematic.internal.document.MutableDocument;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.Util;

/**
 * An atomic array add operation for SchematicValueDelta.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
@SerializeWith( SetValueOperation.Externalizer.class )
public class SetValueOperation extends ArrayOperation {

    protected final Object value;
    protected final int index;

    protected transient Object oldValue;

    public SetValueOperation( Path parentPath,
                              Object value,
                              int index ) {
        super(parentPath, HashCode.compute(parentPath, value, index));
        this.value = value;
        this.index = index;
    }

    @Override
    public SetValueOperation clone() {
        return new SetValueOperation(getParentPath(), cloneValue(value), index);
    }

    @Override
    public void rollback( MutableDocument delegate ) {
        MutableArray array = mutableParent(delegate);
        if (oldValue != null) {
            array.set(index, oldValue);
        } else {
            array.remove(index);
        }
    }

    public int getIndex() {
        return index;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public void replay( MutableDocument delegate ) {
        MutableArray array = mutableParent(delegate);
        oldValue = array.setValue(index, value);
    }

    @Override
    public String toString() {
        return "Set at '" + parentPath + "' the value '" + value + "' (at index " + index + ")";
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof SetValueOperation) {
            SetValueOperation other = (SetValueOperation)obj;
            return equalsIfNotNull(value, other.value) && index == other.index
                   && equalsIfNotNull(getParentPath(), other.getParentPath());

        }
        return false;
    }

    @SerializeWith( Externalizer.class )
    public static final class Externalizer extends SchematicExternalizer<SetValueOperation> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 SetValueOperation put ) throws IOException {
            output.writeObject(put.parentPath);
            output.writeObject(put.index);
            output.writeObject(put.value);
        }

        @Override
        public SetValueOperation readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            Path path = (Path)input.readObject();
            int index = input.readInt();
            Object value = input.readObject();
            return new SetValueOperation(path, value, index);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_SET_VALUE_OPERATION;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends SetValueOperation>> getTypeClasses() {
            return Util.<Class<? extends SetValueOperation>>asSet(SetValueOperation.class);
        }
    }
}
