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
package org.jboss.dna.common.jdbc.model.api;

/**
 * Provides RDBMS supported standatd SQL types info.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public interface SqlTypeInfo extends DatabaseNamedObject {

    /**
     * Returns localized version of type name (may be null). Type name is returned by DatabaseNamedObject.getName () method.
     * 
     * @return localized version of type name (may be null)
     */
    String getLocalizedTypeName();

    /**
     * Sets localized version of type name (may be null). Type name is returned by DatabaseNamedObject.getName () method.
     * 
     * @param localizedTypeName localized version of type name (may be null)
     */
    void setLocalizedTypeName( String localizedTypeName );

    /**
     * Gets SQL type nullability
     * 
     * @return SQL type nullability
     */
    NullabilityType getNullabilityType();

    /**
     * Sets SQL type nullability
     * 
     * @param nullabilityType SQL type nullability
     */
    void setNullabilityType( NullabilityType nullabilityType );

    /**
     * Gets SQL type from java.sql.Types
     * 
     * @return SQL type from java.sql.Types
     */
    SqlType getSqlType();

    /**
     * Sets SQL type from java.sql.Types
     * 
     * @param sqlType the SQL type from java.sql.Types
     */
    void setSqlType( SqlType sqlType );

    /**
     * Gets precision (number of fractional digits/scale) if applicable otherwise 0.
     * 
     * @return precision if applicable otherwise 0
     */
    Long getPrecision();

    /**
     * Sets precision (number of fractional digits/scale) if applicable otherwise 0.
     * 
     * @param precision precision if applicable otherwise 0
     */
    void setPrecision( Long precision );

    /**
     * Returns true if sql type can be a money value, for instance
     * 
     * @return true if sql type can be a money value, for instance
     */
    Boolean isFixedPrecisionScale();

    /**
     * Sets true if sql type can be a money value, for instance
     * 
     * @param fixedPrecisionScale true if sql type can be a money value, for instance
     */
    void setFixedPrecisionScale( Boolean fixedPrecisionScale );

    /**
     * Returns sql type precision radix (usually 2 or 10)
     * 
     * @return sql type precision radix (usually 2 or 10)
     */
    Integer getNumberPrecisionRadix();

    /**
     * sets sql type precision radix (usually 2 or 10)
     * 
     * @param numberPrecisionRadix the sql type precision radix (usually 2 or 10)
     */
    void setNumberPrecisionRadix( Integer numberPrecisionRadix );

    /**
     * Returns minimum scale supported
     * 
     * @return minimum scale supported
     */
    Integer getMinScale();

    /**
     * Sets minimum scale supported
     * 
     * @param minScale minimum scale supported
     */
    void setMinScale( Integer minScale );

    /**
     * Returns maximum scale supported
     * 
     * @return maximum scale supported
     */
    Integer getMaxScale();

    /**
     * Sets maximum scale supported
     * 
     * @param maxScale the maximum scale supported
     */
    void setMaxScale( Integer maxScale );

    /**
     * Returns prefix used to quote a literal (may be null)
     * 
     * @return prefix used to quote a literal (may be null)
     */
    String getLiteralPrefix();

    /**
     * Sets prefix used to quote a literal (may be null)
     * 
     * @param literalPrefix the prefix used to quote a literal (may be null)
     */
    void setLiteralPrefix( String literalPrefix );

    /**
     * Returns suffix used to quote a literal (may be null)
     * 
     * @return suffix used to quote a literal (may be null)
     */
    String getLiteralSuffix();

    /**
     * Sets suffix used to quote a literal (may be null)
     * 
     * @param literalSuffix the suffix used to quote a literal (may be null)
     */
    void setLiteralSuffix( String literalSuffix );

    /**
     * Returns parameters used in creating the type (may be null)
     * 
     * @return parameters used in creating the type (may be null)
     */
    String getCreateParams();

    /**
     * Sets parameters used in creating the type (may be null)
     * 
     * @param createParams the parameters used in creating the type (may be null)
     */
    void setCreateParams( String createParams );

    /**
     * Is sql type case sensitive
     * 
     * @return true if sql type case sensitive
     */
    Boolean isCaseSensitive();

    /**
     * Is sql type case sensitive
     * 
     * @param caseSensitive the true if sql type case sensitive
     */
    void setCaseSensitive( Boolean caseSensitive );

    /**
     * Returns sql type searchability
     * 
     * @return sql type searchability
     */
    SearchabilityType getSearchabilityType();

    /**
     * Sets sql type searchability
     * 
     * @param searchabilityType the sql type searchability
     */
    void setSearchabilityType( SearchabilityType searchabilityType );

    /**
     * Returns true if sql type is unsigned
     * 
     * @return true if sql type is unsigned
     */
    Boolean isUnsigned();

    /**
     * Sets true if sql type is unsigned
     * 
     * @param unsigned true if sql type is unsigned
     */
    void setUnsigned( Boolean unsigned );

    /**
     * Returns true if sql type can be used for an auto-increment value.
     * 
     * @return true if sql type can be used for an auto-increment value.
     */
    Boolean isAutoIncrement();

    /**
     * Sets true if sql type can be used for an auto-increment value.
     * 
     * @param autoIncrement true if sql type can be used for an auto-increment value.
     */
    void setAutoIncrement( Boolean autoIncrement );
}
