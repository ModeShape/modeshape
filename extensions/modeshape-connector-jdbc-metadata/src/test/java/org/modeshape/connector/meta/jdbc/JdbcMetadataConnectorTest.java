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
package org.modeshape.connector.meta.jdbc;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.modeshape.graph.Graph;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.test.ReadableConnectorTest;
import org.junit.After;
import org.xml.sax.SAXException;

public class JdbcMetadataConnectorTest extends ReadableConnectorTest {

    private JdbcMetadataSource source;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() throws Exception {
        this.source = TestEnvironment.configureJdbcMetadataSource("Test Repository", this);

        return source;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws SAXException
     * @throws IOException
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#initializeContent(org.modeshape.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) throws Exception {
        TestEnvironment.executeDdl(this.source.getDataSource(), "create.ddl", this);

        graph = Graph.create(source, context);
    }

    @Override
    @After
    public void afterEach() throws Exception {
        TestEnvironment.executeDdl(this.source.getDataSource(), "drop.ddl", this);

        this.source.close();
    }

    @Override
    public void shouldReturnSameStructureForRepeatedReadBranchRequests() {
        Properties properties = TestEnvironment.propertiesFor(this);

        /*
         * The test Oracle, DB2, and PostgreSQL instances are massive so executing this test that fully loads the whole graph 
         * takes a LONG time.
         * MS SQL and Sybase return all catalogs, even those that the user does not have access to.
         */
        List<String> hugeDbs = Arrays.asList(new String[] {"postgresql8", "oracle10g", "oracle11g", "db2v9", "mssql2008",
            "sybase15"});
        if (hugeDbs.contains(properties.getProperty("database"))) {
            return;
        }
        super.shouldReturnSameStructureForRepeatedReadBranchRequests();
    }

}
