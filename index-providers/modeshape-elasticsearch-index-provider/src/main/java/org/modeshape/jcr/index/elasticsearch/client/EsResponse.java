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
package org.modeshape.jcr.index.elasticsearch.client;

import java.io.IOException;
import java.io.InputStream;
import org.modeshape.schematic.DocumentFactory;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Json;
import org.modeshape.schematic.internal.document.BasicArray;

/**
 * Json document used for exchange data with Elasticsearch engine.
 * 
 * @author kulikov
 */
public class EsResponse {
    private Document document;
    
    public static EsResponse read(InputStream in) throws IOException {
        EsResponse resp = new EsResponse();
        resp.readFromStream(in);
        return resp;
    }
    
    /**
     * Creates new document.
     */
    private EsResponse() {        
    }

    /**
     * Gets property value.
     * 
     * @param name property name.
     * @return property value.
     */
    public Object get(String name) {
        Object obj = document.get(name);
        return (obj instanceof BasicArray) ? ((BasicArray)obj).toArray() : obj;
    }
        
    /**
     * Reads document content from the stream.
     * 
     * @param in input stream.
     * @throws IOException 
     */
    private void readFromStream(InputStream in) throws IOException {
        document = DocumentFactory.newDocument(Json.read(in));
    }
    
    @Override
    public String toString() {
        return document.toString();
    }
}
