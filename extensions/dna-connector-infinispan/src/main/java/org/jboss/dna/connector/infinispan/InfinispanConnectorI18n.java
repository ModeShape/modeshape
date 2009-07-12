package org.jboss.dna.connector.infinispan;

import java.util.Locale;
import java.util.Set;
import org.jboss.dna.common.i18n.I18n;

public final class InfinispanConnectorI18n {

    public static I18n connectorName;
    public static I18n nodeDoesNotExist;
    public static I18n propertyIsRequired;
    public static I18n errorSerializingCachePolicyInSource;
    public static I18n objectFoundInJndiWasNotCacheManager;
    // public static I18n unableToCloneWorkspaces;
    // public static I18n unableToCreateWorkspaces;
    public static I18n unableToCreateWorkspace;
    public static I18n workspaceAlreadyExists;
    public static I18n workspaceDoesNotExist;
    public static I18n workspaceNameWasNotValidConfiguration;
    public static I18n defaultCacheManagerConfigurationNameWasNotValidConfiguration;

    
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
