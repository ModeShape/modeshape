package org.modeshape.connector.meta.jdbc;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.base.PathNode;
import org.modeshape.graph.connector.base.PathWorkspace;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.Path.Segment;

/**
 * Workspace implementation for the JDBC metadata connector.
 */
@NotThreadSafe
class JdbcMetadataWorkspace extends PathWorkspace<PathNode> {

    public final static String TABLES_SEGMENT_NAME = "tables";
    public final static String PROCEDURES_SEGMENT_NAME = "procedures";

    protected final JdbcMetadataRepository repository;

    public JdbcMetadataWorkspace( JdbcMetadataRepository repository,
                                  String name ) {
        super(name, repository.source().getRootNodeUuidObject());
        this.repository = repository;
    }

    @Override
    public PathNode getNode( Path path ) {
        assert path != null;

        PathNode node = null;

        List<Segment> segments = path.getSegmentsList();
        switch (segments.size()) {
            case 0:
                node = getRootNode();
                break;
            case 1:
                node = catalogNodeFor(segments);
                break;
            case 2:
                node = schemaNodeFor(segments);
                break;
            case 3:
                if (TABLES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName())) {
                    node = tablesNodeFor(segments);
                } else if (PROCEDURES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName())) {
                    node = proceduresNodeFor(segments);
                }
                break;
            case 4:
                if (TABLES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName())) {
                    node = tableNodeFor(segments);
                } else if (PROCEDURES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName())) {
                    node = procedureNodeFor(segments);
                }
                break;
            case 5:
                if (TABLES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName())) {
                    node = columnNodeFor(segments);
                }
                break;
            default:
                return null;
        }

        return node;
    }

    private PathNode catalogNodeFor( List<Segment> segments ) throws RepositorySourceException {
        assert segments != null;
        assert segments.size() == 1;

        List<Segment> schemaNames = new LinkedList<Segment>();
        ExecutionContext context = repository.source().getRepositoryContext().getExecutionContext();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        PropertyFactory propFactory = context.getPropertyFactory();

        Path nodePath = pathFactory.createAbsolutePath(segments);

        Connection conn = repository.getConnection();
        String catalogName = segments.get(0).getName().getLocalName();

        try {
            MetadataCollector meta = repository.source().getMetadataCollector();
            if (catalogName.equals(repository.source().getDefaultCatalogName())) catalogName = null;

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
                schemaNames.add(pathFactory.createSegment(repository.source().getDefaultSchemaName()));
            }

            Map<Name, Property> properties = new HashMap<Name, Property>();
            properties.put(JcrLexicon.PRIMARY_TYPE, propFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED));
            properties.put(JcrLexicon.MIXIN_TYPES, propFactory.create(JcrLexicon.MIXIN_TYPES, JdbcMetadataLexicon.CATALOG));

            return new PathNode(null, nodePath.getParent(), nodePath.getLastSegment(), properties, schemaNames);
        } catch (JdbcMetadataException se) {
            throw new RepositorySourceException(JdbcMetadataI18n.couldNotGetSchemaNames.text(catalogName), se);
        } finally {
            repository.closeConnection(conn);
        }
    }

    private PathNode schemaNodeFor( List<Segment> segments ) throws RepositorySourceException {
        assert segments != null;
        assert segments.size() == 2;

        ExecutionContext context = repository.source().getRepositoryContext().getExecutionContext();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        PropertyFactory propFactory = context.getPropertyFactory();

        Path nodePath = pathFactory.createAbsolutePath(segments);

        Connection conn = repository.getConnection();
        String catalogName = segments.get(0).getName().getLocalName();
        if (catalogName.equals(repository.source().getDefaultCatalogName())) catalogName = null;

        String schemaName = segments.get(1).getName().getLocalName();
        if (schemaName.equals(repository.source().getDefaultSchemaName())) schemaName = null;

        try {
            MetadataCollector meta = repository.source().getMetadataCollector();

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
            return new PathNode(null, nodePath.getParent(), nodePath.getLastSegment(), properties, Arrays.asList(children));
        } catch (JdbcMetadataException se) {
            throw new RepositorySourceException(JdbcMetadataI18n.couldNotGetSchemaNames.text(catalogName), se);
        } finally {
            repository.closeConnection(conn);
        }
    }

    private PathNode tablesNodeFor( List<Segment> segments ) throws RepositorySourceException {
        assert segments != null;
        assert segments.size() == 3;
        assert TABLES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName());

        ExecutionContext context = repository.source().getRepositoryContext().getExecutionContext();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        PropertyFactory propFactory = context.getPropertyFactory();

        Path nodePath = pathFactory.createAbsolutePath(segments);

        Connection conn = repository.getConnection();
        String catalogName = segments.get(0).getName().getLocalName();
        if (catalogName.equals(repository.source().getDefaultCatalogName())) catalogName = null;

        String schemaName = segments.get(1).getName().getLocalName();
        if (schemaName.equals(repository.source().getDefaultSchemaName())) schemaName = null;

        try {
            MetadataCollector meta = repository.source().getMetadataCollector();

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

            return new PathNode(null, nodePath.getParent(), nodePath.getLastSegment(), properties, children);
        } catch (JdbcMetadataException se) {
            throw new RepositorySourceException(JdbcMetadataI18n.couldNotGetTableNames.text(catalogName, schemaName), se);
        } finally {
            repository.closeConnection(conn);
        }
    }

    private PathNode tableNodeFor( List<Segment> segments ) throws RepositorySourceException {
        assert segments != null;
        assert segments.size() == 4;
        assert TABLES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName());

        ExecutionContext context = repository.source().getRepositoryContext().getExecutionContext();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        PropertyFactory propFactory = context.getPropertyFactory();

        Path nodePath = pathFactory.createAbsolutePath(segments);

        Connection conn = repository.getConnection();
        String catalogName = segments.get(0).getName().getLocalName();
        if (catalogName.equals(repository.source().getDefaultCatalogName())) catalogName = null;

        String schemaName = segments.get(1).getName().getLocalName();
        if (schemaName.equals(repository.source().getDefaultSchemaName())) schemaName = null;

        String tableName = segments.get(3).getName().getLocalName();

        try {
            MetadataCollector meta = repository.source().getMetadataCollector();

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

            return new PathNode(null, nodePath.getParent(), nodePath.getLastSegment(), properties, children);

        } catch (JdbcMetadataException se) {
            throw new RepositorySourceException(JdbcMetadataI18n.couldNotGetTable.text(catalogName, schemaName, tableName), se);
        } finally {
            repository.closeConnection(conn);
        }
    }

    private PathNode proceduresNodeFor( List<Segment> segments ) throws RepositorySourceException {
        assert segments != null;
        assert segments.size() == 3;
        assert PROCEDURES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName());

        ExecutionContext context = repository.source().getRepositoryContext().getExecutionContext();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        PropertyFactory propFactory = context.getPropertyFactory();

        Path nodePath = pathFactory.createAbsolutePath(segments);

        Connection conn = repository.getConnection();
        String catalogName = segments.get(0).getName().getLocalName();
        if (catalogName.equals(repository.source().getDefaultCatalogName())) catalogName = null;

        String schemaName = segments.get(1).getName().getLocalName();
        if (schemaName.equals(repository.source().getDefaultSchemaName())) schemaName = null;

        try {
            MetadataCollector meta = repository.source().getMetadataCollector();

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

            return new PathNode(null, nodePath.getParent(), nodePath.getLastSegment(), properties, children);
        } catch (JdbcMetadataException se) {
            throw new RepositorySourceException(JdbcMetadataI18n.couldNotGetProcedureNames.text(catalogName, schemaName), se);
        } finally {
            repository.closeConnection(conn);
        }
    }

    private PathNode procedureNodeFor( List<Segment> segments ) throws RepositorySourceException {
        assert segments != null;
        assert segments.size() == 4;
        assert PROCEDURES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName());

        ExecutionContext context = repository.source().getRepositoryContext().getExecutionContext();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        PropertyFactory propFactory = context.getPropertyFactory();

        Path nodePath = pathFactory.createAbsolutePath(segments);

        Connection conn = repository.getConnection();
        String catalogName = segments.get(0).getName().getLocalName();
        if (catalogName.equals(repository.source().getDefaultCatalogName())) catalogName = null;

        String schemaName = segments.get(1).getName().getLocalName();
        if (schemaName.equals(repository.source().getDefaultSchemaName())) schemaName = null;

        String procedureName = segments.get(3).getName().getLocalName();

        try {
            MetadataCollector meta = repository.source().getMetadataCollector();

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

            return new PathNode(null, nodePath.getParent(), nodePath.getLastSegment(), properties, Collections.<Segment>emptyList());
        } catch (JdbcMetadataException se) {
            throw new RepositorySourceException(
                                                JdbcMetadataI18n.couldNotGetProcedure.text(catalogName, schemaName, procedureName),
                                                se);
        } finally {
            repository.closeConnection(conn);
        }
    }

    private PathNode columnNodeFor( List<Segment> segments ) throws RepositorySourceException {
        assert segments != null;
        assert segments.size() == 5;
        assert TABLES_SEGMENT_NAME.equals(segments.get(2).getName().getLocalName());

        ExecutionContext context = repository.source().getRepositoryContext().getExecutionContext();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        PropertyFactory propFactory = context.getPropertyFactory();

        Path nodePath = pathFactory.createAbsolutePath(segments);

        Connection conn = repository.getConnection();
        String catalogName = segments.get(0).getName().getLocalName();
        if (catalogName.equals(repository.source().getDefaultCatalogName())) catalogName = null;

        String schemaName = segments.get(1).getName().getLocalName();
        if (schemaName.equals(repository.source().getDefaultSchemaName())) schemaName = null;

        String tableName = segments.get(3).getName().getLocalName();
        String columnName = segments.get(4).getName().getLocalName();

        try {
            MetadataCollector meta = repository.source().getMetadataCollector();

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
            return new PathNode(null, nodePath.getParent(), nodePath.getLastSegment(), properties,
                                Collections.<Segment>emptyList());
        } catch (JdbcMetadataException se) {
            throw new RepositorySourceException(JdbcMetadataI18n.couldNotGetColumn.text(catalogName,
                                                                                        schemaName,
                                                                                        tableName,
                                                                                        columnName), se);
        } finally {
            repository.closeConnection(conn);
        }
    }

    @Override
    public PathNode getRootNode() throws RepositorySourceException {
        List<Segment> catalogNames = new LinkedList<Segment>();
        ExecutionContext context = repository.source().getRepositoryContext().getExecutionContext();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();

        Connection conn = repository.getConnection();
        try {
            MetadataCollector meta = repository.source().getMetadataCollector();

            for (String catalogName : meta.getCatalogNames(conn)) {
                if (catalogName.length() > 0) {
                    catalogNames.add(pathFactory.createSegment(catalogName));
                }
            }

            if (catalogNames.isEmpty()) {
                // This database must not support catalogs
                catalogNames.add(pathFactory.createSegment(repository.source().getDefaultCatalogName()));
            }

            return new PathNode(repository.source().getRootNodeUuidObject(), null, null, repository.rootNodeProperties(),
                                catalogNames);
        } catch (JdbcMetadataException se) {
            throw new RepositorySourceException(JdbcMetadataI18n.couldNotGetCatalogNames.text(), se);
        } finally {
            repository.closeConnection(conn);
        }
    }

}
