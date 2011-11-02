/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.schematic.internal.marshall;

/**
 * Indexes for object types. These are currently limited to being unsigned ints, so valid values are considered those in
 * the range of 0 to 254. Please note that the use of 255 is forbidden since this is reserved for foreign, or user
 * defined, externalizers.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 * @see org.infinispan.marshall.Ids
 */
public interface Ids {

   int SCHEMATIC_VALUE_LITERAL = 1600;
   int SCHEMATIC_VALUE_DELTA = 1601;
   int SCHEMATIC_VALUE_PUT_OPERATION = 1602;
   int SCHEMATIC_VALUE_REMOVE_OPERATION = 1603;
   int SCHEMATIC_VALUE_RETAIN_ALL_OPERATION = 1604;
   int SCHEMATIC_VALUE_ADD_OPERATION = 1605;
   int SCHEMATIC_VALUE_ADD_IF_ABSENT_OPERATION = 1606;
   int SCHEMATIC_VALUE_CLEAR_OPERATION = 1607;
   int SCHEMATIC_VALUE_REMOVE_AT_INDEX_OPERATION = 1608;
   int SCHEMATIC_VALUE_REMOVE_VALUE_OPERATION = 1609;
   int SCHEMATIC_VALUE_REMOVE_ALL_VALUES_OPERATION = 1610;
   int SCHEMATIC_VALUE_RETAIN_ALL_VALUES_OPERATION = 1611;
   int SCHEMATIC_VALUE_SET_VALUE_OPERATION = 1612;
   int SCHEMATIC_VALUE_PATH = 1613;
   int SCHEMATIC_VALUE_DOCUMENT = 1614;
   int SCHEMATIC_VALUE_SYMBOL = 1615;
   int SCHEMATIC_VALUE_TIMESTAMP = 1616;
   int SCHEMATIC_VALUE_OBJECT_ID = 1617;
   int SCHEMATIC_VALUE_MINKEY = 1618;
   int SCHEMATIC_VALUE_MAXKEY = 1619;
   int SCHEMATIC_VALUE_CODE = 1620;
   int SCHEMATIC_VALUE_BINARY = 1621;
   int SCHEMATIC_VALUE_NULL = 1622;
   int SCHEMATIC_DOCUMENT_CHANGES = 1623;
}
