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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.infinispan.marshall.AbstractExternalizer;
import org.infinispan.schematic.document.Path;
import org.infinispan.schematic.internal.document.MutableArray;
import org.infinispan.schematic.internal.document.MutableArray.Entry;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.Util;

/**
 * An atomic array add operation for SchematicValueDelta.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
public class RetainAllValuesOperation extends ArrayOperation {

   protected final Collection<?> values;
   protected transient List<Entry> removedEntries;

   public RetainAllValuesOperation(Path path, Collection<?> values) {
      super(path);
      this.values = values;
   }

   @Override
   public void rollback(MutableArray delegate) {
      if (removedEntries != null) {
         // Add into the same locations ...
         for (Entry entry : removedEntries) {
            delegate.add(entry.getIndex(), entry.getValue());
         }
      }
   }

   public Collection<?> getRetainedValues() {
      return values;
   }

   public List<Entry> getRemovedEntries() {
      return removedEntries;
   }

   @Override
   public void replay(MutableArray delegate) {
      removedEntries = delegate.retainAllValues(values);
   }

   public static class Externalizer extends AbstractExternalizer<RetainAllValuesOperation> {
      /** The serialVersionUID */
      private static final long serialVersionUID = 1L;

      @Override
      public void writeObject(ObjectOutput output, RetainAllValuesOperation put) throws IOException {
         output.writeObject(put.path);
         output.writeObject(put.values);
      }

      @Override
      public RetainAllValuesOperation readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Path path = (Path) input.readObject();
         Collection<?> values = (Collection<?>) input.readObject();
         return new RetainAllValuesOperation(path, values);
      }

      @Override
      public Integer getId() {
         return Ids.SCHEMATIC_VALUE_RETAIN_ALL_VALUES_OPERATION;
      }

      @SuppressWarnings("unchecked")
      @Override
      public Set<Class<? extends RetainAllValuesOperation>> getTypeClasses() {
         return Util.<Class<? extends RetainAllValuesOperation>> asSet(RetainAllValuesOperation.class);
      }
   }
}