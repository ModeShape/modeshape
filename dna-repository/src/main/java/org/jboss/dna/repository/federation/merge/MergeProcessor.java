/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.repository.federation.merge;

import org.jboss.dna.repository.util.ExecutionContext;

/**
 * @author Randall Hauch
 */
public interface MergeProcessor {

    /**
     * Merge the supplied contributions into a single federated node instance.
     * 
     * @param context the context in which this processor is running; never null
     * @param federatedNode the federated node into which should be place all of the merged properties and child references; never
     *        null
     * @param previousMergePlan the merge plan generated from the most recent previous merge; may be null if the node has not yet
     *        been merged
     * @param newMergePlan the new merge plan that contains the contributions from each of the sources and which may be consulted
     *        (and annotated) to should be filled out by this method implementation; never null
     */
    void merge( ExecutionContext context,
                FederatedNode federatedNode,
                MergePlan previousMergePlan,
                MergePlan newMergePlan );

}
