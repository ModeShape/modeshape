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
import org.infinispan.marshall.AbstractExternalizer;
import org.infinispan.schematic.document.Path;
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
public class AddValueOperation extends ArrayOperation {

    protected static final int APPEND_INDEX = -1;

    protected final Object value;
    protected final int index;
    protected transient int actualIndex = -1;

    public AddValueOperation( Path path,
                              Object value ) {
        super(path);
        this.value = value;
        this.index = APPEND_INDEX;
    }

    public AddValueOperation( Path path,
                              Object value,
                              int index ) {
        super(path);
        this.value = value;
        this.index = index;
    }

    public Object getValue() {
        return value;
    }

    public int getIndex() {
        return index;
    }

    public int getActualIndex() {
        return actualIndex;
    }

    @Override
    public void rollback( MutableDocument delegate ) {
        if (actualIndex > -1) {
            MutableArray array = mutableParent(delegate);
            array.remove(actualIndex);
        }
    }

    @Override
    public void replay( MutableDocument delegate ) {
        MutableArray array = mutableParent(delegate);
        if (index == APPEND_INDEX) {
            actualIndex = array.addValue(value);
        } else {
            array.addValue(index, value);
            actualIndex = index;
        }
    }

    @Override
    public String toString() {
        return "Add to '" + parentPath + "' the value '" + value + "'" + (index >= 0 ? " at index " + index : "");
    }

    public static class Externalizer extends AbstractExternalizer<AddValueOperation> {
        /** The serialVersionUID */
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 AddValueOperation put ) throws IOException {
            output.writeObject(put.parentPath);
            output.writeObject(put.index);
            output.writeObject(put.value);
        }

        @Override
        public AddValueOperation readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            Path path = (Path)input.readObject();
            int index = input.readInt();
            Object value = input.readObject();
            return new AddValueOperation(path, value, index);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_ADD_OPERATION;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends AddValueOperation>> getTypeClasses() {
            return Util.<Class<? extends AddValueOperation>>asSet(AddValueOperation.class);
        }
    }
}
