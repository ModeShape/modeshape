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
package org.modeshape.sequencer.teiid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.modeshape.common.text.Inflector;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.Node;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.property.UuidFactory;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.ValueFormatException;
import org.modeshape.sequencer.teiid.lexicon.EcoreLexicon;

/**
 * A class that can be used to read an XMI file that has been imported into a graph.
 */
public class XmiGraphReader {

    protected static final Map<String, String> EDATATYPE_TO_JCR_TYPENAME;

    static {
        Map<String, String> types = new HashMap<String, String>();
        types.put("eBoolean", "BOOLEAN");
        types.put("eShort", "LONG");
        types.put("eInt", "LONG");
        types.put("eLong", "LONG");
        types.put("eFloat", "DOUBLE");
        types.put("eDouble", "DOUBLE");
        types.put("eBigDecimal", "DECIMAL");
        types.put("eBigInteger", "DECIMAL");
        types.put("eDate", "DATE");
        types.put("eString", "STRING");
        types.put("eChar", "STRING");
        types.put("eByte", "BINARY");
        types.put("eByteArray", "BINARY");
        types.put("eObject", "UNDEFINED");
        types.put("eBooleanObject", "BOOLEAN");
        types.put("eShortObject", "LONG");
        types.put("eIntObject", "LONG");
        types.put("eLongObject", "LONG");
        types.put("eFloatObject", "DOUBLE");
        types.put("eDoubleObject", "DOUBLE");
        types.put("eCharObject", "STRING");
        types.put("eByteObject", "BINARY");
        EDATATYPE_TO_JCR_TYPENAME = Collections.unmodifiableMap(types);
    }

    protected final ExecutionContext context;
    protected final ValueFactories valueFactories;
    protected final ValueFactory<String> stringFactory;
    protected final ValueFactory<Boolean> booleanFactory;
    protected final NameFactory nameFactory;
    protected final PathFactory pathFactory;
    protected final UuidFactory uuidFactory;
    protected final PropertyFactory propertyFactory;
    protected final Subgraph subgraph;
    protected final NamespaceRegistry namespaces;
    protected final Inflector inflector;
    protected final boolean generateShortNames;
    protected String currentNamespaceUri;
    protected final Map<Name, Name> typeNameReplacements = new HashMap<Name, Name>();

    protected XmiGraphReader( Subgraph subgraph,
                              boolean generateShortNames ) {
        this.subgraph = subgraph;
        this.context = subgraph.getGraph().getContext();
        this.valueFactories = subgraph.getGraph().getContext().getValueFactories();
        this.nameFactory = this.valueFactories.getNameFactory();
        this.stringFactory = this.valueFactories.getStringFactory();
        this.booleanFactory = this.valueFactories.getBooleanFactory();
        this.pathFactory = this.valueFactories.getPathFactory();
        this.uuidFactory = valueFactories.getUuidFactory();
        this.propertyFactory = this.context.getPropertyFactory();
        this.namespaces = this.context.getNamespaceRegistry();
        this.inflector = Inflector.getInstance();
        this.generateShortNames = generateShortNames;
    }

    protected void replaceTypeName( Name replaced,
                                    Name with ) {
        typeNameReplacements.put(replaced, with);
    }

    protected void replaceTypeName( String replaced,
                                    String with ) {
        replaceTypeName(nameFrom(replaced), nameFrom(with));
    }

    protected void setCurrentNamespaceUri( String uri ) {
        this.currentNamespaceUri = uri;
    }

    /**
     * Get the current namespace URI.
     * 
     * @return the URI of the current namespace, or null if there is none
     */
    public String getCurrentNamespaceUri() {
        return currentNamespaceUri;
    }

    protected String namespacePrefix( String prefix ) {
        return inflector.lowerCamelCase(prefix);
    }

    protected String firstValue( Node node,
                                 Name propertyName ) {
        return firstValue(node, propertyName, null);
    }

