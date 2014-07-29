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
package org.modeshape.jcr.value.basic;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.GraphI18n;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.IoException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceException;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#NAME} values.
 */
@Immutable
public class NameValueFactory extends AbstractValueFactory<Name> implements NameFactory {

    private static final Name BLANK_NAME = new BasicName("", "");
    private static final Name ANY_NAME = new BasicName("", "*");

    private final NamespaceRegistry.Holder namespaceRegistryHolder;

    /**
     * Create a new instance.
     * 
     * @param namespaceRegistryHolder the holder of the namespace registry; may not be null
     * @param decoder the text decoder; may be null if the default decoder should be used
     * @param factories the set of value factories, used to obtain the {@link ValueFactories#getStringFactory() string value
     *        factory}; may not be null
     */
    public NameValueFactory( NamespaceRegistry.Holder namespaceRegistryHolder,
                             TextDecoder decoder,
                             ValueFactories factories ) {
        super(PropertyType.NAME, decoder, factories);
        CheckArg.isNotNull(namespaceRegistryHolder, "namespaceRegistryHolder");
        this.namespaceRegistryHolder = namespaceRegistryHolder;
    }

    @Override
    public NameFactory with( ValueFactories valueFactories ) {
        return super.valueFactories == valueFactories ? this : new NameValueFactory(namespaceRegistryHolder, super.getDecoder(),
                                                                                    valueFactories);
    }

    @Override
    public NameFactory with( org.modeshape.jcr.value.NamespaceRegistry.Holder namespaceRegistryHolder ) {
        return this.namespaceRegistryHolder == namespaceRegistryHolder ? this : new NameValueFactory(namespaceRegistryHolder,
                                                                                                     super.getDecoder(),
                                                                                                     valueFactories);
    }

    @Override
    public Name create( String value ) {
        return create(value, getDecoder());
    }

    @Override
    public Name create( String value,
                        TextDecoder decoder ) {
        if (value == null) return null;
        if (decoder == null) decoder = getDecoder();
        try {
            if (value.length() == 0) {
                return BLANK_NAME;
            }
            char firstChar = value.charAt(0);
            if (value.length() == 1 && firstChar == '*') {
                return ANY_NAME;
            }
            if (firstChar == ':') {
                if (value.length() == 1) {
                    // This is completely blank (just a ':') ...
                    return BLANK_NAME;
                }
                // Otherwise, it's invalid with a blank prefix and non-blank local name
                throw new ValueFormatException(value, getPropertyType(),
                                               GraphI18n.errorConvertingType.text(String.class.getSimpleName(),
                                                                                  Name.class.getSimpleName(), value));
            }
            if (firstChar == '{') {
                // This might be fully-qualified ...
                int closingBraceIndex = value.indexOf('}', 1);
                if (closingBraceIndex == 1) {
                    // It's an empty pair of braces, so treat it as the blank namespace ...
                    if (value.length() == 2) {
                        // There's nothing else ...
                        return BLANK_NAME;
                    }
                    String namespaceUri = this.namespaceRegistryHolder.getNamespaceRegistry().getNamespaceForPrefix("");
                    String localName = decoder.decode(value.substring(2));
                    return new BasicName(namespaceUri, localName);
                }
                if (closingBraceIndex > 1) {
                    // Closing brace found with chars between ...
                    String namespaceUri = value.substring(1, closingBraceIndex);
                    int nextIndexAfterClosingBrace = closingBraceIndex + 1;
                    String localName = nextIndexAfterClosingBrace < value.length() ? value.substring(nextIndexAfterClosingBrace) : "";
                    // Decode the parts ...
                    return create(namespaceUri, localName, decoder);
                }
            } else {
                // This is not fully-qualified, so see whether the value fits the prefixed name pattern ...
                int colonIndex = value.indexOf(':');
                if (colonIndex < 1) {
                    // There is no namespace prefix ...
                    String namespaceUri = this.namespaceRegistryHolder.getNamespaceRegistry().getNamespaceForPrefix("");
                    String localName = decoder.decode(value);
                    return new BasicName(namespaceUri, localName);
                }
                // There is a namespace ...
                String prefix = value.substring(0, colonIndex);
                prefix = decoder.decode(prefix);
                // Look for a namespace match ...
                String namespaceUri = this.namespaceRegistryHolder.getNamespaceRegistry().getNamespaceForPrefix(prefix);
                // Fail if no namespace is found ...
                if (namespaceUri == null) {
                    throw new NamespaceException(GraphI18n.noNamespaceRegisteredForPrefix.text(prefix));
                }
                int nextIndexAfterColon = colonIndex + 1;
                String localName = nextIndexAfterColon < value.length() ? value.substring(nextIndexAfterColon) : "";
                localName = decoder.decode(localName);
                return new BasicName(namespaceUri, localName);
            }
        } catch (NamespaceException err) {
            throw new ValueFormatException(value, getPropertyType(),
                                           GraphI18n.errorConvertingType.text(String.class.getSimpleName(),
                                                                              Name.class.getSimpleName(), value), err);
        }
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.errorConvertingType.text(String.class.getSimpleName(),
                                                                                                    Name.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Name create( String namespaceUri,
                        String localName ) {
        return create(namespaceUri, localName, getDecoder());
    }

    @Override
    public Name create( String namespaceUri,
                        String localName,
                        TextDecoder decoder ) {
        CheckArg.isNotEmpty(localName, "localName");
        if (decoder == null) decoder = getDecoder();
        namespaceUri = namespaceUri != null ? decoder.decode(namespaceUri.trim()) : null;
        localName = decoder.decode(localName.trim());
        return new BasicName(namespaceUri, localName);
    }

    @Override
    public Name create( int value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Integer.class.getSimpleName(), value));
    }

