package org.modeshape.connector.disk;

import java.util.Locale;
import java.util.Set;
import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.connector.infinispan*</code> packages.
 */
public final class DiskConnectorI18n {

    public static I18n connectorName;
    public static I18n propertyIsRequired;
    public static I18n errorSerializingCachePolicyInSource;
    public static I18n unableToCreateWorkspace;

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

    static {
        try {
            I18n.initialize(DiskConnectorI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }

    public static Set<Locale> getLocalizationProblemLocales() {
        return I18n.getLocalizationProblemLocales(DiskConnectorI18n.class);
    }

    public static Set<String> getLocalizationProblems() {
        return I18n.getLocalizationProblems(DiskConnectorI18n.class);
    }

    public static Set<String> getLocalizationProblems( Locale locale ) {
        return I18n.getLocalizationProblems(DiskConnectorI18n.class, locale);
    }

}