    protected String firstValue( Node node,
                                 Name propertyName,
                                 String defaultValue ) {
        Property property = node.getProperty(propertyName);
        if (property == null || property.isEmpty()) {
            return defaultValue;
        }
        return stringFactory.create(property.getFirstValue());
    }

    protected String firstValue( Node node,
                                 String propertyName ) {
        return firstValue(node, propertyName, null);
    }

    protected String firstValue( Node node,
                                 String propertyName,
                                 String defaultValue ) {
        Property property = node.getProperty(propertyName);
        if (property == null || property.isEmpty()) {
            return defaultValue;
        }
        return stringFactory.create(property.getFirstValue());
    }

    protected boolean firstValue( Node node,
                                  String propertyName,
                                  boolean defaultValue ) {
        Property property = node.getProperty(propertyName);
        if (property == null || property.isEmpty()) {
            return defaultValue;
        }
        return booleanFactory.create(property.getFirstValue());
    }

    protected long firstValue( Node node,
                               String propertyName,
                               long defaultValue ) {
        Property property = node.getProperty(propertyName);
        if (property == null || property.isEmpty()) {
            return defaultValue;
        }
        return this.valueFactories.getLongFactory().create(property.getFirstValue());
    }

    protected double firstValue( Node node,
                                 String propertyName,
                                 double defaultValue ) {
        Property property = node.getProperty(propertyName);
        if (property == null || property.isEmpty()) {
            return defaultValue;
        }
        return this.valueFactories.getDoubleFactory().create(property.getFirstValue());
    }

