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

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import javax.security.auth.Subject;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.request.ChangeRequest;

/**
 * A set of changes that were made atomically. Each change is in the form of a frozen {@link ChangeRequest}.
 */
@Immutable
public final class Changes implements Iterable<ChangeRequest>, Comparable<Changes>, Serializable {

    private static final long serialVersionUID = 1L;

    private final String processId;
    private final Subject subject;
    private final String sourceName;
    private final DateTime timestamp;
    private final List<ChangeRequest> changeRequests;

    public Changes( Subject subject,
                    String sourceName,
                    DateTime timestamp,
                    List<ChangeRequest> requests ) {
        this("", subject, sourceName, timestamp, requests);
    }

    public Changes( String processId,
                    Subject subject,
                    String sourceName,
                    DateTime timestamp,
                    List<ChangeRequest> requests ) {
        this.subject = subject;
        this.sourceName = sourceName;
        this.timestamp = timestamp;
        this.changeRequests = requests;
        this.processId = processId != null ? processId : "";
    }

    /**
     * Get the user that made these changes.
     * 
     * @return the user; never null
     */
    public Subject getSubject() {
        return this.subject;
    }

    /**
     * Get the name of the source that was changed.
     * 
     * @return the source name; never null
     */
    public String getSourceName() {
        return this.sourceName;
    }

    /**
     * Get the timestamp that the changes were made. All changes within the change set were all made at this instant in time.
     * 
     * @return the timestamp of the changes; never null
     */
    public DateTime getTimestamp() {
        return this.timestamp;
    }

    /**
     * Get the identifier of the process where these changes originated. This identifier may be useful in preventing feedbacks.
     * 
     * @return the process identifier; never null
     */
    public String getProcessId() {
        return processId;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<ChangeRequest> iterator() {
        return this.changeRequests.iterator();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getTimestamp().hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Changes that ) {
        if (this == that) return 0;
        return this.getTimestamp().compareTo(that.getTimestamp());
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Changes) {
            Changes that = (Changes)obj;
            if (!this.getProcessId().equals(that.getProcessId())) return false;
            if (!this.getSourceName().equals(that.getSourceName())) return false;
            if (!this.getTimestamp().equals(that.getTimestamp())) return false;
            if (!this.getSubject().equals(that.getSubject())) return false;
            return true;
        }
        return false;
    }
}
