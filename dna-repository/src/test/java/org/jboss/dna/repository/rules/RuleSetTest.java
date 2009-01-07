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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.jboss.dna.common.util.IoUtil;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class RuleSetTest {

    private RuleSet ruleSet;
    private String validName;
    private String validDescription;
    private String validClassname;
    private String[] validClasspath;
    private String validProviderUri;
    private String validRuleSetUri;
    private String validRules;
    private Map<String, Object> validProperties;

    @Before
    public void beforeEach() {
        this.validName = "This is a valid name";
        this.validDescription = "This is a valid description";
        this.validClassname = "com.acme.SuperDuper";
        this.validClasspath = new String[] {"classpath1", "classpath2"};
        this.validProviderUri = "http://www.acme.com/super/duper/rules_engine";
        this.validRuleSetUri = "com.something.whatever doesn't really need to be a URI";
        this.validRules = "when something is true then cheer";
        this.validProperties = new HashMap<String, Object>();
        this.validProperties.put("key1", "value1");
        this.validProperties.put("key2", null);
        this.validProperties.put("key3", "value3".getBytes());
        this.ruleSet = new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri,
                                   validRuleSetUri, validRules, validProperties);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullName() {
        new RuleSet(null, validDescription, validClassname, validClasspath, validProviderUri, validRuleSetUri, validRules,
                    validProperties);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyName() {
        new RuleSet("", validDescription, validClassname, validClasspath, validProviderUri, validRuleSetUri, validRules,
                    validProperties);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowBlankName() {
        new RuleSet("  \t ", validDescription, validClassname, validClasspath, validProviderUri, validRuleSetUri, validRules,
                    validProperties);
    }

    @Test
    public void shouldTrimName() {
        validName = "  this is a valid name with leading and trailing whitespace  ";
        ruleSet = new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri, validRuleSetUri,
                              validRules, validProperties);
        assertThat(ruleSet.getName(), is(validName.trim()));
    }

    @Test
    public void shouldAllowNullOrEmptyOrBlankDescriptionAndShouldReplaceWithEmptyString() {
        ruleSet = new RuleSet(validName, null, validClassname, validClasspath, validProviderUri, validRuleSetUri, validRules,
                              validProperties);
        assertThat(ruleSet.getDescription(), is(""));
        ruleSet = new RuleSet(validName, "", validClassname, validClasspath, validProviderUri, validRuleSetUri, validRules,
                              validProperties);
        assertThat(ruleSet.getDescription(), is(""));
        ruleSet = new RuleSet(validName, "  \t ", validClassname, validClasspath, validProviderUri, validRuleSetUri, validRules,
                              validProperties);
        assertThat(ruleSet.getDescription(), is(""));
    }

    @Test
    public void shouldTrimDescription() {
        ruleSet = new RuleSet(validName, "  valid ", validClassname, validClasspath, validProviderUri, validRuleSetUri,
                              validRules, validProperties);
        assertThat(ruleSet.getDescription(), is("valid"));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullClassname() {
        new RuleSet(validName, validDescription, null, validClasspath, validProviderUri, validRuleSetUri, validRules,
                    validProperties);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyClassname() {
        new RuleSet(validName, validDescription, "", validClasspath, validProviderUri, validRuleSetUri, validRules,
                    validProperties);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowBlankClassname() {
        new RuleSet(validName, validDescription, "   ", validClasspath, validProviderUri, validRuleSetUri, validRules,
                    validProperties);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowClassnameThatDoesNotFollowJavaNamingRules() {
        new RuleSet(validName, validDescription, "not a valid classname", validClasspath, validProviderUri, validRuleSetUri,
                    validRules, validProperties);
    }

    @Test
    public void shouldAllowNullOrEmptyClasspath() {
        new RuleSet(validName, validDescription, validClassname, null, validProviderUri, validRuleSetUri, validRules,
                    validProperties);
    }

    @Test
    public void shouldRemoveNullOrBlankClasspathItems() {
        new RuleSet(validName, validDescription, validClassname, new String[] {}, validProviderUri, validRuleSetUri, validRules,
                    validProperties);
    }

    @Test
    public void shouldRemoveDuplicateClasspathItemsInCaseSensitiveManner() {
        validClasspath = new String[] {"path1", "path2", "path1", "path3", "path2"};
        ruleSet = new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri, validRuleSetUri,
                              validRules, validProperties);
        assertThat(ruleSet.getComponentClasspathArray(), is(new String[] {"path1", "path2", "path3"}));

        validClasspath = new String[] {"path1", "path2", "path1", "path3", "Path2"};
        ruleSet = new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri, validRuleSetUri,
                              validRules, validProperties);
        assertThat(ruleSet.getComponentClasspathArray(), is(new String[] {"path1", "path2", "path3", "Path2"}));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullProviderUri() {
        new RuleSet(validName, validDescription, validClassname, validClasspath, null, validRuleSetUri, validRules,
                    validProperties);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyProviderUri() {
        new RuleSet(validName, validDescription, validClassname, validClasspath, "", validRuleSetUri, validRules, validProperties);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowBlankProviderUri() {
        new RuleSet(validName, validDescription, validClassname, validClasspath, "  \t  ", validRuleSetUri, validRules,
                    validProperties);
    }

    @Test
    public void shouldUseNameInPlaceOfNullRuleSetUri() {
        ruleSet = new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri, null, validRules,
                              validProperties);
        assertThat(ruleSet.getRuleSetUri(), is(ruleSet.getName()));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyRuleSetUri() {
        new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri, "", validRules,
                    validProperties);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowBlankRuleSetUri() {
        new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri, "  \t  ", validRules,
                    validProperties);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullRules() {
        new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri, validRuleSetUri, null,
                    validProperties);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyRules() {
        new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri, validRuleSetUri, "",
                    validProperties);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowBlankRules() {
        new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri, validRuleSetUri, "   \t  ",
                    validProperties);
    }

    @Test
    public void shouldAllowNullOrEmptyProperties() {
        validProperties = null;
        ruleSet = new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri, validRuleSetUri,
                              validRules, validProperties);
        assertThat(ruleSet.getProperties(), is(notNullValue()));
    }

    @Test
    public void shouldWrapByteArrayPropertyValuesWithInputStreamInExecutionSetProperties() throws Exception {
        Map<Object, Object> executionSetProps = ruleSet.getExecutionSetProperties();
        assertThat(executionSetProps.size(), is(ruleSet.getProperties().size()));
        assertThat(executionSetProps.keySet(), is((Set<Object>)new HashSet<Object>(ruleSet.getProperties().keySet())));
        Iterator<Map.Entry<Object, Object>> executionSetPropIter = executionSetProps.entrySet().iterator();
        Iterator<Map.Entry<String, Object>> propIter = ruleSet.getProperties().entrySet().iterator();
        while (executionSetPropIter.hasNext() && propIter.hasNext()) {
            Map.Entry<Object, Object> execSetEntry = executionSetPropIter.next();
            Object execSetKey = execSetEntry.getKey();
            Object execSetValue = execSetEntry.getValue();
            Map.Entry<String, Object> propertyEntry = propIter.next();
            String propertyKey = propertyEntry.getKey();
            Object propertyValue = propertyEntry.getValue();
            // Check the entries ...
            assertThat(execSetKey, is((Object)propertyKey));
            if (propertyValue instanceof byte[]) {
                assertThat(execSetValue, is(instanceOf(Reader.class)));
                String streamValueAsString = IoUtil.read((Reader)execSetValue);
                String propertyValueAsString = new String((byte[])propertyValue);
                assertThat(streamValueAsString, is(propertyValueAsString));
            } else {
                assertThat(execSetValue, is(propertyValue));
            }
        }
        assertThat(executionSetPropIter.hasNext(), is(false));
        assertThat(propIter.hasNext(), is(false));

        new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri, validRuleSetUri, validRules,
                    validProperties);
    }

    @Test
    public void shouldConsiderAnyAttributeDifferencesAsChange() {
        RuleSet copy = ruleSet.clone();
        assertThat(copy.hasChanged(ruleSet), is(false));
        assertThat(copy.hasChanged(copy), is(false));

        copy = new RuleSet(validName + "x", validDescription, validClassname, validClasspath, validProviderUri, validRuleSetUri,
                           validRules, validProperties);
        assertThat(copy.hasChanged(ruleSet), is(true));
        assertThat(copy.hasChanged(copy), is(false));

        copy = new RuleSet(validName, validDescription + "x", validClassname, validClasspath, validProviderUri, validRuleSetUri,
                           validRules, validProperties);
        assertThat(copy.hasChanged(ruleSet), is(true));
        assertThat(copy.hasChanged(copy), is(false));

        copy = new RuleSet(validName, validDescription, validClassname + "x", validClasspath, validProviderUri, validRuleSetUri,
                           validRules, validProperties);
        assertThat(copy.hasChanged(ruleSet), is(true));
        assertThat(copy.hasChanged(copy), is(false));

        copy = new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri + "x", validRuleSetUri,
                           validRules, validProperties);
        assertThat(copy.hasChanged(ruleSet), is(true));
        assertThat(copy.hasChanged(copy), is(false));

        validClasspath = new String[] {"classpath1", "classpath2x"};
        copy = new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri, validRuleSetUri + "x",
                           validRules, validProperties);
        assertThat(copy.hasChanged(ruleSet), is(true));
        assertThat(copy.hasChanged(copy), is(false));

        copy = new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri, validRuleSetUri,
                           validRules + "x", validProperties);
        assertThat(copy.hasChanged(ruleSet), is(true));
        assertThat(copy.hasChanged(copy), is(false));

        validProperties.remove("key1");
        copy = new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri, validRuleSetUri,
                           validRules, validProperties);
        assertThat(copy.hasChanged(ruleSet), is(true));
        assertThat(copy.hasChanged(copy), is(false));
    }

    @Test
    public void shouldConsiderOnlyNameWhenDeterminingIfSame() {
        RuleSet copy = ruleSet.clone();
        assertThat(copy.equals(ruleSet), is(true));

        copy = new RuleSet(validName + "x", validDescription, validClassname, validClasspath, validProviderUri, validRuleSetUri,
                           validRules, validProperties);
        assertThat(copy.equals(ruleSet), is(false));

        copy = new RuleSet(validName, validDescription + "x", validClassname, validClasspath, validProviderUri, validRuleSetUri,
                           validRules, validProperties);
        assertThat(copy.equals(ruleSet), is(true));

        copy = new RuleSet(validName, validDescription, validClassname + "x", validClasspath, validProviderUri, validRuleSetUri,
                           validRules, validProperties);
        assertThat(copy.equals(ruleSet), is(true));

        copy = new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri + "x", validRuleSetUri,
                           validRules, validProperties);
        assertThat(copy.equals(ruleSet), is(true));

        validClasspath = new String[] {"classpath1", "classpath2x"};
        copy = new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri, validRuleSetUri + "x",
                           validRules, validProperties);
        assertThat(copy.equals(ruleSet), is(true));

        copy = new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri, validRuleSetUri,
                           validRules + "x", validProperties);
        assertThat(copy.equals(ruleSet), is(true));

        validProperties.remove("key1");
        copy = new RuleSet(validName, validDescription, validClassname, validClasspath, validProviderUri, validRuleSetUri,
                           validRules, validProperties);
        assertThat(copy.equals(ruleSet), is(true));
    }

}
