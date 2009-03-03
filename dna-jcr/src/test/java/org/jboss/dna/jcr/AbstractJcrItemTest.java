/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Session;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jverhaeg
 */
public class AbstractJcrItemTest {

    private AbstractJcrItem item;

    @Before
    public void before() {
        item = new AbstractJcrItem() {

            public void accept( ItemVisitor visitor ) {
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

            public Session getSession() {
                return null;
            }

            public boolean isNode() {
                return false;
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

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSave() throws Exception {
        item.save();
    }
}
