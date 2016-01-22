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
package org.modeshape.schematic.internal;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.modeshape.schematic.DocumentLibrary;
import org.modeshape.schematic.document.Document;

public class InMemoryDocumentLibrary implements DocumentLibrary, Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final ConcurrentMap<String, Document> documents = new ConcurrentHashMap<>();

    public InMemoryDocumentLibrary( String name ) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Document get( String key ) {
        return documents.get(key);
    }

    @Override
    public Document put( String key,
                         Document document ) {
        return documents.put(key, document);
    }

    @Override
    public Document putIfAbsent( String key,
                                 Document document ) {
        return documents.putIfAbsent(key, document);
    }

    @Override
    public Document replace( String key,
                             Document document ) {
        return documents.replace(key, document);
    }

    @Override
    public Document remove( String key ) {
        return documents.remove(key);
    }
}
