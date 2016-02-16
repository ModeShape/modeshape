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
import java.io.OutputStream;
import org.modeshape.schematic.DocumentFactory;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableDocument;
import org.modeshape.schematic.document.Json;
import org.modeshape.schematic.internal.document.BasicArray;

/**
 * Json document used for exchange data with Elasticsearch engine.
 * 
 * @author kulikov
 */
public class EsRequest {
    private final EditableDocument document;
    
    /**
     * Creates new document.
     */
    public EsRequest() {        
        document = DocumentFactory.newDocument();
    }

    /**
     * Creates new document with initial content.
     * 
     * @param origin initial content.
     */
    public EsRequest(Document origin) {        
        document = DocumentFactory.newDocument(origin);
    }
    
    /**
     * Adds single property value.
     * 
     * @param name property name.
     * @param value property value.
     */
    public void put(String name, Object value) {
        if (value instanceof EsRequest) {
            document.setDocument(name, ((EsRequest)value).document);
        } else {
            document.set(name, value);
        }
    }

    /**
     * Adds multivalued property value.
     * 
     * @param name property name.
     * @param value property values.
     */
    public void put(String name, Object[] values) {
        if (values instanceof EsRequest[]) {
            Object[] docs = new Object[values.length];
            for (int i = 0; i < docs.length; i++) {
                docs[i] = ((EsRequest)values[i]).document;
            }
            document.setArray(name, docs);
        } else {
            document.setArray(name, values);
        }
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
     * Removes property.
     * 
     * @param name property name.
     */
    public void remove(String name) {
        document.remove(name);
    }
    
    /**
     * Writes document content to the stream.
     * 
     * @param out output stream.
     * @throws IOException 
     */
    public void write(OutputStream out) throws IOException {
        Json.write(document, out);
    }
    
    @Override
    public String toString() {
        return document.toString();
    }
}
