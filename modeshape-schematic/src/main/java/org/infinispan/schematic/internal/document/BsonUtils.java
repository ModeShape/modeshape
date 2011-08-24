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

import java.util.regex.Pattern;

import org.infinispan.schematic.document.Null;
import org.infinispan.schematic.document.ObjectId;

public class BsonUtils {

   public static int regexFlagsFrom(String flags) {
      if (flags == null)
         return 0;
      flags = flags.toLowerCase();
      int value = 0;

      for (int i = 0; i < flags.length(); i++) {
         char c = flags.charAt(i);
         switch (c) {
            case 'i':
               value |= Pattern.CASE_INSENSITIVE;
               break;
            case 'm':
               value |= Pattern.MULTILINE;
               break;
            case 'd':
               value |= Pattern.UNIX_LINES;
               break;
            case 'c':
               value |= Pattern.CANON_EQ;
               break;
            case 's':
               value |= Pattern.DOTALL;
               break;
            case 't':
               value |= Pattern.LITERAL;
               break;
            case 'u':
               value |= Pattern.UNICODE_CASE;
               break;
            case 'x':
               value |= Pattern.COMMENTS;
               break;
            case 'g': // global flag
               // ignore
               break;
            default:
               throw new IllegalArgumentException("unrecognized regex flag '" + c + "'");
         }
      }
      return value;
   }

   public static String regexFlagsFor(Pattern pattern) {
      return regexFlagsFor(pattern.flags());
   }

   public static String regexFlagsFor(int flags) {
      if (flags == 0)
         return "";
      StringBuilder sb = new StringBuilder();
      // Order of characters is important here
      if ((flags & Pattern.CANON_EQ) == Pattern.CANON_EQ)
         sb.append('c');
      if ((flags & Pattern.UNIX_LINES) == Pattern.UNIX_LINES)
         sb.append('d');
      if ((flags & Pattern.CASE_INSENSITIVE) == Pattern.CASE_INSENSITIVE)
         sb.append('i');
      if ((flags & Pattern.MULTILINE) == Pattern.MULTILINE)
         sb.append('m');
      if ((flags & Pattern.DOTALL) == Pattern.DOTALL)
         sb.append('s');
      if ((flags & Pattern.LITERAL) == Pattern.LITERAL)
         sb.append('t');
      if ((flags & Pattern.UNICODE_CASE) == Pattern.UNICODE_CASE)
         sb.append('u');
      if ((flags & Pattern.COMMENTS) == Pattern.COMMENTS)
         sb.append('x');
      return sb.toString();
   }

   /**
    * The BSON specification (or rather the <a href="http://www.mongodb.org/display/DOCS/Object+IDs">MongoDB
    * documentation</a>) defines the structure of this data:
    * <p>
    * <quote>"A BSON ObjectID is a 12-byte value consisting of a 4-byte timestamp (seconds since epoch), a 3-byte
    * machine id, a 2-byte process id, and a 3-byte counter. Note that the timestamp and counter fields must be stored
    * big endian unlike the rest of BSON. This is because they are compared byte-by-byte and we want to ensure a mostly
    * increasing order."</quote>
    * </p>
    * 
    * @param bytes
    * @return the ObjectId
    */
   public static ObjectId readObjectId(byte[] bytes) {
      // Compute the values in big-endian ...
      int time = ((bytes[0] & 0xff) << 24) + ((bytes[1] & 0xff) << 16) + ((bytes[2] & 0xff) << 8)
               + ((bytes[3] & 0xff) << 0);
      int machine = ((bytes[4] & 0xff) << 16) + ((bytes[5] & 0xff) << 8) + ((bytes[6] & 0xff) << 0);
      int process = ((bytes[7] & 0xff) << 8) + ((bytes[8] & 0xff) << 0);
      int inc = ((bytes[9] & 0xff) << 16) + ((bytes[10] & 0xff) << 8) + ((bytes[11] & 0xff) << 0);
      // Create the value object ...
      return new ObjectId(time, machine, process, inc);
   }

   /**
    * Read the {@link ObjectId} from the hexadecimal string representation of the ObjectId.
    * 
    * @param bytesInHex
    *           the hexadecimal identifier
    * @return the ObjectId
    */
   public static ObjectId readObjectId(String bytesInHex) {
      byte[] bytes = new byte[12];
      assert bytesInHex.length() == 24;
      for (int i = 0, index = 0; i != 12; ++i) {
         String hexChar = bytesInHex.substring(index, index += 2);
         bytes[i] = (byte) Integer.parseInt(hexChar, 16);
      }
      return readObjectId(bytes);
   }

   /**
    * Write the 12-byte representation of the ObjectId per the BSON specification (or rather the <a
    * href="http://www.mongodb.org/display/DOCS/Object+IDs">MongoDB documentation</a>).
    * 
    * @param id
    *           the ObjectId; may not be null
    * @param b
    *           the bytes into which the object ID should be written
    */
   public static void writeObjectId(ObjectId id, byte[] b) {
      int time = id.getTime();
      int machine = id.getMachine();
      int process = id.getProcess();
      int inc = id.getInc();
      b[0] = (byte) ((time >>> 24) & 0xFF);
      b[1] = (byte) ((time >>> 16) & 0xFF);
      b[2] = (byte) ((time >>> 8) & 0xFF);
      b[3] = (byte) ((time >>> 0) & 0xFF);
      b[4] = (byte) ((machine >>> 16) & 0xFF);
      b[5] = (byte) ((machine >>> 8) & 0xFF);
      b[6] = (byte) ((machine >>> 0) & 0xFF);
      b[7] = (byte) ((process >>> 8) & 0xFF);
      b[8] = (byte) ((process >>> 0) & 0xFF);
      b[9] = (byte) ((inc >>> 16) & 0xFF);
      b[10] = (byte) ((inc >>> 8) & 0xFF);
      b[11] = (byte) ((inc >>> 0) & 0xFF);
   }

   public static boolean valuesAreEqual(Object thisValue, Object thatValue) {
      if (thisValue == thatValue) {
         return true;
      }
      if (thisValue == null || thisValue instanceof Null) {
         return Null.matches(thatValue);
      }
      if (thisValue instanceof Number && thatValue instanceof Number) {
         // Need to handle float vs. doubles
         Number thisNumber = (Number) thisValue;
         Number thatNumber = (Number) thatValue;
         return thisNumber.doubleValue() == thatNumber.doubleValue();
      }
      if (thisValue instanceof Pattern && thatValue instanceof Pattern) {
         // java.util.regex.Pattern does not implement equals!!
         Pattern thisPattern = (Pattern) thisValue;
         Pattern thatPattern = (Pattern) thatValue;
         return thisPattern.pattern().equals(thatPattern.pattern()) && thisPattern.flags() == thatPattern.flags();
      }
      return thisValue.equals(thatValue);
   }

}
