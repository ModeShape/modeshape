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
package org.modeshape.jdbc;

import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import org.modeshape.jdbc.types.BlobTransform;
import org.modeshape.jdbc.types.BooleanTransform;
import org.modeshape.jdbc.types.DateTransform;
import org.modeshape.jdbc.types.DecimalTransform;
import org.modeshape.jdbc.types.DoubleTransform;
import org.modeshape.jdbc.types.LongTransform;
import org.modeshape.jdbc.types.StringTransform;
import org.modeshape.jdbc.types.UUIDTransform;

/**
 * Provides functionality to convert from JCR {@link PropertyType}s and JDBC types.
 */
public final class JcrType {

    private static final int UUID_LENGTH = UUID.randomUUID().toString().length();
    private static final Map<String, JcrType> TYPE_INFO;

    public static final class DefaultDataTypes {
        public static final String STRING = PropertyType.TYPENAME_STRING;
        public static final String BOOLEAN = PropertyType.TYPENAME_BOOLEAN;
        public static final String LONG = PropertyType.TYPENAME_LONG;
        public static final String DOUBLE = PropertyType.TYPENAME_DOUBLE;
        public static final String DECIMAL = PropertyType.TYPENAME_DECIMAL;
        public static final String DATE = PropertyType.TYPENAME_DATE;
        public static final String URI = PropertyType.TYPENAME_URI;
        public static final String WEAK_REF = PropertyType.TYPENAME_WEAKREFERENCE;
        public static final String UNDEFINED = PropertyType.TYPENAME_UNDEFINED;
        public static final String BINARY = PropertyType.TYPENAME_BINARY;
        public static final String REFERENCE = PropertyType.TYPENAME_REFERENCE;
        public static final String PATH = PropertyType.TYPENAME_STRING;
        public static final String NAME = PropertyType.TYPENAME_STRING;
    }

    static {
        Map<String, JcrType> types = new HashMap<String, JcrType>();
        register(types, PropertyType.BINARY, Types.BLOB, "BLOB", JcrBlob.class, 30, Integer.MAX_VALUE, new BlobTransform()); // assumed
        register(types, PropertyType.BOOLEAN, Types.BOOLEAN, "BOOLEAN", Boolean.class, 5, 1, new BooleanTransform()); // 'true' or 'false'
        register(types, PropertyType.DATE, Types.TIMESTAMP, "TIMESTAMP", Timestamp.class, 30, 10, new DateTransform()); // yyyy-MM-dd'T'HH:mm:ss.SSS+HH:mm
        register(types, PropertyType.DOUBLE, Types.DOUBLE, "DOUBLE" ,Double.class, 20, 20, new DoubleTransform()); // assumed
        register(types, PropertyType.DECIMAL, Types.DECIMAL, "BIGDECIMAL", BigDecimal.class, 20, 20, new DecimalTransform()); // assumed
        register(types, PropertyType.LONG, Types.BIGINT, "LONG", Long.class, 20, 19, new LongTransform()); // assumed
        register(types, PropertyType.NAME, Types.VARCHAR, "STRING", String.class, 20, Integer.MAX_VALUE, new StringTransform()); // assumed
        register(types, PropertyType.PATH, Types.VARCHAR, "STRING", String.class, 50, Integer.MAX_VALUE, new StringTransform()); // assumed
        register(types, PropertyType.REFERENCE, Types.VARCHAR, "STRING", UUID.class, UUID_LENGTH, UUID_LENGTH, new UUIDTransform());
        register(types, PropertyType.WEAKREFERENCE, Types.VARCHAR, "STRING", UUID.class, UUID_LENGTH, UUID_LENGTH, new UUIDTransform());
        register(types, PropertyType.URI, Types.VARCHAR, "STRING", String.class, 50, Integer.MAX_VALUE, new StringTransform()); // assumed
        register(types, PropertyType.STRING, Types.VARCHAR, "STRING", String.class, 50, Integer.MAX_VALUE, new StringTransform()); // assumed
        register(types, PropertyType.UNDEFINED, Types.VARCHAR, "STRING", String.class, 50, Integer.MAX_VALUE, new StringTransform()); // same
        // as
        // string
        TYPE_INFO = Collections.unmodifiableMap(types);
    }

