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
package org.modeshape.jcr.value.basic;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.TransientBinaryStore;

/**
 * @author Van Halbert Test property variable syntax options <li>${variable}</li> <li>${variable [,variable,..] }</li> <li>
 *         ${variable [:defaultvalue] }</li>
 */
public class SystemPropertyFactoryTest {

    private static final String TESTPROP = "test.prop";
    private static final String TESTPROPVALUE = "test.prop.value";

    private static final String TESTPROP2 = "test.prop2";
    private static final String TESTPROPVALUE2 = "test.prop.value2";

    private NamespaceRegistry registry;
    private SystemPropertyFactory systemPropertyFactory;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        registry = new SimpleNamespaceRegistry();
        BinaryStore binaryStore = TransientBinaryStore.get();
        systemPropertyFactory = new SystemPropertyFactory(new StandardValueFactories(registry, binaryStore));

        System.setProperty(TESTPROP, TESTPROPVALUE);
        System.setProperty(TESTPROP2, TESTPROPVALUE2);

    }

    @Test
    public void shouldSubstituteSingleVariable() {

        assertThat(systemPropertyFactory.getSubstitutedProperty("${" + TESTPROP + "}"), is(TESTPROPVALUE));
        assertThat(systemPropertyFactory.getSubstitutedProperty("find.the.property." + "${" + TESTPROP + "}"),
                   is("find.the.property." + TESTPROPVALUE));
        assertThat(systemPropertyFactory.getSubstitutedProperty("${" + TESTPROP + "}.find.the.property"),
                   is(TESTPROPVALUE + ".find.the.property"));
        assertThat(systemPropertyFactory.getSubstitutedProperty("find.the.property.${" + TESTPROP + "}.find.the.property"),
                   is("find.the.property." + TESTPROPVALUE + ".find.the.property"));
    }

    @Test
    public void shouldSubstituteFirstFoundMultipleVariables() {

        assertThat(systemPropertyFactory.getSubstitutedProperty("${any.prop," + TESTPROP + "}"), is(TESTPROPVALUE));
        assertThat(systemPropertyFactory.getSubstitutedProperty("find.the.property.${any.prop," + TESTPROP + "}"),
                   is("find.the.property." + TESTPROPVALUE));
        assertThat(systemPropertyFactory.getSubstitutedProperty("${any.prop," + TESTPROP + "}.find.the.property"),
                   is(TESTPROPVALUE + ".find.the.property"));
        assertThat(systemPropertyFactory.getSubstitutedProperty("find.the.property.${any.prop," + TESTPROP
                                                                + "}.find.the.property"),
                   is("find.the.property." + TESTPROPVALUE + ".find.the.property"));

        // this test verifies that the first property is used even though there are 2 properties that can be found
        assertThat(systemPropertyFactory.getSubstitutedProperty("${any.prop," + TESTPROP + "," + TESTPROP2
                                                                + "}.find.the.property"),
                   is(TESTPROPVALUE + ".find.the.property"));

    }

    @Test
    public void shouldSubstituteMultipleVariablesWithDefault() {

        assertThat(systemPropertyFactory.getSubstitutedProperty("${any.prop1,any.prop2:" + TESTPROPVALUE + "}"),
                   is(TESTPROPVALUE));
        assertThat(systemPropertyFactory.getSubstitutedProperty("find.the.property.${any.prop1:" + TESTPROPVALUE + "}"),
                   is("find.the.property." + TESTPROPVALUE));
        assertThat(systemPropertyFactory.getSubstitutedProperty("${any.prop1,any.prop2:" + TESTPROPVALUE + "}.find.the.property"),
                   is(TESTPROPVALUE + ".find.the.property"));
        assertThat(systemPropertyFactory.getSubstitutedProperty("find.the.property.${any.prop:" + TESTPROPVALUE
                                                                + "}.find.the.property"),
                   is("find.the.property." + TESTPROPVALUE + ".find.the.property"));
    }

    @Test
    public void shouldSubstituteMultipleVariableGroups() {

        assertThat(systemPropertyFactory.getSubstitutedProperty("${" + TESTPROP + "}.double.${" + TESTPROP + "}"),
                   is(TESTPROPVALUE + ".double." + TESTPROPVALUE));
        assertThat(systemPropertyFactory.getSubstitutedProperty("${any.prop," + TESTPROP + "}.double.${any.prop," + TESTPROP
                                                                + "}"),
                   is(TESTPROPVALUE + ".double." + TESTPROPVALUE));
        assertThat(systemPropertyFactory.getSubstitutedProperty("${any.prop:" + TESTPROPVALUE + "}.double.${any.prop:"
                                                                + TESTPROPVALUE + "}"),
                   is(TESTPROPVALUE + ".double." + TESTPROPVALUE));

    }

}
