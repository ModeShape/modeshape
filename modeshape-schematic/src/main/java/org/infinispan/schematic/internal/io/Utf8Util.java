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
package org.infinispan.schematic.internal.io;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public final class Utf8Util {

   private static transient Charset utf8;
   private static transient CharsetDecoder utf8Decoder;
   private static transient CharsetEncoder utf8Encoder;

   private Utf8Util() {
      // prevent instantiation
   }

   protected static Charset getUtf8() {
      if (utf8 == null) {
         utf8 = Charset.forName("UTF-8");
      }
      return utf8;
   }

   protected static CharsetDecoder getUtf8Decoder() {
      if (utf8Decoder == null) {
         utf8Decoder = getUtf8().newDecoder();
      }
      return utf8Decoder;
   }

   protected static CharsetEncoder getUtf8Encoder() {
      if (utf8Encoder == null) {
         utf8Encoder = getUtf8().newEncoder();
      }
      return utf8Encoder;
   }

}