    private static void register( Map<String, JcrType> types,
                                  int jcrType,
                                  int jdbcType,
                                  String typeName,
                                  Class<?> clazz,
                                  int displaySize,
                                  int precision,
                                  Transform transform ) {
        JcrType type = new JcrType(jcrType, jdbcType, typeName, clazz, displaySize, precision, transform);
        types.put(type.getJcrName(), type);
    }

    private final int jcrType;
    private final String jcrName;
    private final Class<?> clazz;
    private final int jdbcType;
    private final String typeName;
    private final int displaySize;
    private final int precision;
    private final Transform transform;

    protected JcrType( int jcrType,
                       int jdbcType,
                       String typeName,
                       Class<?> clazz,
                       int displaySize,
                       int precision,
                       Transform transform ) {
        this.jcrType = jcrType;
        this.jcrName = PropertyType.nameFromValue(jcrType).toUpperCase();
        this.clazz = clazz;
        this.displaySize = displaySize;
        this.jdbcType = jdbcType;
        this.typeName = typeName;
        this.precision = precision;
        this.transform = transform;
        assert this.jcrName != null;
        assert this.clazz != null;
        assert this.displaySize > 0;
        assert this.transform != null;
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
     * Get the native type name associated with the JDBC {@link Types} value.
     * 
     * @return the native JDBC type name; never null
     */
    public String getJdbcTypeName() {
        return this.typeName;
    }

    /**
     * Get the default precision used for this JcrType
     * 
     * @return the Integer form of the precision
     */
    public Integer getDefaultPrecision() {
        return new Integer(precision);
    }

    /**
     * Return the {@link Transform} object to use to transform the {@link Value} to the correct data type.
     * 
     * @return Transform
     */
    protected Transform getTransform() {
        return this.transform;
    }

    /**
     * Get the indicator if the value is case sensitive
     * 
     * @return boolean indicating if the value is case sensitive
     */
    public boolean isCaseSensitive() {
        switch (getJcrType()) {
            case PropertyType.DOUBLE:
            case PropertyType.LONG:
            case PropertyType.DECIMAL:
            case PropertyType.WEAKREFERENCE:
            case PropertyType.REFERENCE: // conversion is case-insensitive
            case PropertyType.BOOLEAN: // conversion is case-insensitive
                return false;
        }
        return true;
    }

    /**
     * Get the indicator if the value is considered a signed value.
     * 
     * @return boolean indicating if value is signed.
     */
    public boolean isSigned() {
        switch (getJcrType()) {
            case PropertyType.DOUBLE:
            case PropertyType.LONG:
            case PropertyType.DECIMAL:
            case PropertyType.DATE:
                return true;
        }
        return false;
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

    public Object translateValue( Value value ) throws SQLException {
        if (value == null) return null;
        try {
            return this.getTransform().transform(value);

        } catch (ValueFormatException ve) {
            throw new SQLException(ve.getLocalizedMessage(), ve);
        } catch (IllegalStateException ie) {
            throw new SQLException(ie.getLocalizedMessage(), ie);
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage(), e);
        }

    }

    public static Object translateValueToJDBC( Value value ) throws SQLException {
        String jcrName = PropertyType.nameFromValue(value.getType());
        JcrType jcrtype = typeInfo(jcrName);
        return jcrtype.translateValue(value);
    }

    public static JcrType typeInfo( String jcrTypeName ) {
        return TYPE_INFO.get(jcrTypeName.toUpperCase());
    }

    public static JcrType typeInfo( int jcrType ) {
        return typeInfo(PropertyType.nameFromValue(jcrType));
    }
    
    public static String jdbcType( String jcrTypeName ) {
        JcrType t = typeInfo(jcrTypeName);
        return t.getJdbcTypeName();
    }

}
