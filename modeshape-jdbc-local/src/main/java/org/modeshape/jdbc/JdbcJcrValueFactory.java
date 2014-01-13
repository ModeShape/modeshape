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

import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

/**
 * A factory class which creates {@link javax.jcr.Value} instances from arbitrary objects (avoiding the dependency on the
 * real {@link javax.jcr.ValueFactory} implementations from other modules).
 *
 * @author Horia Chiorean
 */
public final class JdbcJcrValueFactory {

    private JdbcJcrValueFactory() {
    }

    public static Value createValue(Object value) {
        return value == null ? null : new JdbcJcrValue(value);
    }

    private static class JdbcJcrValue implements Value {

        private Object value;

        protected JdbcJcrValue( Object value ) {
            this.value = value;
        }

        @Override
        public boolean getBoolean() throws ValueFormatException, IllegalStateException, RepositoryException {
            if (value instanceof Boolean) {
                return (Boolean)value;
            }
            throw new ValueFormatException("Value not a Boolean");
        }

        @Override
        public Calendar getDate() throws ValueFormatException, IllegalStateException, RepositoryException {
            if (value instanceof Date) {
                Calendar c = Calendar.getInstance();
                c.setTime((Date)value);
                return c;
            }
            throw new ValueFormatException("Value not instance of Date");
        }

        @Override
        public double getDouble() throws ValueFormatException, IllegalStateException, RepositoryException {
            if (value instanceof Double) {
                return ((Double)value);
            }

            throw new ValueFormatException("Value not a Double");
        }

        @Override
        public long getLong() throws ValueFormatException, IllegalStateException, RepositoryException {
            if (value instanceof Long) {
                return ((Long)value);
            }
            throw new ValueFormatException("Value not a Long");
        }

        /**
         * {@inheritDoc}
         *
         * @see javax.jcr.Value#getBinary()
         */
        @Override
        public Binary getBinary() throws RepositoryException {
            if (value instanceof Binary) {
                return ((Binary)value);
            }
            if (value instanceof byte[]) {
                final byte[] bytes = (byte[]) value;
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
                                    //ignore
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
            throw new ValueFormatException("Value not a Decimal");
        }

        @Override
        public InputStream getStream() throws IllegalStateException, RepositoryException {
            if (value instanceof Binary) {
                return ((Binary)value).getStream();
            }
            if (value instanceof InputStream) {
                return ((InputStream)value);
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
            if (value instanceof Date) {
                return PropertyType.DATE;
            }
            if (value instanceof Double) {
                return PropertyType.DOUBLE;
            }
            if (value instanceof Long) {
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
