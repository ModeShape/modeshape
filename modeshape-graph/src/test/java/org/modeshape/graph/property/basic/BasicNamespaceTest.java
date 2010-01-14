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
package org.modeshape.graph.property.basic;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.modeshape.graph.property.basic.BasicNamespace;
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
