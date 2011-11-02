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
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.Util;

/**
 * An atomic array add operation for SchematicValueDelta.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
public class AddValueIfAbsentOperation extends AddValueOperation {

   protected transient boolean added;

   public AddValueIfAbsentOperation(Path path, Object value) {
      super(path, value);
   }

   public boolean isAdded() {
      return added;
   }

   @Override
   public void rollback(MutableArray delegate) {
      if (added) {
         delegate.remove(this.index);
      }
   }

   @Override
   public void replay(MutableArray delegate) {
      added = delegate.addValueIfAbsent(value);
   }

   public static class Externalizer extends AbstractExternalizer<AddValueIfAbsentOperation> {
      /** The serialVersionUID */
      private static final long serialVersionUID = 1L;

      @Override
      public void writeObject(ObjectOutput output, AddValueIfAbsentOperation put) throws IOException {
         output.writeObject(put.path);
         output.writeObject(put.value);
      }

      @Override
      public AddValueIfAbsentOperation readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Path path = (Path) input.readObject();
         Object value = input.readObject();
         return new AddValueIfAbsentOperation(path, value);
      }

      @Override
      public Integer getId() {
         return Ids.SCHEMATIC_VALUE_ADD_IF_ABSENT_OPERATION;
      }

      @SuppressWarnings("unchecked")
      @Override
      public Set<Class<? extends AddValueIfAbsentOperation>> getTypeClasses() {
         return Util.<Class<? extends AddValueIfAbsentOperation>> asSet(AddValueIfAbsentOperation.class);
      }
   }
}