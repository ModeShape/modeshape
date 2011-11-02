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

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;

import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Code;
import org.infinispan.schematic.document.CodeWithScope;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.ObjectId;
import org.infinispan.schematic.document.Symbol;
import org.infinispan.schematic.document.Timestamp;

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
