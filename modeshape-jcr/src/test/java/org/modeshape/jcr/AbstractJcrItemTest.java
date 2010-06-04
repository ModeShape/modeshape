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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.graph.property.Path;

public class AbstractJcrItemTest {

    private AbstractJcrItem item;
    @Mock
    private SessionCache cache;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        item = new AbstractJcrItem(cache) {

            public void accept( ItemVisitor visitor ) {
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.jcr.AbstractJcrItem#path()
             */
            @Override
            Path path() {
                throw new UnsupportedOperationException();
            }

            public String getName() {
                return null;
            }

            public Node getParent() {
                return null;
            }

            public String getPath() {
                return null;
            }

            public boolean isNode() {
                return false;
            }

            public boolean isNew() {
                return false;
            }

            public boolean isModified() {
                return false;
            }

            public void refresh( boolean keepChanges ) {
                throw new UnsupportedOperationException();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public void save() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Test
    public void shouldNotBeNew() throws Exception {
        assertThat(item.isNew(), is(false));
    }

    @Test
    public void shouldNotBeModified() throws Exception {
        assertThat(item.isModified(), is(false));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowRefresh() throws Exception {
        item.refresh(false);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowRemove() throws Exception {
        item.remove();
    }

    @SuppressWarnings( "deprecation" )
    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSave() throws Exception {
        item.save();
    }
}
