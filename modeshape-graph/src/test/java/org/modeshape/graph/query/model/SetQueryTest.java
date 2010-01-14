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
package org.modeshape.graph.query.model;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.junit.Test;

/**
 * 
 */
public class SetQueryTest extends AbstractQueryObjectTest {

    private SetQuery query;
    private QueryCommand left;
    private QueryCommand right;
    private SetQuery.Operation operation;
    private boolean all;

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateWithNullSource() {
        new Query(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateWithNullLeftQueryCommand() {
        left = null;
        right = mock(QueryCommand.class);
        operation = SetQuery.Operation.UNION;
        new SetQuery(left, operation, right, all);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateWithNullRightQueryCommand() {
        left = mock(QueryCommand.class);
        right = null;
        operation = SetQuery.Operation.UNION;
        new SetQuery(left, operation, right, all);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateWithNullOperation() {
        left = mock(QueryCommand.class);
        right = mock(QueryCommand.class);
        operation = null;
        new SetQuery(left, operation, right, all);
    }

    @Test
    public void shouldCreateWithNonNullQueryCommandsAndUnionOperation() {
        left = new Query(new NamedSelector(selector("A")));
        right = new Query(new NamedSelector(selector("B")));
        operation = SetQuery.Operation.UNION;
        query = new SetQuery(left, operation, right, all);
        assertThat(Visitors.readable(query), is("SELECT * FROM A UNION SELECT * FROM B"));
    }

    @Test
    public void shouldCreateWithNonNullQueryCommandsAndUnionAllOperation() {
        left = new Query(new NamedSelector(selector("A")));
        right = new Query(new NamedSelector(selector("B")));
        operation = SetQuery.Operation.UNION;
        all = true;
        query = new SetQuery(left, operation, right, all);
        assertThat(Visitors.readable(query), is("SELECT * FROM A UNION ALL SELECT * FROM B"));
    }

    @Test
    public void shouldCreateWithNonNullQueryCommandsAndIntersectOperation() {
        left = new Query(new NamedSelector(selector("A")));
        right = new Query(new NamedSelector(selector("B")));
        operation = SetQuery.Operation.INTERSECT;
        query = new SetQuery(left, operation, right, all);
        assertThat(Visitors.readable(query), is("SELECT * FROM A INTERSECT SELECT * FROM B"));
    }

    @Test
    public void shouldCreateWithNonNullQueryCommandsAndIntersectAllOperation() {
        left = new Query(new NamedSelector(selector("A")));
        right = new Query(new NamedSelector(selector("B")));
        operation = SetQuery.Operation.INTERSECT;
        all = true;
        query = new SetQuery(left, operation, right, all);
        assertThat(Visitors.readable(query), is("SELECT * FROM A INTERSECT ALL SELECT * FROM B"));
    }

    @Test
    public void shouldCreateWithNonNullQueryCommandsAndExceptOperation() {
        left = new Query(new NamedSelector(selector("A")));
        right = new Query(new NamedSelector(selector("B")));
        operation = SetQuery.Operation.EXCEPT;
        query = new SetQuery(left, operation, right, all);
        assertThat(Visitors.readable(query), is("SELECT * FROM A EXCEPT SELECT * FROM B"));
    }

    @Test
    public void shouldCreateWithNonNullQueryCommandsAndExceptAllOperation() {
        left = new Query(new NamedSelector(selector("A")));
        right = new Query(new NamedSelector(selector("B")));
        operation = SetQuery.Operation.EXCEPT;
        all = true;
        query = new SetQuery(left, operation, right, all);
        assertThat(Visitors.readable(query), is("SELECT * FROM A EXCEPT ALL SELECT * FROM B"));
    }
}
