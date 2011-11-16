package org.modeshape.connector.disk;

import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.connector.infinispan*</code> packages.
 */
public final class DiskConnectorI18n {

    public static I18n connectorName;
    public static I18n propertyIsRequired;
    public static I18n errorSerializingCachePolicyInSource;
    public static I18n unableToCreateWorkspace;
    public static I18n couldNotCreateLockFile;
    public static I18n problemAcquiringFileLock;
    public static I18n problemReleasingFileLock;

    public static I18n namePropertyDescription;
    public static I18n namePropertyLabel;
    public static I18n namePropertyCategory;
    public static I18n defaultWorkspaceNamePropertyDescription;
    public static I18n defaultWorkspaceNamePropertyLabel;
    public static I18n defaultWorkspaceNamePropertyCategory;
    public static I18n rootNodeUuidPropertyDescription;
    public static I18n rootNodeUuidPropertyLabel;
    public static I18n rootNodeUuidPropertyCategory;
    public static I18n predefinedWorkspaceNamesPropertyDescription;
    public static I18n predefinedWorkspaceNamesPropertyLabel;
    public static I18n predefinedWorkspaceNamesPropertyCategory;
    public static I18n retryLimitPropertyDescription;
    public static I18n retryLimitPropertyLabel;
    public static I18n retryLimitPropertyCategory;
    public static I18n updatesAllowedPropertyDescription;
    public static I18n updatesAllowedPropertyLabel;
    public static I18n updatesAllowedPropertyCategory;
    public static I18n repositoryRootPathPropertyDescription;
    public static I18n repositoryRootPathPropertyLabel;
    public static I18n repositoryRootPathPropertyCategory;
    public static I18n lockFileUsedPropertyDescription;
    public static I18n lockFileUsedPropertyLabel;
    public static I18n lockFileUsedPropertyCategory;
    public static I18n largeValueSizeInBytesPropertyDescription;
    public static I18n largeValueSizeInBytesPropertyLabel;
    public static I18n largeValueSizeInBytesPropertyCategory;
    public static I18n largeValuePathPropertyDescription;
    public static I18n largeValuePathPropertyLabel;
    public static I18n largeValuePathPropertyCategory;

    static {
        try {
            I18n.initialize(DiskConnectorI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }
}
