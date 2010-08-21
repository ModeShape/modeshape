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
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class DefaultPropertiesTest {

    @Test
    public void shouldFindDefaultValuesGivenExplicitlyOnTypes() throws IOException {
        assertDefault("xmi:model", "xmi:version", "2.0");
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
    }

    @Test
    public void shouldFindDefaultValuesInheritedByTypes() throws IOException {
        assertDefault("relational:baseTable", "relational:system", false);
        assertDefault("relational:view", "relational:system", false);
    }

    @Test
    public void shouldFindSupertypeNames() {
        assertSupertypes("relational:column, foo:bar mixin someting", "relational:column", "foo:bar");
    }

    protected void assertDefault( String nodeTypeName,
                                  String propertyName,
                                  Object value ) throws IOException {
        assertThat(DefaultProperties.getDefaults().getDefaultFor(nodeTypeName, propertyName), is(value));
    }

    protected void assertSupertypes( String line,
                                     String... supertypeNames ) {
        List<String> actual = DefaultProperties.findSupertypes(line);
        assertThat(actual, is(Arrays.asList(supertypeNames)));
    }

}
