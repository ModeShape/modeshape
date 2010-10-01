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
package org.modeshape.connector.filesystem;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.modeshape.common.text.QuoteEncoder;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.text.XmlNameEncoder;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;

/**
 * A {@link CustomPropertiesFactory} implementation that stores "extra" or "custom" properties for 'nt:file', 'nt:folder', and
 * 'nt:resource' nodes in a separate file that is named the same as the original but with a different extension.
 */
public class StoreProperties extends BasePropertiesFactory {

    private static final long serialVersionUID = 1L;

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
    protected static final Map<Name, Property> NO_PROPERTIES_MAP = Collections.emptyMap();

    private final String extension;
    private final String resourceExtension;
    private String sourceName;
    private TextEncoder encoder = new XmlNameEncoder();
    private TextDecoder decoder = new XmlNameEncoder();
    private QuoteEncoder quoter = new QuoteEncoder();

    /**
     * 
     */
    public StoreProperties() {
        extension = DEFAULT_EXTENSION;
        resourceExtension = DEFAULT_RESOURCE_EXTENSION;
    }

    @Override
    public FilenameFilter getFilenameFilter( final FilenameFilter exclusionFilter ) {
        final Pattern extensionFilter = Pattern.compile(extension.replaceAll("\\.", "\\\\.") + "$");
        final Pattern resourceExtensionFilter = Pattern.compile(resourceExtension.replaceAll("\\.", "\\\\.") + "$");
        return new FilenameFilter() {

            public boolean accept( File dir,
                                   String name ) {
                if (extensionFilter.matcher(name).matches()) return false;
                if (resourceExtensionFilter.matcher(name).matches()) return false;
                if (exclusionFilter != null && !exclusionFilter.accept(dir, name)) return false;
                return true;
            }
        };

    }

    /**
     * @return sourceName
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * @param sourceName Sets sourceName to the specified value.
     */
    public void setSourceName( String sourceName ) {
        this.sourceName = sourceName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.connector.filesystem.CustomPropertiesFactory#getDirectoryProperties(org.modeshape.graph.ExecutionContext,
     *      org.modeshape.graph.Location, java.io.File)
     */
    @Override
    public Collection<Property> getDirectoryProperties( ExecutionContext context,
                                                        Location location,
                                                        File directory ) {
        File parent = directory.getParentFile();
        if (parent == null) return NO_PROPERTIES_COLLECTION;
        return load(propertiesFileFor(directory), context).values();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.connector.filesystem.CustomPropertiesFactory#getFileProperties(org.modeshape.graph.ExecutionContext,
     *      org.modeshape.graph.Location, java.io.File)
     */
    @Override
    public Collection<Property> getFileProperties( ExecutionContext context,
                                                   Location location,
                                                   File file ) {
        return load(propertiesFileFor(file), context).values();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.connector.filesystem.CustomPropertiesFactory#getResourceProperties(org.modeshape.graph.ExecutionContext,
     *      org.modeshape.graph.Location, java.io.File, java.lang.String)
     */
    @Override
    public Collection<Property> getResourceProperties( ExecutionContext context,
                                                       Location location,
                                                       File file,
                                                       String mimeType ) {
        return load(propertiesFileForResource(file), context).values();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.connector.filesystem.CustomPropertiesFactory#recordDirectoryProperties(org.modeshape.graph.ExecutionContext,
     *      java.lang.String, org.modeshape.graph.Location, java.io.File, java.util.Map)
     */
    @Override
    public Set<Name> recordDirectoryProperties( ExecutionContext context,
                                                String sourceName,
                                                Location location,
                                                File file,
                                                Map<Name, Property> properties ) throws RepositorySourceException {
        return write(propertiesFileFor(file), context, properties);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.connector.filesystem.CustomPropertiesFactory#recordFileProperties(org.modeshape.graph.ExecutionContext,
     *      java.lang.String, org.modeshape.graph.Location, java.io.File, java.util.Map)
     */
    @Override
    public Set<Name> recordFileProperties( ExecutionContext context,
                                           String sourceName,
                                           Location location,
                                           File file,
                                           Map<Name, Property> properties ) throws RepositorySourceException {
        return write(propertiesFileFor(file), context, properties);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.connector.filesystem.CustomPropertiesFactory#recordResourceProperties(org.modeshape.graph.ExecutionContext,
     *      java.lang.String, org.modeshape.graph.Location, java.io.File, java.util.Map)
     */
    @Override
    public Set<Name> recordResourceProperties( ExecutionContext context,
                                               String sourceName,
                                               Location location,
                                               File file,
                                               Map<Name, Property> properties ) throws RepositorySourceException {
        return write(propertiesFileForResource(file), context, properties);
    }

