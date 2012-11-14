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
package org.modeshape.jcr.federation.filesystem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import org.modeshape.common.text.QuoteEncoder;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.text.XmlNameEncoder;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.federation.ExtraPropertiesStore;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * An {@link ExtraPropertiesStore} implementation that stores extra properties in legacy sidecar files adjacent to the actual file
 * or directory corresponding to the external node. The format of these legacy files is compatible with thosed used by the
 * ModeShape 2.x file system connector.
 */
class FileSystemConnectorLegacySidecarStorage implements ExtraPropertiesStore {

    /**
     * The regex pattern string used to parse properties. The capture groups are as follows:
     * <ol>
     * <li>property name (encoded)</li>
     * <li>property type string</li>
     * <li>a '[' if the value is multi-valued</li>
     * <li>the single value, or comma-separated values</li>
     * </ol>
     * <p>
     * The expression is: <code>([\S]+)\s*[(](\w+)[)]\s*([\[]?)?([^\]]+)[\]]?</code>
     * </p>
     */
    protected static final String PROPERTY_PATTERN_STRING = "([\\S]+)\\s*[(](\\w+)[)]\\s*([\\[]?)?([^\\]]+)[\\]]?";
    protected static final Pattern PROPERTY_PATTERN = Pattern.compile(PROPERTY_PATTERN_STRING);

    /**
     * The regex pattern string used to parse quoted string property values. This is a repeating expression, and group(0) captures
     * the characters within the quotes (including escaped double quotes).
     * <p>
     * The expression is: <code>\"((((?<=\\)\")|[^"])*)\"</code>
     * </p>
     */
    protected static final String STRING_VALUE_PATTERN_STRING = "\\\"((((?<=\\\\)\\\")|[^\"])*)\\\"";
    protected static final Pattern STRING_VALUE_PATTERN = Pattern.compile(STRING_VALUE_PATTERN_STRING);

    /**
     * The regex pattern string used to parse non-string property values (including hexadecimal-encoded binary values). This is a
     * repeating expression, and group(1) captures the individual values.
     * <p>
     * The expression is: <code>([^\s,]+)\s*[,]*\s*</code>
     * </p>
     */
    protected static final String VALUE_PATTERN_STRING = "([^\\s,]+)\\s*[,]*\\s*";
    protected static final Pattern VALUE_PATTERN = Pattern.compile(VALUE_PATTERN_STRING);

    public static final String DEFAULT_EXTENSION = ".modeshape";
    public static final String DEFAULT_RESOURCE_EXTENSION = ".content.modeshape";

    private final FileSystemConnector connector;
    private final NamespaceRegistry registry;
    private final PropertyFactory propertyFactory;
    private final ValueFactories factories;
    private final ValueFactory<String> stringFactory;
    private final TextEncoder encoder = new XmlNameEncoder();
    private final TextDecoder decoder = new XmlNameEncoder();
    private final QuoteEncoder quoter = new QuoteEncoder();

    protected FileSystemConnectorLegacySidecarStorage( FileSystemConnector connector ) {
        this.connector = connector;
        this.registry = this.connector.registry();
        this.propertyFactory = this.connector.getContext().getPropertyFactory();
        this.factories = this.connector.getContext().getValueFactories();
        this.stringFactory = factories.getStringFactory();
    }

    protected String getExclusionPattern() {
        return "(.+)\\.(content\\.)?modeshape$";

    }

    @Override
    public Map<Name, Property> getProperties( String id ) {
        return load(id, sidecarFile(id));
    }

    @Override
    public void storeProperties( String id,
                                 Map<Name, Property> properties ) {
        write(id, sidecarFile(id), properties);
    }

    @Override
    public void updateProperties( String id,
                                  Map<Name, Property> properties ) {
        File sidecar = sidecarFile(id);
        Map<Name, Property> existing = load(id, sidecar);
        if (existing == null || existing.isEmpty()) {
            write(id, sidecar, properties);
        } else {
            // Merge the changed properties into the existing ...
            for (Map.Entry<Name, Property> entry : properties.entrySet()) {
                Name name = entry.getKey();
                Property property = entry.getValue();
                if (property == null) {
                    existing.remove(name);
                } else {
                    existing.put(name, property);
                }
            }
            // Now write out all of the updated properties ...
            write(id, sidecar, existing);
        }
    }

    @Override
    public boolean removeProperties( String id ) {
        File file = sidecarFile(id);
        if (!file.exists()) return false;
        file.delete();
        return true;
    }

    protected File sidecarFile( String id ) {
        File actualFile = connector.fileFor(id);
        String extension = DEFAULT_EXTENSION;
        if (connector.isContentNode(id)) {
            extension = DEFAULT_RESOURCE_EXTENSION;
        }
        return new File(actualFile.getAbsolutePath() + extension);
    }

    protected Map<Name, Property> load( String id,
                                        File propertiesFile ) {
        if (!propertiesFile.exists() || !propertiesFile.canRead()) return NO_PROPERTIES;
        try {
            String content = IoUtil.read(propertiesFile);
            Map<Name, Property> result = new HashMap<Name, Property>();
            for (String line : StringUtil.splitLines(content)) {
                // Parse each line ...
                Property property = parse(line, result);
                if (property != null) {
                    result.put(property.getName(), property);
                }
            }
            return result;
        } catch (Throwable e) {
            throw new DocumentStoreException(id, e);
        }
    }

