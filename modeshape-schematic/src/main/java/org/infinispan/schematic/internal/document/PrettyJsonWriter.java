/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.infinispan.schematic.internal.document;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Iterator;

import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Document.Field;

public class PrettyJsonWriter extends CompactJsonWriter {

   private static final int MAX_CACHED_INDENTS = 10;
   private static final String[] CACHED_INDENTS;

   static {
      CACHED_INDENTS = new String[MAX_CACHED_INDENTS];
      CACHED_INDENTS[0] = "";
      StringBuilder sb = new StringBuilder();
      for (int i = 1; i != MAX_CACHED_INDENTS; ++i) {
         sb.append(' ').append(' ');
         CACHED_INDENTS[i] = sb.toString();
      }
   }

   private int depth = 0;

   @Override
   protected void write(Document bson, Writer writer) throws IOException {
      if (bson.size() == 0) {
         writer.append("{ }");
         return;
      }
      ++depth;
      writer.append('{').append('\n');
      indent(writer);
      Iterator<Field> iter = bson.fields().iterator();
      if (iter.hasNext()) {
         write(iter.next(), writer);
         while (iter.hasNext()) {
            writer.append(',').append('\n');
            indent(writer);
            write(iter.next(), writer);
         }
      }
      writer.append('\n');
      --depth;
      indent(writer);
      writer.append('}');
   }

   @Override
   protected void write(Iterable<?> arrayValue, Writer writer) throws IOException {
      ++depth;
      writer.append('[');
      boolean first = true;
      for (Object value : arrayValue) {
         if (first) {
            first = false;
            writer.append('\n');
            indent(writer);
         } else {
            writer.append(',').append('\n');
            indent(writer);
         }
         write(value, writer);
      }
      --depth;
      if (first) {
         writer.append(' ');
      } else {
         writer.append('\n');
         indent(writer);
      }
      writer.append(']');
   }

   @Override
   protected void writeArray(Object array, Writer writer) throws IOException {
      final int len = Array.getLength(array);
      if (len == 0) {
         writer.append("[ ]");
         return;
      }
      ++depth;
      writer.append('[').append('\n');
      indent(writer);
      for (int i = 0; i < len; i++) {
         if (i > 0) {
            writer.append(',').append('\n');
            indent(writer);
         }
         write(Array.get(array, i), writer);
      }
      writer.append('\n');
      --depth;
      indent(writer);
      writer.append(']');
   }

   protected void indent(Writer writer) throws IOException {
      if (depth == 0) {
         return;
      }
      if (depth < MAX_CACHED_INDENTS) {
         writer.append(CACHED_INDENTS[depth]);
      } else {
         for (int i = 0; i != depth; ++i) {
            writer.append(' ').append(' ');
         }
      }
   }
}
