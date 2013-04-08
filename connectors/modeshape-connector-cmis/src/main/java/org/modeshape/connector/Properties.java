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
package org.modeshape.connector;

import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Document.Field;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements mapping between several CMIS and JCR  properties.
 * <p/>
 * This implementation of the connector suppose conversation between cmis folders
 * and document into jcr folders and files. Such conversation in its order suppose
 * conversation of the names and values. This utility class provides such work for us.
 *
 * @author kulikov
 */
public class Properties {
    //table which establishes relations between cmis and jcr names 
    //used for properties
    //Relations are defined as list of strings. This way seems preffered because 
    //we have a small amount of such relations and simple linear search give us
    //better performance for small sets.

    /*
      https://docs.jboss.org/author/display/MODE/Storing+files+and+folders
      myFile  (jcr:primaryType=nt:file,
      |        jcr:created=<date>,
      |        jcr:createdBy=<username>)
      + jcr:content  (jcr:primaryType=nt:resource,
                      jcr:lastModified=<date>,
                      jcr:lastModifiedBy=<username>,
                      jcr:mimeType=<mimeType>,
                      jcr:encoding=<null>,
                      jcr:data=<binary-content>)
   */
    //TODO: Mapping is conditional
    //This is true for nt:file and nt:folder
    private final static String[] mapAll = new String[]{
            "cmis:objectId = jcr:uuid",
            "cmis:createdBy = jcr:createdBy",
            "cmis:creationDate = jcr:created",

    };
    //This is true for nt:file
    private final static String[] mapContent = new String[]{
            "cmis:lastModificationDate = jcr:content/@jcr:lastModified",
            "cmis:lastModifiedBy = jcr:content/@jcr:lastModifiedBy",
            "cmis:contentStreamMimeType = jcr:content/@jcr:mimeType",
            //This is nice to have
            "cmis:contentStreamFileName = fn:name()",
    };
    //This is true for nt:folder
    private final static String[] mapFolder = new String[]{
            "cmis:lastModificationDate = jcr:lastModified",
            "cmis:lastModifiedBy = jcr:lastModifiedBy",

    };
    Map<String, Relation> cmisToJcr = new HashMap<String, Relation>();
    Map<String, Relation> jcrToCmis = new HashMap<String, Relation>();
    //this is value factory used for converation of the values
    private ValueFactories valueFactories;

    //list of relations between names
    // private ArrayList<Relation> list = new ArrayList();

    /**
     * Constructs this class instance.
     *
     * @param valueFactories jcr value factory
     */
    public Properties(ValueFactories valueFactories) {
        this.valueFactories = valueFactories;
        //parse strings and create relations
        for (String aMapAll : mapAll) {
            String[] tokens = aMapAll.split("=");
            Relation rel = new Relation(tokens[0].trim(), tokens[1].trim());
            cmisToJcr.put(rel.cmisName, rel);
            jcrToCmis.put(rel.jcrName, rel);
        }
    }

    /**
     * Determines the name of the given property from cmis domain in jcr domain.
     *
     * @param cmisName the name of property in cmis domain.
     * @return the name of the same property in jcr domain.
     */
    public String findJcrName(String cmisName) {
        if (cmisToJcr.containsKey(cmisName)) {
            return cmisToJcr.get(cmisName).jcrName;
        }
        return cmisName;
    }

    /**
     * Determines the name of the given property from jcr domain in cmis domain.
     *
     * @param jcrName the name of property in jcr domain.
     * @return the name of the same property in cmis domain.
     */
    public String findCmisName(String jcrName) {
        if (jcrToCmis.containsKey(jcrName)) {
            return jcrToCmis.get(jcrName).cmisName;
        }
        return jcrName;
    }

    /**
     * Converts type of property.
     *
     * @param propertyType the type of the property in cmis domain.
     * @return the type of the property in jcr domain.
     */
    public int getJcrType(PropertyType propertyType) {
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
     * @param pdef  property definition as declared in cmis repository.
     * @param field the representation of the value of the property in jcr domain.
     * @return value as declared by property definition.
     */
    public Object cmisValue(PropertyDefinition pdef, Field field) {
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
                //FIXME
                return new GregorianCalendar();
            case URI:
                try {
                    return new URI(field.getValueAsString());
                } catch (Exception e) {
                    e.printStackTrace();
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
     * @param pdef     property definition as declared in cmis repository.
     * @param jcrName  the name of the property in jcr domain
     * @param document connectors's view of properties in jcr domain.
     * @return value as declared by property definition.
     */
    public Object cmisValue(PropertyDefinition pdef, String jcrName, Document document) {
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
                //FIXME
                return new GregorianCalendar();
            case URI:
                try {
                    return new URI(document.getString(jcrName));
                } catch (Exception e) {
                    e.printStackTrace();
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
    public Object[] jcrValues(Property property) {
        List values = property.getValues();

        //convert CMIS values to JCR values
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
    private Boolean[] asBooleans(List<Object> values) {
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
    private String[] asStrings(List<Object> values) {
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
    private Long[] asIntegers(List<Object> values) {
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
    private BigDecimal[] asDecimals(List<Object> values) {
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
    private DateTime[] asDateTime(List<Object> values) {
        ValueFactory<DateTime> factory = valueFactories.getDateFactory();
        DateTime[] res = new DateTime[values.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = factory.create(((GregorianCalendar) values.get(i)).getTime());
        }
        return res;
    }

    /**
     * Converts CMIS value of URI type into JCR value of URI type.
     *
     * @param values CMIS values of URI type
     * @return JCR values of URI type
     */
    private URI[] asURI(List<Object> values) {
        ValueFactory<URI> factory = valueFactories.getUriFactory();
        URI[] res = new URI[values.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = factory.create(((GregorianCalendar) values.get(i)).getTime());
        }
        return res;
    }

    /**
     * Converts CMIS value of ID type into JCR value of String type.
     *
     * @param values CMIS values of Id type
     * @return JCR values of String type
     */
    private String[] asIDs(List<Object> values) {
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
    private String[] asHTMLs(List<Object> values) {
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

        private Relation(String cmisName, String jcrName) {
            this.cmisName = cmisName;
            this.jcrName = jcrName;
        }
    }
}
