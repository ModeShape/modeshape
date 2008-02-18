/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
