package org.modeshape.jcr.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Replicates JCR 2.0's Repository interface.
 */
public interface Repository extends javax.jcr.Repository {

    static final String REPOSITORY_NAME = "custom.rep.name";

    static final String REPOSITORY_WORKSPACES = "custom.rep.workspace.names";

    /**
     * An immutable collection of "standard" descriptors, as defined in the JSR-283 specification.
     */
    @SuppressWarnings( "deprecation" )
    public static final Set<String> STANDARD_DESCRIPTORS = Collections.unmodifiableSet(new HashSet<String>(
                                                                                                           Arrays.asList(new String[] {
                                                                                                               LEVEL_1_SUPPORTED,
                                                                                                               LEVEL_2_SUPPORTED,
                                                                                                               OPTION_LOCKING_SUPPORTED,
                                                                                                               OPTION_OBSERVATION_SUPPORTED,
                                                                                                               OPTION_QUERY_SQL_SUPPORTED,
                                                                                                               OPTION_TRANSACTIONS_SUPPORTED,
                                                                                                               OPTION_VERSIONING_SUPPORTED,
                                                                                                               QUERY_XPATH_DOC_ORDER,
                                                                                                               QUERY_XPATH_POS_INDEX,
                                                                                                               WRITE_SUPPORTED,
                                                                                                               IDENTIFIER_STABILITY,
                                                                                                               OPTION_XML_IMPORT_SUPPORTED,
                                                                                                               OPTION_XML_EXPORT_SUPPORTED,
                                                                                                               OPTION_UNFILED_CONTENT_SUPPORTED,
                                                                                                               OPTION_SIMPLE_VERSIONING_SUPPORTED,
                                                                                                               OPTION_ACTIVITIES_SUPPORTED,
                                                                                                               OPTION_BASELINES_SUPPORTED,
                                                                                                               OPTION_ACCESS_CONTROL_SUPPORTED,
                                                                                                               OPTION_LOCKING_SUPPORTED,
                                                                                                               OPTION_OBSERVATION_SUPPORTED,
                                                                                                               OPTION_JOURNALED_OBSERVATION_SUPPORTED,
                                                                                                               OPTION_RETENTION_SUPPORTED,
                                                                                                               OPTION_LIFECYCLE_SUPPORTED,
                                                                                                               OPTION_TRANSACTIONS_SUPPORTED,
                                                                                                               OPTION_WORKSPACE_MANAGEMENT_SUPPORTED,
                                                                                                               OPTION_NODE_AND_PROPERTY_WITH_SAME_NAME_SUPPORTED,
                                                                                                               OPTION_UPDATE_PRIMARY_NODE_TYPE_SUPPORTED,
                                                                                                               OPTION_UPDATE_MIXIN_NODE_TYPES_SUPPORTED,
                                                                                                               OPTION_SHAREABLE_NODES_SUPPORTED,
                                                                                                               OPTION_NODE_TYPE_MANAGEMENT_SUPPORTED,
                                                                                                               NODE_TYPE_MANAGEMENT_INHERITANCE,
                                                                                                               NODE_TYPE_MANAGEMENT_PRIMARY_ITEM_NAME_SUPPORTED,
                                                                                                               NODE_TYPE_MANAGEMENT_ORDERABLE_CHILD_NODES_SUPPORTED,
                                                                                                               NODE_TYPE_MANAGEMENT_RESIDUAL_DEFINITIONS_SUPPORTED,
                                                                                                               NODE_TYPE_MANAGEMENT_AUTOCREATED_DEFINITIONS_SUPPORTED,
                                                                                                               NODE_TYPE_MANAGEMENT_SAME_NAME_SIBLINGS_SUPPORTED,
                                                                                                               NODE_TYPE_MANAGEMENT_PROPERTY_TYPES,
                                                                                                               NODE_TYPE_MANAGEMENT_OVERRIDES_SUPPORTED,
                                                                                                               NODE_TYPE_MANAGEMENT_MULTIVALUED_PROPERTIES_SUPPORTED,
                                                                                                               NODE_TYPE_MANAGEMENT_MULTIPLE_BINARY_PROPERTIES_SUPPORTED,
                                                                                                               NODE_TYPE_MANAGEMENT_VALUE_CONSTRAINTS_SUPPORTED,
                                                                                                               NODE_TYPE_MANAGEMENT_UPDATE_IN_USE_SUPORTED,
                                                                                                               QUERY_LANGUAGES,
                                                                                                               QUERY_STORED_QUERIES_SUPPORTED,
                                                                                                               QUERY_FULL_TEXT_SEARCH_SUPPORTED,
                                                                                                               QUERY_JOINS,
                                                                                                               SPEC_NAME_DESC,
                                                                                                               SPEC_VERSION_DESC,
                                                                                                               REP_NAME_DESC,
                                                                                                               REP_VENDOR_DESC,
                                                                                                               REP_VENDOR_URL_DESC,
                                                                                                               REP_VERSION_DESC,})));

}
