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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * A factory class which creates {@link javax.jcr.Value} instances from arbitrary objects (avoiding the dependency on the real
 * {@link javax.jcr.ValueFactory} implementations from other modules).
 * 
 * @author Horia Chiorean
 */
public final class JdbcJcrValueFactory {

    protected static final SimpleDateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private JdbcJcrValueFactory() {
    }

    /**
     * Creates a new {@link javax.jcr.Value} instance to be used by the JDBC driver.
     * 
     * @param value an actual value which will be wrapped by the JCR value; may be null
     * @return either a {@link javax.jcr.Value} instance or {@code null} if the given argument is {@code null} or is the string
     *         {@code NULL}
     */
    public static Value createValue( Object value ) {
        if (value == null) {
            return null;
        }
        if (value instanceof String && ((String)value).equalsIgnoreCase("NULL")) {
            return null;
        }
        return new JdbcJcrValue(value);
    }

    private static class JdbcJcrValue implements Value {

        private final Object value;

        protected JdbcJcrValue( Object value ) {
            assert value != null;
            this.value = value;
        }

        @Override
        public boolean getBoolean() throws IllegalStateException {
            if (value instanceof Boolean) {
                return (Boolean)value;
            }
            return Boolean.parseBoolean(value.toString());
        }

        @Override
        public Calendar getDate() throws ValueFormatException, IllegalStateException, RepositoryException {
            if (value instanceof Date) {
                Calendar c = Calendar.getInstance();
                c.setTime((Date)value);
                return c;
            } else if (value instanceof Calendar) {
                return (Calendar)value;
            }

            try {
                Date iso8601Format = ISO8601.parse(value.toString());
                Calendar c = Calendar.getInstance();
                c.setTime(iso8601Format);
                return c;
            } catch (ParseException e) {
                throw new ValueFormatException("Value not instance of Date", e);
            }
        }

        @Override
        public double getDouble() throws ValueFormatException, IllegalStateException, RepositoryException {
            if (value instanceof Number) {
                return ((Number)value).doubleValue();
            }

            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                throw new ValueFormatException("Value not a Double", e);
            }
        }

        @Override
        public long getLong() throws ValueFormatException, IllegalStateException, RepositoryException {
            if (value instanceof Number) {
                return ((Number)value).longValue();
            }

            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException e) {
                throw new ValueFormatException("Value not a Long");
            }
        }

        @Override
        public Binary getBinary() throws RepositoryException {
            if (value instanceof Binary) {
                return ((Binary)value);
            }
            if (value instanceof byte[]) {
                final byte[] bytes = (byte[])value;
                return new Binary() {
                    @Override
                    public void dispose() {
                    }

                    @Override
                    public long getSize() {
                        return bytes.length;
                    }

                    @Override
                    public InputStream getStream() {
                        return new ByteArrayInputStream(bytes);
                    }

                    @Override
                    public int read( byte[] b,
                                     long position ) throws IOException {
                        if (getSize() <= position) {
                            return -1;
                        }
                        InputStream stream = null;
                        try {
                            stream = getStream();
                            // Read/skip the next 'position' bytes ...
                            long skip = position;
                            while (skip > 0) {
                                long skipped = stream.skip(skip);
                                if (skipped <= 0) {
                                    return -1;
                                }
                                skip -= skipped;
                            }
                            return stream.read(b);
                        } finally {
                            if (stream != null) {
                                try {
                                    stream.close();
                                } catch (Exception e) {
                                    // ignore
                                }
                            }
                        }
                    }

                };
            }
            throw new ValueFormatException("Value not a Binary");
        }

        @Override
        public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
            if (value instanceof BigDecimal) {
                return ((BigDecimal)value);
            }
            try {
                return new BigDecimal(value.toString());
            } catch (NumberFormatException e) {
                throw new ValueFormatException("Value not a Decimal");
            }
        }

        @Override
        public InputStream getStream() throws IllegalStateException, RepositoryException {
            if (value instanceof Binary) {
                return ((Binary)value).getStream();
            }
            if (value instanceof InputStream) {
                return ((InputStream)value);
            }
            if (value instanceof byte[]) {
                return new ByteArrayInputStream((byte[])value);
            }
            throw new ValueFormatException("Value not an InputStream");
        }

        @Override
        public String getString() throws IllegalStateException {
            if (value instanceof String) {
                return (String)value;
            }
            return value.toString();
        }

        @Override
        public int getType() {
            if (value instanceof String) {
                return PropertyType.STRING;
            }
            if (value instanceof Boolean) {
                return PropertyType.BOOLEAN;
            }
            if (value instanceof Date || value instanceof Calendar) {
                return PropertyType.DATE;
            }
            if (value instanceof Double || value instanceof Float) {
                return PropertyType.DOUBLE;
            }
            if (value instanceof Long || value instanceof Integer) {
                return PropertyType.LONG;
            }
            if (value instanceof BigDecimal) {
                return PropertyType.DECIMAL;
            }
            if (value instanceof byte[] || value instanceof Binary || value instanceof InputStream) {
                return PropertyType.BINARY;
            }
            return PropertyType.UNDEFINED;
        }
    }

}
