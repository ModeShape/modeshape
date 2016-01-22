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
import java.net.HttpURLConnection;
import java.net.URL;
import org.modeshape.schematic.document.Document;
import org.modeshape.jcr.index.elasticsearch.query.MatchAllQuery;

/**
 * HTTP-based interface for the Elasticsearch engine.
 * 
 * @author kulikov
 */
public class EsClient {
    private final String host;
    private final int port;
    
    /**
     * Creates new instance.
     * 
     * @param host the address of the ES engine.
     * @param port the port number of ES engine.
     */
    public EsClient(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    /**
     * Tests for the index existence with specified name.
     * 
     * @param name the name of the index to test.
     * @return true if index with given name exists and false otherwise.
     * @throws IOException communication exception.
     */
    public boolean indexExists(String name) throws IOException {
        String url = String.format("http://%s:%d/%s", host, port, name);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } finally {
            if (connection != null) connection.disconnect();
        }        
    }
    
    /**
     * Creates new index.
     * 
     * @param name the name of the index to test.
     * @param type index type
     * @param mappings field mapping definition.
     * @return true if index was created.
     * @throws IOException communication exception.
     */
    public boolean createIndex(String name, String type, EsRequest mappings) throws IOException {
        if (indexExists(name)) {
            deleteIndex(name);
        }
        
        String url = String.format("http://%s:%d/%s", host, port, name);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (mappings != null) {
                mappings.write(connection.getOutputStream());
            }            
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } finally {
            if (connection != null) connection.disconnect();
        }        
    }

    /**
     * Deletes index.
     * 
     * @param name the name of the index to be deleted.
     * @return true if index was deleted.
     * @throws IOException 
     */
    public boolean deleteIndex(String name) throws IOException {
        String url = String.format("http://%s:%d/%s", host, port, name);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("DELETE");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
    
    /**
     * Indexes document.
     * 
     * @param name the name of the index.
     * @param type index type
     * @param id document id
     * @param doc document
     * @return true if document was indexed.
     * @throws IOException 
     */
    public boolean storeDocument(String name, String type, String id, 
            EsRequest doc) throws IOException {
        String url = String.format("http://%s:%d/%s/%s/%s", host, port, name, type, id);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");            
            doc.write(connection.getOutputStream());
            return connection.getResponseCode() == HttpURLConnection.HTTP_CREATED;
        } finally {
            if (connection != null) connection.disconnect();
        }        
    }

    /**
     * Searches indexed document.
     * 
     * @param name index name.
     * @param type index type.
     * @param id document identifier.
     * @return document if it was found or null.
     * @throws IOException 
     */
    public EsRequest getDocument(String name, String type, String id) throws IOException {
        String url = String.format("http://%s:%d/%s/%s/%s", host, port, name, type, id);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            switch (connection.getResponseCode()) {
                case HttpURLConnection.HTTP_OK :
                    EsResponse doc = EsResponse.read(connection.getInputStream());
                    return new EsRequest((Document) doc.get("_source"));
                case HttpURLConnection.HTTP_NOT_FOUND :
                case HttpURLConnection.HTTP_UNAVAILABLE :
                    return null;
                default :
                    throw new IOException(connection.getResponseMessage());
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
    
    /**
     * Deletes document.
     * 
     * @param name index name.
     * @param type index type.
     * @param id document id
     * @return true if it was deleted.
     * @throws IOException 
     */
    public boolean deleteDocument(String name, String type, String id) throws IOException {
        String url = String.format("http://%s:%d/%s/%s/%s", host, port, name, type, id);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("DELETE");
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
    
    /**
     * Deletes all documents.
     * 
     * @param name index name
     * @param type index type.
     * @throws IOException 
     */
    public void deleteAll(String name, String type) throws IOException {
        String url = String.format("http://%s:%d/%s/%s", host, port, name, type);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("DELETE");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            EsRequest query = new EsRequest();
            query.put("query", new MatchAllQuery().build());
            query.write(connection.getOutputStream());
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage());
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * Flushes index data.
     * 
     * @param name index name.
     * @throws IOException 
     */
    public void flush(String name) throws IOException {
        String url = String.format("http://%s:%d/%s/_flush", host, port, name);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage());
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * Refreshes index data.
     * 
     * @param name index name.
     * @throws IOException 
     */
    public void refresh(String name) throws IOException {
        String url = String.format("http://%s:%d/%s/_refresh", host, port, name);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage());
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
    
    /**
     * Executes query.
     * 
     * @param name index name.
     * @param type index type.
     * @param query query to be executed
     * @return search results in json format.
     * @throws IOException 
     */
    public EsResponse search(String name, String type, EsRequest query) throws IOException {
        String url = String.format("http://%s:%d/%s/%s/_search", host, port, name, type);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            query.write(connection.getOutputStream());
            
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage());
            }
            
            return EsResponse.read(connection.getInputStream());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * Counts entries.
     * 
     * @param name index name.
     * @param type index type.
     * @return number of indexed entries.
     * @throws IOException 
     */
    public long count(String name, String type) throws IOException {
        String url = String.format("http://%s:%d/%s/%s/_count", host, port, name, type);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            
            EsRequest query = new EsRequest();
            query.put("query", new MatchAllQuery().build());
            query.write(connection.getOutputStream());
            
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage());
            }
            
            return (Integer)EsResponse.read(connection.getInputStream()).get("count");
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}

