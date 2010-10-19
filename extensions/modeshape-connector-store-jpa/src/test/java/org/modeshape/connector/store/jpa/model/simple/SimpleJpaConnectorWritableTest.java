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
package org.modeshape.connector.store.jpa.model.simple;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.File;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.connector.store.jpa.JpaSource;
import org.modeshape.connector.store.jpa.TestEnvironment;
import org.modeshape.graph.Graph;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.test.WritableConnectorTest;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.BinaryFactory;
import org.modeshape.graph.property.Property;

public class SimpleJpaConnectorWritableTest extends WritableConnectorTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() {
        // Set the connection properties using the environment defined in the POM files ...
        JpaSource source = TestEnvironment.configureJpaSource("Test Repository", this);
        source.setModel(JpaSource.Models.SIMPLE.getName());

        // Override the inherited properties ...
        source.setCompressData(true);

        return source;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#initializeContent(org.modeshape.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) {
    }

    @FixFor( "MODE-966" )
    @Test
    public void shouldStoreLargeBinaryValue() throws Exception {
        File file = new File("src/test/resources/testdata/test1.xmi");
        assertThat(file.exists(), is(true));
        BinaryFactory binaryFactory = context.getValueFactories().getBinaryFactory();
        Binary binaryValue = binaryFactory.create(file);
        graph.batch()
             .create("/someFile.xmi")
             .with("jcr:primaryType", "nt:file")
             .and()
             .create("/someFile.xmi/jcr:content")
             .with("jcr:priamryType", "nt:resource")
             .and("jcr:data", binaryValue)
             .and()
             .execute();

        // Now read the content back out ...
        Property data = graph.getProperty("jcr:data").on("/someFile.xmi/jcr:content");
        Binary readValue = binaryFactory.create(data.getFirstValue());

        // and verify the content matches ...
        assertThat(binaryValue.getHash(), is(readValue.getHash()));
        assertThat(binaryValue.getBytes(), is(readValue.getBytes()));
    }

}
