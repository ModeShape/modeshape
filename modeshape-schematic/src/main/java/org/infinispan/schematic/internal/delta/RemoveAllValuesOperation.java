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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.infinispan.marshall.SerializeWith;
import org.infinispan.schematic.document.Array.Entry;
import org.infinispan.schematic.document.Path;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.document.BasicArray;
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
@SerializeWith( RemoveAllValuesOperation.Externalizer.class )
public class RemoveAllValuesOperation extends ArrayOperation {

    protected final Collection<?> values;
    protected transient int[] actualIndexes;

    public RemoveAllValuesOperation( Path path,
                                     Collection<?> values ) {
        super(path);
        this.values = values;
    }

    public Collection<?> getValuesToRemove() {
        return values;
    }

    public synchronized List<Entry> getRemovedEntries() {
        List<Entry> entries = new ArrayList<Entry>(this.values.size());
        Iterator<?> valueIter = values.iterator();
        for (int i = 0; i != actualIndexes.length; ++i) {
            int index = actualIndexes[i];
            Object value = valueIter.next();
            entries.add(new BasicArray.BasicEntry(index, value));
        }
        return entries;
    }

    @Override
    public synchronized void rollback( MutableDocument delegate ) {
        if (actualIndexes != null) {
            MutableArray array = mutableParent(delegate);
            // Add into the same locations ...
            int i = 0;
            for (Object value : values) {
                int index = actualIndexes[i++];
                if (index != -1) array.add(index, value);
            }
        }
    }

    @Override
    public synchronized void replay( MutableDocument delegate ) {
        if (!values.isEmpty()) {
            actualIndexes = new int[values.size()];
            int i = 0;
            MutableArray array = mutableParent(delegate);
            for (Object value : values) {
                int actualIndex = array.indexOf(value);
                array.remove(actualIndex);
                actualIndexes[i++] = actualIndex;
            }
        } else {
            actualIndexes = null;
        }
    }

    @Override
    public String toString() {
        return "Remove at '" + parentPath + "' the values: " + values;
    }

    public static final class Externalizer extends SchematicExternalizer<RemoveAllValuesOperation> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 RemoveAllValuesOperation put ) throws IOException {
            output.writeObject(put.parentPath);
            output.writeObject(put.values);
        }

        @Override
        public RemoveAllValuesOperation readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            Path path = (Path)input.readObject();
            Collection<?> values = (Collection<?>)input.readObject();
            return new RemoveAllValuesOperation(path, values);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_REMOVE_ALL_VALUES_OPERATION;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends RemoveAllValuesOperation>> getTypeClasses() {
            return Util.<Class<? extends RemoveAllValuesOperation>>asSet(RemoveAllValuesOperation.class);
        }
    }
}
