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
package org.modeshape.jcr.value;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URI;
import java.text.Collator;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.SecureHash;
import org.modeshape.jcr.GraphI18n;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.value.basic.StringValueFactory;

/**
 * A set of {@link Comparator} objects for the different kinds of property values.
 * 
 * @see PropertyType#getComparator()
 */
@Immutable
public final class ValueComparators {

    /**
     * A comparator of string values.
     */
    public static final Comparator<String> STRING_COMPARATOR = new StringComparator();

    /**
     * A comparator of string values which is locale-dependent.
     *
     * @param locale a {@link Locale} instance, may not be null
     * @return a string comparator
     */
    public static Comparator<String> collatorComparator(Locale locale) {
        return new CollatorComparator(locale);
    }

    /**
     * A comparator of long values.
     */
    public static final Comparator<Long> LONG_COMPARATOR = new LongComparator();

    /**
     * A comparator of double values.
     */
    public static final Comparator<Double> DOUBLE_COMPARATOR = new DoubleComparator();

    /**
     * A comparator of decimal values.
     */
    public static final Comparator<BigDecimal> DECIMAL_COMPARATOR = new DecimalComparator();

    /**
     * A comparator of boolean values.
     */
    public static final Comparator<Boolean> BOOLEAN_COMPARATOR = new BooleanComparator();

    /**
     * A comparator of binary values. Although {@link BinaryValue} is {@link Comparable}, this comparator does not rely upon any
     * particular Binary implementation. Thus, Binary implementations can use this for their {@link Comparable#compareTo(Object)}
     * implementation.
     */
    public static final Comparator<BinaryValue> BINARY_COMPARATOR = new BinaryValueComparator();

    /**
     * A comparator of date-time instances.
     */
    public static final Comparator<DateTime> DATE_TIME_COMPARATOR = new DateTimeComparator();

    /**
     * A comparator of date values.
     */
    public static final Comparator<Date> DATE_COMPARATOR = new DateComparator();

    /**
     * A comparator of calendar values.
     */
    public static final Comparator<Calendar> CALENDAR_COMPARATOR = new CalendarComparator();

    /**
     * A comparator of name values.
     */
    public static final Comparator<Name> NAME_COMPARATOR = new NameComparator();
    /**
     * A comparator of path values.
     */
    public static final Comparator<Path> PATH_COMPARATOR = new PathComparator();

    /**
     * A comparator of path segment values.
     */
    public static final Comparator<Path.Segment> PATH_SEGMENT_COMPARATOR = new PathSegmentComparator();

    /**
     * A comparator of path segment names, excluding same-name-sibling indexes.
     */
    public static final Comparator<Path.Segment> PATH_SEGMENT_NAME_COMPARATOR = new PathSegmentNameComparator();

    /**
     * A comparator of URI values.
     */
    public static final Comparator<URI> URI_COMPARATOR = new UriComparator();

    /**
     * A comparator of UUID values.
     */
    public static final Comparator<UUID> UUID_COMPARATOR = new UuidComparator();

    /**
     * A comparator of reference values.
     */
    public static final Comparator<Reference> REFERENCE_COMPARATOR = new ReferenceComparator();

    /**
     * A comparator of other values.
     */
    public static final Comparator<Object> OBJECT_COMPARATOR = new ObjectComparator();

    protected static final class StringComparator implements Comparator<String>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( String o1,
                            String o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }

        @Override
        public boolean equals( Object obj ) {
            return obj instanceof StringComparator;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }
    
    protected static final class CollatorComparator implements Comparator<String>, Serializable {
        private static final long serialVersionUID = 1L;
        private Locale locale;
        private transient Collator collator;

        protected CollatorComparator( Locale locale ) {
            CheckArg.isNotNull(locale, "locale");
            this.locale = locale;
        }

        @Override
        public int compare( String o1,
                            String o2 ) {
            return collator().compare(o1, o2);
        }

        @Override
        public boolean equals( Object obj ) {
            return obj instanceof CollatorComparator;
        }

        @Override
        public int hashCode() {
            return 1;
        }
        
        private Collator collator() {   
            if (collator == null) {
                collator = Collator.getInstance(locale);
            }
            return collator;
        }
    }

    protected static final class BooleanComparator implements Comparator<Boolean>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( Boolean o1,
                            Boolean o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }

