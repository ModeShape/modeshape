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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.rules.ConfigurationException;
import javax.rules.RuleServiceProviderManager;
import org.drools.jsr94.rules.RuleServiceProviderImpl;
import org.jboss.dna.common.util.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author John Verhaeg
 * @author Randall Hauch
 */
public class RuleEngineTest {

    public static final String TEMP_DATA_PATH = TestUtil.TEMP_DATA_PATH + "RuleEngineTest/";

    private String droolsUrl;
    private String droolsProviderClassname;
    private RuleEngine engine;
    private String validExecutionSetName;
    private URL ruleFile;
    private URL dslFile;
    private Map<String, Object> properties;
    private Reader dslReader;

    static {
        try {
            RuleServiceProviderManager.registerRuleServiceProvider(RuleServiceProviderImpl.RULE_SERVICE_PROVIDER, RuleServiceProviderImpl.class);
        } catch (ConfigurationException e) {
            System.err.println("Unable to regiser Rule Service  Provider " + RuleServiceProviderImpl.RULE_SERVICE_PROVIDER);
        }
    }

    @Before
    public void beforeEach() throws Exception {
        this.droolsUrl = org.drools.jsr94.rules.RuleServiceProviderImpl.RULE_SERVICE_PROVIDER;
        this.droolsProviderClassname = org.drools.jsr94.rules.RuleServiceProviderImpl.class.getName();
        this.engine = new RuleEngine(this.droolsUrl, this.droolsProviderClassname, RuleEngineTest.class.getClassLoader());
        this.validExecutionSetName = "executionSetName";
        this.ruleFile = this.getClass().getResource("/rule_test.dslr");
        this.dslFile = this.getClass().getResource("/rule_test.dsl");
        this.properties = new HashMap<String, Object>();
    }

    @After
    public void afterEach() throws Exception {
        if (dslReader != null) {
            try {
                dslReader.close();
            } finally {
                dslReader = null;
            }
        }
        this.engine.shutdown();
    }

    @Test
    public void shouldRegisterExecutionSetWhenPreviouslyRegistered() throws Exception {
        dslReader = new InputStreamReader(dslFile.openStream());
        properties.put("javax.rules.admin.RuleExecutionSet.source", "drl"); // as opposed to "xml"
        properties.put("javax.rules.admin.RuleExecutionSet.dsl", dslReader);
        boolean found = engine.registerRuleExecutionSet(validExecutionSetName, ruleFile, properties);
        assertThat(found, is(false));
        dslReader.close();

        // Do it again ...
        dslReader = new InputStreamReader(dslFile.openStream());
        properties = new HashMap<String, Object>();
        properties.put("javax.rules.admin.RuleExecutionSet.source", "drl"); // as opposed to "xml"
        properties.put("javax.rules.admin.RuleExecutionSet.dsl", dslReader);
        found = this.engine.registerRuleExecutionSet(validExecutionSetName, ruleFile, properties);
        assertThat(found, is(true));
    }

    @Test
    public void shouldExecuteSimpleRules() throws Exception {
        // Create the execution set ...
        dslReader = new InputStreamReader(dslFile.openStream());
        properties.put("javax.rules.admin.RuleExecutionSet.source", "drl");
        properties.put("javax.rules.admin.RuleExecutionSet.dsl", dslReader);
        boolean found = engine.registerRuleExecutionSet(validExecutionSetName, ruleFile, properties);
        assertThat(found, is(false));
        dslReader.close();

        // Create some simple fact objects ...
        RuleInput info = new RuleInput();
        info.setFileName("someOtherInput.dsl");
        info.setHeader("This is the sequencer header");
        info.setMimeType("text");

        // Run the rules ...
        Set<String> output = new LinkedHashSet<String>();
        Map<String, Object> globals = new HashMap<String, Object>();
        globals.put("output", output);

        List<?> results = engine.executeRules(validExecutionSetName, globals, info);
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

    // @Ignore
    // @Test
    // public void shouldBeConfigurable() throws Exception {
    // RuleEngineConfig config = new RuleEngineConfig();
    // Set<String> sequencers = new LinkedHashSet<String>();
    // config.addGlobal("sequencers", sequencers);
    // config.setProperty("source", "drl");
    // config.setProperty("dsl", new InputStreamReader(RuleEngineTest.class.getResourceAsStream("/rule_test.dsl")));
    // config.addRules(new InputStreamReader(RuleEngineTest.class.getResourceAsStream("/rule_test.dslr")));
    // RuleEngine engine = config.createRuleEngine();
    //
    // RuleInput info = new RuleInput();
    // info.setFileName("sequencer.dsl");
    // info.setHeader("[when]after \"{value}\"=$info: RuleInput( sequenced == true ) from RuleInput( id matches
    // \"{value}\"
    // )");
    // info.setMimeType("text");
    //
    // List<?> results = engine.execute(info);
    // assertThat(results, is(notNullValue()));
    // assertThat(results.isEmpty(), is(false));
    // Object result = results.get(0);
    // assertThat(result, is(instanceOf(RuleInput.class)));
    // assertThat(sequencers.isEmpty(), is(false));
    // Object sequencer = sequencers.iterator().next();
    // assertThat(sequencer, is(instanceOf(String.class)));
    // assertThat(((String)sequencer), is("A"));
    // }
}
