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

import java.util.List;
import org.modeshape.jcr.query.model.Column;
import org.modeshape.jcr.query.plan.PlanNode.Type;

/**
 * A {@link ProcessingComponent} implementation that performs a {@link Type#PROJECT PROJECT} operation to reduce the columns that
 * appear in the results.
 */
public class ProjectComponent extends DelegatingComponent {

    public ProjectComponent( ProcessingComponent delegate,
                             List<Column> columns ) {
        super(delegate, delegate.getColumns().subSelect(columns));
    }

    @Override
    public List<Object[]> execute() {
        return delegate().execute();
    }
}
