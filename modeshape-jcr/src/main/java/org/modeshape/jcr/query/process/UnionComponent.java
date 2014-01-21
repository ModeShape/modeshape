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
package org.modeshape.jcr.query.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.QueryResults.TupleReformatter;

/**
 */
public class UnionComponent extends SetOperationComponent {

    public UnionComponent( QueryContext context,
                           Columns columns,
                           Iterable<ProcessingComponent> sources,
                           boolean alreadySorted,
                           boolean all ) {
        super(context, columns, sources, alreadySorted, all);
    }

    @Override
    public List<Object[]> execute() {
        List<Object[]> tuples = new ArrayList<Object[]>();
        Iterator<TupleReformatter> reformatters = sourceReformatters.iterator();
        for (ProcessingComponent source : sources()) {
            List<Object[]> results = source.execute();
            TupleReformatter reformatter = reformatters.next();
            if (reformatter != null) {
                for (Object[] tuple : results) {
                    tuples.add(reformatter.reformat(tuple));
                }
            } else {
                tuples.addAll(results);
            }
        }
        if (removeDuplicatesComparator != null) {
            Collections.sort(tuples, this.removeDuplicatesComparator);
            removeDuplicatesIfRequested(tuples);
        }
        return tuples;
    }
}
