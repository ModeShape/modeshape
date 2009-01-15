/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.common.jdbc.model.spi;

import java.util.Set;
import junit.framework.TestCase;
import org.jboss.dna.common.jdbc.model.DefaultModelFactory;
import org.jboss.dna.common.jdbc.model.api.Parameter;
import org.jboss.dna.common.jdbc.model.api.StoredProcedure;
import org.jboss.dna.common.jdbc.model.api.StoredProcedureResultType;

/**
 * StoredProcedureBean test
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class StoredProcedureBeanTest extends TestCase {

    private StoredProcedure bean;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // create
        bean = new StoredProcedureBean();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        // release
        bean = null;

        super.tearDown();
    }

    public void testSetResultType() {
        // set
        bean.setResultType(StoredProcedureResultType.RETURNS_RESULT);
        // check
        assertSame("Unable to set result type", StoredProcedureResultType.RETURNS_RESULT, bean.getResultType());
    }

    public void testGetParameters() {
        Set<Parameter> parameters = bean.getParameters();
        // check
        assertNotNull("Unable to get parameters", parameters);
        assertTrue("Parameter set should be empty by default", parameters.isEmpty());
    }

    public void testAddParameter() {
        String PARAMETER_NAME = "My parameter";
        // create parameter
        Parameter parameter = new DefaultModelFactory().createParameter();
        // set name
        parameter.setName(PARAMETER_NAME);
        // add
        bean.addParameter(parameter);
        // check
        assertFalse("Parameter set should not be empty", bean.getParameters().isEmpty());
    }

    public void testDeleteParameter() {
        String PARAMETER_NAME = "My parameter";
        // create parameter
        Parameter parameter = new DefaultModelFactory().createParameter();
        // set name
        parameter.setName(PARAMETER_NAME);
        // add
        bean.addParameter(parameter);
        // check
        assertFalse("Parameter set should not be empty", bean.getParameters().isEmpty());

        // delete
        bean.deleteParameter(parameter);
        // check
        assertTrue("Parameter set should be empty", bean.getParameters().isEmpty());
    }

    public void testFindParameterByName() {
        String PARAMETER_NAME = "My parameter";
        // create parameter
        Parameter parameter = new DefaultModelFactory().createParameter();
        // set name
        parameter.setName(PARAMETER_NAME);
        // add
        bean.addParameter(parameter);
        // check
        assertSame("Unable to find parameter", parameter, bean.findParameterByName(PARAMETER_NAME));
    }

}
