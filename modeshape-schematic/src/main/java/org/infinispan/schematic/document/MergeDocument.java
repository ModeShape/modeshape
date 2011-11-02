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
package org.infinispan.schematic.document;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.infinispan.schematic.internal.document.BsonUtils;

/**
 * A {@link Document} implementation that presents the merger of two other documents, where the first document is used
 * before the second.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
public class MergeDocument implements Document {

   private static final long serialVersionUID = 1L;

   private final Document doc1;
   private final Document doc2;

   /**
    * Create a document that contains all of the fields from the two documents, with the first document taking
    * precedence.
    * 
    * @param document1
    *           the first (preceding) document; may not be null
    * @param document2
    *           the second document; may not be null
    */
   public MergeDocument(Document document1, Document document2) {
      this.doc1 = document1;
      this.doc2 = document2;
   }

   /**
    * Create a document that contains all of the fields from the three documents, with the first document taking
    * precedence over the others, and the second taking precedence over the third.
    * 
    * @param document1
    *           the first (preceding) document; may not be null
    * @param document2
    *           the second document; may not be null
    * @param document3
    *           the third document; may not be null
    */
   public MergeDocument(Document document1, Document document2, Document document3) {
      this.doc1 = document1;
      this.doc2 = new MergeDocument(document2, document3);
   }
   
   @Override
   public MergeDocument clone() {
      return new MergeDocument(doc1.clone(),doc2.clone());
   }

   @Override
   public Object get(String name) {
      Object result = doc1.get(name);
      return result != null ? result : doc2.get(name);
   }

   @Override
   public Boolean getBoolean(String name) {
      Boolean result = doc1.getBoolean(name);
      return result != null ? result : doc2.getBoolean(name);
   }

   @Override
   public boolean getBoolean(String name, boolean defaultValue) {
      Boolean result = doc1.getBoolean(name);
      return result != null ? result : doc2.getBoolean(name, defaultValue);
   }

   @Override
   public Integer getInteger(String name) {
      Integer result = doc1.getInteger(name);
      return result != null ? result : doc2.getInteger(name);
   }

   @Override
   public int getInteger(String name, int defaultValue) {
      Integer result = doc1.getInteger(name);
      return result != null ? result : doc2.getInteger(name, defaultValue);
   }

   @Override
   public Long getLong(String name) {
      Long result = doc1.getLong(name);
      return result != null ? result : doc2.getLong(name);
   }

   @Override
   public long getLong(String name, long defaultValue) {
      Long result = doc1.getLong(name);
      return result != null ? result : doc2.getLong(name, defaultValue);
   }

   @Override
   public Double getDouble(String name) {
      Double result = doc1.getDouble(name);
      return result != null ? result : doc2.getDouble(name);
   }

   @Override
   public double getDouble(String name, double defaultValue) {
      Double result = doc1.getDouble(name);
      return result != null ? result : doc2.getDouble(name, defaultValue);
   }

   @Override
   public Number getNumber(String name) {
      Number result = doc1.getNumber(name);
      return result != null ? result : doc2.getNumber(name);
   }

   @Override
   public Number getNumber(String name, Number defaultValue) {
      Number result = doc1.getNumber(name);
      return result != null ? result : doc2.getNumber(name, defaultValue);
   }

   @Override
   public String getString(String name) {
      return getString(name, null);
   }

   @Override
   public String getString(String name, String defaultValue) {
      String result = doc1.getString(name);
      return result != null ? result : doc2.getString(name, defaultValue);
   }

   @Override
   public List<?> getArray(String name) {
      List<?> result = doc1.getArray(name);
      return result != null ? result : doc2.getArray(name);
   }

   @Override
   public Document getDocument(String name) {
      Document result1 = doc1.getDocument(name);
      Document result2 = doc2.getDocument(name);
      if (result1 == null)
         return result2;
      return result2 != null ? new MergeDocument(result1, result2) : result1;
   }

   @Override
   public boolean isNull(String name) {
      return doc1.isNull(name) && doc2.isNull(name);
   }

   @Override
   public boolean isNullOrMissing(String name) {
      return doc1.isNullOrMissing(name) && doc2.isNullOrMissing(name);
   }

   @Override
   public MaxKey getMaxKey(String name) {
      MaxKey result = doc1.getMaxKey(name);
      return result != null ? result : doc2.getMaxKey(name);
   }

   @Override
   public MinKey getMinKey(String name) {
      MinKey result = doc1.getMinKey(name);
      return result != null ? result : doc2.getMinKey(name);
   }

   @Override
   public Code getCode(String name) {
      Code result = doc1.getCode(name);
      return result != null ? result : doc2.getCode(name);
   }

   @Override
   public CodeWithScope getCodeWithScope(String name) {
      CodeWithScope result = doc1.getCodeWithScope(name);
      return result != null ? result : doc2.getCodeWithScope(name);
   }

   @Override
   public ObjectId getObjectId(String name) {
      ObjectId result = doc1.getObjectId(name);
      return result != null ? result : doc2.getObjectId(name);
   }

   @Override
   public Binary getBinary(String name) {
      Binary result = doc1.getBinary(name);
      return result != null ? result : doc2.getBinary(name);
   }

   @Override
   public Symbol getSymbol(String name) {
      Symbol result = doc1.getSymbol(name);
      return result != null ? result : doc2.getSymbol(name);
   }

   @Override
   public Pattern getPattern(String name) {
      Pattern result = doc1.getPattern(name);
      return result != null ? result : doc2.getPattern(name);
   }

   @Override
   public UUID getUuid(String name) {
      UUID result = doc1.getUuid(name);
      return result != null ? result : doc2.getUuid(name);
   }

   @Override
   public UUID getUuid(String name, UUID defaultValue) {
      UUID result = doc1.getUuid(name);
      return result != null ? result : doc2.getUuid(name, defaultValue);
   }

   @Override
   public int getType(String name) {
      int type = doc1.getType(name);
      return type > -1 ? type : doc2.getType(name);
   }

   @Override
   public Map<String, ? extends Object> toMap() {
      // Add the vaues from 'doc2' first, since they'd be overwritten by those in 'doc1' ...
      Map<String, Object> result = new HashMap<String, Object>(doc2.toMap());
      result.putAll(doc1.toMap());
      return result;
   }

   @Override
   public Iterable<Field> fields() {
      if (doc1.isEmpty())
         return doc2.fields();
      if (doc2.isEmpty())
         return doc1.fields();
      // Otherwise, they're both not empty ...
      final Iterator<Field> iter1 = doc1.fields().iterator();
      final Iterator<Field> iter2 = doc2.fields().iterator();
      return new Iterable<Field>() {
         @Override
         public Iterator<Field> iterator() {
            return new Iterator<Field>() {
               private boolean first = true;

               @Override
               public boolean hasNext() {
                  boolean result = false;
                  if (first) {
                     result = iter1.hasNext();
                     if (result) {
                        return result;
                     }
                     // No more in iter1 ...
                     first = false;
                  }
                  return iter2.hasNext();
               }

               @Override
               public Field next() {
                  return first ? iter1.next() : iter2.next();
               }

               @Override
               public void remove() {
                  if (first) {
                     iter1.remove();
                  } else {
                     iter2.remove();
                  }
               }
            };
         }
      };
   }

   @Override
   public boolean containsField(String name) {
      return doc1.containsField(name) || doc2.containsField(name);
   }

   @Override
   public boolean containsAll(Document document) {
      if (document == null) {
         return true;
      }
      for (Field field : document.fields()) {
         Object thisValue = this.get(field.getName());
         Object thatValue = field.getValue();
         if (!BsonUtils.valuesAreEqual(thisValue, thatValue)) {
            return false;
         }
      }
      return true;
   }

   @Override
   public Set<String> keySet() {
      Set<String> keys = new HashSet<String>(doc1.keySet());
      keys.addAll(doc2.keySet());
      return keys;
   }

   @Override
   public int size() {
      return keySet().size();
   }

   @Override
   public boolean isEmpty() {
      return doc1.isEmpty() && doc2.isEmpty();
   }

}
