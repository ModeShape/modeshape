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
package org.modeshape.graph;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.util.HashSet;
import java.util.Set;
import org.modeshape.common.component.ClassLoaderFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExecutionContextTest {

    private ExecutionContext context;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
    }

    @After
    public void afterEach() {
        context = null;
    }

    @Test
    public void shouldHaveIdentifierThatIsNotNull() {
        assertThat(context.getId(), is(notNullValue()));
    }

    @Test
    public void shouldHaveIdentifierThatIsNotBlank() {
        assertThat(context.getId().length(), is(not(0)));
        assertThat(context.getId().trim().length(), is(not(0)));
    }

    @Test
    public void shouldHaveIdentifierThatIsUnique() {
        // Can't really test this, but we certainly can test that there are no duplicates in many contexts ...
        Set<String> ids = new HashSet<String>();
        for (int i = 0; i != 50; ++i) {
            assertThat(ids.add(new ExecutionContext().getId()), is(true));
        }
    }

    @Test
    public void shouldCreateSubcontextsWithDifferentIdentifiers() {
        ExecutionContext newContext = context.with(mock(ClassLoaderFactory.class));
        assertThat(newContext.getId(), is(not(context.getId())));
    }
}
