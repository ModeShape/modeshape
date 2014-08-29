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
package org.modeshape.jcr.api.query;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import org.modeshape.jcr.api.query.qom.QueryObjectModelFactory;

/**
 * A specialization of the standard JCR {@link javax.jcr.query.QueryManager} interface that returns the ModeShape-specific
 * extension interfaces from {@link #getQOMFactory()} and {@link #getQuery(Node)}.
 */
public interface QueryManager extends javax.jcr.query.QueryManager {

    @Override
    public QueryObjectModelFactory getQOMFactory();

    @Override
    public Query createQuery( String statement,
                              String language ) throws InvalidQueryException, RepositoryException;

    @Override
    public Query getQuery( Node node ) throws InvalidQueryException, RepositoryException;
}