    @Override
    public Name create( long value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Long.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Name create( boolean value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Boolean.class.getSimpleName(), value));
    }

    @Override
    public Name create( float value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Float.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Name create( double value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Double.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Name create( BigDecimal value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          BigDecimal.class.getSimpleName(), value));
    }

    @Override
    public Name create( Calendar value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Calendar.class.getSimpleName(), value));
    }

    @Override
    public Name create( Date value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Date.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Name create( DateTime value ) throws ValueFormatException {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          DateTime.class.getSimpleName(), value));
    }

    @Override
    public Name create( Name value ) {
        return value;
    }

    @Override
    public Name create( Path value ) {
        if (value == null) return null;
        if (!value.isAbsolute() && value.size() == 1) {
            // A relative name of length 1 is converted to a name
            Path.Segment segment = value.getLastSegment();
            // Can only convert if the path has no SNS index ...
            if (!segment.hasIndex()) return segment.getName();
        }
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.errorConvertingType.text(Path.class.getSimpleName(),
                                                                                                    Name.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Name create( Path.Segment segment ) {
        if (segment == null) return null;
        // Can only convert if the path has no SNS index ...
        if (!segment.hasIndex()) return segment.getName();
        throw new ValueFormatException(segment, getPropertyType(),
                                       GraphI18n.errorConvertingType.text(Path.Segment.class.getSimpleName(),
                                                                          Name.class.getSimpleName(), segment));
    }

    @Override
    public Name create( Reference value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Reference.class.getSimpleName(), value));
    }

    @Override
    public Name create( URI value ) {
        if (value == null) return null;
        String asciiString = value.toASCIIString();
        // Remove any leading "./" ...
        if (asciiString.startsWith("./") && asciiString.length() > 2) {
            asciiString = asciiString.substring(2);
        }
        if (asciiString.indexOf('/') == -1) {
            return create(asciiString);
        }
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.errorConvertingType.text(URI.class.getSimpleName(),
                                                                                                    Path.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Name create( UUID value ) throws IoException {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    UUID.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Name create( NodeKey value ) throws ValueFormatException {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          NodeKey.class.getSimpleName(), value));
    }

    @Override
    public Name create( byte[] value ) {
        // First attempt to create a string from the value, then a long from the string ...
        return create(getStringValueFactory().create(value));
    }

    @Override
    public Name create( BinaryValue value ) throws ValueFormatException, IoException {
        // First create a string and then create the boolean from the string value ...
        return create(getStringValueFactory().create(value));
    }

    @Override
    public Name create( InputStream stream ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream));
    }

    @Override
    public NamespaceRegistry getNamespaceRegistry() {
        return namespaceRegistryHolder.getNamespaceRegistry();
    }

    @Override
    public Name[] createEmptyArray( int length ) {
        return new Name[length];
    }
}
