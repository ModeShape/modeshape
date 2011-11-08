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
import org.infinispan.schematic.internal.document.MutableDocument;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.Util;

/**
 * An atomic put operation for SchematicValueDelta.
 * 
 * @author (various)
 */
public class PutOperation extends Operation {
    protected final String fieldName;
    protected final Object oldValue;
    protected final Object newValue;

    public PutOperation( Path parentPath,
                         String fieldName,
                         Object oldValue,
                         Object newValue ) {
        super(parentPath);
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public void rollback( MutableDocument delegate ) {
        MutableDocument parent = mutableParent(delegate);
        assert parent != null;
        if (oldValue == null) {
            parent.remove(fieldName);
        } else {
            parent.put(fieldName, oldValue);
        }
    }

    @Override
    public void replay( MutableDocument delegate ) {
        MutableDocument parent = mutableParent(delegate);
        assert parent != null;
        parent.put(fieldName, newValue);
    }

    @Override
    public String toString() {
        return "Put at '" + parentPath + "' the '" + fieldName + "' field value '" + newValue
               + (oldValue != null ? "' (replaces '" + oldValue + "')" : "");
    }

    public static class Externalizer extends AbstractExternalizer<PutOperation> {
        /** The serialVersionUID */
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 PutOperation put ) throws IOException {
            output.writeObject(put.parentPath);
            output.writeUTF(put.fieldName);
            output.writeObject(put.newValue);
        }

        @Override
        public PutOperation readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            Path path = (Path)input.readObject();
            String fieldName = input.readUTF();
            Object newValue = input.readObject();
            return new PutOperation(path, fieldName, null, newValue);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_PUT_OPERATION;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends PutOperation>> getTypeClasses() {
            return Util.<Class<? extends PutOperation>>asSet(PutOperation.class);
        }
    }
}
