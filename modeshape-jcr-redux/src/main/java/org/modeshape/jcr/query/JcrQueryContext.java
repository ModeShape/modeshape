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

    QueryResults execute( QueryCommand query,
                          PlanHints hints,
                          Map<String, Object> variables ) throws RepositoryException;

    NodeIterator emptyNodeIterator();

    void recordDuration( long nanos,
                         TimeUnit unit,
                         String query,
                         String language );
}
