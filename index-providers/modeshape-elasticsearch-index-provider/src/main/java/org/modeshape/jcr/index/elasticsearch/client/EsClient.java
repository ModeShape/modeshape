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
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
        CloseableHttpClient client = HttpClients.createDefault();
        HttpHead head = new HttpHead(String.format("http://%s:%d/%s", host, port, name));
        try {
            CloseableHttpResponse response = client.execute(head);
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } finally {
            head.releaseConnection();
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

        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost method = new HttpPost(String.format("http://%s:%d/%s", host, port, name));
        try {
            StringEntity requestEntity = new StringEntity(mappings.toString(), ContentType.APPLICATION_JSON);
            method.setEntity(requestEntity);
            CloseableHttpResponse resp = client.execute(method);
            return resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } finally {
            method.releaseConnection();
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
        CloseableHttpClient client = HttpClients.createDefault();
        HttpDelete delete = new HttpDelete(String.format("http://%s:%d/%s", host, port, name));
        try {
            CloseableHttpResponse resp = client.execute(delete);
            return resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } finally {
            delete.releaseConnection();
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
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost method = new HttpPost(String.format("http://%s:%d/%s/%s/%s", host, port, name, type, id));
        try {
            StringEntity requestEntity = new StringEntity(doc.toString(), ContentType.APPLICATION_JSON);
            method.setEntity(requestEntity);
            CloseableHttpResponse resp = client.execute(method);
            int statusCode = resp.getStatusLine().getStatusCode();
            return statusCode == HttpStatus.SC_CREATED || statusCode == HttpStatus.SC_OK;
        } finally {
            method.releaseConnection();
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
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet method = new HttpGet(String.format("http://%s:%d/%s/%s/%s", host, port, name, type, id));
        try {
            CloseableHttpResponse resp = client.execute(method);
            int status = resp.getStatusLine().getStatusCode();
            switch (status) {
                case HttpStatus.SC_OK :
                    EsResponse doc = EsResponse.read(resp.getEntity().getContent());
                    return new EsRequest((Document) doc.get("_source"));
                case HttpStatus.SC_NOT_ACCEPTABLE:
                case HttpStatus.SC_NOT_FOUND:
                    return null;
                default:
                    throw new IOException(resp.getStatusLine().getReasonPhrase());
            }
        } finally {
            method.releaseConnection();
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
        CloseableHttpClient client = HttpClients.createDefault();
        HttpDelete delete = new HttpDelete(String.format("http://%s:%d/%s/%s/%s", host, port, name, type, id));
        try {
            return client.execute(delete).getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } finally {
            delete.releaseConnection();
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
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost method = new HttpPost(String.format("http://%s:%d/%s/%s", host, port, name, type));
        try {
            EsRequest query = new EsRequest();
            query.put("query", new MatchAllQuery().build());
            StringEntity requestEntity = new StringEntity(query.toString(), ContentType.APPLICATION_JSON);
            method.setEntity(requestEntity);
            method.setHeader(" X-HTTP-Method-Override", "DELETE");
            CloseableHttpResponse resp = client.execute(method);
            int status = resp.getStatusLine().getStatusCode();
            if (status != HttpStatus.SC_OK) {
                throw new IOException(resp.getStatusLine().getReasonPhrase());
            }
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * Flushes index data.
     *
     * @param name index name.
     * @throws IOException
     */
    public void flush(String name) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost method = new HttpPost(String.format("http://%s:%d/%s/_flush", host, port, name));
        try {
            CloseableHttpResponse resp = client.execute(method);
            int status = resp.getStatusLine().getStatusCode();
            if (status != HttpStatus.SC_OK) {
                throw new IOException(resp.getStatusLine().getReasonPhrase());
            }
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * Refreshes index data.
     *
     * @param name index name.
     * @throws IOException
     */
    public void refresh(String name) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost method = new HttpPost(String.format("http://%s:%d/%s/_refresh", host, port, name));
        try {
            CloseableHttpResponse resp = client.execute(method);
            int status = resp.getStatusLine().getStatusCode();
            if (status != HttpStatus.SC_OK) {
                throw new IOException(resp.getStatusLine().getReasonPhrase());
            }
        } finally {
            method.releaseConnection();
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
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost method = new HttpPost(String.format("http://%s:%d/%s/%s/_search", host, port, name, type));
        try {
            StringEntity requestEntity = new StringEntity(query.toString(), ContentType.APPLICATION_JSON);
            method.setEntity(requestEntity);

            CloseableHttpResponse resp = client.execute(method);
            int status = resp.getStatusLine().getStatusCode();
            if (status != HttpStatus.SC_OK) {
                throw new IOException(resp.getStatusLine().getReasonPhrase());
            }
            return EsResponse.read(resp.getEntity().getContent());
        } finally {
            method.releaseConnection();
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
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost method = new HttpPost(String.format("http://%s:%d/%s/%s/_count", host, port, name, type));
        try {
            EsRequest query = new EsRequest();
            query.put("query", new MatchAllQuery().build());
            StringEntity requestEntity = new StringEntity(query.toString(), ContentType.APPLICATION_JSON);
            method.setEntity(requestEntity);

            CloseableHttpResponse resp = client.execute(method);
            int status = resp.getStatusLine().getStatusCode();
            if (status != HttpStatus.SC_OK) {
                throw new IOException(resp.getStatusLine().getReasonPhrase());
            }
            return (Integer) EsResponse.read(resp.getEntity().getContent()).get("count");
        } finally {
            method.releaseConnection();
        }
    }

}
