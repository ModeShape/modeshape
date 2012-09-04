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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.sequencer.teiid.lexicon.RelationalLexicon;
import org.modeshape.sequencer.teiid.lexicon.XmiLexicon;

/**
 *
 */
public class XmiElementTest {

    private static final String INITIAL_NAME = "partName";

    private static final String UUID = "1e40dcf2-8113-4c0b-81de-5a9dbf8bede0";
    private static final String UUID_ATTR_NAME = XmiLexicon.ModelId.UUID;
    private static final String UUID_ATTR_NAMESPACE_PREFIX = XmiLexicon.Namespace.PREFIX;
    private static final String UUID_ATTR_NAMESPACE_URI = XmiLexicon.Namespace.URI;
    private static final XmiAttribute UUID_ATTRIBUTE = new XmiAttribute(UUID_ATTR_NAME);

    private static final String CHILD_NAME = "childName";
    private static final String CHILD_NAMESPACE_PREFIX = "prefix";
    private static final String CHILD_NAMESPACE_URI = "uri";

    static {
        UUID_ATTRIBUTE.setValue(UUID);
        UUID_ATTRIBUTE.setNamespacePrefix(UUID_ATTR_NAMESPACE_PREFIX);
        UUID_ATTRIBUTE.setNamespaceUri(UUID_ATTR_NAMESPACE_URI);
    }

    private XmiElement element;

    private XmiElement createChild() {
        final XmiElement child = new XmiElement(CHILD_NAME);
        child.setNamespacePrefix(CHILD_NAMESPACE_PREFIX);
        child.setNamespaceUri(CHILD_NAMESPACE_URI);
        this.element.addChild(child);
        return child;
    }

    @Before
    public void beforeEach() {
        this.element = new XmiElement(INITIAL_NAME) {};
    }

    @Test
    public void shouldHaveEmptyAttributesAfterConstruction() {
        assertNotNull(this.element.getAttributes());
        assertTrue(this.element.getAttributes().isEmpty());
    }

    @Test
    public void shouldHaveEmptyChildrenAfterConstruction() {
        assertNotNull(this.element.getChildren());
        assertTrue(this.element.getChildren().isEmpty());
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowAddingNullAttribute() {
        this.element.addAttribute(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowAddingNullChild() {
        this.element.addChild(null);
    }

    @Test
    public void shouldAddAttribute() {
        this.element.addAttribute(UUID_ATTRIBUTE);
        assertThat(this.element.getAttributes().size(), is(1));
        assertThat(this.element.getAttributes().iterator().next(), is(UUID_ATTRIBUTE));
    }

    @Test
    public void shouldSetUuud() {
        this.element.addAttribute(UUID_ATTRIBUTE);
        assertThat(this.element.getUuid(), is(UUID));
    }

    @Test
    public void shouldFindAttribute() {
        this.element.addAttribute(UUID_ATTRIBUTE);
        assertThat(this.element.getAttribute(UUID_ATTR_NAME, UUID_ATTR_NAMESPACE_URI), is(UUID_ATTRIBUTE));
    }

    @Test
    public void shouldNotFindAttributeIfIncorrectUri() {
        this.element.addAttribute(UUID_ATTRIBUTE);
        assertNull(this.element.getAttribute(XmiLexicon.ModelId.UUID, RelationalLexicon.Namespace.URI));
    }

    @Test
    public void shouldAddChild() {
        XmiElement child = createChild();
        assertThat(this.element.getChildren().size(), is(1));
        assertThat(this.element.getChildren().iterator().next(), is(child));
        assertThat(child.getParent(), is(this.element));
    }

    @Test
    public void shouldFindChild() {
        XmiElement child = createChild();
        XmiElement kid = this.element.findChild(CHILD_NAME, CHILD_NAMESPACE_URI);
        assertNotNull(kid);
        assertThat(kid, is(child));
    }

    @Test
    public void shouldBeEqualIfStateIsTheSame() {
        XmiElement thisChild = createChild();
        XmiElement thatChild = new XmiElement(thisChild.getName());
        thatChild.setNamespacePrefix(thisChild.getNamespacePrefix());
        thatChild.setNamespaceUri(thisChild.getNamespaceUri());
        thisChild.getParent().addChild(thatChild);
        assertThat(thatChild, is(thisChild));
    }
}
