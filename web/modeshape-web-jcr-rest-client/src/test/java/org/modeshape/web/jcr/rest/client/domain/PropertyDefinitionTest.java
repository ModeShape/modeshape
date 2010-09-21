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
package org.modeshape.web.jcr.rest.client.domain;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.version.OnParentVersionAction;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.IoUtil;

public class PropertyDefinitionTest {

    private String declaringNodeTypeName;
    private String name;
    private int requiredType;
    private boolean isAutoCreated;
    private boolean isMandatory;
    private boolean isProtected;
    private boolean isFullTextSearchable;
    private boolean isMultiple;
    private boolean isQueryOrderable;
    private int onParentVersion;
    private List<String> defaultValues;
    private List<String> valueConstraints;
    private List<String> availableQueryOperations;
    private Map<String, NodeType> nodeTypes;
    private PropertyDefinition defn;
    private PropertyDefinition defn2;

    // private PropertyDefinition defn;

    @Before
    public void beforeEach() {
        declaringNodeTypeName = "my:nodeType";
        nodeTypes = new HashMap<String, NodeType>();
        name = "my:prop";
        requiredType = PropertyType.DOUBLE;
        isAutoCreated = true;
        isMandatory = true;
        isProtected = true;
        isFullTextSearchable = true;
        isMultiple = true;
        isQueryOrderable = true;
        onParentVersion = OnParentVersionAction.COPY;
        defaultValues = new ArrayList<String>();
        valueConstraints = new ArrayList<String>();
        availableQueryOperations = new ArrayList<String>();
    }

    @Test
    public void shouldCreateInstance() {
        createPropertyDefinition();
    }

    @Test
    public void shouldCreateWithNullDefaultValueList() {
        defaultValues = null;
        createPropertyDefinition();
    }

    @Test
    public void shouldCreateWithEmptyDefaultValueList() {
        defaultValues.clear();
        createPropertyDefinition();
    }

    @Test
    public void shouldCreateWithOneDefaultValue() {
        defaultValues.add("3");
        createPropertyDefinition();
    }

    @Test
    public void shouldCreateWithTwoDefaultValues() {
        defaultValues.add("3");
        defaultValues.add("5");
        createPropertyDefinition();
    }

    @Test
    public void shouldCreateWithThreeDefaultValues() {
        defaultValues.add("3");
        defaultValues.add("5");
        defaultValues.add("8");
        createPropertyDefinition();
    }

    @Test
    public void shouldReturnNullDeclaringNodeTypeIfNotFoundInMap() {
        nodeTypes.clear();
        defn = createPropertyDefinition();
        assertThat(defn.getDeclaringNodeType(), is(nullValue()));
    }

    @Test
    public void shouldReturnNullDeclaringNodeTypeIfNoMapIsFound() {
        nodeTypes = null;
        defn = createPropertyDefinition();
        assertThat(defn.getDeclaringNodeType(), is(nullValue()));
    }

    @Test
    public void shouldConsiderEqualIfSameNameAndRequiredTypeAndMultiplicity() {
        defn = createPropertyDefinition();
        defn2 = createPropertyDefinition();
        assertThat(defn.equals(defn2), is(true));
    }

    @Test
    public void shouldConsiderNotEqualIfDifferentName() {
        defn = createPropertyDefinition();
        name = "other:name";
        defn2 = createPropertyDefinition();
        assertThat(defn.equals(defn2), is(false));
    }

    @Test
    public void shouldConsiderNotEqualIfDifferentRequiredType() {
        defn = createPropertyDefinition();
        requiredType = requiredType == PropertyType.PATH ? PropertyType.BOOLEAN : PropertyType.PATH;
        defn2 = createPropertyDefinition();
        assertThat(defn.equals(defn2), is(false));
    }

    @Test
    public void shouldConsiderNotEqualIfDifferentMultiplicity() {
        defn = createPropertyDefinition();
        isMultiple = !isMultiple;
        defn2 = createPropertyDefinition();
        assertThat(defn.equals(defn2), is(false));
    }

    @Test
    public void shouldAllowConversionOfDefaultValueFromDateToCompatibleTypes() throws Exception {
        defaultValues.add("2010-03-22T01:02:03.456Z");
        requiredType = PropertyType.DATE;
        defn = createPropertyDefinition();
        assertThat(defn.getDefaultValues()[0].getString(), is("2010-03-22T01:02:03.456Z"));
        assertThat(defn.getDefaultValues()[0].getDate(), is(dateFrom("2010-03-22T01:02:03.456Z")));
        assertThat(defn.getDefaultValues()[0].getLong(), is(1269219723456L));
    }

    @Test
    public void shouldAllowConversionOfDefaultValueFromDateBeforeUtcToCompatibleTypes() throws Exception {
        defaultValues.add("2010-03-22T01:02:03.456+08:00"); // 8 hours ahead of UTC
        requiredType = PropertyType.DATE;
        defn = createPropertyDefinition();
        assertThat(defn.getDefaultValues()[0].getString(), is("2010-03-22T01:02:03.456+08:00"));
        assertThat(defn.getDefaultValues()[0].getDate(), is(dateFrom("2010-03-22T01:02:03.456+08:00")));
        assertThat(defn.getDefaultValues()[0].getLong(), is(1269190923456L));
        // Verify that the supplied time in millis is 8 hours ahead of the same time in UTC millis ...
        assertThat(TimeUnit.HOURS.convert(1269219723456L - 1269190923456L, TimeUnit.MILLISECONDS), is(8L));
    }

