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
@SerializeWith( RemoveAtIndexOperation.Externalizer.class )
public class RemoveAtIndexOperation extends ArrayOperation {

    protected final int index;
    protected transient Object actualValue = null;

    public RemoveAtIndexOperation( Path path,
                                   int index ) {
        super(path);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public Object getRemovedValue() {
        return actualValue;
    }

    @Override
    public void rollback( MutableDocument delegate ) {
        if (actualValue != null) {
            MutableArray array = mutableParent(delegate);
            array.add(index, actualValue);
        }
    }

    @Override
    public void replay( MutableDocument delegate ) {
        MutableArray array = mutableParent(delegate);
        actualValue = array.remove(index);
    }

    @Override
    public String toString() {
        return "Remove at '" + parentPath + "' the value at index " + index;
    }

    public static final class Externalizer extends SchematicExternalizer<RemoveAtIndexOperation> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 RemoveAtIndexOperation put ) throws IOException {
            output.writeObject(put.parentPath);
            output.writeObject(put.index);
        }

        @Override
        public RemoveAtIndexOperation readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            Path path = (Path)input.readObject();
            int index = input.readInt();
            return new RemoveAtIndexOperation(path, index);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_REMOVE_AT_INDEX_OPERATION;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends RemoveAtIndexOperation>> getTypeClasses() {
            return Util.<Class<? extends RemoveAtIndexOperation>>asSet(RemoveAtIndexOperation.class);
        }
    }
}