    protected File propertiesFileFor( File fileOrDirectory ) {
        return new File(fileOrDirectory.getPath() + extension);
    }

    protected File propertiesFileForResource( File fileOrDirectory ) {
        return new File(fileOrDirectory.getPath() + resourceExtension);
    }

    protected Map<Name, Property> load( File propertiesFile,
                                        ExecutionContext context ) throws RepositorySourceException {
        if (!propertiesFile.exists() || !propertiesFile.canRead()) return NO_PROPERTIES_MAP;
        try {
            String content = IoUtil.read(propertiesFile);
            ValueFactories factories = context.getValueFactories();
            PropertyFactory propFactory = context.getPropertyFactory();
            Map<Name, Property> result = new HashMap<Name, Property>();
            for (String line : StringUtil.splitLines(content)) {
                // Parse each line ...
                Property property = parse(line, factories, propFactory);
                if (property != null) {
                    result.put(property.getName(), property);
                }
            }
            return result;
        } catch (IOException e) {
            throw new RepositorySourceException(sourceName, e);
        }
    }

    protected Set<Name> write( File propertiesFile,
                               ExecutionContext context,
                               Map<Name, Property> properties ) throws RepositorySourceException {
        if (properties.isEmpty()) {
            if (propertiesFile.exists()) {
                // Delete the file ...
                propertiesFile.delete();
            }
            return Collections.emptySet();
        }
        Set<Name> names = new HashSet<Name>();
        try {
            ValueFactory<String> strings = context.getValueFactories().getStringFactory();
            Writer fileWriter = new FileWriter(propertiesFile);
            try {
                // Write the primary type first ...
                Property primaryType = properties.get(JcrLexicon.PRIMARY_TYPE);
                if (primaryType != null) {
                    write(primaryType, fileWriter, strings);
                    names.add(primaryType.getName());
                }
                // Then write the mixin types ...
                Property mixinTypes = properties.get(JcrLexicon.MIXIN_TYPES);
                if (mixinTypes != null) {
                    write(mixinTypes, fileWriter, strings);
                    names.add(mixinTypes.getName());
                }
                // Then write the UUID ...
                Property uuid = properties.get(JcrLexicon.UUID);
                if (uuid != null) {
                    write(uuid, fileWriter, strings);
                    names.add(uuid.getName());
                }
                // Then all the others ...
                for (Property property : properties.values()) {
                    if (property == primaryType || property == mixinTypes || property == uuid) continue;
                    write(property, fileWriter, strings);
                    names.add(property.getName());
                }
            } finally {
                fileWriter.close();
            }
        } catch (IOException e) {
            throw new RepositorySourceException(sourceName, e);
        }
        return names;
    }

    protected void write( Property property,
                          Writer stream,
                          ValueFactory<String> strings ) throws IOException {
        String name = strings.create(property.getName());
        stream.append(encoder.encode(name));
        if (property.isEmpty()) return;
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
                str = StringUtil.getHexString(((Binary)value).getBytes());
            } else {
                str = strings.create(value);
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
                              ValueFactories factories,
                              PropertyFactory propFactory ) {
        if (line.length() == 0) return null; // blank line
        char firstChar = line.charAt(0);
        if (firstChar == '#') return null; // comment line
        if (firstChar == ' ') return null; // ignore line
        Matcher matcher = PROPERTY_PATTERN.matcher(line);
        if (!matcher.matches()) {
            // It should be an empty multi-valued property, and the line consists only of the name ...
            Name name = factories.getNameFactory().create(decoder.decode(line));
            return propFactory.create(name);
        }

        String nameString = decoder.decode(matcher.group(1));
        String typeString = matcher.group(2);
        String valuesString = matcher.group(4);

        Name name = factories.getNameFactory().create(nameString);
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
        return propFactory.create(name, type, values);
    }
}
