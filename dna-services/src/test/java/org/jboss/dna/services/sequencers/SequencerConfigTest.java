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

package org.jboss.dna.services.sequencers;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.jboss.dna.maven.MavenId;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class SequencerConfigTest {

    private SequencerConfig configA;
    private SequencerConfig configB;
    private SequencerConfig configA2;
    private String validName;
    private String validDescription;
    private String validClassname;
    private String[] validRunRules;
    private MavenId[] validMavenIds;

    @Before
    public void beforeEach() throws Exception {
        this.validName = "valid configuration name";
        this.validDescription = "a sequencer";
        this.validClassname = MockSequencerA.class.getName();
        this.validRunRules = new String[] {"mime type is 'text/plain'"};
        this.validMavenIds = MavenId.createClasspath("com.acme:configA:1.0,com.acme:configB:1.0");
        this.configA = new SequencerConfig("configA", validDescription, MockSequencerA.class.getName(), validMavenIds, validRunRules);
        this.configB = new SequencerConfig("configB", validDescription, MockSequencerB.class.getName(), validMavenIds, validRunRules);
        this.configA2 = new SequencerConfig("conFigA", validDescription, MockSequencerA.class.getName(), validMavenIds, validRunRules);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullNameInConstructor() {
        new SequencerConfig(null, validDescription, validClassname, validMavenIds, validRunRules);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyNameInConstructor() {
        new SequencerConfig("", validDescription, validClassname, validMavenIds, validRunRules);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowBlankNameInConstructor() {
        new SequencerConfig("   \t", validDescription, validClassname, validMavenIds, validRunRules);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullClassNameInConstructor() {
        new SequencerConfig(validName, validDescription, null, validMavenIds, validRunRules);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyClassNameInConstructor() {
        new SequencerConfig(validName, validDescription, "", validMavenIds, validRunRules);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowBlankClassNameInConstructor() {
        new SequencerConfig(validName, validDescription, "   \t", validMavenIds, validRunRules);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowInvalidClassNameInConstructor() {
        new SequencerConfig(validName, validDescription, "12.this is not a valid classname", validMavenIds, validRunRules);
    }

    @Test
    public void shouldConsiderSameIfNamesAreEqualIgnoringCase() {
        assertThat(configA.isSame(configA2), is(true));
    }

    @Test
    public void shouldConsiderNotSameIfNamesAreNotEqualIgnoringCase() {
        assertThat(configA.isSame(configB), is(false));
    }

    @Test
    public void shouldNotAddNullOrBlankRunRule() {
        assertThat(SequencerConfig.buildRunRuleSet(null, "", "   ", validRunRules[0]).size(), is(1));
    }

    @Test
    public void shouldNotAddSameRunRuleMoreThanOnce() {
        assertThat(SequencerConfig.buildRunRuleSet(validRunRules[0], validRunRules[0], validRunRules[0]).size(), is(1));
    }

    @Test
    public void shouldHaveNonNullRunRuleCollectionWhenThereAreNoRunRules() {
        configA = new SequencerConfig("configA", validDescription, validClassname, validMavenIds);
        assertThat(configA.getRunRules().size(), is(0));
    }

    @Test
    public void shouldSetClasspathWithValidMavenIds() {
        assertThat(configA.getSequencerClasspath().size(), is(validMavenIds.length));
        assertThat(configA.getSequencerClasspathArray(), is(validMavenIds));
    }

    @Test
    public void shouldGetNonNullSequencerClasspathWhenEmpty() {
        configA = new SequencerConfig("configA", validDescription, validClassname, null, validRunRules);
        assertThat(configA.getSequencerClasspath().size(), is(0));
        assertThat(configA.getSequencerClasspathArray().length, is(0));
    }

}
