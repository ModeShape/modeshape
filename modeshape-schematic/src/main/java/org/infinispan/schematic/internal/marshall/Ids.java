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
package org.infinispan.schematic.internal.marshall;

/**
 * Indexes for object types. These are currently limited to being unsigned ints, so valid values are considered those in the range
 * of 0 to 254. Please note that the use of 255 is forbidden since this is reserved for foreign, or user defined, externalizers.
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
    int SCHEMATIC_VALUE_PUT_IF_ABSENT_OPERATION = 1624;
    int SCHEMATIC_VALUE_DELTA_DOCUMENT = 1625;
}
