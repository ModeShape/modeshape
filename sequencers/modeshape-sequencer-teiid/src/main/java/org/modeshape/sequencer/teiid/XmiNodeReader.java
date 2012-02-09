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

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import org.modeshape.common.text.Inflector;
import org.modeshape.jcr.api.ValueFactory;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.sequencer.teiid.lexicon.EcoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.XmiLexicon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A class that can be used to read an XMI file that has been imported into a {@link Node node} hierarchy.
 */
public class XmiNodeReader {

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

    protected final Sequencer.Context context;
    protected final ValueFactory valueFactory;
    protected final Node rootNode;
    protected final NamespaceRegistry namespaceRegistry;
    protected final Inflector inflector;
    protected final boolean generateShortNames;
    protected String currentNamespaceUri;
    protected final Map<String, String> typeNameReplacements = new HashMap<String, String>();

    protected XmiNodeReader( Node rootNode,
                             Sequencer.Context context,
                             boolean generateShortNames ) throws RepositoryException {
        this.rootNode = rootNode;
        this.context = context;
        this.valueFactory = context.valueFactory();
        this.namespaceRegistry = rootNode.getSession().getWorkspace().getNamespaceRegistry();
        this.inflector = Inflector.getInstance();
        this.generateShortNames = generateShortNames;
    }

    protected void replaceTypeName( String replaced,
                                    String with ) {
        typeNameReplacements.put(nameFrom(replaced), nameFrom(with));
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
                                 String propertyName ) throws RepositoryException {
        return firstValue(node, propertyName, null);
    }

    protected String firstValue( Node node,
                                 String propertyName,
                                 String defaultValue ) throws RepositoryException {
        Property property = node.getProperty(propertyName);
        if (property == null) {
            return defaultValue;
        }
        return property.getString();
    }

    protected boolean firstValue( Node node,
                                  String propertyName,
                                  boolean defaultValue ) throws RepositoryException {
        Property property = node.getProperty(propertyName);
        if (property == null) {
            return defaultValue;
        }
        return property.getBoolean();
    }

    protected long firstValue( Node node,
                               String propertyName,
                               long defaultValue ) throws RepositoryException {
        Property property = node.getProperty(propertyName);
        if (property == null) {
            return defaultValue;
        }
        return property.getLong();
    }

    protected double firstValue( Node node,
                                 String propertyName,
                                 double defaultValue ) throws RepositoryException {
        Property property = node.getProperty(propertyName);
        if (property == null) {
            return defaultValue;
        }
        return property.getDouble();
    }

    protected List<String> values( Node node,
                                   String propertyName,
                                   String regexDelimiter ) throws RepositoryException {
        Property property = node.getProperty(propertyName);
        if (property == null) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<String>();
        for (Value value : property.getValues()) {
            String stringValue = value.getString();
            for (String val : stringValue.split(regexDelimiter)) {
                if (val != null) values.add(val);
            }
        }
        return values;
    }

    protected List<String> names( Node node,
                                  String propertyName,
                                  String regexDelimiter ) throws RepositoryException {
        Property property = node.getProperty(propertyName);
        if (property == null) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<String>();
        for (Value value : property.getValues()) {
            String stringValue = value.getString();
            for (String val : stringValue.split(regexDelimiter)) {
                if (val != null) {
                    values.add(nameFrom(val));
                }
            }
        }
        return values;
    }

    protected String jcrTypeNameFor( String dataType ) {
        if (EcoreLexicon.Namespace.URI.equals(uriFrom(dataType))) {
            // This is a built-in datatype ...
            String localName = localNameFrom(dataType);
            String result = EDATATYPE_TO_JCR_TYPENAME.get(localName);
            if (result != null) return result;
        }
        return "UNDEFINED";
    }

    protected String nameFrom( String name ) {
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
        String result = null;
        String[] parts = value.split("\\#");
        if (parts.length == 2) {
            String uri = parts[0].trim();
            String localName = nameFrom(parts[1]); // removes '#//'
            result = shortenName(valueFactory.createName(uri, localName));
        } else {
            value = inflector.underscore(value, '_', '-', '.');
            value = inflector.lowerCamelCase(value, '_');
            result = shortenName(valueFactory.createName(value));
        }
        return createNameWithCurrentNamespace(result);
    }

    protected String createNameWithCurrentNamespace( String name ) {
        boolean hasNamespace = name.contains(":");
        if (this.currentNamespaceUri != null && !hasNamespace) {
            return valueFactory.createName(currentNamespaceUri, name);
        }
        return name;
    }

    protected String typeNameFrom( String name ) {
        String localName = localNameFrom(name);
        String uri = uriFrom(name);
        String singularLocalName = inflector.singularize(localName);
        if (this.currentNamespaceUri != null && uri == null) {
            name = valueFactory.createName(this.currentNamespaceUri, singularLocalName);
        }
        if (!singularLocalName.equals(localName)) {
            name = valueFactory.createName(uri, singularLocalName);
        }
        String replacement = typeNameReplacements.get(name);
        name = replacement != null ? replacement : name;
        return shortenName(name);
    }

