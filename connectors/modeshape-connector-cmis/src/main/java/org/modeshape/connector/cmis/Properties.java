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
package org.modeshape.connector.cmis;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Document.Field;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;

/**
 * Implements mapping between several CMIS and JCR properties. This implementation of the connector suppose conversation between
 * cmis folders and document into jcr folders and files. Such conversation in its order suppose conversation of the names and
 * values. This utility class provides such work for us.
 * 
 * @author kulikov
 */
@SuppressWarnings( "synthetic-access" )
public class Properties {
    // table which establishes relations between cmis and jcr names
    // used for properties of teh folders and documents
    // Relations are defined as list of strings. This way seems preffered because
    // we have a small amount of such relations and simple linear search give us
    // better performance for small sets.
    private final static String[] map = new String[] {"cmis:objectId = jcr:uuid", "cmis:createdBy = jcr:createdBy",
        "cmis:creationDate = jcr:created", "cmis:lastModificationDate = jcr:lastModified", "cmis:lastModifiedBy = -"};

    // this is value factory used for converation of the values
    private ValueFactories valueFactories;

    // list of relations between names
    private ArrayList<Relation> list = new ArrayList<Relation>();

    /**
     * Constructs this class instance.
     * 
     * @param valueFactories jcr value factory
     */
    public Properties( ValueFactories valueFactories ) {
        this.valueFactories = valueFactories;
        // parse strings and create relations
        for (int i = 0; i < map.length; i++) {
            String[] tokens = map[i].split("=");

            String lhs = tokens[0].trim();
            String rhs = tokens[1].trim();

            lhs = lhs.equals("-") ? null : lhs;
            rhs = rhs.equals("-") ? null : rhs;

            list.add(new Relation(lhs, rhs));
        }
    }

