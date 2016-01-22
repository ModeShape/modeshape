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
package org.modeshape.schematic.internal.schema;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.schematic.internal.schema.DocumentTransformer.SystemPropertyAccessor;

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
        String pathEnvironmentValue = System.getenv("PATH");
        if (pathEnvironmentValue != null) {
            //test variable substitution for environment values
            assertSystemPropertySubstituted("${PATH}", pathEnvironmentValue);
        }
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
