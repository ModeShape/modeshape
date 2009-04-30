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

import org.jboss.dna.common.jdbc.model.api.NullabilityType;
import org.jboss.dna.common.jdbc.model.api.SearchabilityType;
import org.jboss.dna.common.jdbc.model.api.SqlType;
import org.jboss.dna.common.jdbc.model.api.SqlTypeInfo;

/**
 * Provides RDBMS supported standatd SQL types info.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class SqlTypeInfoBean extends DatabaseNamedObjectBean implements SqlTypeInfo {
    private static final long serialVersionUID = -3336885010975318256L;
    private String localizedTypeName;
    private NullabilityType nullabilityType;
    private SqlType sqlType;
    private Long precision;
    private Boolean fixedPrecisionScale;
    private Integer numberPrecisionRadix;
    private Integer minScale;
    private Integer maxScale;
    private String literalPrefix;
    private String literalSuffix;
    private String createParams;
    private Boolean caseSensitive;
    private SearchabilityType searchabilityType;
    private Boolean unsigned;
    private Boolean autoIncrement;

    /**
     * Default constructor
     */
    public SqlTypeInfoBean() {
    }

    /**
     * Returns localized version of type name (may be null). Type name is returned by DatabaseNamedObject.getName () method.
     * 
     * @return localized version of type name (may be null)
     */
    public String getLocalizedTypeName() {
        return localizedTypeName;
    }

    /**
     * Sets localized version of type name (may be null). Type name is returned by DatabaseNamedObject.getName () method.
     * 
     * @param localizedTypeName localized version of type name (may be null)
     */
    public void setLocalizedTypeName( String localizedTypeName ) {
        this.localizedTypeName = localizedTypeName;
    }

    /**
     * Gets SQL type nullability
     * 
     * @return SQL type nullability
     */
    public NullabilityType getNullabilityType() {
        return nullabilityType;
    }

    /**
     * Sets SQL type nullability
     * 
     * @param nullabilityType SQL type nullability
     */
    public void setNullabilityType( NullabilityType nullabilityType ) {
        this.nullabilityType = nullabilityType;
    }

    /**
     * Gets SQL type from java.sql.Types
     * 
     * @return SQL type from java.sql.Types
     */
    public SqlType getSqlType() {
        return sqlType;
    }

    /**
     * Sets SQL type from java.sql.Types
     * 
     * @param sqlType the SQL type from java.sql.Types
     */
    public void setSqlType( SqlType sqlType ) {
        this.sqlType = sqlType;
    }

    /**
     * Gets precision (number of fractional digits/scale) if applicable otherwise 0.
     * 
     * @return precision if applicable otherwise 0
     */
    public Long getPrecision() {
        return precision;
    }

    /**
     * Sets precision (number of fractional digits/scale) if applicable otherwise 0.
     * 
     * @param precision precision if applicable otherwise 0
     */
    public void setPrecision( Long precision ) {
        this.precision = precision;
    }

    /**
     * Returns true if sql type can be a money value, for instance
     * 
     * @return true if sql type can be a money value, for instance
     */
    public Boolean isFixedPrecisionScale() {
        return fixedPrecisionScale;
    }

    /**
     * Sets true if sql type can be a money value, for instance
     * 
     * @param fixedPrecisionScale true if sql type can be a money value, for instance
     */
    public void setFixedPrecisionScale( Boolean fixedPrecisionScale ) {
        this.fixedPrecisionScale = fixedPrecisionScale;
    }

    /**
     * Returns sql type precision radix (usually 2 or 10)
     * 
     * @return sql type precision radix (usually 2 or 10)
     */
    public Integer getNumberPrecisionRadix() {
        return numberPrecisionRadix;
    }

    /**
     * sets sql type precision radix (usually 2 or 10)
     * 
     * @param numberPrecisionRadix the sql type precision radix (usually 2 or 10)
     */
    public void setNumberPrecisionRadix( Integer numberPrecisionRadix ) {
        this.numberPrecisionRadix = numberPrecisionRadix;
    }

    /**
     * Returns minimum scale supported
     * 
     * @return minimum scale supported
     */
    public Integer getMinScale() {
        return minScale;
    }

    /**
     * Sets minimum scale supported
     * 
     * @param minScale minimum scale supported
     */
    public void setMinScale( Integer minScale ) {
        this.minScale = minScale;
    }

    /**
     * Returns maximum scale supported
     * 
     * @return maximum scale supported
     */
    public Integer getMaxScale() {
        return maxScale;
    }

    /**
     * Sets maximum scale supported
     * 
     * @param maxScale the maximum scale supported
     */
    public void setMaxScale( Integer maxScale ) {
        this.maxScale = maxScale;
    }

    /**
     * Returns prefix used to quote a literal (may be null)
     * 
     * @return prefix used to quote a literal (may be null)
     */
    public String getLiteralPrefix() {
        return literalPrefix;
    }

    /**
     * Sets prefix used to quote a literal (may be null)
     * 
     * @param literalPrefix the prefix used to quote a literal (may be null)
     */
    public void setLiteralPrefix( String literalPrefix ) {
        this.literalPrefix = literalPrefix;
    }

    /**
     * Returns suffix used to quote a literal (may be null)
     * 
     * @return suffix used to quote a literal (may be null)
     */
    public String getLiteralSuffix() {
        return literalSuffix;
    }

    /**
     * Sets suffix used to quote a literal (may be null)
     * 
     * @param literalSuffix the suffix used to quote a literal (may be null)
     */
    public void setLiteralSuffix( String literalSuffix ) {
        this.literalSuffix = literalSuffix;
    }

    /**
     * Returns parameters used in creating the type (may be null)
     * 
     * @return parameters used in creating the type (may be null)
     */
    public String getCreateParams() {
        return createParams;
    }

    /**
     * Sets parameters used in creating the type (may be null)
     * 
     * @param createParams the parameters used in creating the type (may be null)
     */
    public void setCreateParams( String createParams ) {
        this.createParams = createParams;
    }

    /**
     * Is sql type case sensitive
     * 
     * @return true if sql type case sensitive
     */
    public Boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
     * Is sql type case sensitive
     * 
     * @param caseSensitive the true if sql type case sensitive
     */
    public void setCaseSensitive( Boolean caseSensitive ) {
        this.caseSensitive = caseSensitive;
    }

    /**
     * Returns sql type searchability
     * 
     * @return sql type searchability
     */
    public SearchabilityType getSearchabilityType() {
        return searchabilityType;
    }

    /**
     * Sets sql type searchability
     * 
     * @param searchabilityType the sql type searchability
     */
    public void setSearchabilityType( SearchabilityType searchabilityType ) {
        this.searchabilityType = searchabilityType;
    }

    /**
     * Returns true if sql type is unsigned
     * 
     * @return true if sql type is unsigned
     */
    public Boolean isUnsigned() {
        return unsigned;
    }

    /**
     * Sets true if sql type is unsigned
     * 
     * @param unsigned true if sql type is unsigned
     */
    public void setUnsigned( Boolean unsigned ) {
        this.unsigned = unsigned;
    }

    /**
     * Returns true if sql type can be used for an auto-increment value.
     * 
     * @return true if sql type can be used for an auto-increment value.
     */
    public Boolean isAutoIncrement() {
        return autoIncrement;
    }

    /**
     * Sets true if sql type can be used for an auto-increment value.
     * 
     * @param autoIncrement true if sql type can be used for an auto-increment value.
     */
    public void setAutoIncrement( Boolean autoIncrement ) {
        this.autoIncrement = autoIncrement;
    }
}