    protected void write( String id,
                          File propertiesFile,
                          Map<Name, Property> properties ) {
        if (properties.isEmpty()) {
            if (propertiesFile.exists()) {
                // Delete the file ...
                propertiesFile.delete();
            }
            return;
        }
        try {
            Writer fileWriter = null;
            try {
                // Write the primary type first ...
                Property primaryType = properties.get(JcrLexicon.PRIMARY_TYPE);
                if (primaryType != null) {
                    fileWriter = new FileWriter(propertiesFile);
                    write(primaryType, fileWriter);
                }
                // Then write the mixin types ...
                Property mixinTypes = properties.get(JcrLexicon.MIXIN_TYPES);
                if (mixinTypes != null) {
                    if (fileWriter == null) fileWriter = new FileWriter(propertiesFile);
                    write(mixinTypes, fileWriter);
                }
                // Then write the UUID ...
                Property uuid = properties.get(JcrLexicon.UUID);
                if (uuid != null) {
                    if (fileWriter == null) fileWriter = new FileWriter(propertiesFile);
                    write(uuid, fileWriter);
                }
                // Then all the others ...
                for (Property property : properties.values()) {
                    if (property == null) continue;
                    if (property == primaryType || property == mixinTypes || property == uuid) continue;
                    if (fileWriter == null) fileWriter = new FileWriter(propertiesFile);
                    write(property, fileWriter);
                }
            } finally {
                if (fileWriter != null) {
                    fileWriter.close();
                } else {
                    // Nothing was written, so remove the sidecar file ...
                    propertiesFile.delete();
                }
            }
        } catch (Throwable e) {
            throw new DocumentStoreException(id, e);
        }
    }

    protected void write( Property property,
                          Writer stream ) throws IOException, RepositoryException {
        String name = stringFactory.create(property.getName());
        stream.append(encoder.encode(name));
        if (property.isEmpty()) {
            stream.append('\n');
            stream.flush();
            return;
        }
        stream.append(" (");
        PropertyType type = PropertyType.discoverType(property.getFirstValue());
        stream.append(type.getName().toLowerCase());
        stream.append(") ");
        if (property.isMultiple()) {
            stream.append('[');
        }
        boolean first = true;
        boolean quote = type == PropertyType.STRING;
        for (Object value : property) {
            if (first) first = false;
            else stream.append(", ");
            String str = null;
            if (value instanceof Binary) {
                byte[] bytes = IoUtil.readBytes(((Binary)value).getStream());
                str = StringUtil.getHexString(bytes);
            } else {
                str = stringFactory.create(value);
            }
            if (quote) {
                stream.append('"');
                stream.append(quoter.encode(str));
                stream.append('"');
            } else {
                stream.append(str);
            }
        }
        if (property.isMultiple()) {
            stream.append(']');
        }
        stream.append('\n');
        stream.flush();
    }

    protected Property parse( String line,
                              Map<Name, Property> result ) throws RepositoryException {
        if (line.length() == 0) return null; // blank line
        char firstChar = line.charAt(0);
        if (firstChar == '#') return null; // comment line
        if (firstChar == ' ') return null; // ignore line
        Matcher matcher = PROPERTY_PATTERN.matcher(line);
        NameFactory nameFactory = factories.getNameFactory();
        if (!matcher.matches()) {
            // It should be an empty multi-valued property, and the line consists only of the name ...
            Name name = nameFactory.create(decoder.decode(line));
            return propertyFactory.create(name);
        }

        String nameString = decoder.decode(matcher.group(1));
        String typeString = matcher.group(2);
        String valuesString = matcher.group(4);

        Name name = null;
        try {
            name = factories.getNameFactory().create(nameString);
        } catch (ValueFormatException e) {
            // See MODE-1281. Earlier versions would write out an empty property without the trailing line feed,
            // so we need to consider this case now. About the only thing we can do is look for two namespace-prefixed names
            // ...
            if (nameString.indexOf(':') < nameString.lastIndexOf(':')) {
                // This is likely two names smashed together. Use the namespace prefixes to look for where we can break this
                // ...
                Set<String> prefixes = new LinkedHashSet<String>();
                prefixes.add(JcrLexicon.Namespace.PREFIX);
                for (String prefix : registry.getPrefixes()) {
                    prefixes.add(prefix);
                }
                for (String prefix : prefixes) {
                    int index = nameString.lastIndexOf(prefix + ":");
                    if (index <= 0) continue;
                    // Otherwise, we found a match. Parse the first property name, and create an empty property ...
                    name = nameFactory.create(nameString.substring(0, index));
                    result.put(name, propertyFactory.create(name));
                    // Now parse the name of the next property and continue ...
                    name = nameFactory.create(nameString.substring(index));
                    break;
                }
            } else {
                throw e;
            }
        }
        PropertyType type = PropertyType.valueFor(typeString);

        Pattern pattern = VALUE_PATTERN;
        ValueFactory<?> valueFactory = factories.getValueFactory(type);
        boolean binary = false;
        boolean decode = false;
        if (type == PropertyType.STRING) {
            // Parse the double-quoted value(s) ...
            pattern = STRING_VALUE_PATTERN;
            decode = true;
        } else if (type == PropertyType.BINARY) {
            binary = true;
        }
        Matcher valuesMatcher = pattern.matcher(valuesString);
        List<Object> values = new ArrayList<Object>();
        while (valuesMatcher.find()) {
            String valueString = valuesMatcher.group(1);
            if (binary) {
                // The value is a hexadecimal-encoded byte array ...
                byte[] binaryValue = StringUtil.fromHexString(valueString);
                Object value = valueFactory.create(binaryValue);
                values.add(value);
            } else {
                if (decode) valueString = quoter.decode(valueString);
                Object value = valueFactory.create(valueString);
                values.add(value);
            }
        }
        if (values.isEmpty()) return null;
        return propertyFactory.create(name, type, values);
    }
}
