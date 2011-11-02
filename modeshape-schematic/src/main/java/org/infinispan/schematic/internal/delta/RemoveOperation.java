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
import org.infinispan.schematic.document.Immutable;
import org.infinispan.schematic.document.Path;
import org.infinispan.schematic.internal.document.MutableDocument;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.Util;

/**
 * An atomic remove operation for SchematicValueDelta.
 * 
 * @author (various)
 */
@Immutable
public class RemoveOperation extends Operation {
    protected final Object oldValue;

    private transient boolean removed;

    public RemoveOperation( Path path,
                            Object oldValue ) {
        super(path);
        this.oldValue = oldValue;
    }

    @Override
    public void rollback( MutableDocument delegate ) {
        MutableDocument parent = mutableParentInDelegate(delegate);
        assert parent != null;
        if (oldValue != null) {
            delegate.put(path.getLast(), oldValue);
        }
    }

    @Override
    public void replay( MutableDocument delegate ) {
        MutableDocument parent = mutableParentInDelegate(delegate);
        assert parent != null;
        removed = parent.remove(path.getLast()) != null;
    }

    public boolean isRemoved() {
        return removed;
    }

    public static class Externalizer extends AbstractExternalizer<RemoveOperation> {
        /** The serialVersionUID */
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 RemoveOperation remove ) throws IOException {
            output.writeObject(remove.path);
        }

        @Override
        public RemoveOperation readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            Path path = (Path)input.readObject();
            return new RemoveOperation(path, null);
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
