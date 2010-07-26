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
package org.modeshape.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 */
public class JcrDriverFileIntegrationTest extends ConnectionResultsComparator {
	private static Connection connection = null;
	
   @BeforeClass
    public static void beforeAll() throws Exception {
	   String url = JcrDriver.FILE_URL_PREFIX + "src/test/resources/carConfigRepository.xml";
	   JcrDriver driver = new JcrDriver();
	   connection = driver.connect(url, new Properties());

   }
   
   @AfterClass
   public static void afterAll() throws Exception {
	   connection.close();
   }

    @Test
	public void connectToDriverUsingFileOption() throws SQLException {
    	String[] expected = {
    			"jcr:primaryType[STRING]",
    			"mode:root"
    			};
            executeTest(connection, "SELECT * FROM [nt:base]", expected, 2);
		   
	    }
    
    @Test
    public void shouldGetAllTables() throws SQLException {
    	this.compareColumns = false;

    	String[] expected = {
    			"TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    TABLE_TYPE[String]    REMARKS[String]    TYPE_CAT[String]    TYPE_SCHEM[String]    TYPE_NAME[String]    SELF_REFERENCING_COL_NAME[String]    REF_GENERATION[String]",
    			"Cars    NULL    mix:created    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mix:etag    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mix:language    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mix:lastModified    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mix:lifecycle    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mix:lockable    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mix:managedRetention    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mix:mimeType    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mix:referenceable    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mix:shareable    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mix:simpleVersionable    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mix:title    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mix:versionable    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mode:defined    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mode:lock    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mode:locks    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mode:namespace    VIEW    Is Mixin: false    NULL    NULL    NULL    mode:uri    DERIVED",
    			"Cars    NULL    mode:namespaces    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mode:nodeTypes    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mode:resource    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:data    DERIVED",
    			"Cars    NULL    mode:root    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mode:system    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    mode:versionStorage    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    ns001:Car    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    nt:activity    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    nt:address    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    nt:base    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    nt:childNodeDefinition    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    nt:configuration    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    nt:file    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:content    DERIVED",
    			"Cars    NULL    nt:folder    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    nt:frozenNode    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    nt:hierarchyNode    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    nt:linkedFile    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:content    DERIVED",
    			"Cars    NULL    nt:naturalText    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    nt:nodeType    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    nt:propertyDefinition    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    nt:query    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    nt:resource    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:data    DERIVED",
    			"Cars    NULL    nt:unstructured    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    nt:version    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    nt:versionHistory    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    nt:versionLabels    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"Cars    NULL    nt:versionedChild    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED"
    			};

    	DatabaseMetaData dbmd = connection.getMetaData();
    	ResultSet rs = dbmd.getTables("%", "%", "%", new String[] {});
	    assertResultsSetEquals(rs, expected); 
	    assertRowCount(44);
    }
    
