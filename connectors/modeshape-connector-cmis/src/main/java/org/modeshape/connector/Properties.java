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

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import org.apache.chemistry.opencmis.client.api.Property;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;

/**
 * Implements mapping between several CMIS and JCR names of properties.
 *
 * @author kulikov
 */
public class Properties {
    private final static String[] map = new String[] {
        "cmis:objectId = jcr:uuid",
        "cmis:createdBy = jcr:createdBy",
        "cmis:creationDate = jcr:created",
        "cmis:lastModificationDate = jcr:lastModified",
        "cmis:lastModifiedBy = jcr:lastModifiedBy"
    };

    private ValueFactories valueFactories;
    private ArrayList<Relation> list = new ArrayList();

    public Properties(ValueFactories valueFactories) {
        this.valueFactories = valueFactories;
        for (int i = 0; i < map.length; i++) {
           String[] tokens = map[i].split("=");
           list.add(new Relation(tokens[0].trim(), tokens[1].trim()));
        }
    }

    public String findJcrName(String cmisName) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).cmisName.equals(cmisName)) {
                return list.get(i).jcrName;
            }
        }
        return cmisName;
    }

    public String findCmisName(String jcrName) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).jcrName.equals(jcrName)) {
                return list.get(i).cmisName;
            }
        }
        return jcrName;
    }

    public Object cmisValue(Property property, String jcrName, Document document) {
        if (property.isMultiValued()) {

        }

        switch (property.getType()) {
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
                }
                break;
            case ID:
                return document.getString(jcrName);
            case HTML:
                return document.getString(jcrName);
        }

        return null;
    }

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
            default :
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
    private URI[] asURI(List<Object> values) {
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
