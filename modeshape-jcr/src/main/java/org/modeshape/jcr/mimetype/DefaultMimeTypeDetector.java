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

package org.modeshape.jcr.mimetype;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import org.modeshape.common.util.StringUtil;
import org.modeshape.schematic.annotation.ThreadSafe;

/**
 * A {@link MimeTypeDetector} implementation which uses a properties file to load existing mime types and decides based on the 
 * name of a binary what the mime type is.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@ThreadSafe
public final class DefaultMimeTypeDetector implements MimeTypeDetector {
    
    private final Map<String, String> mimeTypes;
    
    public DefaultMimeTypeDetector() {
        mimeTypes = loadMimeTypes();
    }
    
    private Map<String, String> loadMimeTypes() {
        try (BufferedReader reader = 
                     new BufferedReader(new InputStreamReader(DefaultMimeTypeDetector.class.getResourceAsStream("mimetypes.properties")))) {
            Map<String, String> result = new HashMap<>();
            reader.lines().forEach(line -> {
                String[] parts = line.split("\\s");
                if (parts.length >= 2) {
                    String mimeType = parts[0];
                    for (int i = 1; i < parts.length; i++) {
                        String extension = parts[i].trim();
                        if (!StringUtil.isBlank(extension)) {
                            result.put(extension, mimeType);
                        }
                    }
                }
            });
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public String mimeTypeOf(String name, Binary binaryValue) throws RepositoryException, IOException {
        if (name == null) {
            return null;
        }
        int lastDotIdx = name.lastIndexOf('.');
        String extension = lastDotIdx > 0 && lastDotIdx + 1 < name.length() ? name.substring(lastDotIdx + 1) : name;
        return mimeTypes.get(extension);
    }
}
