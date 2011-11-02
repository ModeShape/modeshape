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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.infinispan.schematic.document.Bson.Type;

/**
 * Primary read-only interface for an in-memory representation of JSON/BSON objects.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
public interface Document extends Serializable {

   /**
    * Gets the value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @return The field value, if found, or null otherwise
    */
   Object get(String name);

   /**
    * Get the boolean value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @return The boolean field value, if found, or null if there is no such pair or if the value is not a boolean
    */
   Boolean getBoolean(String name);

   /**
    * Get the boolean value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @param defaultValue
    *           the default value to return if there is no such pair or if the value is not a boolean
    * @return The boolean field value if found, or <code>defaultValue</code> if there is no such pair or if the value is
    *         not a boolean
    */
   boolean getBoolean(String name, boolean defaultValue);

   /**
    * Get the integer value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @return The integer field value, if found, or null if there is no such pair or if the value is not an integer
    */
   Integer getInteger(String name);

   /**
    * Get the integer value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @param defaultValue
    *           the default value to return if there is no such pair or if the value is not a integer
    * @return The integer field value if found, or <code>defaultValue</code> if there is no such pair or if the value is
    *         not a integer
    */
   int getInteger(String name, int defaultValue);

   /**
    * Get the integer value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @return The long field value, if found, or null if there is no such pair or if the value is not a long value
    */
   Long getLong(String name);

   /**
    * Get the long value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @param defaultValue
    *           the default value to return if there is no such pair or if the value is not a long value
    * @return The long field value if found, or <code>defaultValue</code> if there is no such pair or if the value is
    *         not a long value
    */
   long getLong(String name, long defaultValue);

   /**
    * Get the double value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @return The double field value, if found, or null if there is no such pair or if the value is not a double
    */
   Double getDouble(String name);

   /**
    * Get the double value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @param defaultValue
    *           the default value to return if there is no such pair or if the value is not a double
    * @return The double field value if found, or <code>defaultValue</code> if there is no such pair or if the value is
    *         not a double
    */
   double getDouble(String name, double defaultValue);

   /**
    * Get the number value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @return The double field value, if found, or null if there is no such pair or if the value is not a number
    */
   Number getNumber(String name);

   /**
    * Get the number value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @param defaultValue
    *           the default value to return if there is no such pair or if the value is not a number
    * @return The number field value if found, or <code>defaultValue</code> if there is no such pair or if the value is
    *         not a number
    */
   Number getNumber(String name, Number defaultValue);

   /**
    * Get the string value in this document for the given field name. This method will return the string even if the
    * actual value is a {@link Symbol}.
    * 
    * @param name
    *           The name of the pair
    * @return The string field value, if found, or null if there is no such pair or if the value is not a string
    */
   String getString(String name);

   /**
    * Get the string value in this document for the given field name. This method will return the string even if the
    * actual value is a {@link Symbol}.
    * 
    * @param name
    *           The name of the pair
    * @param defaultValue
    *           the default value to return if there is no such pair or if the value is not a string
    * @return The string field value if found, or <code>defaultValue</code> if there is no such pair or if the value is
    *         not a string
    */
   String getString(String name, String defaultValue);

   /**
    * Get the array value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @return The array field value (as a list), if found, or null if there is no such pair or if the value is not an
    *         array
    */
   List<?> getArray(String name);

   /**
    * Get the document value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @return The document field value, if found, or null if there is no such pair or if the value is not a document
    */
   Document getDocument(String name);

   /**
    * Determine whether this object has a pair with the given the name and the value is null. This is equivalent to
    * calling:
    * 
    * <pre>
    * this.get(name) instanceof Null;
    * </pre>
    * 
    * @param name
    *           The name of the pair
    * @return <code>true</code> if the field has been set to a {@link Null} value, or false otherwise
    * @see #isNullOrMissing(String)
    */
   boolean isNull(String name);

   /**
    * Determine whether this object has a pair with the given the name and the value is null, or if this object has no
    * field with the given name. This is equivalent to calling:
    * 
    * <pre>
    * Null.matches(this.get(name));
    * </pre>
    * 
    * @param name
    *           The name of the pair
    * @return <code>true</code> if the field value for the name is null or if there is no such field.
    * @see #isNull(String)
    */
   boolean isNullOrMissing(String name);

   /**
    * Get the {@link MaxKey} value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @return The {@link MaxKey} field value, if found, or null if there is no such pair or if the value is not a
    *         {@link MaxKey}
    */
   MaxKey getMaxKey(String name);

   /**
    * Get the {@link MinKey} value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @return The {@link MinKey} field value, if found, or null if there is no such pair or if the value is not a
    *         {@link MinKey}
    */
   MinKey getMinKey(String name);

   /**
    * Get the {@link Code} value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @return The {@link Code} field value, if found, or null if there is no such pair or if the value is not a
    *         {@link Code}
    */
   Code getCode(String name);

   /**
    * Get the {@link CodeWithScope} value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @return The {@link CodeWithScope} field value, if found, or null if there is no such pair or if the value is not a
    *         {@link CodeWithScope}
    */
   CodeWithScope getCodeWithScope(String name);

   /**
    * Get the {@link ObjectId} value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @return The {@link ObjectId} field value, if found, or null if there is no such pair or if the value is not a
    *         {@link ObjectId}
    */
   ObjectId getObjectId(String name);

   /**
    * Get the {@link Binary} value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @return The {@link Binary} field value, if found, or null if there is no such pair or if the value is not a
    *         {@link Binary}
    */
   Binary getBinary(String name);

   /**
    * Get the {@link Symbol} value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @return The {@link Symbol} field value, if found, or null if there is no such pair or if the value is not a
    *         {@link Symbol}
    */
   Symbol getSymbol(String name);

   /**
    * Get the {@link Pattern} value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @return The {@link Pattern} field value, if found, or null if there is no such pair or if the value is not a
    *         {@link Pattern}
    */
   Pattern getPattern(String name);

   /**
    * Get the {@link UUID} value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @return The {@link UUID} field value, if found, or null if there is no such pair or if the value is not a
    *         {@link UUID}
    */
   UUID getUuid(String name);

   /**
    * Get the {@link UUID} value in this document for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @param defaultValue
    *           the default value to return if there is no such pair or if the value is not a string
    * @return The {@link UUID} field value if found, or <code>defaultValue</code> if there is no such pair or if the
    *         value is not a UUID (or a string that is convertable from a UUID)
    */
   UUID getUuid(String name, UUID defaultValue);

   /**
    * Get the {@link Type} constant that describes the type of value for the given field name.
    * 
    * @param name
    *           The name of the pair
    * @return the {@link Type} constant describing the value, or -1 if there is no field with the supplied name
    */
   int getType(String name);

   /**
    * Returns a map representing this BSONObject.
    * 
    * @return the map
    */
   Map<String, ? extends Object> toMap();

   /**
    * Obtain an iterator over the {@link Field}s in this object.
    * 
    * @return a field iterator; never null
    */
   Iterable<Field> fields();

   /**
    * Checks if this object contains a field with the given name.
    * 
    * @param name
    *           The name of the pair for which to check
    * @return true if this document contains a field with the supplied name, or false otherwise
    */
   boolean containsField(String name);

   /**
    * Checks if this object contains all of the fields in the supplied document.
    * 
    * @param document
    *           The document with the fields that should be in this document
    * @return true if this document contains all of the fields in the supplied document, or false otherwise
    */
   boolean containsAll(Document document);

   /**
    * Returns this object's fields' names
    * 
    * @return The names of the fields in this object
    */
   Set<String> keySet();

   /**
    * Return the number of name-value pairs in this object.
    * 
    * @return the number of name-value pairs; never negative
    */
   int size();

   /**
    * Return whether this document contains no fields and is therefore empty.
    * 
    * @return true if there are no fields in this document, or false if there is at least one.
    */
   boolean isEmpty();

   /**
    * Obtain a clone of this document.
    * 
    * @return the clone of this document; never null
    */
   Document clone();

   static interface Field extends Comparable<Field> {

      /**
       * Get the name of the field
       * 
       * @return the field's name; never null
       */
      String getName();

      /**
       * Get the value of the field.
       * 
       * @return the field's value; may be null
       */
      Object getValue();

      String getValueAsString();

      Integer getValueAsInt();

      boolean getValueAsBoolean();

      Binary getValueAsBinary();

      Number getValueAsNumber();

      Pattern getValueAsPattern();

      Double getValueAsDouble();

      UUID getValueAsUuid();

      Document getValueAsDocument();
   }

}
