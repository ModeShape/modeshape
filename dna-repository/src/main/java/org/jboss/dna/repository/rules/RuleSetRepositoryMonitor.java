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
package org.jboss.dna.repository.rules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.collection.SimpleProblems;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.repository.RepositoryI18n;
import org.jboss.dna.repository.observation.NodeChange;
import org.jboss.dna.repository.observation.NodeChangeListener;
import org.jboss.dna.repository.observation.NodeChanges;
import org.jboss.dna.repository.observation.ObservationService;
import org.jboss.dna.repository.util.JcrExecutionContext;
import org.jboss.dna.repository.util.JcrTools;

/**
 * A component that can listen to a JCR repository and keep the {@link RuleSet} instances of a {@link RuleService} synchronized
 * with that repository.
 * <p>
 * This class is a {@link NodeChangeListener} that can {@link ObservationService#addListener(NodeChangeListener) subscribe} to
 * changes in one or more JCR repositories being monitored by an {@link ObservationService}. As changes under the rule sets
 * branch are discovered, they are processed asynchronously. This ensure that the processing of the repository contents does not
 * block the other listeners of the {@link ObservationService}.
 * </p>
 * 
 * @author Randall Hauch
 */
@ThreadSafe
public class RuleSetRepositoryMonitor implements NodeChangeListener {

    public static final String DEFAULT_JCR_ABSOLUTE_PATH = "/dna:system/dna:ruleSets/";

    protected static final String JCR_PATH_DELIM = "/";

    private final JcrExecutionContext executionContext;
    private final RuleService ruleService;
    private final String jcrAbsolutePath;
    private final Pattern ruleSetNamePattern;
    private final ExecutorService executorService;
    private Logger logger;

    /**
     * Create an instance that can listen to the {@link RuleSet} definitions stored in a JCR repository and ensure that the
     * {@link RuleSet} instances of a {@link RuleService} reflect the definitions in the repository.
     * 
     * @param ruleService the rule service that should be kept in sync with the JCR repository.
     * @param jcrAbsolutePath the absolute path to the branch where the rule sets are defined; if null or empty, the
     *        {@link #DEFAULT_JCR_ABSOLUTE_PATH default path} is used
     * @param executionContext the context in which this monitor is to execute
     * @throws IllegalArgumentException if the rule service or execution context is null, or if the supplied
     *         <code>jcrAbsolutePath</code> is invalid
     */
    public RuleSetRepositoryMonitor( RuleService ruleService,
                                     String jcrAbsolutePath,
                                     JcrExecutionContext executionContext ) {
        CheckArg.isNotNull(ruleService, "rule service");
        CheckArg.isNotNull(executionContext, "execution context");
        this.ruleService = ruleService;
        this.executionContext = executionContext;
        this.executorService = Executors.newSingleThreadExecutor();
        this.logger = Logger.getLogger(this.getClass());
        if (jcrAbsolutePath != null) jcrAbsolutePath = jcrAbsolutePath.trim();
        this.jcrAbsolutePath = jcrAbsolutePath != null && jcrAbsolutePath.length() != 0 ? jcrAbsolutePath : DEFAULT_JCR_ABSOLUTE_PATH;
        try {
            // Create the pattern to extract the rule set name from the absolute path ...
            String leadingPath = this.jcrAbsolutePath;
            if (!leadingPath.endsWith(JCR_PATH_DELIM)) leadingPath = leadingPath + JCR_PATH_DELIM;
            this.ruleSetNamePattern = Pattern.compile(leadingPath + "([^/]+)/?.*");
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException(
                                               RepositoryI18n.unableToBuildRuleSetRegularExpressionPattern.text(e.getPattern(),
                                                                                                                jcrAbsolutePath,
                                                                                                                e.getDescription()));
        }
    }

    /**
     * @return ruleService
     */
    public RuleService getRuleService() {
        return this.ruleService;
    }

    /**
     * @return jcrAbsolutePath
     */
    public String getAbsolutePathToRuleSets() {
        return this.jcrAbsolutePath;
    }

    /**
     * @return logger
     */
    public Logger getLogger() {
        return this.logger;
    }

    /**
     * @param logger Sets logger to the specified value.
     */
    public void setLogger( Logger logger ) {
        this.logger = logger;
    }

    /**
     * {@inheritDoc}
     */
    public void onNodeChanges( NodeChanges changes ) {
        final Map<String, Set<String>> ruleSetNamesByWorkspaceName = new HashMap<String, Set<String>>();
        for (NodeChange nodeChange : changes) {
            if (nodeChange.isNotOnPath(this.jcrAbsolutePath)) continue;
            // Use a regular expression on the absolute path to get the name of the rule set that is affected ...
            Matcher matcher = this.ruleSetNamePattern.matcher(nodeChange.getAbsolutePath());
            if (!matcher.matches()) continue;
            String ruleSetName = matcher.group(1);
            // Record the repository name ...
            String workspaceName = nodeChange.getRepositoryWorkspaceName();
            Set<String> ruleSetNames = ruleSetNamesByWorkspaceName.get(workspaceName);
            if (ruleSetNames == null) {
                ruleSetNames = new HashSet<String>();
                ruleSetNamesByWorkspaceName.put(workspaceName, ruleSetNames);
            }
            // Record the rule set name ...
            ruleSetNames.add(ruleSetName);

        }
        if (ruleSetNamesByWorkspaceName.isEmpty()) return;
        // Otherwise there are changes, so submit the names to the executor service ...
        this.executorService.execute(new Runnable() {

            public void run() {
                processRuleSets(ruleSetNamesByWorkspaceName);
            }
        });
    }

