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

import junit.framework.TestCase;
import org.jboss.dna.common.jdbc.model.api.NullabilityType;
import org.jboss.dna.common.jdbc.model.api.SearchabilityType;
import org.jboss.dna.common.jdbc.model.api.SqlType;
import org.jboss.dna.common.jdbc.model.api.SqlTypeInfo;

/**
 * SqlTypeInfoBean test
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class SqlTypeInfoBeanTest extends TestCase {

    private SqlTypeInfo bean;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // create
        bean = new SqlTypeInfoBean();

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

    public void testSetLocalizedTypeName() {
        String localizedTypeName = "Localized";
        // set
        bean.setLocalizedTypeName(localizedTypeName);
        // check
        assertEquals("Unable to set localized Type Name", localizedTypeName, bean.getLocalizedTypeName());
    }

    public void testSetNullabilityType() {
        // set
        bean.setNullabilityType(NullabilityType.NULLABLE);
        // check
        assertSame("Unable to set nullability type", NullabilityType.NULLABLE, bean.getNullabilityType());
    }

    public void testSetSqlType() {
        // set
        bean.setSqlType(SqlType.VARCHAR);
        // check
        assertSame("Unable to set SQL type", SqlType.VARCHAR, bean.getSqlType());
    }

    public void testSetPrecision() {
        Long precision = new Long(5);
        // set
        bean.setPrecision(precision);
        // check
        assertSame("Unable to set precision", precision, bean.getPrecision());
    }

    public void testSetFixedPrecisionScale() {
        Boolean fixedPrecisionScale = Boolean.TRUE;
        // set
        bean.setFixedPrecisionScale(fixedPrecisionScale);
        // check
        assertSame("Unable to set fixed precision scale", fixedPrecisionScale, bean.isFixedPrecisionScale());
    }

    public void testSetNumberPrecisionRadix() {
        Integer numberPrecisionRadix = new Integer(10);
        // set
        bean.setNumberPrecisionRadix(numberPrecisionRadix);
        // check
        assertSame("Unable to set number precision radix", numberPrecisionRadix, bean.getNumberPrecisionRadix());
    }

    public void testSetMinScale() {
        Integer minScale = new Integer(1);
        // set
        bean.setMinScale(minScale);
        // check
        assertSame("Unable to set min scale", minScale, bean.getMinScale());
    }

    public void testSetMaxScale() {
        Integer maxScale = new Integer(10);
        // set
        bean.setMaxScale(maxScale);
        // check
        assertSame("Unable to set max scale", maxScale, bean.getMaxScale());
    }

    public void testSetLiteralPrefix() {
        String literalPrefix = "Prefix";
        // set
        bean.setLiteralPrefix(literalPrefix);
        // check
        assertEquals("Unable to set literal prefix", literalPrefix, bean.getLiteralPrefix());
    }

    public void testSetLiteralSuffix() {
        String literalSuffix = "Suffix";
        // set
        bean.setLiteralSuffix(literalSuffix);
        // check
        assertEquals("Unable to set literal suffix", literalSuffix, bean.getLiteralSuffix());
    }

    public void testSetCreateParams() {
        String createParams = "My params";
        // set
        bean.setCreateParams(createParams);
        // check
        assertEquals("Unable to set create params", createParams, bean.getCreateParams());
    }

    public void testSetCaseSensitive() {
        Boolean caseSensitive = Boolean.TRUE;
        // set
        bean.setCaseSensitive(caseSensitive);
        // check
        assertSame("Unable to set case sensitive", caseSensitive, bean.isCaseSensitive());
    }

    public void testSetSearchabilityType() {
        // set
        bean.setSearchabilityType(SearchabilityType.BASIC);
        // check
        assertSame("Unable to set searchability type", SearchabilityType.BASIC, bean.getSearchabilityType());
    }

    public void testSetUnsigned() {
        Boolean unsigned = Boolean.TRUE;
        // set
        bean.setUnsigned(unsigned);
        // check
        assertSame("Unable to set unsigned", unsigned, bean.isUnsigned());
    }

    public void testSetAutoIncrement() {
        Boolean autoIncrement = Boolean.TRUE;
        // set
        bean.setAutoIncrement(autoIncrement);
        // check
        assertSame("Unable to set auto increment", autoIncrement, bean.isAutoIncrement());
    }

}
