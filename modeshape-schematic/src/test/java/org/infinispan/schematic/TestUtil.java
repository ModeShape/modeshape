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
