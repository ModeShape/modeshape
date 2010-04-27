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
package org.modeshape.jdbc;

import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.jcr.PropertyType;

/**
 * Provides functionality to convert from JCR {@link PropertyType}s and JDBC types.
 */
public final class JcrType {

    private static final Map<String, JcrType> TYPE_INFO;

    static {
        Map<String, JcrType> types = new HashMap<String, JcrType>();
        register(types, PropertyType.BINARY, Types.BLOB, JcrBlob.class, 30); // assumed
        register(types, PropertyType.BOOLEAN, Types.BOOLEAN, Boolean.class, 5); // 'true' or 'false'
        register(types, PropertyType.DATE, Types.TIMESTAMP, Timestamp.class, 30); // yyyy-MM-dd'T'HH:mm:ss.SSS+HH:mm
        register(types, PropertyType.DOUBLE, Types.FLOAT, Float.class, 20); // assumed
        register(types, PropertyType.LONG, Types.BIGINT, Long.class, 20); // assumed
        register(types, PropertyType.NAME, Types.VARCHAR, String.class, 20); // assumed
        register(types, PropertyType.PATH, Types.VARCHAR, String.class, 50); // assumed
        register(types, PropertyType.REFERENCE, Types.BLOB, UUID.class, UUID.randomUUID().toString().length());
        register(types, PropertyType.STRING, Types.VARCHAR, String.class, 50); // assumed
        register(types, PropertyType.UNDEFINED, Types.VARCHAR, String.class, 50); // same as string
        TYPE_INFO = Collections.unmodifiableMap(types);
    }

    private static void register( Map<String, JcrType> types,
                                  int jcrType,
                                  int jdbcType,
                                  Class<?> clazz,
                                  int displaySize ) {
        JcrType type = new JcrType(jcrType, jdbcType, clazz, displaySize);
        types.put(type.getJcrName(), type);
    }

    private final int jcrType;
    private final String jcrName;
    private final Class<?> clazz;
    private final int jdbcType;
    private final int displaySize;

    protected JcrType( int jcrType,
                       int jdbcType,
                       Class<?> clazz,
                       int displaySize ) {
        this.jcrType = jcrType;
        this.jcrName = PropertyType.nameFromValue(jcrType);
        this.clazz = clazz;
        this.displaySize = displaySize;
        this.jdbcType = jdbcType;
        assert this.jcrName != null;
        assert this.clazz != null;
        assert this.displaySize > 0;
    }

    /**
     * Get the name of the JCR type.
     * 
     * @return the JCR type name; never null
     */
    public String getJcrName() {
        return jcrName;
    }

    /**
     * Get the JCR {@link PropertyType} value.
     * 
     * @return the JCR property type; never null
     */
    public int getJcrType() {
        return jcrType;
    }

    /**
     * Get the JDBC {@link Types} value.
     * 
     * @return the JDBC type; never null
     */
    public int getJdbcType() {
        return jdbcType;
    }

    /**
     * Get the Java class used to represent values for this type.
     * 
     * @return the representation class; never null
     */
    public Class<?> getRepresentationClass() {
        return clazz;
    }

    /**
     * Get the nominal display size for the given type. This may not be large enough for certain string and binary values.
     * 
     * @return the nominal display size; always positive
     * @see ResultSetMetaData#getColumnDisplaySize(int)
     */
    public int getNominalDisplaySize() {
        return displaySize;
    }

    /**
     * Get the immutable built-in map from the type names to the Java representation class.
     * 
     * @return the built-in type map
     */
    public static Map<String, JcrType> builtInTypeMap() {
        return TYPE_INFO;
    }

}