    /**
     * Determine the name of the property that is used to hold the "href" URL literal value of the EObject reference.
     * <p>
     * The resulting name fits one of two patterns, depending upon whether the {@code local part} of the
     * reference name is singular or plural. If singular, then the resulting name will have "Href" appended to the
     * {@code local part} of the supplied name. If plural, then the resulting name will consist of the
     * singularlized {@code local part} appended with "Hrefs". In all cases, the
     * {@code namespace URI} of the resulting name will match that of the supplied name.
     * </p>
     *
     * @param eObjectReferenceName the name of the normal EObject reference property, which is typically a href
     * @return the name
     */
    protected String nameForHref( String eObjectReferenceName ) {
        String localPart = localNameFrom(eObjectReferenceName);
        String uri = localNameFrom(eObjectReferenceName);
        String singular = inflector.singularize(localPart);
        String suffix = singular.equals(localPart) ? "Href" : "Hrefs";
        return valueFactory.createName(uri, singular + suffix);
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
    protected String nameForResolvedReference( String eObjectReferenceName ) {
        return eObjectReferenceName;
    }

    /**
     * Determine the name of the property that is used to hold the name(s) for the resolved EObject reference.
     * <p>
     * The resulting name fits one of two patterns, depending upon whether the {@code local part} of the
     * reference name is singular or plural. If singular, then the resulting name will have "Name" appended to the
     * {@code local part} of the supplied name. If plural, then the resulting name will consist of the
     * singularlized {@code local part} appended with "Names". In all cases, the
     * {@code namespace URI} of the resulting name will match that of the supplied name.
     * </p>
     *
     * @param eObjectReferenceName the name of the normal EObject reference property, which is typically a href
     * @return the name
     */
    protected String nameForResolvedName( String eObjectReferenceName ) {
        String localPart = localNameFrom(eObjectReferenceName);
        String uri = uriFrom(eObjectReferenceName);
        String singular = inflector.singularize(localPart);
        String suffix = singular.equals(localPart) ? "Name" : "Names";
        return valueFactory.createName(uri, singular + suffix);
    }

    /**
     * Determine the name of the property that is used to hold the identifier(s) for the resolved EObject reference.
     * <p>
     * The resulting name fits one of two patterns, depending upon whether the {@code local part} of the
     * reference name is singular or plural. If singular, then the resulting name will have "XmiUuid" appended to the
     * {@code local part} of the supplied name. If plural, then the resulting name will consist of the
     * singularlized {@code local part} appended with "XmiUuids". In all cases, the
     * {@code namespace URI} of the resulting name will match that of the supplied name.
     * </p>
     *
     * @param eObjectReferenceName the name of the normal EObject reference property, which is typically a href
     * @return the name
     */
    protected String nameForResolvedId( String eObjectReferenceName ) {
        String localPart = localNameFrom(eObjectReferenceName);
        String uri = uriFrom(eObjectReferenceName);
        String singular = inflector.singularize(localPart);
        String suffix = singular.equals(localPart) ? "XmiUuid" : "XmiUuids";
        return valueFactory.createName(uri, singular + suffix);
    }

    protected String shortenName( String name ) {
        int uriToLocalNameSeparatorIdx = name.indexOf(":");
        if (generateShortNames && uriToLocalNameSeparatorIdx != -1) {
            // If the local name begins with the namespace prefix, remove it ...
            String localName = localNameFrom(name);
            String uri = uriFrom(name);

            try {
                String prefix = namespaceRegistry.getPrefix(uri);
                if (localName.startsWith(prefix) && localName.length() > prefix.length()) {
                    localName = localName.substring(prefix.length());
                    localName = inflector.underscore(localName, '_', '-', '.');
                    localName = inflector.lowerCamelCase(localName, '_');
                    if (localName.length() > 0) {
                        return valueFactory.createName(uri, localName);
                    }
                }

            } catch (RepositoryException e) {
                //prefix doesn't exist
            }
        }
        return name;
    }

    protected String localNameFrom( String fullName ) {
        return fullName.contains(":") ? fullName.substring(fullName.lastIndexOf(":") + 1) : fullName;
    }

    protected String uriFrom( String fullName ) {
        return fullName.contains(":") ? fullName.substring(0, fullName.lastIndexOf(":")).replaceAll("\\{", "").replaceAll("\\}",
                                                                                                                          "")
                : null;
    }

    protected String uuidFor( Node node ) throws RepositoryException {
        return node.getIdentifier();
        //TODO author=Horia Chiorean date=2/9/12 description=Check this
        //        Property property = node.getProperty(JcrLexicon.UUID);
        //        if (property == null || property.isEmpty()) {
        //            property = node.getProperty(ModeShapeLexicon.UUID);
        //        }
        //        assert property != null;
        //        UUID result = this.uuidFactory.create(property.getFirstValue());
        //        assert result != null;
        //        return result;
    }

    protected String xmiUuidFor( Node node ) throws RepositoryException {
        Property mmuuidProperty = node.getProperty(XmiLexicon.UUID);
        if (mmuuidProperty == null) {
            return null;
        }
        return uuidFromString(mmuuidProperty.getString());
    }

    /**
     * Extracts the "mmuuid" values from the property if the property is indeed an XMI reference to local objects.
     *
     * @param property the property
     * @return the list of mmuuid values, or null if this property does not contain any references; never empty
     */
    protected List<String> references( Property property ) throws RepositoryException {
        List<String> result = new LinkedList<String>();
        for (Value value : property.getValues()) {
            String str = value.getString();
            if (str.startsWith("mmuuid/")) {
                // It is a local reference ...
                String[] references = str.split("\\s");
                for (String reference : references) {
                    String uuid = uuidFromString(reference);
                    result.add(uuid);
                    if (!property.isMultiple() && references.length == 1) {
                        // This is the only property value, and only one reference in it ...
                        return result;
                    }
                }
            } else {
                assert result.isEmpty();
                return null;
            }
        }
        return result;
    }

    private String uuidFromString( String value ) {
        String prefix = "mmuuid:";
        if (value.startsWith(prefix)) {
            value = value.substring(prefix.length() + 1);
        }
        //this only validates that value is a legal UUID
        return UUID.fromString(value).toString();
    }
}