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
 * Provides database table type specific metadata.
 * <P>
 * The table type is:
 * <OL>
 * <LI><B>TABLE_TYPE</B> String => table type. Typical types are "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
 * "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
 * </OL>
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public interface TableType extends DatabaseNamedObject {
    public static String DEF_TABLE_TYPE_TABLE = "TABLE";
    public static String DEF_TABLE_TYPE_VIEW = "VIEW";
    public static String DEF_TABLE_TYPE_SYS_TABLE = "SYSTEM TABLE";
    public static String DEF_TABLE_TYPE_GLOBAL_TEMP = "GLOBAL TEMPORARY";
    public static String DEF_TABLE_TYPE_LOCAL_TEMP = "LOCAL TEMPORARY";
    public static String DEF_TABLE_TYPE_ALIAS = "ALIAS";
    public static String DEF_TABLE_TYPE_SYNONYM = "SYNONYM";

    /**
     * Is table type represents TABLE
     * 
     * @param tableTypeName the table type string
     * @return true if table type represents TABLE
     */
    Boolean isTable( String tableTypeName );

    /**
     * Is current table type represents TABLE
     * 
     * @return true if current table type represents TABLE
     */
    Boolean isTable();

    /**
     * Is table type represents VIEW
     * 
     * @param tableTypeName the table type string
     * @return true if table type represents VIEW
     */
    Boolean isView( String tableTypeName );

    /**
     * Is current table type represents VIEW
     * 
     * @return true if current table type represents VIEW
     */
    Boolean isView();

    /**
     * Is table type represents SYSTEM TABLE
     * 
     * @param tableTypeName the table type string
     * @return true if table type represents SYSTEM TABLE
     */
    Boolean isSystemTable( String tableTypeName );

    /**
     * Is current table type represents SYSTEM TABLE
     * 
     * @return true if current table type represents SYSTEM TABLE
     */
    Boolean isSystemTable();

    /**
     * Is current table type represents GLOBAL TEMPORARY
     * 
     * @param tableTypeName the table type string
     * @return true if current table type represents GLOBAL TEMPORARY
     */
    Boolean isGlobalTemporary( String tableTypeName );

    /**
     * Is current table type represents GLOBAL TEMPORARY
     * 
     * @return true if table type represents GLOBAL TEMPORARY
     */
    Boolean isGlobalTemporary();

    /**
     * Is table type represents LOCAL TEMPORARY
     * 
     * @param tableTypeName the table type string
     * @return true if table type represents LOCAL TEMPORARY
     */
    Boolean islocalTemporary( String tableTypeName );

    /**
     * Is current table type represents LOCAL TEMPORARY
     * 
     * @return true if current table type represents LOCAL TEMPORARY
     */
    Boolean isLocalTemporary();

    /**
     * Is table type represents ALIAS
     * 
     * @param tableTypeName the table type string
     * @return true if table type represents ALIAS
     */
    Boolean isAlias( String tableTypeName );

    /**
     * Is current table type represents ALIAS
     * 
     * @return true if current table type represents ALIAS
     */
    Boolean isAlias();

    /**
     * Is table type represents SYNONYM
     * 
     * @param tableTypeName the table type string
     * @return true if table type represents SYNONYM
     */
    Boolean isSynonym( String tableTypeName );

    /**
     * Is current table type represents SYNONYM
     * 
     * @return true if current table type represents SYNONYM
     */
    Boolean isSynonym();
}
