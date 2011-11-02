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

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;

import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Bson;
import org.infinispan.schematic.document.Code;
import org.infinispan.schematic.document.CodeWithScope;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Null;
import org.infinispan.schematic.document.ObjectId;
import org.infinispan.schematic.document.Symbol;
import org.infinispan.schematic.document.Timestamp;

public class DefaultDocumentValueFactory implements DocumentValueFactory, Serializable {

   public static final DocumentValueFactory INSTANCE = new DefaultDocumentValueFactory();

   private static final long serialVersionUID = 1L;

   @Override
   public String createString(String value) {
      return value;
   }

   @Override
   public Integer createInt(int value) {
      return value;
   }

   @Override
   public Long createLong(long value) {
      return value;
   }

   @Override
   public Boolean createBoolean(boolean value) {
      return value;
   }

   @Override
   public Double createDouble(double value) {
      return value;
   }

   @Override
   public Date createDate(String iso) throws ParseException {
      if (iso == null || iso.length() < 20)
         return null;
      int indexOfLastChar = iso.length() - 1;
      char lastChar = iso.charAt(indexOfLastChar);
      if (lastChar == 'Z' || lastChar == 'z') {
         // the date format doesn't like 'Z', so use timezone and offset (e.g., "GMT+00:00") ...
         iso = iso.substring(0, indexOfLastChar) + "GMT+00:00";
      } else {
         // 1997-07-16T19:20:30.45
         // 0123456789012345678901
         iso = iso.substring(0, 22) + "GMT" + iso.substring(22);
      }
      return Bson.getDateParsingFormatter().parse(iso);
   }

   @Override
   public Date createDate(long millis) {
      return new Date(millis);
   }

   @Override
   public Timestamp createTimestamp(int time, int inc) {
      return new Timestamp(time, inc);
   }

   @Override
   public ObjectId createObjectId(String hex) {
      return BsonUtils.readObjectId(hex);
   }

   @Override
   public ObjectId createObjectId(byte[] bytes) {
      return BsonUtils.readObjectId(bytes);
   }

   @Override
   public ObjectId createObjectId(int time, int machine, int process, int inc) {
      return new ObjectId(time, machine, process, inc);
   }

   @Override
   public Symbol createSymbol(String value) {
      return new Symbol(value);
   }

   @Override
   public Pattern createRegex(String pattern, String flags) {
      return createRegex(pattern, BsonUtils.regexFlagsFrom(flags));
   }

   @Override
   public Pattern createRegex(String pattern, int flags) {
      return Pattern.compile(pattern, flags);
   }

   @Override
   public Object createNull() {
      return Null.getInstance();
   }

   @Override
   public Binary createBinary(byte type, byte[] data) {
      return new Binary(type, data);
   }

   @Override
   public UUID createUuid(String uuid) {
      return UUID.fromString(uuid);
   }

   @Override
   public UUID createUuid(long part1, long part2) {
      return new UUID(part1, part2);
   }

   @Override
   public Code createCode(String code) {
      return new Code(code);
   }

   @Override
   public CodeWithScope createCode(String code, Document scope) {
      if (scope instanceof DocumentEditor) {
         scope = ((DocumentEditor) scope).unwrap();
      }
      return new CodeWithScope(code, scope);
   }

}
