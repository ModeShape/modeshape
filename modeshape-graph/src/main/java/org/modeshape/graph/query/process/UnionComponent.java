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
package org.modeshape.graph.query.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.QueryResults.Columns;

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

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.process.ProcessingComponent#execute()
     */
    @Override
    public List<Object[]> execute() {
        List<Object[]> tuples = new ArrayList<Object[]>();
        for (ProcessingComponent source : sources()) {
            List<Object[]> results = source.execute();
            tuples.addAll(results);
        }
        if (removeDuplicatesComparator != null) {
            Collections.sort(tuples, this.removeDuplicatesComparator);
            removeDuplicatesIfRequested(tuples);
        }
        return tuples;
    }
}
