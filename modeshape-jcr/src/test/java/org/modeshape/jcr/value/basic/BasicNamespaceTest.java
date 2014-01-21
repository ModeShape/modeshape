/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.value.basic;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class BasicNamespaceTest {

    private BasicNamespace ns1;
    private BasicNamespace ns2;
    private BasicNamespace ns3;
    private BasicNamespace ns4;
    private BasicNamespace ns5;
    private String validUri1;
    private String validUri2;
    private String validUri3;
    private String validPrefix1;
    private String validPrefix2;
    private String validPrefix3;

    @Before
    public void beforeEach() {
        validUri1 = "";
        validUri2 = "http://www.example.com";
        validUri3 = "http://www.acme.com";
        validPrefix1 = "";
        validPrefix2 = "a";
        validPrefix3 = "b";
        ns1 = new BasicNamespace(validPrefix1, validUri1);
        ns2 = new BasicNamespace(validPrefix1, validUri2);
        ns3 = new BasicNamespace(validPrefix2, validUri1);
        ns4 = new BasicNamespace(validPrefix2, validUri2);
        ns5 = new BasicNamespace(validPrefix3, validUri3);
    }

    @Test
    public void shouldHaveSamePrefixPassedIntoConstructor() {
        assertThat(ns1.getPrefix(), is(validPrefix1));
        assertThat(ns2.getPrefix(), is(validPrefix1));
        assertThat(ns3.getPrefix(), is(validPrefix2));
        assertThat(ns4.getPrefix(), is(validPrefix2));
        assertThat(ns5.getPrefix(), is(validPrefix3));
    }

    @Test
    public void shouldHaveSameNamespaceUriPassedIntoConstructor() {
        assertThat(ns1.getNamespaceUri(), is(validUri1));
        assertThat(ns2.getNamespaceUri(), is(validUri2));
        assertThat(ns3.getNamespaceUri(), is(validUri1));
        assertThat(ns4.getNamespaceUri(), is(validUri2));
        assertThat(ns5.getNamespaceUri(), is(validUri3));
    }

    @Test
    public void shouldConsiderAsEqualAnyNamespacesWithSameUri() {
        assertThat(ns1.equals(ns3), is(true));
        assertThat(ns3.equals(ns1), is(true));
        assertThat(ns2.equals(ns4), is(true));
        assertThat(ns4.equals(ns2), is(true));
        assertThat(ns5.equals(ns5), is(true));
    }

    @Test
    public void shouldNotConsiderAsEqualAnyNamespacesWithDifferentUris() {
        assertThat(ns1.equals(ns2), is(false));
        assertThat(ns2.equals(ns1), is(false));
        assertThat(ns3.equals(ns4), is(false));
        assertThat(ns4.equals(ns3), is(false));
    }

}
