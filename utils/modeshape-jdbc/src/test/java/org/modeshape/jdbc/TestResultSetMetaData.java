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

import javax.jcr.PropertyType;

/**
 * @author vanhalbert
 * 
 * This provides common result set metadata used by various tests
 *
 */
public class TestResultSetMetaData {
    
    public static final String STRING = PropertyType.nameFromValue(PropertyType.STRING);
    public static final String DOUBLE = PropertyType.nameFromValue(PropertyType.DOUBLE);
    public static final String LONG = PropertyType.nameFromValue(PropertyType.LONG);
    public static final String PATH = PropertyType.nameFromValue(PropertyType.PATH);
    public static final String REFERENCE = PropertyType.nameFromValue(PropertyType.REFERENCE);

    
    public static String[] COLUMN_NAMES;
    public static String[] TABLE_NAMES;
    public static String[] TYPE_NAMES;
    
    public static String SQL_SELECT = "Select propA FROM typeA";
    
    static {
	COLUMN_NAMES = new String[] {"propA", "propB", "propC", "propD", "propE"};
	TABLE_NAMES = new String[] {"typeA", "typeB", "typeA", "", "typeA"};
	TYPE_NAMES = new String[] {STRING, LONG, PATH, REFERENCE, DOUBLE};

    }

}
