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
package org.modeshape.connector.filesystem;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * @author johnament
 */
public class InclusionExclusionFilenameFilterTest {

    private InclusionExclusionFilenameFilter filter;

    @Before
    public void setUp() {
        filter = new InclusionExclusionFilenameFilter();
    }

    @Test
    public void testEmptyFilter() {
        assertTrue(filter.accept(null, "anystring"));
    }

    @Test
    public void testInclusionOnly() {
        filter.setInclusionPattern("(.+)\\.mode");
        assertTrue(filter.accept(null, "myfile.mode"));
        assertFalse(filter.accept(null, "anotherfile.txt"));
    }

    @Test
    public void testExclusionOnly() {
        filter.setExclusionPattern("(.+)\\.mode");
        assertFalse(filter.accept(null, "myfile.mode"));
        assertTrue(filter.accept(null, "anotherfile.txt"));
    }

    @Test
    public void testInclusionExclusion() {
        filter.setInclusionPattern("(.+)\\.mode");
        filter.setExclusionPattern("ignore_me(.+)\\.mode");
        assertTrue(filter.accept(null, "validfile.mode"));
        assertFalse(filter.accept(null, "ignore_meinvalidfile.mode"));
    }

}
