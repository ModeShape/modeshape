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
package org.modeshape.test.integration;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import org.junit.Test;
import org.modeshape.common.FixFor;

public class JcrImportExportIntegrationTest extends AbstractAdHocModeShapeTest {

    @FixFor( "MODE-687" )
    @Test
    public void shouldBeAbleToImportSystemFileIntoRepository() throws Exception {
        startEngine("config/configRepositoryForDroolsImportExport.xml", "Repo");
        assertNode("/", "mode:root");
        // import the file ...
        importContent(getClass(), "io/drools/systemViewImport.xml");
        session().refresh(false);
        // Verify the file was imported ...
        assertNode("/drools:repository", "nt:folder");
    }

    @FixFor( "MODE-1026" )
    @Test
    public void shouldBeAbleToImportFileWithValuesDefinedByXsiTypeAttributes() throws Exception {
        startEngine("config/configRepositoryForDroolsImportExport.xml", "Repo");
        assertNode("/", "mode:root");
        // import the file ...
        importContent(getClass(), "io/drools/mortgage-sample-repository.xml");
        session().refresh(false);
        // Verify the file was imported ...
        assertNode("/drools:repository", "nt:folder");
        assertNode("/drools:repository/drools:package_area", "nt:folder");
        assertNode("/drools:repository/drools:package_area/mortgages", "drools:packageNodeType");
        assertNode("/drools:repository/drools:package_area/mortgages/assets", "drools:versionableAssetFolder");
        Node dsl = assertNode("/drools:repository/drools:package_area/mortgages/assets/ApplicantDsl", "drools:assetNodeType");
        Property property = dsl.getProperty("drools:content");
        assertThat(property.getType(), is(PropertyType.STRING));
        assertThat(property.getValue().getString().startsWith("[when]"), is(true));
    }

}
