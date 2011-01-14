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
package org.modeshape.connector.store.jpa.model.simple;

import java.security.NoSuchAlgorithmException;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQuery;
import javax.persistence.Query;
import javax.persistence.Table;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.property.PropertyType;

/**
 * A single property value that is too large to be stored on the individual node, and which will be shared among all properties
 * that have the same value. Note that the large values are stored independently of workspace, so one large value may be shared by
 * properties of nodes in different workspaces.
 */
@Entity
@Table( name = "MODE_SIMPLE_LARGE_VALUES" )
@NamedQuery( name = "LargeValueEntity.deleteAllUnused", query = "delete LargeValueEntity lve where lve.hash not in (select values.hash from NodeEntity node join node.largeValues values)" )
public class LargeValueEntity {

    /**
     * The MySQL delete statement to remove all unused LargeValueEntity records.
     * <p>
     * Normally, the "LargeValueEntity.deleteAllUnused" named query attempts to delete all of the LargeValueEntity records that
     * have an SHA1 that is not in the set of SHA1 values in the "usages" intersect table. This works well on all DBMSes except
     * for MySQL, which is unable to select from the table being deleted (see MODE-691). Therefore, we use a MySQL-specific native
     * query to do this deletion.
     * </p>
     * <p>
     * This delete statement uses a left outer join between the LargeValueEntity and "usages" table and a criteria on the result
     * to ensure that the only records returned are those without any "usages" records in the tuples. This works because a left
     * outer join between tables A and B always contains all records from A, whether or not there are no corresponding records
     * from B. And, if a criteria is added such that a column from B that can never be null is actually null, then we know the
     * result will contain only those records from A that do <i>not</i> have a corresponding B record. In our case, this ends up
     * deleting only those LargeValueEntity records that are not referenced in the "usages" table (i.e., they are not used).
     * </p>
     * <p>
     * We can do this native SQL because this only is used for the MySQL dialect.
     * </p>
     */
    private static final String MYSQL_DELETE_ALL_UNUSED_LARGE_VALUE_ENTITIES = "DELETE lv FROM MODE_SIMPLE_LARGE_VALUES AS lv "
                                                                               + "LEFT OUTER JOIN ModeShape_LARGEVALUE_USAGES AS lvu "
                                                                               + "ON lv.SHA1 = lvu.largeValues_SHA1 WHERE lvu.ID IS NULL";

    @Id
    @Column( name = "SHA1", nullable = false, length = 40 )
    private String hash;

    /**
     * The property type for this value. Typically, this is {@link PropertyType#STRING} or {@link PropertyType#BINARY}, although
     * technically it could be any type.
     */
    @Enumerated( value = EnumType.STRING )
    @Column( name = "TYPE", nullable = false )
    private PropertyType type;

    /**
     * The number of bytes in this value.
     */
    @Column( name = "LENGTH", nullable = false )
    private long length;

    /**
     * Flag specifying whether the binary data is stored in a compressed format.
     */
    @Column( name = "COMPRESSED", nullable = true )
    private Boolean compressed;

    /**
     * Lazily-fetched value
     */
    @Lob
    @Column( name = "DATA", nullable = false )
    private byte[] data;

    public String getHash() {
        return hash;
    }

    public void setHash( String hash ) {
        this.hash = hash;
    }

    /**
     * @return length
     */
    public long getLength() {
        return length;
    }

    /**
     * @param length Sets length to the specified value.
     */
    public void setLength( long length ) {
        this.length = length;
    }

    /**
     * @return type
     */
    public PropertyType getType() {
        return type;
    }

    /**
     * @param type Sets type to the specified value.
     */
    public void setType( PropertyType type ) {
        this.type = type;
    }

    /**
     * @return data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * @param data Sets data to the specified value.
     */
    public void setData( byte[] data ) {
        this.data = data;
    }

    /**
     * @return compressed
     */
    public boolean isCompressed() {
        return compressed != null && compressed.booleanValue();
    }

    /**
     * @param compressed Sets compressed to the specified value.
     */
    public void setCompressed( boolean compressed ) {
        this.compressed = Boolean.valueOf(compressed);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return hash.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof LargeValueEntity) {
            LargeValueEntity that = (LargeValueEntity)obj;
            if (this.getHash().equals(that.getHash())) return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Large " + this.type + " value (hash=" + this.getHash() + ",compressed=" + isCompressed() + ")";
    }

    /**
     * Delete all unused large value entities.
     * 
     * @param manager the manager; never null
     * @param dialect the dialect
     * @return whether all the unused records were able to be removed in this pass
     */
    public static boolean deleteUnused( EntityManager manager,
                                        String dialect ) {
        assert manager != null;

        try {
            if (dialect != null && dialect.toLowerCase().indexOf("mysql") != -1) {
                // Because MySQL does not allow DELETE statements with subselects against the same table,
                // we have to use a different approach here. There's really no effective way to do this
                // in HQL, but since we're only dealing with MySQL, we can drop to native SQL and
                // do a very efficient delete ...
                Query delete = manager.createNativeQuery(MYSQL_DELETE_ALL_UNUSED_LARGE_VALUE_ENTITIES);
                delete.executeUpdate();
                return true;
            }
            // For all dialects other than MySQL, we can just use the one delete statement ...
            Query delete = manager.createNamedQuery("LargeValueEntity.deleteAllUnused");
            delete.executeUpdate();
            return true;
        } finally {
            manager.flush();
        }
    }

    private static byte[] computeHash( byte[] value ) {
        try {
            return SecureHash.getHash(SecureHash.Algorithm.SHA_1, value);
        } catch (NoSuchAlgorithmException e) {
            throw new SystemFailureException(e);
        }
    }

    public static LargeValueEntity create( byte[] data,
                                           PropertyType type,
                                           boolean compressed ) {
        String hashStr = StringUtil.getHexString(computeHash(data));
        LargeValueEntity entity = new LargeValueEntity();

        entity.setData(data);
        entity.setType(type);
        entity.setCompressed(compressed);
        entity.setHash(hashStr);
        return entity;
    }
}
