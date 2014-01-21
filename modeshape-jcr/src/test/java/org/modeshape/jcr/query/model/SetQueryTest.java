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
package org.modeshape.jcr.query.model;

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
