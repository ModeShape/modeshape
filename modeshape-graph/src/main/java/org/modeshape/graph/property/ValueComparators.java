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
package org.modeshape.graph.property;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.modeshape.common.util.SecureHash;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.property.basic.StringValueFactory;

/**
 * A set of {@link Comparator} objects for the different kinds of property values.
 * 
 * @see PropertyType#getComparator()
 */
@Immutable
public class ValueComparators {

    /**
     * A comparator of string values.
     */
    public static final Comparator<String> STRING_COMPARATOR = new Comparator<String>() {

        public int compare( String o1,
                            String o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }
    };
    /**
     * A comparator of long values.
     */
    public static final Comparator<Long> LONG_COMPARATOR = new Comparator<Long>() {

        public int compare( Long o1,
                            Long o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }
    };
    /**
     * A comparator of double values.
     */
    public static final Comparator<Double> DOUBLE_COMPARATOR = new Comparator<Double>() {

        public int compare( Double o1,
                            Double o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }
    };
    /**
     * A comparator of decimal values.
     */
    public static final Comparator<BigDecimal> DECIMAL_COMPARATOR = new Comparator<BigDecimal>() {

        public int compare( BigDecimal o1,
                            BigDecimal o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }
    };
    /**
     * A comparator of binary values. Although {@link Binary} is {@link Comparable}, this comparator does not rely upon any
     * particular Binary implementation. Thus, Binary implementations can use this for their {@link Comparable#compareTo(Object)}
     * implementation.
     */
    public static final Comparator<Binary> BINARY_COMPARATOR = new Comparator<Binary>() {

        public int compare( Binary o1,
                            Binary o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            try {
                o1.acquire();
                try {
                    o2.acquire();
                    final long len1 = o1.getSize();
                    final long len2 = o2.getSize();
                    if (len1 < len2) return -1;
                    if (len1 > len2) return 1;

                    // Compare using the hashes, if available
                    byte[] hash1 = o1.getHash();
                    byte[] hash2 = o2.getHash();
                    if (hash1.length != 0 || hash2.length != 0) {
                        assert hash1.length == hash2.length;
                        for (int i = 0; i != hash1.length; ++i) {
                            int diff = hash1[i] - hash2[i];
                            if (diff != 0) return diff;
                        }
                        return 0;
                        // If the hashes match, then we should assume that the values match.
                        // That's the whole point of using a secure hash.
                    }

                    // One or both of the hashes could not be generated, so we have to go compare
                    // the whole values. This is unfortunate, but should happen very rarely (if ever)
                    // as long as the BinaryValue.getHash() is always implemented

                    // Otherwise they are the same length ...
                    InputStream stream1 = null;
                    InputStream stream2 = null;
                    try {
                        stream1 = o1.getStream();
                        stream2 = o2.getStream();
                        byte[] buffer1 = new byte[1024];
                        byte[] buffer2 = new byte[1024];
                        while (true) {
                            int numRead1 = stream1.read(buffer1);
                            if (numRead1 < 0) break;
                            int numRead2 = stream2.read(buffer2);
                            if (numRead1 != numRead2) {
                                throw new IoException(GraphI18n.errorReadingPropertyValueBytes.text());
                            }
                            for (int i = 0; i != numRead1; ++i) {
                                int diff = buffer1[i] - buffer2[i];
                                if (diff != 0) return diff;
                            }
                        }
                        return 0;
                    } catch (IOException e) {
                        throw new IoException(GraphI18n.errorReadingPropertyValueBytes.text());
                    } finally {
                        if (stream1 != null) {
                            try {
                                stream1.close();
                            } catch (IOException e) {
                                // do nothing
                            }
                        }
                        if (stream2 != null) {
                            try {
                                stream2.close();
                            } catch (IOException e) {
                                // do nothing
                            }
                        }
                    }
                } finally {
                    o2.release();
                }
            } finally {
                o1.release();
            }
        }
    };
    /**
     * A comparator of boolean values.
     */
    public static final Comparator<Boolean> BOOLEAN_COMPARATOR = new Comparator<Boolean>() {

        public int compare( Boolean o1,
                            Boolean o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }
    };
    /**
     * A comparator of date-time instances.
     */
    public static final Comparator<DateTime> DATE_TIME_COMPARATOR = new Comparator<DateTime>() {

        public int compare( DateTime o1,
                            DateTime o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }
    };
    /**
     * A comparator of date values.
     */
    public static final Comparator<Date> DATE_COMPARATOR = new Comparator<Date>() {

        public int compare( Date o1,
                            Date o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }
    };
    /**
     * A comparator of calendar values.
     */
    public static final Comparator<Calendar> CALENDAR_COMPARATOR = new Comparator<Calendar>() {

        public int compare( Calendar o1,
                            Calendar o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }
    };
    /**
     * A comparator of name values.
     */
    public static final Comparator<Name> NAME_COMPARATOR = new Comparator<Name>() {

        public int compare( Name o1,
                            Name o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }
    };
    /**
     * A comparator of path values.
     */
    public static final Comparator<Path> PATH_COMPARATOR = new Comparator<Path>() {

        public int compare( Path o1,
                            Path o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }
    };
    /**
     * A comparator of path segment values.
     */
    public static final Comparator<Path.Segment> PATH_SEGMENT_COMPARATOR = new Comparator<Path.Segment>() {

        public int compare( Path.Segment o1,
                            Path.Segment o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }
    };
    /**
     * A comparator of path segment names, excluding same-name-sibling indexes.
     */
    public static final Comparator<Path.Segment> PATH_SEGMENT_NAME_COMPARATOR = new Comparator<Path.Segment>() {

        public int compare( Path.Segment o1,
                            Path.Segment o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.getName().compareTo(o2.getName());
        }
    };
    /**
     * A comparator of URI values.
     */
    public static final Comparator<URI> URI_COMPARATOR = new Comparator<URI>() {

        public int compare( URI o1,
                            URI o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }
    };
    /**
     * A comparator of UUID values.
     */
    public static final Comparator<UUID> UUID_COMPARATOR = new Comparator<UUID>() {

        public int compare( UUID o1,
                            UUID o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }
    };
    /**
     * A comparator of reference values.
     */
    public static final Comparator<Reference> REFERENCE_COMPARATOR = new Comparator<Reference>() {

        public int compare( Reference o1,
                            Reference o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }
    };
    /**
     * A comparator of other values.
     */
    public static final Comparator<Object> OBJECT_COMPARATOR = new Comparator<Object>() {

        @SuppressWarnings( "unchecked" )
        public int compare( Object o1,
                            Object o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            PropertyType type1 = PropertyType.discoverType(o1);
            PropertyType type2 = PropertyType.discoverType(o2);

            // Canonicalize the values ...
            o1 = type1.getCanonicalValue(o1);
            o2 = type2.getCanonicalValue(o2);

            if (type1 != PropertyType.OBJECT && type2 != PropertyType.OBJECT) {
                if (type1 == type2) return ((Comparator<Object>)type1.getComparator()).compare(o1, o2);

                // The types are different but the classes are the same ...
                if (type1.getValueClass().isAssignableFrom(type2.getValueClass())) {
                    return ((Comparator<Object>)type1.getComparator()).compare(o1, o2);
                }
                if (type2.getValueClass().isAssignableFrom(type1.getValueClass())) {
                    return ((Comparator<Object>)type2.getComparator()).compare(o1, o2);
                }
            }

            // The types are different. See if one is a BINARY value (because we can use the secure
            // hashes to efficiently do the comparison) ...
            ValueFactory<String> stringFactory = getStringValueFactory();
            String value1 = null;
            String value2 = null;
            if (type1 == PropertyType.BINARY || type2 == PropertyType.BINARY) {
                try {
                    byte[] hash1 = null;
                    byte[] hash2 = null;
                    // We don't have access to a binary factory, so do this brute force.
                    // Conver the non-binary value to a string, then compute the hash of the string ...
                    if (type1 == PropertyType.BINARY) {
                        value2 = stringFactory.create(o2);
                        hash2 = SecureHash.getHash(SecureHash.Algorithm.SHA_1, value2.getBytes());
                        hash1 = ((Binary)o1).getHash();
                    } else {
                        assert type2 == PropertyType.BINARY;
                        value1 = stringFactory.create(o1);
                        hash1 = SecureHash.getHash(SecureHash.Algorithm.SHA_1, value1.getBytes());
                        hash2 = ((Binary)o2).getHash();
                    }
                    // Compute the difference in the hashes ...
                    if (hash1.length == hash2.length) {
                        for (int i = 0; i != hash1.length; ++i) {
                            int diff = hash1[i] - hash2[i];
                            if (diff != 0) return diff;
                        }
                        return 0;
                    }
                } catch (Throwable error) {
                    // If anything went wrong, just continue with the string comparison
                }
            }

            // The types are different and must be converted ...
            if (value1 == null) value1 = stringFactory.create(o1);
            if (value2 == null) value2 = stringFactory.create(o2);
            return value1.compareTo(value2);
        }
    };

    // This is loaded lazily so that there is not a circular dependency between PropertyType (depends on this),
    // StringValueFactory (depends on PropertyType), and OBJECT_COMPARATOR (which depends on StringValueFactory) ...
    private static ValueFactory<String> STRING_VALUE_FACTORY;

    protected static final ValueFactory<String> getStringValueFactory() {
        // No locking is required, because it doesn't matter if we create several instances during initialization ...
        if (STRING_VALUE_FACTORY == null) {
            STRING_VALUE_FACTORY = new StringValueFactory(Path.NO_OP_DECODER, Path.NO_OP_ENCODER);
        }
        return STRING_VALUE_FACTORY;
    }
}
