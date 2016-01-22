/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.schematic.internal.document;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;
import org.modeshape.schematic.document.Binary;
import org.modeshape.schematic.document.Bson;
import org.modeshape.schematic.document.Code;
import org.modeshape.schematic.document.CodeWithScope;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Null;
import org.modeshape.schematic.document.ObjectId;
import org.modeshape.schematic.document.Symbol;
import org.modeshape.schematic.document.Timestamp;

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
      } else if (iso.length() >= 22) {
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
