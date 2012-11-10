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
import org.infinispan.schematic.document.Immutable;
import org.infinispan.schematic.document.Path;
import org.infinispan.schematic.internal.HashCode;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.document.MutableDocument;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.Util;

/**
 * An atomic remove operation for SchematicValueDelta.
 * 
 * @author (various)
 */
@Immutable
@SerializeWith( RemoveOperation.Externalizer.class )
public class RemoveOperation extends Operation {
    protected final String fieldName;
    protected final Object oldValue;

    private transient boolean removed;

    public RemoveOperation( Path parentPath,
                            String fieldName,
                            Object oldValue ) {
        super(parentPath, HashCode.compute(parentPath, fieldName /*,oldValue*/));
        this.fieldName = fieldName;
        this.oldValue = oldValue;
    }

    @Override
    public RemoveOperation clone() {
        return new RemoveOperation(getParentPath(), fieldName, cloneValue(oldValue));
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public void rollback( MutableDocument delegate ) {
        if (oldValue != null) {
            delegate = mutableParent(delegate);
            delegate.put(fieldName, oldValue);
        }
    }

    @Override
    public void replay( MutableDocument delegate ) {
        MutableDocument parent = mutableParent(delegate);
        assert parent != null;
        removed = parent.remove(fieldName) != null;
    }

    public boolean isRemoved() {
        return removed;
    }

    @Override
    public String toString() {
        return "Remove from '" + parentPath + "' the '" + fieldName + "' field value '" + oldValue + "'";
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof RemoveOperation) {
            RemoveOperation other = (RemoveOperation)obj;
            // Note we don't consider the 'oldValue', since two removes are equivalent based only upon the field name ...
            return equalsIfNotNull(fieldName, other.fieldName) /*&& equalsIfNotNull(oldValue, other.oldValue)*/
                   && equalsIfNotNull(getParentPath(), other.getParentPath());

        }
        return false;
    }

    public static final class Externalizer extends SchematicExternalizer<RemoveOperation> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 RemoveOperation remove ) throws IOException {
            output.writeObject(remove.parentPath);
            output.writeUTF(remove.fieldName);
        }

        @Override
        public RemoveOperation readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            Path parentPath = (Path)input.readObject();
            String fieldName = input.readUTF();
            return new RemoveOperation(parentPath, fieldName, null);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_REMOVE_OPERATION;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends RemoveOperation>> getTypeClasses() {
            return Util.<Class<? extends RemoveOperation>>asSet(RemoveOperation.class);
        }
    }
}
