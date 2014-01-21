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
