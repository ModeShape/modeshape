package org.modeshape.connector.infinispan;

import java.util.Locale;
import java.util.Set;
import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.connector.infinispan*</code> packages.
 */
public final class InfinispanConnectorI18n {

    public static I18n connectorName;
    public static I18n propertyIsRequired;
    public static I18n errorSerializingCachePolicyInSource;
    public static I18n objectFoundInJndiWasNotCacheContainer;
    public static I18n unableToCreateWorkspace;
    public static I18n configFileNotFound;
    public static I18n configFileNotValid;

    public static I18n namePropertyDescription;
    public static I18n namePropertyLabel;
    public static I18n namePropertyCategory;
    public static I18n cacheContainerJndiNamePropertyDescription;
    public static I18n cacheContainerJndiNamePropertyLabel;
    public static I18n cacheContainerJndiNamePropertyCategory;
    public static I18n cacheConfigurationNamePropertyDescription;
    public static I18n cacheConfigurationNamePropertyLabel;
    public static I18n cacheConfigurationNamePropertyCategory;
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
    public static I18n remoteInfinispanServerListPropertyDescription;
    public static I18n remoteInfinispanServerListPropertyLabel;
    public static I18n remoteInfinispanServerListPropertyCategory;

    static {
        try {
            I18n.initialize(InfinispanConnectorI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }

    public static Set<Locale> getLocalizationProblemLocales() {
        return I18n.getLocalizationProblemLocales(InfinispanConnectorI18n.class);
    }

    public static Set<String> getLocalizationProblems() {
        return I18n.getLocalizationProblems(InfinispanConnectorI18n.class);
    }

    public static Set<String> getLocalizationProblems( Locale locale ) {
        return I18n.getLocalizationProblems(InfinispanConnectorI18n.class, locale);
    }

}
