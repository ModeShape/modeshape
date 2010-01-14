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

import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.query.QueryResults.Statistics;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.plan.PlanNode;

/**
 * Interface for a query processor.
 */
public interface Processor {

    /**
     * Process the supplied query plan for the given command and return the results.
     * 
     * @param context the context in which the command is being processed
     * @param command the command being executed
     * @param statistics the time metrics up until this execution
     * @param plan the plan to be processed
     * @return the results of the query
     */
    QueryResults execute( QueryContext context,
                          QueryCommand command,
                          Statistics statistics,
                          PlanNode plan );
}
