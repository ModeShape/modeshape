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
package org.modeshape.sequencer.teiid.xmi;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class XmiBasePartTest {

    private static final String INITIAL_NAME = "partName";

    private XmiBasePart part;

    @Before
    public void beforeEach() {
        this.part = new XmiBasePart(INITIAL_NAME) {};
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyNameAtConstruction() {
        new XmiBasePart("") {};
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullNameAtConstruction() {
        new XmiBasePart(null) {};
    }

    @Test
    public void shouldSetNameAtConstruction() {
        assertThat(this.part.getName(), is(INITIAL_NAME));
    }

    @Test
    public void shouldSetNamespacePrefix() {
        final String expected = "newPrefix";
        this.part.setNamespacePrefix(expected);
        assertThat(this.part.getNamespacePrefix(), is(expected));
    }

    @Test
    public void shouldSetNamespacePrefixToNull() {
        final String expected = null;
        this.part.setNamespacePrefix(expected);
        assertThat(this.part.getNamespacePrefix(), is(expected));
    }

    @Test
    public void shouldSetNamespacePrefixToEmpty() {
        final String expected = "";
        this.part.setNamespacePrefix(expected);
        assertThat(this.part.getNamespacePrefix(), is(expected));
    }

    @Test
    public void shouldSetNamespaceUri() {
        final String expected = "newUri";
        this.part.setNamespaceUri(expected);
        assertThat(this.part.getNamespaceUri(), is(expected));
    }

    @Test
    public void shouldSetNamespaceUriToNull() {
        final String expected = null;
        this.part.setNamespaceUri(expected);
        assertThat(this.part.getNamespaceUri(), is(expected));
    }

    @Test
    public void shouldSetNamespaceUriToEmpty() {
        final String expected = "";
        this.part.setNamespaceUri(expected);
        assertThat(this.part.getNamespaceUri(), is(expected));
    }

    @Test
    public void shouldSetValue() {
        final String expected = "newValue";
        this.part.setValue(expected);
        assertThat(this.part.getValue(), is(expected));
    }

    @Test
    public void shouldSetValueToNull() {
        final String expected = null;
        this.part.setValue(expected);
        assertThat(this.part.getValue(), is(expected));
    }

    @Test
    public void shouldSetValueToEmpty() {
        final String expected = "";
        this.part.setValue(expected);
        assertThat(this.part.getValue(), is(expected));
    }

    @Test
    public void shouldHaveQNameEqualToNameWhenNoPrefix() {
        assertThat(this.part.getQName(), is(INITIAL_NAME));
    }

    @Test
    public void shouldHaveCorrectQName() {
        final String prefix = "prefix";
        this.part.setNamespacePrefix(prefix);
        assertThat(this.part.getQName(), is("prefix:" + INITIAL_NAME));
    }

    @Test
    public void shouldBeEqualWhenStateIsTheSame() {
        XmiBasePart thatPart = new XmiBasePart(this.part.getName()) {};
        thatPart.setNamespacePrefix("prefix");
        this.part.setNamespacePrefix(thatPart.getNamespacePrefix());

        thatPart.setNamespaceUri("uri");
        this.part.setNamespaceUri(thatPart.getNamespaceUri());

        thatPart.setValue("value");
        this.part.setValue(thatPart.getValue());

        assertThat(this.part, is(thatPart));
    }

    @Test
    public void shouldNotBeEqualWhenStateIsDifferent() {
        XmiBasePart thatPart = new XmiBasePart(this.part.getName()) {};
        thatPart.setNamespacePrefix("prefix");
        assertFalse(this.part.equals(thatPart));

        this.part.setNamespacePrefix(thatPart.getNamespacePrefix());
        thatPart.setNamespaceUri("uri");
        assertFalse(this.part.equals(thatPart));

        this.part.setNamespaceUri(thatPart.getNamespaceUri());
        thatPart.setValue("value");

        assertFalse(this.part.equals(thatPart));
        this.part.setValue(thatPart.getValue());

        assertThat(this.part, is(thatPart));
    }

    @Test
    public void shouldHaveSameHascodeWhenStateIsEqual() {
        XmiBasePart thatPart = new XmiBasePart(this.part.getName()) {};
        thatPart.setNamespacePrefix("prefix");
        this.part.setNamespacePrefix(thatPart.getNamespacePrefix());

        thatPart.setNamespaceUri("uri");
        this.part.setNamespaceUri(thatPart.getNamespaceUri());

        thatPart.setValue("value");
        this.part.setValue(thatPart.getValue());

        assertThat(this.part.hashCode(), is(thatPart.hashCode()));
    }
}
