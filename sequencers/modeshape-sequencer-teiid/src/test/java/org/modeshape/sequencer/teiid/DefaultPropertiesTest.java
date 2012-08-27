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
package org.modeshape.sequencer.teiid;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class DefaultPropertiesTest {

    @Test
    public void shouldFindDefaultValuesGivenExplicitlyOnTypes() {
        assertDefault("xmi:model", "xmi:version", 2.0D);
        assertDefault("mmcore:model", "mmcore:modelType", "UNKNOWN");
        assertDefault("mmcore:model", "mmcore:supportsDistinct", true);
        assertDefault("mmcore:model", "mmcore:supportsWhereAll", true);
        assertDefault("mmcore:import", "mmcore:modelType", "UNKNOWN");
        assertDefault("relational:column", "relational:nullable", "NULLABLE");
        assertDefault("relational:column", "relational:autoIncremented", false);
        assertDefault("relational:column", "relational:selectable", true);
        assertDefault("relational:column", "relational:searchability", "SEARCHABLE");
        assertDefault("relational:column", "relational:radix", 10L);
        assertDefault("relational:column", "relational:nullValueCount", -1L);
        assertDefault("relational:foreignKey", "relational:foreignKeyMultiplicity", "ZERO_TO_MANY");
        assertDefault("relational:foreignKey", "relational:primaryKeyMultiplicity", "ONE");
        assertDefault("relational:index", "relational:nullable", true);
        assertDefault("relational:table", "relational:system", false);
        assertDefault("relational:procedureParameter", "relational:direction", null);
        assertDefault("relational:procedureParameter", "relational:nullable", "NULLABLE");
        assertDefault("relational:procedure", "relational:updateCount", null);
        assertDefault("relational:logicalRelationshipEnd", "relational:multiplicity", null);
        assertDefault("transform:withSql", "transform:insertAllowed", true);
        assertDefault("transform:withSql", "transform:outputLocked", false);
        assertDefault("jdbcs:imported", "jdbcs:convertCaseInModel", null);
        assertDefault("jdbcs:imported", "jdbcs:generateSourceNamesInModel", "UNQUALIFIED");

        // vdb.cnd
        assertDefault("vdb:virtualDatabase", "vdb:version", 1L);
        assertDefault("vdb:virtualDatabase", "vdb:preview", false);
        assertDefault("vdb:model", "vdb:visible", true);
        assertDefault("vdb:model", "vdb:builtIn", false);
        assertDefault("vdb:marker", "vdb:severity", "WARNING");
        assertDefault("vdb:dataRole", "vdb:anyAuthenticated", false);
        assertDefault("vdb:dataRole", "vdb:allowCreateTemporaryTables", false);
        assertDefault("vdb:permission", "vdb:allowCreate", false);
        assertDefault("vdb:permission", "vdb:allowRead", false);
        assertDefault("vdb:permission", "vdb:allowUpdate", false);
        assertDefault("vdb:permission", "vdb:allowDelete", false);
        assertDefault("vdb:permission", "vdb:allowExecute", false);
        assertDefault("vdb:permission", "vdb:allowAlter", false);
    }

    @Test
    public void shouldFindDefaultValuesInheritedByTypes() {
        assertDefault("relational:baseTable", "relational:system", false);
        assertDefault("relational:view", "relational:system", false);
        assertDefault("vdb:model", "mmcore:maxSetSize", 100L);
        assertDefault("vdb:model", "xmi:version", 2.0D);
    }

    @Test
    public void shouldFindSupertypeNames() {
        assertSupertypes("relational:column, foo:bar mixin someting", "relational:column", "foo:bar");
    }

    protected void assertDefault( String nodeTypeName,
                                  String propertyName,
                                  Object value ) {
        assertThat(DefaultProperties.getDefaults().getDefaultFor(nodeTypeName, propertyName), is(value));
    }

    protected void assertSupertypes( String line,
                                     String... supertypeNames ) {
        List<String> actual = DefaultProperties.findSupertypes(line);
        assertThat(actual, is(Arrays.asList(supertypeNames)));
    }

}