        @Override
        public boolean equals( Object obj ) {
            return obj instanceof BooleanComparator;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    protected static final class LongComparator implements Comparator<Long>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( Long o1,
                            Long o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }

        @Override
        public boolean equals( Object obj ) {
            return obj instanceof LongComparator;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    protected static final class DoubleComparator implements Comparator<Double>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( Double o1,
                            Double o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }

        @Override
        public boolean equals( Object obj ) {
            return obj instanceof DoubleComparator;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    protected static final class DecimalComparator implements Comparator<BigDecimal>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( BigDecimal o1,
                            BigDecimal o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }

        @Override
        public boolean equals( Object obj ) {
            return obj instanceof DecimalComparator;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    protected static final class DateTimeComparator implements Comparator<DateTime>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( DateTime o1,
                            DateTime o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }

        @Override
        public boolean equals( Object obj ) {
            return obj instanceof DateTimeComparator;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    protected static final class DateComparator implements Comparator<Date>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( Date o1,
                            Date o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }

        @Override
        public boolean equals( Object obj ) {
            return obj instanceof DateComparator;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    protected static final class CalendarComparator implements Comparator<Calendar>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( Calendar o1,
                            Calendar o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }

        @Override
        public boolean equals( Object obj ) {
            return obj instanceof CalendarComparator;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    protected static final class NameComparator implements Comparator<Name>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( Name o1,
                            Name o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }

        @Override
        public boolean equals( Object obj ) {
            return obj instanceof NameComparator;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    protected static final class PathComparator implements Comparator<Path>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( Path o1,
                            Path o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }

        @Override
        public boolean equals( Object obj ) {
            return obj instanceof PathComparator;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    protected static final class PathSegmentComparator implements Comparator<Path.Segment>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( Path.Segment o1,
                            Path.Segment o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }

        @Override
        public boolean equals( Object obj ) {
            return obj instanceof PathSegmentComparator;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    protected static final class PathSegmentNameComparator implements Comparator<Path.Segment>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( Path.Segment o1,
                            Path.Segment o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.getName().compareTo(o2.getName());
        }

        @Override
        public boolean equals( Object obj ) {
            return obj instanceof PathSegmentNameComparator;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    protected static final class UriComparator implements Comparator<URI>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( URI o1,
                            URI o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }

        @Override
        public boolean equals( Object obj ) {
            return obj instanceof UriComparator;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    protected static final class UuidComparator implements Comparator<UUID>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( UUID o1,
                            UUID o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }

        @Override
        public boolean equals( Object obj ) {
            return obj instanceof UuidComparator;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    protected static final class ReferenceComparator implements Comparator<Reference>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( Reference o1,
                            Reference o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }

        @Override
        public boolean equals( Object obj ) {
            return obj instanceof ReferenceComparator;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    protected static final class ObjectComparator implements Comparator<Object>, Serializable {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings( "unchecked" )
        @Override
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
                        hash1 = ((BinaryValue)o1).getHash();
                    } else {
                        assert type2 == PropertyType.BINARY;
                        value1 = stringFactory.create(o1);
                        hash1 = SecureHash.getHash(SecureHash.Algorithm.SHA_1, value1.getBytes());
                        hash2 = ((BinaryValue)o2).getHash();
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

        @Override
        public boolean equals( Object obj ) {
            return obj instanceof ObjectComparator;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    protected static final class BinaryValueComparator implements Comparator<BinaryValue>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( BinaryValue o1,
                            BinaryValue o2 ) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            try {
                try {
                    // Check the lengths first ...
                    final long len1 = o1.getSize();
                    final long len2 = o2.getSize();
                    if (len1 < len2) return -1;
                    if (len1 > len2) return 1;

                    // Compare using the hashes, if available
                    String hash1 = o1.getHexHash();
                    String hash2 = o2.getHexHash();
                    if (hash1 != null && hash2 != null) {
                        // If the hashes match, then we should assume that the values match.
                        // That's the whole point of using a secure hash.
                        return hash1.compareTo(hash2);
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
                    } catch (RepositoryException e) {
                        throw new IoException(GraphI18n.errorReadingPropertyValueBytes.text());
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
                    o2.dispose();
                }
            } finally {
                o1.dispose();
            }
        }

        @Override
        public boolean equals( Object obj ) {
            return obj instanceof BinaryValueComparator;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

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
