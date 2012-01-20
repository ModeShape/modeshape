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
package org.infinispan.schematic.internal.schema;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.infinispan.schematic.internal.schema.DocumentTransformer.SystemPropertyAccessor;
import org.junit.Before;
import org.junit.Test;

/**
 * Test property variable syntax options
 * <ul>
 * <li>${variable}</li>
 * <li>${variable [,variable,..] }</li>
 * <li>${variable [:defaultvalue] }</li>
 * </ul>
 * 
 * @author Van Halbert
 */
public class SystemPropertyFactoryTest {

    private static final String TESTPROP = "test.prop";
    private static final String TESTPROPVALUE = "test.prop.value";

    private static final String TESTPROP2 = "test.prop2";
    private static final String TESTPROPVALUE2 = "test.prop.value2";

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        System.setProperty(TESTPROP, TESTPROPVALUE);
        System.setProperty(TESTPROP2, TESTPROPVALUE2);
    }

    protected void assertSystemPropertySubstituted( String input,
                                                    String expected ) {
        assertThat(DocumentTransformer.getSubstitutedProperty(input, SystemPropertyAccessor.INSTANCE), is(expected));
    }

    @Test
    public void shouldSubstituteSingleVariable() {

        assertSystemPropertySubstituted("${" + TESTPROP + "}", TESTPROPVALUE);
        assertSystemPropertySubstituted("find.the.property." + "${" + TESTPROP + "}", "find.the.property." + TESTPROPVALUE);
        assertSystemPropertySubstituted("${" + TESTPROP + "}.find.the.property", TESTPROPVALUE + ".find.the.property");
        assertSystemPropertySubstituted("find.the.property.${" + TESTPROP + "}.find.the.property", "find.the.property."
                                                                                                   + TESTPROPVALUE
                                                                                                   + ".find.the.property");
    }

    @Test
    public void shouldSubstituteFirstFoundMultipleVariables() {

        assertSystemPropertySubstituted("${any.prop," + TESTPROP + "}", TESTPROPVALUE);
        assertSystemPropertySubstituted("find.the.property.${any.prop," + TESTPROP + "}", "find.the.property." + TESTPROPVALUE);
        assertSystemPropertySubstituted("${any.prop," + TESTPROP + "}.find.the.property", TESTPROPVALUE + ".find.the.property");
        assertSystemPropertySubstituted("find.the.property.${any.prop," + TESTPROP + "}.find.the.property",
                                        "find.the.property." + TESTPROPVALUE + ".find.the.property");

        // this test verifies that the first property is used even though there are 2 properties that can be found
        assertSystemPropertySubstituted("${any.prop," + TESTPROP + "," + TESTPROP2 + "}.find.the.property",
                                        TESTPROPVALUE + ".find.the.property");

    }

    @Test
    public void shouldSubstituteMultipleVariablesWithDefault() {

        assertSystemPropertySubstituted("${any.prop1,any.prop2:" + TESTPROPVALUE + "}", TESTPROPVALUE);
        assertSystemPropertySubstituted("find.the.property.${any.prop1:" + TESTPROPVALUE + "}", "find.the.property."
                                                                                                + TESTPROPVALUE);
        assertSystemPropertySubstituted("${any.prop1,any.prop2:" + TESTPROPVALUE + "}.find.the.property", TESTPROPVALUE
                                                                                                          + ".find.the.property");
        assertSystemPropertySubstituted("find.the.property.${any.prop:" + TESTPROPVALUE + "}.find.the.property",
                                        "find.the.property." + TESTPROPVALUE + ".find.the.property");
    }

    @Test
    public void shouldSubstituteMultipleVariableGroups() {

        assertSystemPropertySubstituted("${" + TESTPROP + "}.double.${" + TESTPROP + "}", TESTPROPVALUE + ".double."
                                                                                          + TESTPROPVALUE);
        assertSystemPropertySubstituted("${any.prop," + TESTPROP + "}.double.${any.prop," + TESTPROP + "}", TESTPROPVALUE
                                                                                                            + ".double."
                                                                                                            + TESTPROPVALUE);
        assertSystemPropertySubstituted("${any.prop:" + TESTPROPVALUE + "}.double.${any.prop:" + TESTPROPVALUE + "}",
                                        TESTPROPVALUE + ".double." + TESTPROPVALUE);

    }

}
