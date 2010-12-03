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
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
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
@NamedQueries( {
    @NamedQuery( name = "LargeValueEntity.selectUnused", query = "select largeValue.hash from LargeValueEntity largeValue where largeValue.hash not in (select values.hash from NodeEntity node join node.largeValues values)" ),
    @NamedQuery( name = "LargeValueEntity.deleteAllUnused", query = "delete LargeValueEntity lve where lve.hash not in (select values.hash from NodeEntity node join node.largeValues values)" ),
    @NamedQuery( name = "LargeValueEntity.deleteIn", query = "delete LargeValueEntity lve where lve.hash in (:inValues)" )} )
public class LargeValueEntity {

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
     * @return the number of deleted large values
     */
    @SuppressWarnings( "unchecked" )
    public static int deleteUnused( EntityManager manager,
                                    String dialect ) {
        assert manager != null;

        int result = 0;
        if (dialect != null && dialect.toLowerCase().indexOf("mysql") != -1) {
            // Unfortunately, we cannot delete all the unused large values in a single statement
            // because of MySQL (see MODE-691). Therefore, we need to do this in multiple steps:
            // 1) Find the set of hashes that are not used anymore
            // 2) Delete each of these rows, using bulk deletes with a small number (20) of hashes at a time

            Query select = manager.createNamedQuery("LargeValueEntity.selectUnused");
            List<String> hashes = select.getResultList();
            if (hashes.isEmpty()) return 0;

            // Delete the unused large entities, (up to) 20 at a time
            int endIndex = hashes.size();
            int fromIndex = 0;
            do {
                int toIndex = Math.min(fromIndex + 20, endIndex);
                Query query = manager.createNamedQuery("LargeValueEntity.deleteIn");
                query.setParameter("inValues", hashes.subList(fromIndex, toIndex));
                query.executeUpdate();
                result += toIndex - fromIndex;
                fromIndex = toIndex;
            } while (fromIndex < endIndex);
        } else {
            // For all dialects other than MySQL, we can just use the one delete statement ...
            Query delete = manager.createNamedQuery("LargeValueEntity.deleteAllUnused");
            result = delete.executeUpdate();
        }

        manager.flush();
        return result;
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