    @Test
    public void shouldAllowConversionOfDefaultValueFromDateAfterUtcToCompatibleTypes() throws Exception {
        defaultValues.add("2010-03-22T01:02:03.456-08:00"); // 8 hours after of UTC
        requiredType = PropertyType.DATE;
        defn = createPropertyDefinition();
        assertThat(defn.getDefaultValues()[0].getString(), is("2010-03-22T01:02:03.456-08:00"));
        assertThat(defn.getDefaultValues()[0].getDate(), is(dateFrom("2010-03-22T01:02:03.456-08:00")));
        assertThat(defn.getDefaultValues()[0].getLong(), is(1269248523456L));
        // Verify that the supplied time in millis is 8 hours behind the same time in UTC millis ...
        assertThat(TimeUnit.HOURS.convert(1269219723456L - 1269248523456L, TimeUnit.MILLISECONDS), is(-8L));
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldAllowConversionOfDefaultValueFromDoubleToCompatibleTypes() throws Exception {
        defaultValues.add("8");
        requiredType = PropertyType.DOUBLE;
        defn = createPropertyDefinition();
        assertThat(defn.getDefaultValues()[0].getString(), is("8"));
        assertThat(defn.getDefaultValues()[0].getDouble(), is(8.0));
        assertThat(defn.getDefaultValues()[0].getLong(), is(8L));
        assertThat(defn.getDefaultValues()[0].getDecimal(), is(new BigDecimal(8)));
        assertThat(toString(defn.getDefaultValues()[0].getBinary()), is("8"));
        assertThat(toString(defn.getDefaultValues()[0].getStream()), is("8"));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldFailToConvertDefaultValueFromDoubleToBoolean() throws Exception {
        defaultValues.add("8");
        requiredType = PropertyType.DOUBLE;
        defn = createPropertyDefinition();
        defn.getDefaultValues()[0].getBoolean();
    }

    @Test
    public void shouldConvertDefaultValueFromDoubleToDate() throws Exception {
        defaultValues.add("8");
        requiredType = PropertyType.DOUBLE;
        defn = createPropertyDefinition();
        defn.getDefaultValues()[0].getDate();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldConvertDefaultValueFromNameToDate() throws Exception {
        defaultValues.add("jcr:name");
        requiredType = PropertyType.NAME;
        defn = createPropertyDefinition();
        defn.getDefaultValues()[0].getDate();
    }

    @Test
    public void shouldParseDate() {
        PropertyDefinition.parseDate("2010-03-22T01:02:03.456-0800");
        PropertyDefinition.parseDate("2010-03-22T01:02:03.456-08:00");
        PropertyDefinition.parseDate("2010-03-22T01:02:03.456+0800");
        PropertyDefinition.parseDate("2010-03-22T01:02:03.456+08:00");
        PropertyDefinition.parseDate("2010-03-22T01:02:03.456Z");
    }

    protected Calendar dateFrom( String dateStr ) {
        return PropertyDefinition.parseDate(dateStr);
    }

    protected String toString( Binary binary ) throws IOException, RepositoryException {
        return toString(binary.getStream());
    }

    protected String toString( InputStream stream ) throws IOException {
        return IoUtil.read(stream);
    }

    protected PropertyDefinition createPropertyDefinition() {
        PropertyDefinition defn = new PropertyDefinition(declaringNodeTypeName, name, requiredType, isAutoCreated, isMandatory,
                                                         isProtected, isFullTextSearchable, isMultiple, isQueryOrderable,
                                                         onParentVersion, defaultValues, valueConstraints,
                                                         availableQueryOperations, nodeTypes);
        assertValidDefinition(defn);
        return defn;
    }

    protected void assertValidDefinition( PropertyDefinition defn ) {
        if (defaultValues == null) defaultValues = new ArrayList<String>();
        if (valueConstraints == null) valueConstraints = new ArrayList<String>();
        if (availableQueryOperations == null) availableQueryOperations = new ArrayList<String>();
        assertThat(defn.isAutoCreated(), is(isAutoCreated));
        assertThat(defn.isFullTextSearchable(), is(isFullTextSearchable));
        assertThat(defn.isMandatory(), is(isMandatory));
        assertThat(defn.isMultiple(), is(isMultiple));
        assertThat(defn.isProtected(), is(isProtected));
        assertThat(defn.isQueryOrderable(), is(isQueryOrderable));
        assertThat(defn.getName(), is(name));
        assertThat(defn.getDeclaringNodeTypeName(), is(declaringNodeTypeName));
        assertThat(defn.getAvailableQueryOperators(), is(arrayOf(availableQueryOperations)));
        assertThat(defn.getValueConstraints(), is(arrayOf(valueConstraints)));
        assertThat(arrayOf(defn.getDefaultValues()), is(arrayOf(defaultValues)));
        assertThat(defn.isAutoCreated(), is(isAutoCreated));
    }

    protected String[] arrayOf( Collection<String> values ) {
        return values.toArray(new String[values.size()]);
    }

    protected String[] arrayOf( Value[] values ) {
        String[] result = new String[values.length];
        try {
            for (int i = 0; i != values.length; ++i) {
                result[i] = values[i].getString();
            }
        } catch (RepositoryException e) {
            fail(e.getMessage());
        }
        return result;
    }
}
