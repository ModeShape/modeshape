/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.meta.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.connector.LockFailedException;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.path.DefaultPathNode;
import org.jboss.dna.graph.connector.path.PathNode;
import org.jboss.dna.graph.connector.path.PathRepository;
import org.jboss.dna.graph.connector.path.PathWorkspace;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.Path.Segment;
import org.jboss.dna.graph.query.QueryResults;
import org.jboss.dna.graph.request.AccessQueryRequest;
import org.jboss.dna.graph.request.LockBranchRequest.LockScope;

@ThreadSafe
public class JdbcMetadataRepository extends PathRepository {

    public final static String TABLES_SEGMENT_NAME = "tables";
    public final static String PROCEDURES_SEGMENT_NAME = "procedures";

    private final Logger log = Logger.getLogger(JdbcMetadataRepository.class);
    private final JdbcMetadataSource source;
    private Map<Name, Property> rootNodeProperties;
    private String databaseProductName;
    private String databaseProductVersion;
    private int databaseMajorVersion;
    private int databaseMinorVersion;

    public JdbcMetadataRepository( JdbcMetadataSource source ) {
        super(source.getName(), source.getRootUuid(), source.getDefaultWorkspaceName());
        this.source = source;
        initialize();
    }

    @Override
    protected synchronized void initialize() {
        if (!this.workspaces.isEmpty()) return;

        String defaultWorkspaceName = getDefaultWorkspaceName();
        this.workspaces.put(defaultWorkspaceName, new JdbcMetadataWorkspace(defaultWorkspaceName));

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

            rootNodeProperties.put(JcrLexicon.PRIMARY_TYPE, propFactory.create(JcrLexicon.PRIMARY_TYPE, DnaLexicon.ROOT));
            rootNodeProperties.put(JcrLexicon.MIXIN_TYPES, propFactory.create(JcrLexicon.PRIMARY_TYPE,
                                                                              JdbcMetadataLexicon.DATABASE_ROOT));

            rootNodeProperties = Collections.unmodifiableMap(rootNodeProperties);
        } catch (SQLException se) {
            throw new IllegalStateException(JdbcMetadataI18n.couldNotGetDatabaseMetadata.text(), se);
        } finally {
            closeConnection(conn);
        }
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
            log.error(se, JdbcMetadataI18n.errorClosingConnection);
        }
    }

    @ThreadSafe
    @SuppressWarnings( "synthetic-access" )
    private class JdbcMetadataWorkspace implements PathWorkspace {

        private final String name;

        JdbcMetadataWorkspace( String name ) {
            this.name = name;
        }

        public Path getLowestExistingPath( Path path ) {
            PathFactory pathFactory = source.getRepositoryContext().getExecutionContext().getValueFactories().getPathFactory();

            Path lastWorkingPath = pathFactory.createRootPath();

            for (Segment segment : path.getSegmentsList()) {
                Path newPathToTry = pathFactory.create(lastWorkingPath, segment);

                try {
                    getNode(newPathToTry);
                    lastWorkingPath = newPathToTry;
                } catch (PathNotFoundException pnfe) {
                    return lastWorkingPath;
                }
            }

            // If we got here, someone called getLowestExistingPath on a path that was invalid but now isn't.
            return path;
        }

        public String getName() {
            return this.name;
        }

        public PathNode getNode( Path path ) {
            assert path != null;

            List<Segment> segments = path.getSegmentsList();
            switch (segments.size()) {
                case 0:
                    return getRoot();
                case 1:
                    return catalogNodeFor(segments);
                case 2:
                    return schemaNodeFor(segments);
                case 3:
                    if (TABLES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName())) {
                        return tablesNodeFor(segments);
                    } else if (PROCEDURES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName())) {
                        return proceduresNodeFor(segments);
                    }

                    return null;
                case 4:
                    if (TABLES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName())) {
                        return tableNodeFor(segments);
                    } else if (PROCEDURES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName())) {
                        return procedureNodeFor(segments);
                    }
                    return null;
                case 5:
                    if (TABLES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName())) {
                        return columnNodeFor(segments);
                    }
                    return null;
                default:
                    return null;
            }
        }

        private PathNode catalogNodeFor( List<Segment> segments ) throws RepositorySourceException {
            assert segments != null;
            assert segments.size() == 1;

            List<Segment> schemaNames = new LinkedList<Segment>();
            ExecutionContext context = source.getRepositoryContext().getExecutionContext();
            PathFactory pathFactory = context.getValueFactories().getPathFactory();
            PropertyFactory propFactory = context.getPropertyFactory();

            Path nodePath = pathFactory.createAbsolutePath(segments);

            Connection conn = getConnection();
            String catalogName = segments.get(0).getName().getLocalName();

            try {
                MetadataCollector meta = source.getMetadataCollector();
                if (catalogName.equals(source.getDefaultCatalogName())) catalogName = null;

                // Make sure that this is a valid catalog for this database
                List<String> catalogNames = meta.getCatalogNames(conn);

                /*
                 * If a "real" (not default) catalog name is provided but it is not a valid
                 * catalog name for this database OR if the default catalog name is being used
                 * but this database uses real catalog names, then no catalog with that name exists.
                 * 
                 * This gets complicated by the fact that some DBMSes use an empty string for a catalog
                 * which also gets mapped to the default catalog name in our system
                 */
                boolean catalogMatchesDefaultName = catalogNames.isEmpty() || catalogNames.contains("");

                if ((catalogName != null && !catalogNames.contains(catalogName))
                    || (catalogName == null && !catalogMatchesDefaultName)) {
                    return null;
                }

                List<String> schemaNamesFromMeta = new ArrayList<String>(meta.getSchemaNames(conn, catalogName));

                for (String schemaName : schemaNamesFromMeta) {
                    if (schemaName.length() > 0) {
                        schemaNames.add(pathFactory.createSegment(schemaName));
                    }
                }

                if (schemaNames.isEmpty()) {
                    schemaNames.add(pathFactory.createSegment(source.getDefaultSchemaName()));
                }

                Map<Name, Property> properties = new HashMap<Name, Property>();
                properties.put(JcrLexicon.PRIMARY_TYPE, propFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED));
                properties.put(JcrLexicon.MIXIN_TYPES, propFactory.create(JcrLexicon.MIXIN_TYPES, JdbcMetadataLexicon.CATALOG));

                return new DefaultPathNode(nodePath, properties, schemaNames);
            } catch (JdbcMetadataException se) {
                throw new RepositorySourceException(JdbcMetadataI18n.couldNotGetSchemaNames.text(catalogName), se);
            } finally {
                closeConnection(conn);
            }
        }

        private PathNode schemaNodeFor( List<Segment> segments ) throws RepositorySourceException {
            assert segments != null;
            assert segments.size() == 2;

            ExecutionContext context = source.getRepositoryContext().getExecutionContext();
            PathFactory pathFactory = context.getValueFactories().getPathFactory();
            PropertyFactory propFactory = context.getPropertyFactory();

            Path nodePath = pathFactory.createAbsolutePath(segments);

            Connection conn = getConnection();
            String catalogName = segments.get(0).getName().getLocalName();
            if (catalogName.equals(source.getDefaultCatalogName())) catalogName = null;

            String schemaName = segments.get(1).getName().getLocalName();
            if (schemaName.equals(source.getDefaultSchemaName())) schemaName = null;

            try {
                MetadataCollector meta = source.getMetadataCollector();

                // Make sure that the schema exists in the given catalog
                List<String> schemaNames = meta.getSchemaNames(conn, catalogName);

                /*
                 * If a "real" (not default) catalog name is provided but it is not a valid
                 * catalog name for this database OR if the default catalog name is being used
                 * but this database uses real catalog names, then no catalog with that name exists.
                 */
                if ((schemaName != null && !schemaNames.contains(schemaName)) || (schemaName == null && !schemaNames.isEmpty())) {
                    return null;
                }

                Map<Name, Property> properties = new HashMap<Name, Property>();
                properties.put(JcrLexicon.PRIMARY_TYPE, propFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED));
                properties.put(JcrLexicon.MIXIN_TYPES, propFactory.create(JcrLexicon.MIXIN_TYPES, JdbcMetadataLexicon.SCHEMA));

                Segment[] children = new Segment[] {pathFactory.createSegment(TABLES_SEGMENT_NAME),
                    pathFactory.createSegment(PROCEDURES_SEGMENT_NAME)};

                return new DefaultPathNode(nodePath, properties, Arrays.asList(children));
            } catch (JdbcMetadataException se) {
                throw new RepositorySourceException(JdbcMetadataI18n.couldNotGetSchemaNames.text(catalogName), se);
            } finally {
                closeConnection(conn);
            }
        }

        private PathNode tablesNodeFor( List<Segment> segments ) throws RepositorySourceException {
            assert segments != null;
            assert segments.size() == 3;
            assert TABLES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName());

            ExecutionContext context = source.getRepositoryContext().getExecutionContext();
            PathFactory pathFactory = context.getValueFactories().getPathFactory();
            PropertyFactory propFactory = context.getPropertyFactory();

            Path nodePath = pathFactory.createAbsolutePath(segments);

            Connection conn = getConnection();
            String catalogName = segments.get(0).getName().getLocalName();
            if (catalogName.equals(source.getDefaultCatalogName())) catalogName = null;

            String schemaName = segments.get(1).getName().getLocalName();
            if (schemaName.equals(source.getDefaultSchemaName())) schemaName = null;

            try {
                MetadataCollector meta = source.getMetadataCollector();

                // Make sure that the schema exists in the given catalog
                List<String> schemaNames = meta.getSchemaNames(conn, catalogName);

                /*
                 * If a "real" (not default) catalog name is provided but it is not a valid
                 * catalog name for this database OR if the default catalog name is being used
                 * but this database uses real catalog names, then no catalog with that name exists.
                 */
                if ((schemaName != null && !schemaNames.contains(schemaName)) || (schemaName == null && !schemaNames.isEmpty())) {
                    return null;
                }

                Map<Name, Property> properties = new HashMap<Name, Property>();
                properties.put(JcrLexicon.PRIMARY_TYPE, propFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED));
                properties.put(JcrLexicon.MIXIN_TYPES, propFactory.create(JcrLexicon.MIXIN_TYPES, JdbcMetadataLexicon.TABLES));

                List<TableMetadata> tables = meta.getTables(conn, catalogName, schemaName, null);
                List<Segment> children = new ArrayList<Segment>(tables.size());

                for (TableMetadata table : tables) {
                    children.add(pathFactory.createSegment(table.getName()));
                }

                return new DefaultPathNode(nodePath, properties, children);
            } catch (JdbcMetadataException se) {
                throw new RepositorySourceException(JdbcMetadataI18n.couldNotGetTableNames.text(catalogName, schemaName), se);
            } finally {
                closeConnection(conn);
            }
        }

        private PathNode tableNodeFor( List<Segment> segments ) throws RepositorySourceException {
            assert segments != null;
            assert segments.size() == 4;
            assert TABLES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName());

            ExecutionContext context = source.getRepositoryContext().getExecutionContext();
            PathFactory pathFactory = context.getValueFactories().getPathFactory();
            PropertyFactory propFactory = context.getPropertyFactory();

            Path nodePath = pathFactory.createAbsolutePath(segments);

            Connection conn = getConnection();
            String catalogName = segments.get(0).getName().getLocalName();
            if (catalogName.equals(source.getDefaultCatalogName())) catalogName = null;

            String schemaName = segments.get(1).getName().getLocalName();
            if (schemaName.equals(source.getDefaultSchemaName())) schemaName = null;

            String tableName = segments.get(3).getName().getLocalName();

            try {
                MetadataCollector meta = source.getMetadataCollector();

                List<TableMetadata> tables = meta.getTables(conn, catalogName, schemaName, tableName);

                // Make sure that the table exists in the given catalog and schema
                if (tables.isEmpty()) {
                    return null;
                }
                assert tables.size() == 1;
                TableMetadata table = tables.get(0);

                Map<Name, Property> properties = new HashMap<Name, Property>();
                Name propName;
                propName = JcrLexicon.PRIMARY_TYPE;
                properties.put(propName, propFactory.create(propName, JcrNtLexicon.UNSTRUCTURED));
                propName = JcrLexicon.MIXIN_TYPES;
                properties.put(propName, propFactory.create(propName, JdbcMetadataLexicon.TABLE));

                if (table.getType() != null) {
                    propName = JdbcMetadataLexicon.TABLE_TYPE;
                    properties.put(propName, propFactory.create(propName, table.getType()));
                }
                if (table.getDescription() != null) {
                    propName = JdbcMetadataLexicon.DESCRIPTION;
                    properties.put(propName, propFactory.create(propName, table.getDescription()));
                }
                if (table.getTypeCatalogName() != null) {
                    propName = JdbcMetadataLexicon.TYPE_CATALOG_NAME;
                    properties.put(propName, propFactory.create(propName, table.getTypeCatalogName()));
                }
                if (table.getTypeSchemaName() != null) {
                    propName = JdbcMetadataLexicon.TYPE_SCHEMA_NAME;
                    properties.put(propName, propFactory.create(propName, table.getTypeSchemaName()));
                }
                if (table.getTypeName() != null) {
                    propName = JdbcMetadataLexicon.TYPE_NAME;
                    properties.put(propName, propFactory.create(propName, table.getTypeName()));
                }
                if (table.getSelfReferencingColumnName() != null) {
                    propName = JdbcMetadataLexicon.SELF_REFERENCING_COLUMN_NAME;
                    properties.put(propName, propFactory.create(propName, table.getSelfReferencingColumnName()));
                }
                if (table.getReferenceGenerationStrategyName() != null) {
                    propName = JdbcMetadataLexicon.REFERENCE_GENERATION_STRATEGY_NAME;
                    properties.put(propName, propFactory.create(propName, table.getReferenceGenerationStrategyName()));
                }

                List<ColumnMetadata> columns = meta.getColumns(conn, catalogName, schemaName, tableName, null);
                List<Segment> children = new ArrayList<Segment>(columns.size());

                for (ColumnMetadata column : columns) {
                    children.add(pathFactory.createSegment(column.getName()));
                }

                return new DefaultPathNode(nodePath, properties, children);
            } catch (JdbcMetadataException se) {
                throw new RepositorySourceException(JdbcMetadataI18n.couldNotGetTable.text(catalogName, schemaName, tableName),
                                                    se);
            } finally {
                closeConnection(conn);
            }
        }

        private PathNode proceduresNodeFor( List<Segment> segments ) throws RepositorySourceException {
            assert segments != null;
            assert segments.size() == 3;
            assert PROCEDURES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName());

            ExecutionContext context = source.getRepositoryContext().getExecutionContext();
            PathFactory pathFactory = context.getValueFactories().getPathFactory();
            PropertyFactory propFactory = context.getPropertyFactory();

            Path nodePath = pathFactory.createAbsolutePath(segments);

            Connection conn = getConnection();
            String catalogName = segments.get(0).getName().getLocalName();
            if (catalogName.equals(source.getDefaultCatalogName())) catalogName = null;

            String schemaName = segments.get(1).getName().getLocalName();
            if (schemaName.equals(source.getDefaultSchemaName())) schemaName = null;

            try {
                MetadataCollector meta = source.getMetadataCollector();

                // Make sure that the schema exists in the given catalog
                List<String> schemaNames = meta.getSchemaNames(conn, catalogName);

                /*
                 * If a "real" (not default) catalog name is provided but it is not a valid
                 * catalog name for this database OR if the default catalog name is being used
                 * but this database uses real catalog names, then no catalog with that name exists.
                 */
                if ((schemaName != null && !schemaNames.contains(schemaName)) || (schemaName == null && !schemaNames.isEmpty())) {
                    return null;
                }

                Map<Name, Property> properties = new HashMap<Name, Property>();
                properties.put(JcrLexicon.PRIMARY_TYPE, propFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED));
                properties.put(JcrLexicon.MIXIN_TYPES, propFactory.create(JcrLexicon.MIXIN_TYPES, JdbcMetadataLexicon.PROCEDURES));

                List<ProcedureMetadata> procedures = meta.getProcedures(conn, catalogName, schemaName, null);
                List<Segment> children = new ArrayList<Segment>(procedures.size());

                for (ProcedureMetadata procedure : procedures) {
                    children.add(pathFactory.createSegment(procedure.getName()));
                }

                return new DefaultPathNode(nodePath, properties, children);
            } catch (JdbcMetadataException se) {
                throw new RepositorySourceException(JdbcMetadataI18n.couldNotGetProcedureNames.text(catalogName, schemaName), se);
            } finally {
                closeConnection(conn);
            }
        }

        private PathNode procedureNodeFor( List<Segment> segments ) throws RepositorySourceException {
            assert segments != null;
            assert segments.size() == 4;
            assert PROCEDURES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName());

            ExecutionContext context = source.getRepositoryContext().getExecutionContext();
            PathFactory pathFactory = context.getValueFactories().getPathFactory();
            PropertyFactory propFactory = context.getPropertyFactory();

            Path nodePath = pathFactory.createAbsolutePath(segments);

            Connection conn = getConnection();
            String catalogName = segments.get(0).getName().getLocalName();
            if (catalogName.equals(source.getDefaultCatalogName())) catalogName = null;

            String schemaName = segments.get(1).getName().getLocalName();
            if (schemaName.equals(source.getDefaultSchemaName())) schemaName = null;

            String procedureName = segments.get(3).getName().getLocalName();

            try {
                MetadataCollector meta = source.getMetadataCollector();

                List<ProcedureMetadata> procedures = meta.getProcedures(conn, catalogName, schemaName, procedureName);

                // Make sure that the table exists in the given catalog and schema
                if (procedures.isEmpty()) {
                    return null;
                }

                /*
                 * Some RDMSes support overloaded procedures and thus can return multiple records for the
                 * same procedure name in the same catalog and schema (e.g., HSQLDB and the Math.abs procedure).
                 * 
                 * That means that:
                 *  1. CollectorMetadata.getProcedures(...) needs to consider overloaded procedures when determining
                 *     the stable order in which the procedures should be returned
                 *  2. Procedure nodes can have an SNS index > 1  
                 */
                if (segments.get(3).getIndex() > procedures.size()) {
                    return null;
                }

                ProcedureMetadata procedure = procedures.get(segments.get(3).getIndex() - 1);

                Map<Name, Property> properties = new HashMap<Name, Property>();
                Name propName;
                propName = JcrLexicon.PRIMARY_TYPE;
                properties.put(propName, propFactory.create(propName, JcrNtLexicon.UNSTRUCTURED));
                propName = JcrLexicon.MIXIN_TYPES;
                properties.put(propName, propFactory.create(propName, JdbcMetadataLexicon.PROCEDURE));

                if (procedure.getDescription() != null) {
                    propName = JdbcMetadataLexicon.DESCRIPTION;
                    properties.put(propName, propFactory.create(propName, procedure.getDescription()));
                }
                propName = JdbcMetadataLexicon.PROCEDURE_RETURN_TYPE;
                properties.put(propName, propFactory.create(propName, procedure.getType()));

                return new DefaultPathNode(nodePath, properties, Collections.<Segment>emptyList());
            } catch (JdbcMetadataException se) {
                throw new RepositorySourceException(JdbcMetadataI18n.couldNotGetProcedure.text(catalogName,
                                                                                               schemaName,
                                                                                               procedureName), se);
            } finally {
                closeConnection(conn);
            }
        }

        private PathNode columnNodeFor( List<Segment> segments ) throws RepositorySourceException {
            assert segments != null;
            assert segments.size() == 5;
            assert TABLES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName());

            ExecutionContext context = source.getRepositoryContext().getExecutionContext();
            PathFactory pathFactory = context.getValueFactories().getPathFactory();
            PropertyFactory propFactory = context.getPropertyFactory();

            Path nodePath = pathFactory.createAbsolutePath(segments);

            Connection conn = getConnection();
            String catalogName = segments.get(0).getName().getLocalName();
            if (catalogName.equals(source.getDefaultCatalogName())) catalogName = null;

            String schemaName = segments.get(1).getName().getLocalName();
            if (schemaName.equals(source.getDefaultSchemaName())) schemaName = null;

            String tableName = segments.get(3).getName().getLocalName();
            String columnName = segments.get(4).getName().getLocalName();

            try {
                MetadataCollector meta = source.getMetadataCollector();

                List<ColumnMetadata> columns = meta.getColumns(conn, catalogName, schemaName, tableName, columnName);

                // Make sure that the column exists in the given table, catalog, and schema
                if (columns.isEmpty()) {
                    return null;
                }

                assert columns.size() == 1 : "Duplicate column named " + columnName;
                ColumnMetadata column = columns.get(0);

                Map<Name, Property> properties = new HashMap<Name, Property>();
                Name propName;
                propName = JcrLexicon.PRIMARY_TYPE;
                properties.put(propName, propFactory.create(propName, JcrNtLexicon.UNSTRUCTURED));
                propName = JcrLexicon.MIXIN_TYPES;
                properties.put(propName, propFactory.create(propName, JdbcMetadataLexicon.COLUMN));

                propName = JdbcMetadataLexicon.JDBC_DATA_TYPE;
                properties.put(propName, propFactory.create(propName, column.getJdbcDataType()));
                propName = JdbcMetadataLexicon.TYPE_NAME;
                properties.put(propName, propFactory.create(propName, column.getTypeName()));
                propName = JdbcMetadataLexicon.COLUMN_SIZE;
                properties.put(propName, propFactory.create(propName, column.getColumnSize()));
                propName = JdbcMetadataLexicon.DECIMAL_DIGITS;
                properties.put(propName, propFactory.create(propName, column.getDecimalDigits()));
                propName = JdbcMetadataLexicon.RADIX;
                properties.put(propName, propFactory.create(propName, column.getRadix()));
                if (column.getNullable() != null) {
                    propName = JdbcMetadataLexicon.NULLABLE;
                    properties.put(propName, propFactory.create(propName, column.getNullable()));
                }
                if (column.getDescription() != null) {
                    propName = JdbcMetadataLexicon.DESCRIPTION;
                    properties.put(propName, propFactory.create(propName, column.getDescription()));
                }
                if (column.getDefaultValue() != null) {
                    propName = JdbcMetadataLexicon.DEFAULT_VALUE;
                    properties.put(propName, propFactory.create(propName, column.getDefaultValue()));
                }
                propName = JdbcMetadataLexicon.LENGTH;
                properties.put(propName, propFactory.create(propName, column.getLength()));
                propName = JdbcMetadataLexicon.ORDINAL_POSITION;
                properties.put(propName, propFactory.create(propName, column.getOrdinalPosition()));
                if (column.getScopeCatalogName() != null) {
                    propName = JdbcMetadataLexicon.SCOPE_CATALOG_NAME;

                    properties.put(propName, propFactory.create(propName, column.getScopeCatalogName()));
                }
                if (column.getScopeSchemaName() != null) {
                    propName = JdbcMetadataLexicon.SCOPE_SCHEMA_NAME;
                    properties.put(propName, propFactory.create(propName, column.getScopeSchemaName()));
                }
                if (column.getScopeTableName() != null) {
                    propName = JdbcMetadataLexicon.SCOPE_TABLE_NAME;
                    properties.put(propName, propFactory.create(propName, column.getScopeTableName()));
                }
                if (column.getSourceJdbcDataType() != null) {
                    propName = JdbcMetadataLexicon.SOURCE_JDBC_DATA_TYPE;
                    properties.put(propName, propFactory.create(propName, column.getSourceJdbcDataType()));
                }
                return new DefaultPathNode(nodePath, properties, Collections.<Segment>emptyList());
            } catch (JdbcMetadataException se) {
                throw new RepositorySourceException(JdbcMetadataI18n.couldNotGetColumn.text(catalogName,
                                                                                            schemaName,
                                                                                            tableName,
                                                                                            columnName), se);
            } finally {
                closeConnection(conn);
            }
        }

        public PathNode getRoot() throws RepositorySourceException {
            List<Segment> catalogNames = new LinkedList<Segment>();
            ExecutionContext context = source.getRepositoryContext().getExecutionContext();
            PathFactory pathFactory = context.getValueFactories().getPathFactory();

            Connection conn = getConnection();
            try {
                MetadataCollector meta = source.getMetadataCollector();

                for (String catalogName : meta.getCatalogNames(conn)) {
                    if (catalogName.length() > 0) {
                        catalogNames.add(pathFactory.createSegment(catalogName));
                    }
                }

                if (catalogNames.isEmpty()) {
                    // This database must not support catalogs
                    catalogNames.add(pathFactory.createSegment(source.getDefaultCatalogName()));
                }

                return new RootNode(catalogNames);
            } catch (JdbcMetadataException se) {
                throw new RepositorySourceException(JdbcMetadataI18n.couldNotGetCatalogNames.text(), se);
            } finally {
                closeConnection(conn);
            }
        }

        /**
         * This connector does not support connector-level, persistent locking of nodes.
         * 
         * @param node
         * @param lockScope
         * @param lockTimeoutInMillis
         * @throws LockFailedException
         */
        public void lockNode( PathNode node,
                              LockScope lockScope,
                              long lockTimeoutInMillis ) throws LockFailedException {
            // Locking is not supported by this connector
        }

        /**
         * This connector does not support connector-level, persistent locking of nodes.
         * 
         * @param node the node to be unlocked
         */
        public void unlockNode( PathNode node ) {
            // Locking is not supported by this connector
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.connector.map.MapWorkspace#query(org.jboss.dna.graph.ExecutionContext,
         *      org.jboss.dna.graph.request.AccessQueryRequest)
         */
        public QueryResults query( ExecutionContext context,
                                   AccessQueryRequest accessQuery ) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.connector.map.MapWorkspace#search(org.jboss.dna.graph.ExecutionContext, java.lang.String)
         */
        public QueryResults search( ExecutionContext context,
                                    String fullTextSearchExpression ) {
            return null;
        }
    }

    @SuppressWarnings( "synthetic-access" )
    private class RootNode implements PathNode {
        private final List<Segment> catalogNames;

        private RootNode( List<Segment> catalogNames ) {
            this.catalogNames = catalogNames;
        }

        public List<Segment> getChildSegments() {
            return catalogNames;
        }

        public Path getPath() {
            ExecutionContext context = source.getRepositoryContext().getExecutionContext();
            return context.getValueFactories().getPathFactory().createRootPath();
        }

        public Map<Name, Property> getProperties() {
            return rootNodeProperties;
        }

        public Property getProperty( ExecutionContext context,
                                     String name ) {
            NameFactory nameFactory = context.getValueFactories().getNameFactory();
            return rootNodeProperties.get(nameFactory.create(name));
        }

        public Property getProperty( Name name ) {
            return rootNodeProperties.get(name);
        }

        public Set<Name> getUniqueChildNames() {
            Set<Name> childNames = new HashSet<Name>(catalogNames.size());

            for (Segment catalogName : catalogNames) {
                childNames.add(catalogName.getName());
            }
            return childNames;
        }
    }

}
