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
package org.modeshape.jcr;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import javax.jcr.RepositoryException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JcrNodeTypeManagerTest extends MultiUseAbstractTest {

    private static final String MIXIN1 = "mix:lockable";
    private static final String MIXIN2 = "mix:referenceable";
    private static final String[] MIXINS = new String[] {MIXIN1, MIXIN2};

    private static final String HIERARCHY_NODE_TYPE = "nt:hierarchyNode";

    private static final String SUBTYPE1 = "nt:folder"; // subtype of HIERARCHY_NODE_TYPE
    private static final String SUBTYPE2 = "nt:file"; // subtype of HIERARCHY_NODE_TYPE
    private static final String[] SUBTYPES = new String[] {SUBTYPE1, SUBTYPE2};

    private static final String NO_MATCH_TYPE = "nt:query";

    private static final String[] SUBTYPES_MIXINS;

    static {
        SUBTYPES_MIXINS = new String[SUBTYPES.length + MIXINS.length];
        System.arraycopy(SUBTYPES, 0, SUBTYPES_MIXINS, 0, SUBTYPES.length);
        System.arraycopy(MIXINS, 0, SUBTYPES_MIXINS, SUBTYPES.length, MIXINS.length);
    }

    @BeforeClass
    public static final void beforeAll() throws Exception {
        MultiUseAbstractTest.beforeAll();
    }

    @AfterClass
    public static final void afterAll() throws Exception {
        MultiUseAbstractTest.afterAll();
    }

    private JcrNodeTypeManager nodeTypeMgr;

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
        nodeTypeMgr = session().nodeTypeManager();
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullSubTypeNames() throws RepositoryException {
        this.nodeTypeMgr.isDerivedFrom(null, "nt:base", MIXINS);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptySubTypeNames() throws Exception {
        this.nodeTypeMgr.isDerivedFrom(new String[0], "nt:base", MIXINS);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullPrimaryType() throws Exception {
        this.nodeTypeMgr.isDerivedFrom(SUBTYPES, null, MIXINS);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyPrimaryType() throws Exception {
        this.nodeTypeMgr.isDerivedFrom(SUBTYPES, "", MIXINS);
    }

    @Test
    public void shouldBeDerivedFromIfSubtypeMatchesPrimaryType() throws Exception {
        assertTrue(this.nodeTypeMgr.isDerivedFrom(SUBTYPES, SUBTYPE2, null));
        assertTrue(this.nodeTypeMgr.isDerivedFrom(SUBTYPES, SUBTYPE2, MIXINS));
    }

    @Test
    public void shouldBeDerivedFromIfSubtypeMatchesMixin() throws Exception {
        assertTrue(this.nodeTypeMgr.isDerivedFrom(new String[] {MIXIN2}, SUBTYPE1, MIXINS));
    }

    @Test
    public void shouldBeDerivedFromIfSubtypeIsActualSubType() throws Exception {
        assertTrue(this.nodeTypeMgr.isDerivedFrom(SUBTYPES, HIERARCHY_NODE_TYPE, MIXINS));
    }

    @Test
    public void shouldNotBeDerivedFromIfNoMatch() throws Exception {
        assertFalse(this.nodeTypeMgr.isDerivedFrom(SUBTYPES, NO_MATCH_TYPE, MIXINS));
    }

    @Test
    public void shouldReturnTrueForHasNodeTypeWithExistingNodeTypeName() throws Exception {
        assertTrue(nodeTypeMgr.hasNodeType("nt:base"));
        assertTrue(nodeTypeMgr.hasNodeType(HIERARCHY_NODE_TYPE));
        assertTrue(nodeTypeMgr.hasNodeType(MIXIN1));

    }

    @Test
    public void shouldReturnFalseForHasNodeTypeWithNonexistantNodeTypeName() throws Exception {
        assertFalse(nodeTypeMgr.hasNodeType("someArgleBargle"));
        assertFalse(nodeTypeMgr.hasNodeType(HIERARCHY_NODE_TYPE + "x"));
    }
}
