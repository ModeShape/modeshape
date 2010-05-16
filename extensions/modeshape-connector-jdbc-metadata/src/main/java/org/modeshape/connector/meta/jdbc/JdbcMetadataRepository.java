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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.base.PathNode;
import org.modeshape.graph.connector.base.PathTransaction;
import org.modeshape.graph.connector.base.Repository;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.request.InvalidWorkspaceException;

@ThreadSafe
public class JdbcMetadataRepository extends Repository<PathNode, JdbcMetadataWorkspace> {

    public final static String TABLES_SEGMENT_NAME = "tables";
    public final static String PROCEDURES_SEGMENT_NAME = "procedures";

    private static final Logger LOGGER = Logger.getLogger(JdbcMetadataRepository.class);
    private final JdbcMetadataSource source;
    private Map<Name, Property> rootNodeProperties;
    private String databaseProductName;
    private String databaseProductVersion;
    private int databaseMajorVersion;
    private int databaseMinorVersion;

    public JdbcMetadataRepository( JdbcMetadataSource source ) {
        super(source);
        this.source = source;
        initialize();
    }

    final JdbcMetadataSource source() {
        return source;
    }

    @Override
    protected synchronized void initialize() {
        ExecutionContext context = source.getRepositoryContext().getExecutionContext();
        PropertyFactory propFactory = context.getPropertyFactory();
        this.rootNodeProperties = new HashMap<Name, Property>();

        Connection conn = getConnection();
        try {
            Name propName;
            DatabaseMetaData dmd = conn.getMetaData();

            databaseProductName = dmd.getDatabaseProductName();
            databaseProductVersion = dmd.getDatabaseProductVersion();
            databaseMajorVersion = dmd.getDatabaseMajorVersion();
            databaseMinorVersion = dmd.getDatabaseMinorVersion();

            propName = JdbcMetadataLexicon.DATABASE_PRODUCT_NAME;
            rootNodeProperties.put(propName, propFactory.create(propName, databaseProductName));
            propName = JdbcMetadataLexicon.DATABASE_PRODUCT_VERSION;
            rootNodeProperties.put(propName, propFactory.create(propName, databaseProductVersion));
            propName = JdbcMetadataLexicon.DATABASE_MAJOR_VERSION;
            rootNodeProperties.put(propName, propFactory.create(propName, databaseMajorVersion));
            propName = JdbcMetadataLexicon.DATABASE_MINOR_VERSION;
            rootNodeProperties.put(propName, propFactory.create(propName, databaseMinorVersion));

            rootNodeProperties.put(JcrLexicon.PRIMARY_TYPE, propFactory.create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.ROOT));
            rootNodeProperties.put(JcrLexicon.MIXIN_TYPES, propFactory.create(JcrLexicon.MIXIN_TYPES,
                                                                              JdbcMetadataLexicon.DATABASE_ROOT));

            rootNodeProperties = Collections.unmodifiableMap(rootNodeProperties);
        } catch (SQLException se) {
            throw new IllegalStateException(JdbcMetadataI18n.couldNotGetDatabaseMetadata.text(), se);
        } finally {
            closeConnection(conn);
        }

        super.initialize();
    }

    Connection getConnection() {
        try {
            return source.getDataSource().getConnection();
        } catch (SQLException se) {
            throw new IllegalStateException(JdbcMetadataI18n.errorObtainingConnection.text(), se);
        }
    }

    void closeConnection( Connection connection ) {
        assert connection != null;
        try {
            connection.close();
        } catch (SQLException se) {
            LOGGER.error(se, JdbcMetadataI18n.errorClosingConnection);
        }
    }

    final Map<Name, Property> rootNodeProperties() {
        return rootNodeProperties;
    }

    @Override
    public JdbcMetadataTransaction startTransaction( ExecutionContext context,
                                                     boolean readonly ) {
        return new JdbcMetadataTransaction(this, source.getRootNodeUuidObject());
    }

    @NotThreadSafe
    protected class JdbcMetadataTransaction extends PathTransaction<PathNode, JdbcMetadataWorkspace> {

        public JdbcMetadataTransaction( Repository<PathNode, JdbcMetadataWorkspace> repository,
                                        UUID rootNodeUuid ) {
            super(repository, rootNodeUuid);
        }

        @Override
        protected PathNode createNode( Segment name,
                                       Path parentPath,
                                       Iterable<Property> properties ) {
            throw new RepositorySourceException(JdbcMetadataI18n.sourceIsReadOnly.text(source().getName()));
        }

        public boolean destroyWorkspace( JdbcMetadataWorkspace workspace ) throws InvalidWorkspaceException {
            throw new RepositorySourceException(JdbcMetadataI18n.sourceIsReadOnly.text(source().getName()));
        }

        public JdbcMetadataWorkspace getWorkspace( String name,
                                                   JdbcMetadataWorkspace originalToClone ) throws InvalidWorkspaceException {
            if (name.equals(source().getDefaultWorkspaceName())) {
                return new JdbcMetadataWorkspace(JdbcMetadataRepository.this, name);
            }

            throw new InvalidWorkspaceException(JdbcMetadataI18n.sourceIsReadOnly.text(source().getName()));
        }

    }

}
