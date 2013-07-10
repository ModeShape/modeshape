/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr.value.binary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import org.apache.commons.compress.utils.IOUtils;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.AlreadyExistsException;
import com.datastax.driver.core.exceptions.InvalidQueryException;

/**
 * @author kulikov
 */
public class CassandraBinaryStore extends AbstractBinaryStore {

    private static final boolean ALIVE = true;
    private static final boolean UNUSED = false;

    private Cluster cluster;
    private Session session;
    private String address;

    private FileSystemBinaryStore cache;

    public CassandraBinaryStore( String address ) {
        this.address = address;
        this.cache = TransientBinaryStore.get();
    }

    @Override
    protected String getStoredMimeType( BinaryValue source ) throws BinaryStoreException {
        try {
            checkContentExists(source);
            ResultSet rs = session.execute("SELECT mime_type FROM modeshape.binary WHERE cid = '" + source.getKey() + "';");
            Row row = rs.one();
            if (row == null) {
                throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(source.getKey(), session));
            }
            return row.getString("mime_type");
        } catch (BinaryStoreException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BinaryStoreException(e);
        }
    }

    private void checkContentExists( BinaryValue source ) throws BinaryStoreException {
        if (!contentExists(source.getKey(), true)) {
            throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(source.getKey(), session));
        }
    }

    @Override
    protected void storeMimeType( BinaryValue source,
                                  String mimeType ) throws BinaryStoreException {
        try {
            session.execute("UPDATE modeshape.binary SET mime_type='" + mimeType + "' where cid='" + source.getKey() + "';");
        } catch (RuntimeException e) {
            throw new BinaryStoreException(e);
        }
    }

    @Override
    public void storeExtractedText( BinaryValue source,
                                    String extractedText ) throws BinaryStoreException {
        try {
            session.execute("UPDATE modeshape.binary SET ext_text='" + extractedText + "' where cid='" + source.getKey() + "';");
        } catch (RuntimeException e) {
            throw new BinaryStoreException(e);
        }
    }

    @Override
    public String getExtractedText( BinaryValue source ) throws BinaryStoreException {
        try {
            checkContentExists(source);
            ResultSet rs = session.execute("SELECT ext_text FROM modeshape.binary WHERE cid = '" + source.getKey() + "';");
            Row row = rs.one();
            if (row == null) {
                throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(source.getKey(), session));
            }
            return row.getString("ext_text");
        } catch (BinaryStoreException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BinaryStoreException(e);
        }
    }

    @Override
    public BinaryValue storeValue( InputStream stream ) throws BinaryStoreException {
        // store into temporary file system store and get SHA-1
        BinaryValue temp = cache.storeValue(stream);
        try {
            // prepare new binary key based on SHA-1
            BinaryKey key = new BinaryKey(temp.getKey().toString());

            // check for duplicate content
            if (this.contentExists(key, ALIVE)) {
                return new StoredBinaryValue(this, key, temp.getSize());
            }

            // check unused content
            if (this.contentExists(key, UNUSED)) {
                session.execute("UPDATE modeshape.binary SET usage=1 WHERE cid='" + key + "';");
                return new StoredBinaryValue(this, key, temp.getSize());
            }

            // store content
            PreparedStatement query = session.prepare("INSERT INTO modeshape.binary (cid, usage_time, payload, usage) VALUES ( ?,?,?,1 );");
            BoundStatement statement = new BoundStatement(query);
            session.execute(statement.bind(key.toString(), new Date(), buffer(stream)));
            return new StoredBinaryValue(this, key, temp.getSize());
        } catch (BinaryStoreException e) {
            throw e;
        } catch (IOException e) {
            throw new BinaryStoreException(e);
        } catch (RuntimeException e) {
            throw new BinaryStoreException(e);
        } finally {
            // remove content from temp store
            cache.markAsUnused(temp.getKey());
        }
    }

    @Override
    public InputStream getInputStream( BinaryKey key ) throws BinaryStoreException {
        try {
            ResultSet rs = session.execute("SELECT payload FROM modeshape.binary WHERE cid='" + key.toString() + "' and usage=1;");
            Row row = rs.one();
            if (row == null) {
                throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(key, session));
            }

            ByteBuffer buffer = row.getBytes("payload");
            return new BufferedInputStream(buffer);
        } catch (BinaryStoreException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BinaryStoreException(e);
        }
    }

    @Override
    public void markAsUnused( Iterable<BinaryKey> keys ) throws BinaryStoreException {
        try {
            for (BinaryKey key : keys) {
                session.execute("UPDATE modeshape.binary SET usage=0 where cid='" + key + "';");
            }
        } catch (RuntimeException e) {
            throw new BinaryStoreException(e);
        }
    }

    @Override
    public void removeValuesUnusedLongerThan( long minimumAge,
                                              TimeUnit unit ) throws BinaryStoreException {
        try {
            Date deadline = new Date(new Date().getTime() - unit.toMillis(minimumAge));
            // When querying using 2nd indexes, Cassandra
            // (it's not CQL specific) requires that you use an '=' for at least one of
            // the indexed column in the where clause. This is a limitation of Cassandra.
            // So we have to do some tricks here
            ResultSet rs = session.execute("SELECT cid from modeshape.binary where usage=0 and usage_time < "
                                           + deadline.getTime() + " allow filtering;");

            Iterator<Row> rows = rs.iterator();
            while (rows.hasNext()) {
                session.execute("DELETE from modeshape.binary where cid = '" + rows.next().getString("cid") + "';");
            }

            rs = session.execute("SELECT cid from modeshape.binary where usage=1 and usage_time < " + deadline.getTime()
                                 + " allow filtering;");
            rows = rs.iterator();
            while (rows.hasNext()) {
                session.execute("DELETE from modeshape.binary where cid = '" + rows.next().getString("cid") + "';");
            }
        } catch (RuntimeException e) {
            throw new BinaryStoreException(e);
        }
    }

    @Override
    public Iterable<BinaryKey> getAllBinaryKeys() throws BinaryStoreException {
        try {
            ResultSet rs = session.execute("SELECT cid from modeshape.binary WHERE usage=1;");
            Iterator<Row> it = rs.iterator();
            HashSet<BinaryKey> keys = new HashSet<BinaryKey>();
            while (it.hasNext()) {
                keys.add(new BinaryKey(it.next().getString("cid")));
            }
            return keys;
        } catch (RuntimeException e) {
            throw new BinaryStoreException(e);
        }
    }

    @Override
    public void start() {
        cluster = Cluster.builder().addContactPoint(address).build();
        Metadata metadata = cluster.getMetadata();
        System.out.printf("Connected to cluster: %s\n", metadata.getClusterName());
        for (Host host : metadata.getAllHosts()) {
            System.out.printf("Datacenter: %s; Host: %s; Rack: %s\n", host.getDatacenter(), host.getAddress(), host.getRack());
        }

        session = cluster.connect();
        try {
            session.execute("CREATE KEYSPACE modeshape WITH replication "
                            + "= {'class':'SimpleStrategy', 'replication_factor':3};");
        } catch (AlreadyExistsException e) {
        }

        session.execute("USE modeshape;");

        try {
            session.execute("CREATE TABLE modeshape.binary(" + "cid text PRIMARY KEY," + "mime_type text," + "ext_text text,"
                            + "usage int," + "usage_time timestamp," + "payload blob)");
        } catch (AlreadyExistsException e) {
        }

        try {
            session.execute("CREATE INDEX USAGE_IDX ON modeshape.binary (usage);");
        } catch (InvalidQueryException e) {
            // exists
        }

        try {
            session.execute("CREATE INDEX EXPIRE_IDX ON modeshape.binary (usage_time);");
        } catch (InvalidQueryException e) {
            // exists
        }
    }

    /**
     * Test content for existence.
     * 
     * @param key content identifier
     * @param alive true inside used content and false for checking within content marked as unused.
     * @return true if content found
     * @throws BinaryStoreException
     */
    private boolean contentExists( BinaryKey key,
                                   boolean alive ) throws BinaryStoreException {
        try {
            String query = "SELECT payload from modeshape.binary where cid='" + key.toString() + "'";
            query = alive ? query + " and usage=1;" : query + " and usage = 0;";
            ResultSet rs = session.execute(query);
            return rs.iterator().hasNext();
        } catch (RuntimeException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Converts input stream into ByteBuffer.
     * 
     * @param stream
     * @return the byte buffer
     * @throws IOException
     */
    private ByteBuffer buffer( InputStream stream ) throws IOException {
        stream.reset();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copy(stream, bout);
        return ByteBuffer.wrap(bout.toByteArray());
    }

    protected final class BufferedInputStream extends InputStream {
        private ByteBuffer buffer;

        protected BufferedInputStream( ByteBuffer buffer ) {
            this.buffer = buffer;
        }

        @Override
        public int read() {
            return buffer.position() < buffer.limit() ? buffer.get() & 0xff : -1;
        }

    }
}
