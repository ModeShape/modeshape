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

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;
import org.modeshape.schematic.document.Binary;
import org.modeshape.schematic.document.Code;
import org.modeshape.schematic.document.CodeWithScope;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.ObjectId;
import org.modeshape.schematic.document.Symbol;
import org.modeshape.schematic.document.Timestamp;

public interface DocumentValueFactory {

   String createString(String value);

   Integer createInt(int value);

   Long createLong(long value);

   Boolean createBoolean(boolean value);

   Double createDouble(double value);

   Date createDate(String iso) throws ParseException;

   Date createDate(long millis);

   Timestamp createTimestamp(int time, int inc);

   ObjectId createObjectId(String hex);

   ObjectId createObjectId(byte[] bytes);

   ObjectId createObjectId(int time, int machine, int process, int inc);

   Symbol createSymbol(String value);

   Pattern createRegex(String pattern, String flags);

   Pattern createRegex(String pattern, int flags);

   Object createNull();

   Binary createBinary(byte type, byte[] data);

   UUID createUuid(String uuid);

   UUID createUuid(long part1, long part2);

   Code createCode(String code);

   CodeWithScope createCode(String code, Document scope);

}