    @Test
    public void shouldGetOnlyColumnsForCarPrefixedTables() throws SQLException {
    	this.compareColumns = false;

    	String[] expected = {
    			"TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    COLUMN_NAME[String]    DATA_TYPE[Long]    TYPE_NAME[String]    COLUMN_SIZE[Long]    BUFFER_LENGTH[Long]    DECIMAL_DIGITS[Long]    NUM_PREC_RADIX[Long]    NULLABLE[Long]    REMARKS[String]    COLUMN_DEF[String]    SQL_DATA_TYPE[Long]    SQL_DATETIME_SUB[Long]    CHAR_OCTET_LENGTH[Long]    ORDINAL_POSITION[Long]    IS_NULLABLE[String]    SCOPE_CATLOG[String]    SCOPE_SCHEMA[String]    SCOPE_TABLE[String]    SOURCE_DATA_TYPE[Long]",
    			"Cars    NULL    ns001:Car    *    12    undefined    50    NULL    0    0    2        NULL    0    0    0    1    YES    NULL    NULL    NULL    0",
    			"Cars    NULL    ns001:Car    *    12    undefined    50    NULL    0    0    2        NULL    0    0    0    2    YES    NULL    NULL    NULL    0",
    			"Cars    NULL    ns001:Car    jcr:mixinTypes    12    Name    20    NULL    0    0    2        NULL    0    0    0    3    YES    NULL    NULL    NULL    0",
    			"Cars    NULL    ns001:Car    jcr:primaryType    12    Name    20    NULL    0    0    1        NULL    0    0    0    4    NO    NULL    NULL    NULL    0",
    			"Cars    NULL    ns001:Car    modeint:multiValuedProperties    12    String    50    NULL    0    0    2        NULL    0    0    0    5    YES    NULL    NULL    NULL    0",
    			"Cars    NULL    ns001:Car    modeint:nodeDefinition    12    String    50    NULL    0    0    2        NULL    0    0    0    6    YES    NULL    NULL    NULL    0",
    			"Cars    NULL    ns001:Car    ns001:engine    12    String    50    NULL    0    0    2        NULL    0    0    0    7    YES    NULL    NULL    NULL    0",
    			"Cars    NULL    ns001:Car    ns001:lengthInInches    6    Double    20    NULL    0    0    2        NULL    0    0    0    8    YES    NULL    NULL    NULL    0",
    			"Cars    NULL    ns001:Car    ns001:maker    12    String    50    NULL    0    0    2        NULL    0    0    0    9    YES    NULL    NULL    NULL    0",
    			"Cars    NULL    ns001:Car    ns001:model    12    String    50    NULL    0    0    2        NULL    0    0    0    10    YES    NULL    NULL    NULL    0",
    			"Cars    NULL    ns001:Car    ns001:mpgCity    -5    Long    20    NULL    0    0    2        NULL    0    0    0    11    YES    NULL    NULL    NULL    0",
    			"Cars    NULL    ns001:Car    ns001:mpgHighway    -5    Long    20    NULL    0    0    2        NULL    0    0    0    12    YES    NULL    NULL    NULL    0",
    			"Cars    NULL    ns001:Car    ns001:msrp    12    String    50    NULL    0    0    2        NULL    0    0    0    13    YES    NULL    NULL    NULL    0",
    			"Cars    NULL    ns001:Car    ns001:userRating    -5    Long    20    NULL    0    0    2        NULL    0    0    0    14    YES    NULL    NULL    NULL    0",
    			"Cars    NULL    ns001:Car    ns001:valueRating    -5    Long    20    NULL    0    0    2        NULL    0    0    0    15    YES    NULL    NULL    NULL    0",
    			"Cars    NULL    ns001:Car    ns001:wheelbaseInInches    6    Double    20    NULL    0    0    2        NULL    0    0    0    16    YES    NULL    NULL    NULL    0",
    			"Cars    NULL    ns001:Car    ns001:year    12    String    50    NULL    0    0    2        NULL    0    0    0    17    YES    NULL    NULL    NULL    0"
    			};   	

    	DatabaseMetaData dbmd = connection.getMetaData();
    	ResultSet rs = dbmd.getColumns("%", "%", "%Car", "%");  
	    assertResultsSetEquals(rs, expected); 
	    assertRowCount(11);
 
    }
    
    @Test
    public void shouldGetOnlyMSRPColumnForCarTable() throws SQLException {
    	this.compareColumns = false;
    	
    	String[] expected = {
    			"TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    COLUMN_NAME[String]    DATA_TYPE[Long]    TYPE_NAME[String]    COLUMN_SIZE[Long]    BUFFER_LENGTH[Long]    DECIMAL_DIGITS[Long]    NUM_PREC_RADIX[Long]    NULLABLE[Long]    REMARKS[String]    COLUMN_DEF[String]    SQL_DATA_TYPE[Long]    SQL_DATETIME_SUB[Long]    CHAR_OCTET_LENGTH[Long]    ORDINAL_POSITION[Long]    IS_NULLABLE[String]    SCOPE_CATLOG[String]    SCOPE_SCHEMA[String]    SCOPE_TABLE[String]    SOURCE_DATA_TYPE[Long]",
    			"Cars    NULL    ns001:Car    ns001:msrp    12    String    50    NULL    0    0    2        NULL    0    0    0    1    YES    NULL    NULL    NULL    0"
    			};


    	DatabaseMetaData dbmd = connection.getMetaData();
    	ResultSet rs = dbmd.getColumns("%", "%", "ns001:Car", "ns001:msrp");
	    assertResultsSetEquals(rs, expected); 
	    assertRowCount(1);
 
    }

	 

}
