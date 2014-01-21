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
package org.modeshape.jcr.query;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.query.QueryResults.Location;
import org.modeshape.jcr.query.model.QueryCommand;
import org.modeshape.jcr.query.plan.PlanHints;
import org.modeshape.jcr.query.validate.Schemata;
import org.modeshape.jcr.value.Name;

/**
 * The context in which queries are executed.
 */
public interface JcrQueryContext {

    boolean isLive();

    ExecutionContext getExecutionContext();

    Schemata getSchemata();

    Node store( String absolutePath,
                Name nodeType,
                String language,
                String statement ) throws RepositoryException;

    Node getNode( Location location ) throws RepositoryException;

    Value createValue( int propertyType,
                       Object value );

    CancellableQuery createExecutableQuery( QueryCommand query,
                                            PlanHints hints,
                                            Map<String, Object> variables ) throws RepositoryException;

    NodeIterator emptyNodeIterator();

    void recordDuration( long nanos,
                         TimeUnit unit,
                         String query,
                         String language );
}