    protected List<String> values( Node node,
                                   String propertyName,
                                   String regexDelimiter ) {
        Property property = node.getProperty(propertyName);
        if (property == null || property.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<String>();
        for (Object value : property) {
            String stringValue = stringFactory.create(value);
            for (String val : stringValue.split(regexDelimiter)) {
                if (val != null) values.add(val);
            }
        }
        return values;
    }

    protected List<Name> names( Node node,
                                String propertyName,
                                String regexDelimiter ) {
        Property property = node.getProperty(propertyName);
        if (property == null || property.isEmpty()) {
            return Collections.emptyList();
        }
        List<Name> values = new ArrayList<Name>();
        for (Object value : property) {
            String stringValue = stringFactory.create(value);
            for (String val : stringValue.split(regexDelimiter)) {
                if (val != null) {
                    values.add(nameFrom(val));
                }
            }
        }
        return values;
    }

    protected String jcrTypeNameFor( Name dataType ) {
        if (EcoreLexicon.Namespace.URI.equals(dataType.getNamespaceUri())) {
            // This is a built-in datatype ...
            String localName = dataType.getLocalName();
            String result = EDATATYPE_TO_JCR_TYPENAME.get(localName);
            if (result != null) return result;
        }
        return "UNDEFINED";
    }

    protected Path pathFrom( Object path ) {
        return pathFactory.create(path);
    }

    protected Path relativePathFrom( Name... names ) {
        return pathFactory.createRelativePath(names);
    }

    protected Path path( Path parent,
                         Name name ) {
        return path(parent, name, 1);
    }

    protected Path path( Path parent,
                         String relativePath ) {
        return pathFactory.create(parent, relativePath);
    }

    protected Path path( Path parent,
                         Name name,
                         int snsIndex ) {
        return path(parent, pathFactory.createSegment(name, snsIndex));
    }

    protected Path path( Path parent,
                         Path.Segment segment ) {
        return pathFactory.create(parent, segment);
    }

    protected Property property( Name name,
                                 Object... values ) {
        return propertyFactory.create(name, values);
    }

    protected Property property( Name name,
                                 PropertyType type,
                                 Object... values ) {
        return propertyFactory.create(name, type, values);
    }

    protected Name nameFrom( String name ) {
        String value = name;
        if (value.startsWith("#//")) {
            value = value.substring(3);
        } else if (value.startsWith("//")) {
            value = value.substring(2);
        }
        if (value.startsWith("ecore:EDataType ")) {
            value = value.substring("ecore:DataType ".length() + 1);
            // otherwise, just continue ...
        }
        // Look for the namespace ...
        Name result = null;
        String[] parts = value.split("\\#");
        if (parts.length == 2) {
            String uri = parts[0].trim();
            String localName = nameFrom(parts[1]).getLocalName(); // removes '#//'
            result = shortenName(nameFactory.create(uri, localName));
        } else {
            value = inflector.underscore(value, '_', '-', '.');
            value = inflector.lowerCamelCase(value, '_');
            result = shortenName(nameFactory.create(value));
        }
        return nameFrom(result);
    }

    protected Name nameFrom( Name name ) {
        if (this.currentNamespaceUri != null && name.getNamespaceUri().isEmpty()) {
            name = nameFactory.create(this.currentNamespaceUri, name.getLocalName());
        }
        return name;
    }

    protected Name typeNameFrom( Name name ) {
        String singularLocalName = inflector.singularize(name.getLocalName());
        if (this.currentNamespaceUri != null && name.getNamespaceUri().isEmpty()) {
            name = nameFactory.create(this.currentNamespaceUri, singularLocalName);
        }
        if (!singularLocalName.equals(name.getLocalName())) {
            name = nameFactory.create(name.getNamespaceUri(), singularLocalName);
        }
        Name replacement = typeNameReplacements.get(name);
        name = replacement != null ? replacement : name;
        return shortenName(name);
    }

    /**
     * Determine the name of the property that is used to hold the "href" URL literal value of the EObject reference.
     * <p>
     * The resulting name fits one of two patterns, depending upon whether the {@link Name#getLocalName() local part} of the
     * reference name is singular or plural. If singular, then the resulting name will have "Href" appended to the
     * {@link Name#getLocalName() local part} of the supplied name. If plural, then the resulting name will consist of the
     * singularlized {@link Name#getLocalName() local part} appended with "Hrefs". In all cases, the
     * {@link Name#getNamespaceUri() namespace URI} of the resulting name will match that of the supplied name.
     * </p>
     * 
     * @param eObjectReferenceName the name of the normal EObject reference property, which is typically a href
     * @return the name
     */
    protected Name nameForHref( Name eObjectReferenceName ) {
        String localPart = eObjectReferenceName.getLocalName();
        String singular = inflector.singularize(localPart);
        String suffix = singular.equals(localPart) ? "Href" : "Hrefs";
        return nameFactory.create(eObjectReferenceName.getNamespaceUri(), singular + suffix);
    }

    /**
     * Determine the name of the property that is used to hold the WEAKREFERENCE values for the resolved EObject reference.
     * <p>
     * This method just returns the name without modification.
     * </p>
     * 
     * @param eObjectReferenceName the name of the normal EObject reference property, which is typically a href
     * @return the name
     */
    protected Name nameForResolvedReference( Name eObjectReferenceName ) {
        return eObjectReferenceName;
    }

    /**
     * Determine the name of the property that is used to hold the name(s) for the resolved EObject reference.
     * <p>
     * The resulting name fits one of two patterns, depending upon whether the {@link Name#getLocalName() local part} of the
     * reference name is singular or plural. If singular, then the resulting name will have "Name" appended to the
     * {@link Name#getLocalName() local part} of the supplied name. If plural, then the resulting name will consist of the
     * singularlized {@link Name#getLocalName() local part} appended with "Names". In all cases, the
     * {@link Name#getNamespaceUri() namespace URI} of the resulting name will match that of the supplied name.
     * </p>
     * 
     * @param eObjectReferenceName the name of the normal EObject reference property, which is typically a href
     * @return the name
     */
    protected Name nameForResolvedName( Name eObjectReferenceName ) {
        String localPart = eObjectReferenceName.getLocalName();
        String singular = inflector.singularize(localPart);
        String suffix = singular.equals(localPart) ? "Name" : "Names";
        return nameFactory.create(eObjectReferenceName.getNamespaceUri(), singular + suffix);
    }

    /**
     * Determine the name of the property that is used to hold the identifier(s) for the resolved EObject reference.
     * <p>
     * The resulting name fits one of two patterns, depending upon whether the {@link Name#getLocalName() local part} of the
     * reference name is singular or plural. If singular, then the resulting name will have "XmiUuid" appended to the
     * {@link Name#getLocalName() local part} of the supplied name. If plural, then the resulting name will consist of the
     * singularlized {@link Name#getLocalName() local part} appended with "XmiUuids". In all cases, the
     * {@link Name#getNamespaceUri() namespace URI} of the resulting name will match that of the supplied name.
     * </p>
     * 
     * @param eObjectReferenceName the name of the normal EObject reference property, which is typically a href
     * @return the name
     */
    protected Name nameForResolvedId( Name eObjectReferenceName ) {
        String localPart = eObjectReferenceName.getLocalName();
        String singular = inflector.singularize(localPart);
        String suffix = singular.equals(localPart) ? "XmiUuid" : "XmiUuids";
        return nameFactory.create(eObjectReferenceName.getNamespaceUri(), singular + suffix);
    }

    protected String stringFrom( Object value ) {
        return stringFactory.create(value);
    }

    protected Name shortenName( Name name ) {
        if (generateShortNames) {
            // If the local name begins with the namespace prefix, remove it ...
            String localName = name.getLocalName();
            String prefix = namespaces.getPrefixForNamespaceUri(name.getNamespaceUri(), false);
            if (prefix != null && localName.startsWith(prefix) && localName.length() > prefix.length()) {
                localName = localName.substring(prefix.length());
                localName = inflector.underscore(localName, '_', '-', '.');
                localName = inflector.lowerCamelCase(localName, '_');
                if (localName.length() > 0) return nameFactory.create(name.getNamespaceUri(), localName);
            }
        }
        return name;
    }

    protected UUID uuidFor( Node node ) {
        Property property = node.getProperty(JcrLexicon.UUID);
        if (property == null || property.isEmpty()) {
            property = node.getProperty(ModeShapeLexicon.UUID);
        }
        assert property != null;
        UUID result = this.uuidFactory.create(property.getFirstValue());
        assert result != null;
        return result;
    }

    protected UUID xmiUuidFor( Node node ) {
        Property property = node.getProperty("xmi:uuid");
        if (property == null) return null;
        String mmuuid = stringFactory.create(property.getFirstValue());
        if (mmuuid.startsWith("mmuuid:")) mmuuid = mmuuid.substring(7);
        UUID result = property == null ? null : this.uuidFactory.create(mmuuid);
        assert result != null;
        return result;
    }

    /**
     * Extracts the "mmuuid" values from the property if the property is indeed an XMI reference to local objects.
     * 
     * @param property the property
     * @return the list of mmuuid values, or null if this property does not contain any references; never empty
     */
    protected List<UUID> references( Property property ) {
        List<UUID> result = null;
        try {
            for (Object value : property) {
                if (value instanceof String) {
                    String str = (String)value;
                    if (str.startsWith("mmuuid/")) {
                        // It is a local reference ...
                        String[] references = str.split("\\s");
                        for (String reference : references) {
                            UUID uuid = this.uuidFactory.create(reference.substring(7));
                            if (result == null) {
                                if (property.isSingle() && references.length == 1) {
                                    // This is the only property value, and only one reference in it ...
                                    return Collections.singletonList(uuid);
                                }
                                // This isn't the only property value ...
                                result = new LinkedList<UUID>();
                            }
                            result.add(uuid);
                        }
                    } else {
                        assert result == null;
                        return null;
                    }
                } else {
                    assert result == null;
                    return null;
                }
            }
        } catch (ValueFormatException e) {
            // One of the values starts with "mmuuid/" but the remainder was not a UUID, so just continue ...
        }
        return result;
    }

}
