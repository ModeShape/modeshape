/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.dna.common.component.ClassLoaderFactory;
import org.jboss.dna.common.component.StandardClassLoaderFactory;
import org.jboss.dna.common.util.IoUtil;
import org.jboss.dna.common.util.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class RuleServiceTest {

    protected static final String DROOLS_PROVIDER_CLASSNAME = "org.drools.jsr94.rules.RuleServiceProviderImpl";
    protected static final String DROOLS_PROVIDER_URI = "http://drools.org/";

    private RuleService ruleService;
    private ClassLoaderFactory classLoaderFactory;

    protected RuleSet createDroolsRuleSet( String desc ) throws IOException {
        String name = "SampleRuleSet";
        String description = desc != null ? desc : "This is a sample rule set that uses Drools";
        String providerClassname = DROOLS_PROVIDER_CLASSNAME;
        String providerUri = DROOLS_PROVIDER_URI;
        String[] providerClasspath = new String[] {};
        String ruleSetUri = null;
        URL ruleFile = this.getClass().getResource("/rule_test.dslr");
        URL dslFile = this.getClass().getResource("/rule_test.dsl");
        String rules = IoUtil.read(ruleFile.openStream());
        byte[] dslBytes = IoUtil.readBytes(dslFile.openStream());
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("javax.rules.admin.RuleExecutionSet.source", "drl"); // as opposed to "xml"
        properties.put("javax.rules.admin.RuleExecutionSet.dsl", dslBytes);
        return new RuleSet(name, description, providerClassname, providerClasspath, providerUri, ruleSetUri, rules, properties);
    }

    protected RuleSet createRuleSetWithInvalidProvider() throws IOException {
        String name = "SampleRuleSet"; // same name as Drools rule set
        String description = "This is a sample rule set that uses Drools";
        String providerClassname = DROOLS_PROVIDER_CLASSNAME;
        String providerUri = DROOLS_PROVIDER_URI + "this/is/incorrect";
        String[] providerClasspath = new String[] {};
        String ruleSetUri = null;
        URL ruleFile = this.getClass().getResource("/rule_test.dslr");
        URL dslFile = this.getClass().getResource("/rule_test.dsl");
        String rules = IoUtil.read(ruleFile.openStream());
        byte[] dslBytes = IoUtil.readBytes(dslFile.openStream());
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("javax.rules.admin.RuleExecutionSet.source", "drl"); // as opposed to "xml"
        properties.put("javax.rules.admin.RuleExecutionSet.dsl", dslBytes);
        return new RuleSet(name, description, providerClassname, providerClasspath, providerUri, ruleSetUri, rules, properties);
    }

    @Before
    public void beforeEach() {
        this.ruleService = new RuleService();
        this.classLoaderFactory = new StandardClassLoaderFactory();
    }

    @After
    public void afterEach() {
        this.ruleService.getAdministrator().shutdown();
    }

    @Test
    public void shouldHaveDefaultClasspathFactoryUponConstruction() {
        assertThat(ruleService.getClassLoaderFactory(), is(notNullValue()));
        assertThat(ruleService.getClassLoaderFactory(), is(sameInstance(RuleService.DEFAULT_CLASSLOADER_FACTORY)));
    }

    @Test
    public void shouldHaveLoggerUponConstruction() {
        assertThat(ruleService.getLogger(), is(notNullValue()));
    }

    @Test
    public void shouldHaveEmptyRuleSetCollectionUponConstruction() {
        assertThat(ruleService.getRuleSets(), is(notNullValue()));
        assertThat(ruleService.getRuleSets().isEmpty(), is(true));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldReturnUnmodifiableCollectionOfRuleSets() {
        ruleService.getRuleSets().add(null);
    }

    @Test
    public void shouldSetClassLoaderFactoryToDefaultWhenPassedNull() {
        assertThat(ruleService.getClassLoaderFactory(), is(sameInstance(RuleService.DEFAULT_CLASSLOADER_FACTORY)));
        ruleService.setClassLoaderFactory(classLoaderFactory);
        assertThat(ruleService.getClassLoaderFactory(), is(sameInstance(classLoaderFactory)));
        ruleService.setClassLoaderFactory(null);
        assertThat(ruleService.getClassLoaderFactory(), is(sameInstance(RuleService.DEFAULT_CLASSLOADER_FACTORY)));
    }

    @Test
    public void shouldSetLoggerToDefaultWhenPassedNull() {
        Logger defaultLogger = ruleService.getLogger();
        assertThat(ruleService.getLogger(), is(notNullValue()));
        Logger orgLogger = Logger.getLogger("org");
        ruleService.setLogger(orgLogger);
        assertThat(ruleService.getLogger().getName(), is(orgLogger.getName()));
        assertThat(ruleService.getLogger(), is(sameInstance(orgLogger)));
        ruleService.setLogger(null);
        assertThat(ruleService.getLogger().getName(), is(defaultLogger.getName()));
        assertThat(ruleService.getLogger(), is(not(orgLogger)));
    }

    @Test( expected = InvalidRuleSetException.class )
    public void shouldNotAddInvalidRuleSet() throws Exception {
        ruleService.addRuleSet(createRuleSetWithInvalidProvider());

    }

    @Test
    public void shouldAddValidRuleSet() throws Exception {
        int numRuleSetsBefore = ruleService.getRuleSets().size();
        RuleSet validRuleSet = createDroolsRuleSet(null);
        assertThat(ruleService.addRuleSet(validRuleSet), is(true));
        assertThat(ruleService.getRuleSets().size(), is(numRuleSetsBefore + 1));
    }

    @Test
    public void shouldNotUpdateValidRuleSetWithInvalidRuleSet() throws Exception {
        RuleSet validRuleSet = createDroolsRuleSet(null);
        final String ruleSetName = validRuleSet.getName();

        assertThat(ruleService.addRuleSet(validRuleSet), is(true));
        assertThat(ruleService.getRuleSets().size(), is(1));
        assertThat(ruleService.getRuleSets().iterator().next(), is(validRuleSet));
        // Verify this rule set works ...
        executeRuleSet(ruleSetName);

        // Try to update the rule set with something that's invalid ...
        RuleSet invalidRuleSet = createRuleSetWithInvalidProvider();
        assertThat(invalidRuleSet.getName(), is(ruleSetName));
        assertThat(invalidRuleSet.getName(), is(validRuleSet.getName()));
        try {
            ruleService.addRuleSet(invalidRuleSet);
            fail("Should not get here");
        } catch (InvalidRuleSetException e) {
            // This is expected since it was a bad rule set ...
        }

        // Verify the original rule set is still there and still works ...
        assertThat(ruleService.getRuleSets().size(), is(1));
        assertThat(ruleService.getRuleSets().iterator().next(), is(validRuleSet));
        executeRuleSet(ruleSetName);
    }

    @Test
    public void shouldModifyExistingRuleSetWhenAddedOnlyWhenChanged() throws Exception {
        int numRuleSetsBefore = ruleService.getRuleSets().size();
        RuleSet validRuleSet = createDroolsRuleSet(null);
        assertThat(ruleService.addRuleSet(validRuleSet), is(true));
        assertThat(ruleService.getRuleSets().size(), is(numRuleSetsBefore + 1));

        // Try to add an unchanged rule set ...
        RuleSet validRuleSet2 = createDroolsRuleSet(null);
        assertThat(validRuleSet2.hasChanged(validRuleSet), is(false));
        assertThat(ruleService.addRuleSet(validRuleSet2), is(false));
        assertThat(ruleService.getRuleSets().size(), is(numRuleSetsBefore + 1));

        // Try to add a changed rule set ...
        RuleSet validRuleSet3 = createDroolsRuleSet("This is an alternative description that causes it to be changed");
        assertThat(validRuleSet3.hasChanged(validRuleSet), is(true));
        assertThat(ruleService.addRuleSet(validRuleSet3), is(true));
        assertThat(ruleService.getRuleSets().size(), is(numRuleSetsBefore + 1));
    }

    @Test
    public void shouldExecuteRuleSet() throws Exception {
        RuleSet validRuleSet = createDroolsRuleSet(null);
        assertThat(ruleService.addRuleSet(validRuleSet), is(true));

        executeRuleSet(validRuleSet.getName());
    }

    protected void executeRuleSet( String ruleSetName ) {
        // Create some simple fact objects ...
        RuleInput info = new RuleInput();
        info.setFileName("someOtherInput.dsl");
        info.setHeader("This is the sequencer header");
        info.setMimeType("text");

        // Run the rules ...
        Set<String> output = new LinkedHashSet<String>();
        Map<String, Object> globals = new HashMap<String, Object>();
        globals.put("output", output);

        List<?> results = ruleService.executeRules(ruleSetName, globals, info);
        assertThat(results, is(notNullValue()));
        assertThat(results.isEmpty(), is(false));
        assertThat(results.size(), is(2));
        assertTrue(results.get(0) instanceof RuleResult || results.get(1) instanceof RuleResult);
        // Object result = results.get(0);
        // assertThat(result, is(instanceOf(RuleResult.class)));
        assertThat(output.isEmpty(), is(false));
        Object sequencer = output.iterator().next();
        assertThat(sequencer, is(instanceOf(String.class)));
        assertThat(((String)sequencer), is("A"));
    }

}
