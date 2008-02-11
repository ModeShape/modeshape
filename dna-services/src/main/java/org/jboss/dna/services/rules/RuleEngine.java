/*
 *
 */
package org.jboss.dna.services.rules;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.rules.RuleRuntime;
import javax.rules.RuleServiceProvider;
import javax.rules.RuleServiceProviderManager;
import javax.rules.StatelessRuleSession;
import javax.rules.admin.LocalRuleExecutionSetProvider;
import javax.rules.admin.RuleAdministrator;
import javax.rules.admin.RuleExecutionSet;

/**
 * @author John Verhaeg
 */
public final class RuleEngine {

    private final RuleServiceProvider ruleServiceProvider;
    private final Map<String, Object> globals;
    private String uri;

    RuleEngine( RuleEngineConfig config ) throws Exception {

        this.globals = config.getGlobals();

        // Get the RuleExecutionSetProvider
        config.getClassLoader().loadClass(config.getServiceProviderClassName());
        ruleServiceProvider = RuleServiceProviderManager.getRuleServiceProvider(config.getServiceProviderUri());
        RuleAdministrator ruleAdministrator = ruleServiceProvider.getRuleAdministrator();
        LocalRuleExecutionSetProvider ruleExecutionSetProvider = ruleAdministrator.getLocalRuleExecutionSetProvider(null);

        // Register the RuleExecutionSets with the RuleAdministrator
        for (Reader rule : config.getRules()) {
            RuleExecutionSet ruleExecutionSet = ruleExecutionSetProvider.createRuleExecutionSet(rule, config.getProperties());
            if (uri == null) {
                uri = ruleExecutionSet.getName();
            } else if (!uri.equals(ruleExecutionSet.getName())) {
                throw new RuntimeException("Rules must all be part of the same namespace");
            }
            ruleAdministrator.registerRuleExecutionSet(uri, ruleExecutionSet, null);
        }

        //
        // public RuleEngine( Session session,
        // String nodeType ) throws RepositoryException {
        // EventListener listener = new EventListener() {
        //
        // public void onEvent( EventIterator events ) {
        // }
        // };
        // session.getWorkspace().getObservationManager().addEventListener(listener,
        // Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED,
        // session.getRootNode().getPath(),
        // true,
        // null,
        // new String[] {nodeType},
        // false);
        // }
    }

    public List<?> execute( Object fact ) throws Exception {
        RuleRuntime ruleRuntime = ruleServiceProvider.getRuleRuntime();
        StatelessRuleSession session = (StatelessRuleSession)ruleRuntime.createRuleSession(uri, globals, RuleRuntime.STATELESS_SESSION_TYPE);
        List<?> results = null;
        try {
            List<Object> facts = new ArrayList<Object>();
            facts.add(fact);
            results = session.executeRules(facts);
        } finally {
            session.release();
        }
        return results;
    }
}
