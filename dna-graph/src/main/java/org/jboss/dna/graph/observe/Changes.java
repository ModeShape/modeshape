/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.observe;

import javax.security.auth.Subject;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.request.ChangeRequest;

/**
 * A set of changes that were made atomically. Each change is in the form of a frozen {@link ChangeRequest}.
 */
@Immutable
public interface Changes extends Iterable<ChangeRequest> {

    /**
     * Get the user that made these changes.
     * 
     * @return the user; never null
     */
    public Subject getUser();

    /**
     * Get the name of the source that was changed.
     * 
     * @return the source name; never null
     */
    public String getSourceName();

    /**
     * Get the timestamp that the changes were made. All changes within the change set were all made at this instant in time.
     * 
     * @return the timestamp of the changes; never null
     */
    public DateTime getTimestamp();
}
