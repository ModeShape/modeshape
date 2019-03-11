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
package org.modeshape.jcr.query.validate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.modeshape.jcr.query.validate.Schemata.Column;
import org.modeshape.jcr.query.validate.Schemata.Key;

/**
 * 
 */
public class ImmutableKey implements Key {

    private final Set<Column> columns;

    protected ImmutableKey( Iterable<Column> columns ) {
        assert columns != null;
        Set<Column> columnSet = new HashSet<>();
        for (Column column : columns) {
            if (column != null) columnSet.add(column);
        }
        assert !columnSet.isEmpty();
        this.columns = Collections.unmodifiableSet(columnSet);
    }

    protected ImmutableKey( Column... columns ) {
        assert columns != null;
        assert columns.length > 0;
        Set<Column> columnSet = new HashSet<>();
        for (Column column : columns) {
            if (column != null) columnSet.add(column);
        }
        assert !columnSet.isEmpty();
        this.columns = Collections.unmodifiableSet(columnSet);
    }

    @Override
    public Set<Column> getColumns() {
        return columns;
    }

    @Override
    public boolean hasColumns( Column... columns ) {
        return hasColumns(Arrays.asList(columns));
    }

    @Override
    public boolean hasColumns( Iterable<Column> columns ) {
        Collection<Column> coll;
        if (columns instanceof Collection<?>) {
            coll = (Collection<Column>)columns;
        } else {
            coll = new ArrayList<>();
            columns.forEach(coll::add);
        }
        return coll.size() == this.columns.size() && this.columns.containsAll(coll);
    }

    @Override
    public String toString() {
        return columns.stream().map(Object::toString).collect(Collectors.joining(", ", "[", "]"));
    }
}
