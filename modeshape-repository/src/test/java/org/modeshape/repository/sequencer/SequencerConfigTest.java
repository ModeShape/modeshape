/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.modeshape.repository.sequencer;

import java.util.Collections;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.modeshape.repository.sequencer.SequencerConfig;
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
    private String[] validPathExpressions;
    private String[] validMavenIds;

    @Before
    public void beforeEach() {
        this.validName = "valid configuration name";
        this.validDescription = "a sequencer";
        this.validClassname = MockSequencerA.class.getName();
        this.validPathExpressions = new String[] {"/a/b/c/d[e/@attribute] => ."};
        this.validMavenIds = new String[] {"com.acme:configA:1.0,com.acme:configB:1.0"};
        this.configA = new SequencerConfig("configA", validDescription, Collections.<String, Object>emptyMap(), MockSequencerA.class.getName(), validMavenIds,
                                           validPathExpressions);
        this.configB = new SequencerConfig("configB", validDescription, Collections.<String, Object>emptyMap(), MockSequencerB.class.getName(), validMavenIds,
                                           validPathExpressions);
        this.configA2 = new SequencerConfig("conFigA", validDescription, Collections.<String, Object>emptyMap(), MockSequencerA.class.getName(), validMavenIds,
                                            validPathExpressions);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullNameInConstructor() {
        new SequencerConfig(null, validDescription, Collections.<String, Object>emptyMap(), validClassname, validMavenIds, validPathExpressions);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyNameInConstructor() {
        new SequencerConfig("", validDescription, Collections.<String, Object>emptyMap(), validClassname, validMavenIds, validPathExpressions);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowBlankNameInConstructor() {
        new SequencerConfig("   \t", validDescription, Collections.<String, Object>emptyMap(), validClassname, validMavenIds, validPathExpressions);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullClassNameInConstructor() {
        new SequencerConfig(validName, validDescription, Collections.<String, Object>emptyMap(), null, validMavenIds, validPathExpressions);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyClassNameInConstructor() {
        new SequencerConfig(validName, validDescription, Collections.<String, Object>emptyMap(), "", validMavenIds, validPathExpressions);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowBlankClassNameInConstructor() {
        new SequencerConfig(validName, validDescription, Collections.<String, Object>emptyMap(), "   \t", validMavenIds, validPathExpressions);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowInvalidClassNameInConstructor() {
        new SequencerConfig(validName, validDescription, Collections.<String, Object>emptyMap(), "12.this is not a valid classname", validMavenIds, validPathExpressions);
    }

    @Test
    public void shouldConsiderSameIfNamesAreEqualIgnoringCase() {
        assertThat(configA.equals(configA2), is(true));
    }

    @Test
    public void shouldConsiderNotSameIfNamesAreNotEqualIgnoringCase() {
        assertThat(configA.equals(configB), is(false));
    }

    @Test
    public void shouldNotAddNullOrBlankPathExpressions() {
        assertThat(SequencerConfig.buildPathExpressionSet(null, "", "   ", validPathExpressions[0]).size(), is(1));
    }

    @Test
    public void shouldNotAddSamePathExpressionMoreThanOnce() {
        assertThat(SequencerConfig.buildPathExpressionSet(validPathExpressions[0],
                                                          validPathExpressions[0],
                                                          validPathExpressions[0]).size(), is(1));
    }

    @Test
    public void shouldHaveNonNullPathExpressionCollectionWhenThereAreNoPathExpressions() {
        configA = new SequencerConfig("configA", validDescription, Collections.<String, Object>emptyMap(), validClassname, validMavenIds);
        assertThat(configA.getPathExpressions().size(), is(0));
    }

    @Test
    public void shouldSetClasspathWithValidMavenIds() {
        assertThat(configA.getComponentClasspath().size(), is(validMavenIds.length));
        assertThat(configA.getComponentClasspathArray(), is(validMavenIds));
    }

    @Test
    public void shouldGetNonNullSequencerClasspathWhenEmpty() {
        configA = new SequencerConfig("configA", validDescription, Collections.<String, Object>emptyMap(), validClassname, null, validPathExpressions);
        assertThat(configA.getComponentClasspath().size(), is(0));
        assertThat(configA.getComponentClasspathArray().length, is(0));
    }

}
