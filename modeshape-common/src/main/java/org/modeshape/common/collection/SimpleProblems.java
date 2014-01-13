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
package org.modeshape.common.collection;

import java.util.LinkedList;
import java.util.List;
import org.modeshape.common.annotation.NotThreadSafe;

/**
 * A simple {@link Problems} collection. The problems will be {@link #iterator() returned} in the order in which they were
 * encountered (although this cannot be guaranteed in contexts involving multiple threads or processes).
 */
@NotThreadSafe
public class SimpleProblems extends AbstractProblems {
    private static final long serialVersionUID = 1L;

    private List<Problem> problems;

    @Override
    protected void addProblem( Problem problem ) {
        if (problem == null) return;
        if (problems == null) problems = new LinkedList<Problem>();
        problems.add(problem);
    }

    @Override
    protected List<Problem> getProblems() {
        return this.problems != null ? problems : EMPTY_PROBLEMS;
    }

    @Override
    public void addAll( Iterable<Problem> problems ) {
        if (problems != null) {
            if (problems == this) return;
            if (this.problems == null) this.problems = new LinkedList<Problem>();
            for (Problem problem : problems) {
                this.problems.add(problem);
            }
        }
    }
}
