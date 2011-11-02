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
package org.infinispan.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TestUtil {

   public static boolean delete(File fileOrDirectory) {
      if (fileOrDirectory == null)
         return false;
      if (!fileOrDirectory.exists())
         return false;

      // The file/directory exists, so if a directory delete all of the contents ...
      if (fileOrDirectory.isDirectory()) {
         for (File childFile : fileOrDirectory.listFiles()) {
            delete(childFile); // recursive call (good enough for now until we need something better)
         }
         // Now an empty directory ...
      }
      // Whether this is a file or empty directory, just delete it ...
      return fileOrDirectory.delete();
   }

   public static InputStream resource(String resourcePath) {
      InputStream stream = TestUtil.class.getClassLoader().getResourceAsStream(resourcePath);
      if (stream == null) {
         File file = new File(resourcePath);
         if (!file.exists()) {
            file = new File("src/test/resources" + resourcePath);
         }
         if (!file.exists()) {
            file = new File("src/test/resources/" + resourcePath);
         }
         if (file.exists()) {
            try {
               stream = new FileInputStream(file);
            } catch (IOException e) {
               throw new AssertionError("Failed to open stream to \"" + file.getAbsolutePath() + "\"");
            }
         }
      }
      assert stream != null : "Resource at \"" + resourcePath + "\" could not be found";
      return stream;
   }

}
