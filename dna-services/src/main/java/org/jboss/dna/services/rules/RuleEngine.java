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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.rules.RuleRuntime;
import javax.rules.RuleServiceProvider;
import javax.rules.RuleServiceProviderManager;
import javax.rules.StatelessRuleSession;
import javax.rules.admin.LocalRuleExecutionSetProvider;
import javax.rules.admin.RuleAdministrator;
import javax.rules.admin.RuleExecutionSet;
import javax.rules.admin.RuleExecutionSetDeregistrationException;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.SystemFailureException;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.ClassUtil;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.common.util.StringUtil;

/**
 * A simple rules engine, built on top of JSR-94.
 * @author John Verhaeg
 * @author Randall Hauch
 */
@ThreadSafe
public final class RuleEngine {

    @GuardedBy( "this" )
    private RuleServiceProvider ruleServiceProvider;
    private final String serviceProviderUrl;
    private final String serviceProviderClassName;
    private final ClassLoader classLoader;
    private final Logger logger;

    /**
     * Create an instance of the rule engine that uses the JSR-94 service provider given by the supplied information.
     * @param serviceProviderUrl the URL of the service provider
     * @param serviceProviderClassName the class name of the service provider
     * @param classLoader the class loader that should be used to load the service provider; if null, the thread's current
     * {@link Thread#getContextClassLoader() context class loader} is used, or if that is null the class loader used to load this
     * class is used
     * @throws IllegalArgumentException if either of the service provider URL or class name is null or empty, or if the class name
     * does not follow the standard Java class naming conventions.
     */
    public RuleEngine( String serviceProviderUrl, String serviceProviderClassName, ClassLoader classLoader ) {
        ArgCheck.isNotEmpty(serviceProviderUrl, "serviceProviderUrl");
        ArgCheck.isNotEmpty(serviceProviderClassName, "serviceProviderClassName");
        ClassUtil.isFullyQualifiedClassname(serviceProviderClassName);
        this.serviceProviderClassName = serviceProviderClassName;
        this.serviceProviderUrl = serviceProviderUrl;
        if (classLoader == null) classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) classLoader = this.getClass().getClassLoader();
        this.classLoader = classLoader;
        this.logger = Logger.getLogger(this.getClass());
    }

    /**
     * Get the class loader that is used by this rules engine.
     * @return the class loader; never null
     */
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    /**
     * Get the name of the {@link RuleServiceProvider} implementation class.
     * @return the rule service provider implementation class; never null
     */
    public String getServiceProviderClassName() {
        return this.serviceProviderClassName;
    }

    /**
     * Get the {@link RuleServiceProvider}'s URL.
     * @return the URL for the service provider
     */
    public String getServiceProviderUrl() {
        return this.serviceProviderUrl;
    }

    /**
     * Clear this rule engine of all cached state and rule execution sets. Calling this method is analogous to re-creating a new
     * instance of this class.
     */
    public synchronized void clear() {
        this.ruleServiceProvider = null;
    }

    /**
     * Utility method to obtain the rule service provider implementation. This method will cause the implementation to be loaded
     * if it has not yet been.
     * @return the service provider implementation
     */
    protected synchronized RuleServiceProvider getRuleServiceProvider() {
        if (this.ruleServiceProvider == null) {
            try {
                // Use JSR-94 to load the RuleServiceProvider instance ...
                this.classLoader.loadClass(this.serviceProviderClassName);
                this.ruleServiceProvider = RuleServiceProviderManager.getRuleServiceProvider(this.serviceProviderUrl);
                this.logger.debug("Loaded the rule service provider {} ({})", this.serviceProviderUrl, this.serviceProviderClassName);
            } catch (Throwable t) {
                String msg = "Unable to instantiate the service provider {1} ({2})";
                msg = StringUtil.createString(msg, this.serviceProviderUrl, this.serviceProviderClassName);
                throw new SystemFailureException(msg, t);
            }
        }
        return this.ruleServiceProvider;
    }

    /**
     * Register a set of rules as an execution set. This must be done before the rule
     * @param name the name of the rule execution set
     * @param rule the reader to the rule(s)
     * @param properties the properties, or null
     * @return true if an existing rule execution set was replaced by the new rules
     * @throws IllegalArgumentException if the name is null or empty, or if the rule reference is null
     */
    public synchronized boolean registerRuleExecutionSet( String name, Reader rule, Map<String, Object> properties ) {
        ArgCheck.isNotEmpty(name, "name");

        boolean foundExisting = false;
        RuleServiceProvider provider = getRuleServiceProvider();
        assert provider != null;
        RuleAdministrator ruleAdministrator = null;
        RuleExecutionSet executionSet = null;
        try {
            ruleAdministrator = provider.getRuleAdministrator();

            // Create and register the rule execution set (do this before deregistering, in case there is a problem)...
            LocalRuleExecutionSetProvider ruleExecutionSetProvider = ruleAdministrator.getLocalRuleExecutionSetProvider(null);
            executionSet = ruleExecutionSetProvider.createRuleExecutionSet(rule, properties);
        } catch (Throwable t) {
            String msg = "Error creating rule execution set '{1}'";
            msg = StringUtil.createString(msg, name);
            throw new SystemFailureException(msg, t);
        }
        try {
            // Make sure it's not registered (deregister if it exists) ...
            try {
                ruleAdministrator.deregisterRuleExecutionSet(name, properties);
                foundExisting = true;
                this.logger.debug("Deregistered the rule execution set '{}'", name);
            } catch (RuleExecutionSetDeregistrationException e) {
                // do nothing ...
            }
            ruleAdministrator.registerRuleExecutionSet(name, executionSet, null);
            this.logger.debug("Registered the rule execution set '{}'", name);
        } catch (Throwable t) {
            String msg = "Error registering rule execution set '{1}'";
            msg = StringUtil.createString(msg, name);
            throw new SystemFailureException(msg, t);
        }
        return foundExisting;
    }

    public boolean registerRuleExecutionSet( String name, URL rule, Map<String, Object> properties ) {
        InputStream ruleStream = null;
        try {
            ruleStream = rule.openStream();
            return registerRuleExecutionSet(name, new InputStreamReader(ruleStream), properties);
        } catch (IOException e) {
            String msg = "Error reading rule from {1} while creating and registering execution set '{2}'";
            msg = StringUtil.createString(msg, rule, name);
            throw new SystemFailureException(msg, e);
        } finally {
            if (ruleStream != null) {
                try {
                    ruleStream.close();
                } catch (IOException e) {
                    // Ignore ...
                }
            }
        }
    }

    public boolean registerRuleExecutionSet( String name, String rule, Map<String, Object> properties ) {
        return registerRuleExecutionSet(name, new StringReader(rule), properties);
    }

    public boolean registerRuleExecutionSet( String name, String[] rules, Map<String, Object> properties ) {
        StringBuilder sb = new StringBuilder();
        for (String rule : rules) {
            sb.append(rule);
            sb.append("\n");
        }
        return registerRuleExecutionSet(name, sb.toString(), properties);
    }

    public List<?> executeRules( String executionSetName, Map<String, Object> globals, Object... facts ) {
        List<?> result = null;
        try {
            RuleRuntime ruleRuntime = getRuleServiceProvider().getRuleRuntime();
            StatelessRuleSession session = (StatelessRuleSession)ruleRuntime.createRuleSession(executionSetName, globals, RuleRuntime.STATELESS_SESSION_TYPE);
            try {
                result = session.executeRules(Arrays.asList(facts));
            } finally {
                session.release();
            }
            if (this.logger.isTraceEnabled()) {
                String msg = "Executed rule set '{1}' with globals {2} and facts {3} resulting in {4}";
                msg = StringUtil.createString(msg, executionSetName, StringUtil.readableString(globals), StringUtil.readableString(facts), StringUtil.readableString(result));
                this.logger.trace(msg);
            }
        } catch (Throwable t) {
            String msg = "Error executing rule set '{1}' and with globals {2} and facts {3}";
            msg = StringUtil.createString(msg, executionSetName, StringUtil.readableString(globals), StringUtil.readableString(facts));
            throw new SystemFailureException(msg, t);
        }
        return result;
    }

    public synchronized void shutdown() {
        try {
            RuleServiceProvider provider = getRuleServiceProvider();
            RuleRuntime ruleRuntime = provider.getRuleRuntime();
            RuleAdministrator ruleAdministrator = provider.getRuleAdministrator();
            @SuppressWarnings( "unchecked" ) List<String> registrations = ruleRuntime.getRegistrations();
            for (String name : registrations) {
                try {
                    ruleAdministrator.deregisterRuleExecutionSet(name, null);
                    this.logger.debug("Deregistered the rule execution set '{}'", name);
                } catch (RuleExecutionSetDeregistrationException e) {
                    this.logger.debug("Error while deregistered the rule execution set '{}'", name);
                }
            }
        } catch (Throwable t) {
            String msg = "Error shutting down rules engine";
            throw new SystemFailureException(msg, t);
        }
    }
}