    /**
     * Process the rule sets given by the supplied names, keyed by the repository workspace name.
     * 
     * @param ruleSetNamesByWorkspaceName the set of rule set names keyed by the repository workspace name
     */
    protected void processRuleSets( Map<String, Set<String>> ruleSetNamesByWorkspaceName ) {
        final JcrTools tools = this.executionContext.getTools();
        final String relPathToRuleSets = getAbsolutePathToRuleSets().substring(1);
        for (Map.Entry<String, Set<String>> entry : ruleSetNamesByWorkspaceName.entrySet()) {
            String workspaceName = entry.getKey();
            Session session = null;
            try {
                session = this.executionContext.getSessionFactory().createSession(workspaceName);
                // Look up the node that represents the parent of the rule set nodes ...
                Node ruleSetsNode = session.getRootNode().getNode(relPathToRuleSets);

                for (String ruleSetName : entry.getValue()) {
                    // Look up the node that represents the rule set...
                    if (ruleSetsNode.hasNode(ruleSetName)) {
                        // We don't handle multiple siblings with the same name, so this should grab the first one ...
                        Node ruleSetNode = ruleSetsNode.getNode(ruleSetName);
                        RuleSet ruleSet = buildRuleSet(ruleSetName, ruleSetNode, tools);
                        if (ruleSet != null) {
                            // Only do something if the RuleSet was instantiated ...
                            getRuleService().addRuleSet(ruleSet);
                        }
                    } else {
                        // The node doesn't exist, so remove the rule set ...
                        getRuleService().removeRuleSet(ruleSetName);
                    }
                }
            } catch (RepositoryException e) {
                getLogger().error(e, RepositoryI18n.errorObtainingSessionToRepositoryWorkspace, workspaceName);
            } finally {
                if (session != null) session.logout();
            }
        }
    }

    /**
     * Create a rule set from the supplied node. This is called whenever a branch of the repository is changed.
     * <p>
     * This implementation expects a node of type 'dna:ruleSet' and the following properties (expressed as XPath statements
     * relative to the supplied node):
     * <ul>
     * <li>The {@link RuleSet#getDescription() description} is obtained from the "<code>./@jcr:description</code>" string
     * property. This property is optional.</li>
     * <li>The {@link RuleSet#getComponentClassname() classname} is obtained from the "<code>./@dna:classname</code>" string
     * property. This property is required.</li>
     * <li>The {@link RuleSet#getComponentClasspath() classpath} is obtained from the "<code>./@dna:classpath</code>" string
     * property. This property is optional, and if abscent then the classpath will be assumed from the current context.</li>
     * <li>The {@link RuleSet#getProviderUri() provider URI} is obtained from the "<code>./@dna:serviceProviderUri</code>"
     * string property, and corresponds to the URI of the JSR-94 rules engine service provider. This property is required.</li>
     * <li>The {@link RuleSet#getRuleSetUri() rule set URI} is obtained from the "<code>./@dna:ruleSetUri</code>" string
     * property. This property is optional and defaults to the node name (e.g., "<code>./@jcr:name</code>").</li>
     * <li>The {@link RuleSet#getRules() definition of the rules} is obtained from the "<code>./@dna:rules</code>" string
     * property. This property is required and must be in a form suitable for the JSR-94 rules engine.</li>
     * <li>The {@link RuleSet#getProperties() properties} are obtained from the "<code>./dna:properties[contains(@jcr:mixinTypes,'dna:propertyContainer')]/*[@jcr:nodeType='dna:property']</code>"
     * property nodes, where the name of the property is extracted from the property node's "<code>./@jcr:name</code>" string
     * property and the value of the property is extracted from the property node's "<code>./@dna:propertyValue</code>" string
     * property. Rule set properties are optional.</li>
     * </ul>
     * </p>
     * 
     * @param name the name of the rule set; never null
     * @param ruleSetNode the node representing the rule set; null if the rule set doesn't exist
     * @param tools
     * @return the rule set for the information stored in the repository, or null if the rule set does not exist or has errors
     */
    protected RuleSet buildRuleSet( String name,
                                    Node ruleSetNode,
                                    JcrTools tools ) {
        if (ruleSetNode == null) return null;

        SimpleProblems simpleProblems = new SimpleProblems();
        String description = tools.getPropertyAsString(ruleSetNode, "jcr:description", false, simpleProblems);
        String classname = tools.getPropertyAsString(ruleSetNode, "dna:classname", true, simpleProblems);
        String[] classpath = tools.getPropertyAsStringArray(ruleSetNode, "dna:classpath", false, simpleProblems);
        String providerUri = tools.getPropertyAsString(ruleSetNode, "dna:serviceProviderUri", true, simpleProblems);
        String ruleSetUri = tools.getPropertyAsString(ruleSetNode, "dna:ruleSetUri", true, name, simpleProblems);
        String rules = tools.getPropertyAsString(ruleSetNode, "dna:rules", true, simpleProblems);
        Map<String, Object> properties = tools.loadProperties(ruleSetNode, simpleProblems);
        if (simpleProblems.hasProblems()) {
            // There are problems, so store and save them, and then return null ...
            try {
                if (tools.storeProblems(ruleSetNode, simpleProblems)) ruleSetNode.save();
            } catch (RepositoryException e) {
                this.logger.error(e, RepositoryI18n.errorWritingProblemsOnRuleSet, tools.getReadable(ruleSetNode));
            }
            return null;
        }
        // There are no problems with this rule set, so make sure that there are no persisted problems anymore ...
        try {
            if (tools.removeProblems(ruleSetNode)) ruleSetNode.save();
        } catch (RepositoryException e) {
            this.logger.error(e, RepositoryI18n.errorWritingProblemsOnRuleSet, tools.getReadable(ruleSetNode));
        }
        return new RuleSet(name, description, classname, classpath, providerUri, ruleSetUri, rules, properties);
    }

}
