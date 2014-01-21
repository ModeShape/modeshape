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
package org.modeshape.jdbc.metadata;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import javax.lang.model.type.NullType;

/**
 * <p>
 * This is a helper class used to obtain SQL type information for java types. The SQL type information is obtained from
 * java.sql.Types class. The integers and strings returned by methods in this class are based on constants in java.sql.Types.
 */

public final class JDBCSQLTypeInfo {

    // Prevent instantiation
    private JDBCSQLTypeInfo() {
    }

    public static final class DefaultDataTypes {
        public static final String STRING = "string"; //$NON-NLS-1$
        public static final String BOOLEAN = "boolean"; //$NON-NLS-1$
        public static final String BYTE = "byte"; //$NON-NLS-1$
        public static final String SHORT = "short"; //$NON-NLS-1$
        public static final String CHAR = "char"; //$NON-NLS-1$
        public static final String INTEGER = "integer"; //$NON-NLS-1$
        public static final String LONG = "long"; //$NON-NLS-1$
        public static final String BIG_INTEGER = "biginteger"; //$NON-NLS-1$
        public static final String FLOAT = "float"; //$NON-NLS-1$
        public static final String DOUBLE = "double"; //$NON-NLS-1$
        public static final String BIG_DECIMAL = "bigdecimal"; //$NON-NLS-1$
        public static final String DATE = "date"; //$NON-NLS-1$
        public static final String TIME = "time"; //$NON-NLS-1$
        public static final String TIMESTAMP = "timestamp"; //$NON-NLS-1$
        public static final String OBJECT = "object"; //$NON-NLS-1$
        public static final String NULL = "null"; //$NON-NLS-1$
        public static final String BLOB = "blob"; //$NON-NLS-1$
        public static final String CLOB = "clob"; //$NON-NLS-1$
        public static final String XML = "xml"; //$NON-NLS-1$
    }

    // java class names
    public static final class DefaultDataClasses {
        public static final Class<String> STRING = String.class;
        public static final Class<Boolean> BOOLEAN = Boolean.class;
        public static final Class<Byte> BYTE = Byte.class;
        public static final Class<Short> SHORT = Short.class;
        public static final Class<Character> CHAR = Character.class;
        public static final Class<Integer> INTEGER = Integer.class;
        public static final Class<Long> LONG = Long.class;
        public static final Class<BigInteger> BIG_INTEGER = BigInteger.class;
        public static final Class<Float> FLOAT = Float.class;
        public static final Class<Double> DOUBLE = Double.class;
        public static final Class<BigDecimal> BIG_DECIMAL = BigDecimal.class;
        public static final Class<java.sql.Date> DATE = java.sql.Date.class;
        public static final Class<Time> TIME = Time.class;
        public static final Class<Timestamp> TIMESTAMP = Timestamp.class;
        public static final Class<Object> OBJECT = Object.class;
        public static final Class<NullType> NULL = NullType.class;
    }

}