    /**
     * Determines the name of the given property from cmis domain in jcr domain.
     * 
     * @param cmisName the name of property in cmis domain.
     * @return the name of the same property in jcr domain.
     */
    public String findJcrName( String cmisName ) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).cmisName != null && list.get(i).cmisName.equals(cmisName)) {
                return list.get(i).jcrName;
            }
        }
        return cmisName;
    }

    /**
     * Determines the name of the given property from jcr domain in cmis domain.
     * 
     * @param jcrName the name of property in jcr domain.
     * @return the name of the same property in cmis domain.
     */
    public String findCmisName( String jcrName ) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).jcrName != null && list.get(i).jcrName.equals(jcrName)) {
                return list.get(i).cmisName;
            }
        }
        return jcrName;
    }

    /**
     * Converts type of property.
     * 
     * @param propertyType the type of the property in cmis domain.
     * @return the type of the property in jcr domain.
     */
    public int getJcrType( PropertyType propertyType ) {
        switch (propertyType) {
            case BOOLEAN:
                return javax.jcr.PropertyType.BOOLEAN;
            case DATETIME:
                return javax.jcr.PropertyType.DATE;
            case DECIMAL:
                return javax.jcr.PropertyType.DECIMAL;
            case HTML:
                return javax.jcr.PropertyType.STRING;
            case INTEGER:
                return javax.jcr.PropertyType.LONG;
            case URI:
                return javax.jcr.PropertyType.URI;
            case ID:
                return javax.jcr.PropertyType.STRING;
            default:
                return javax.jcr.PropertyType.UNDEFINED;
        }
    }

    /**
     * Calculates value of the property corresponding to cmis domain.
     * 
     * @param pdef property definition as declared in cmis repository.
     * @param field the representation of the value of the property in jcr domain.
     * @return value as declared by property definition.
     */
    public Object cmisValue( PropertyDefinition<?> pdef,
                             Field field ) {
        switch (pdef.getPropertyType()) {
            case STRING:
                return field.getValueAsString();
            case BOOLEAN:
                return field.getValueAsBoolean();
            case DECIMAL:
                return BigDecimal.valueOf(field.getValueAsInt());
            case INTEGER:
                return field.getValueAsInt();
            case DATETIME:
                // FIXME
                return new GregorianCalendar();
            case URI:
                try {
                    return new URI(field.getValueAsString());
                } catch (Exception e) {
                }
                break;
            case ID:
                return field.getValueAsUuid();
            case HTML:
                return field.getValueAsString();
        }

        return null;
    }

    /**
     * Calculates value of the property corresponding to cmis domain.
     * 
     * @param pdef property definition as declared in cmis repository.
     * @param jcrName the name of the property in jcr domain
     * @param document connectors's view of properties in jcr domain.
     * @return value as declared by property definition.
     */
    public Object cmisValue( PropertyDefinition<?> pdef,
                             String jcrName,
                             Document document ) {
        switch (pdef.getPropertyType()) {
            case STRING:
                return document.getString(jcrName);
            case BOOLEAN:
                return document.getBoolean(jcrName);
            case DECIMAL:
                return BigDecimal.valueOf(document.getLong(jcrName));
            case INTEGER:
                return document.getInteger(jcrName);
            case DATETIME:
                // FIXME
                return new GregorianCalendar();
            case URI:
                try {
                    return new URI(document.getString(jcrName));
                } catch (Exception e) {
                }
                break;
            case ID:
                return document.getString(jcrName);
            case HTML:
                return document.getString(jcrName);
        }

        return null;
    }

    /**
     * Converts value of the property for the jcr domain.
     * 
     * @param property property in cmis domain
     * @return value of the given property in jcr domain.
     */
    public Object[] jcrValues( Property<?> property ) {
        @SuppressWarnings( "unchecked" )
        List<Object> values = (List<Object>)property.getValues();

        // convert CMIS values to JCR values
        switch (property.getType()) {
            case STRING:
                return asStrings(values);
            case BOOLEAN:
                return asBooleans(values);
            case DECIMAL:
                return asDecimals(values);
            case INTEGER:
                return asIntegers(values);
            case DATETIME:
                return asDateTime(values);
            case URI:
                return asURI(values);
            case ID:
                return asIDs(values);
            case HTML:
                return asHTMLs(values);
            default:
                return null;
        }
    }

    /**
     * Converts CMIS value of boolean type into JCR value of boolean type.
     * 
     * @param values CMIS values of boolean type
     * @return JCR values of boolean type
     */
    private Boolean[] asBooleans( List<Object> values ) {
        ValueFactory<Boolean> factory = valueFactories.getBooleanFactory();
        Boolean[] res = new Boolean[values.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = factory.create(values.get(i));
        }
        return res;
    }

    /**
     * Converts CMIS value of string type into JCR value of string type.
     * 
     * @param values CMIS values of string type
     * @return JCR values of string type
     */
    private String[] asStrings( List<Object> values ) {
        ValueFactory<String> factory = valueFactories.getStringFactory();
        String[] res = new String[values.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = factory.create(values.get(i));
        }
        return res;
    }

    /**
     * Converts CMIS value of integer type into JCR value of boolean type.
     * 
     * @param values CMIS values of integer type
     * @return JCR values of integer type
     */
    private Long[] asIntegers( List<Object> values ) {
        ValueFactory<Long> factory = valueFactories.getLongFactory();
        Long[] res = new Long[values.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = factory.create(values.get(i));
        }
        return res;
    }

    /**
     * Converts CMIS value of decimal type into JCR value of boolean type.
     * 
     * @param values CMIS values of decimal type
     * @return JCR values of decimal type
     */
    private BigDecimal[] asDecimals( List<Object> values ) {
        ValueFactory<BigDecimal> factory = valueFactories.getDecimalFactory();
        BigDecimal[] res = new BigDecimal[values.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = factory.create(values.get(i));
        }
        return res;
    }

    /**
     * Converts CMIS value of date/time type into JCR value of date/time type.
     * 
     * @param values CMIS values of gregorian calendar type
     * @return JCR values of date/time type
     */
    private DateTime[] asDateTime( List<Object> values ) {
        ValueFactory<DateTime> factory = valueFactories.getDateFactory();
        DateTime[] res = new DateTime[values.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = factory.create(((GregorianCalendar)values.get(i)).getTime());
        }
        return res;
    }

    /**
     * Converts CMIS value of URI type into JCR value of URI type.
     * 
     * @param values CMIS values of URI type
     * @return JCR values of URI type
     */
    private URI[] asURI( List<Object> values ) {
        ValueFactory<URI> factory = valueFactories.getUriFactory();
        URI[] res = new URI[values.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = factory.create(((GregorianCalendar)values.get(i)).getTime());
        }
        return res;
    }

    /**
     * Converts CMIS value of ID type into JCR value of String type.
     * 
     * @param values CMIS values of Id type
     * @return JCR values of String type
     */
    private String[] asIDs( List<Object> values ) {
        ValueFactory<String> factory = valueFactories.getStringFactory();
        String[] res = new String[values.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = factory.create(values.get(i));
        }
        return res;
    }

    /**
     * Converts CMIS value of HTML type into JCR value of String type.
     * 
     * @param values CMIS values of HTML type
     * @return JCR values of String type
     */
    private String[] asHTMLs( List<Object> values ) {
        ValueFactory<String> factory = valueFactories.getStringFactory();
        String[] res = new String[values.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = factory.create(values.get(i));
        }
        return res;
    }

    private class Relation {
        private String jcrName;
        private String cmisName;

        private Relation( String cmisName,
                          String jcrName ) {
            this.cmisName = cmisName;
            this.jcrName = jcrName;
        }
    }
}
