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
package org.modeshape.graph.observe;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.SecurityContext;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.request.ChangeRequest;

/**
 * A set of changes that were made atomically. Each change is in the form of a frozen {@link ChangeRequest}.
 */
@Immutable
public class Changes implements Comparable<Changes>, Serializable {

    private static final long serialVersionUID = 1L;

    protected final String processId;
    protected final String contextId;
    protected final String userName;
    protected final String sourceName;
    protected final DateTime timestamp;
    protected final Map<String, String> data;
    protected final List<ChangeRequest> changeRequests;

    /**
     * @param processId the process identifier; may be null
     * @param contextId the context identifier; may be null
     * @param userName the username; may not be null
     * @param sourceName the source name; may not be null
     * @param timestamp the timestamp; may not be null
     * @param requests the requests; may not be null or empty
     * @param data the immutable customized map of data to be associated with these changes; or null if there is no such data
     */
    public Changes( String processId,
                    String contextId,
                    String userName,
                    String sourceName,
                    DateTime timestamp,
                    List<ChangeRequest> requests,
                    Map<String, String> data ) {
        assert requests != null;
        assert !requests.isEmpty();
        this.userName = userName;
        this.sourceName = sourceName;
        this.timestamp = timestamp;
        this.changeRequests = Collections.unmodifiableList(requests);
        this.processId = processId != null ? processId : "";
        this.contextId = contextId != null ? contextId : "";
        this.data = data != null ? data : Collections.<String, String>emptyMap();
        assert this.userName != null;
        assert this.sourceName != null;
        assert this.timestamp != null;
        assert this.changeRequests != null;
        assert this.processId != null;
    }

    protected Changes( Changes changes ) {
        this.userName = changes.userName;
        this.sourceName = changes.sourceName;
        this.timestamp = changes.timestamp;
        this.changeRequests = changes.changeRequests;
        this.processId = changes.processId;
        this.contextId = changes.contextId;
        this.data = changes.data;
        assert this.userName != null;
        assert this.sourceName != null;
        assert this.timestamp != null;
        assert this.changeRequests != null;
        assert this.processId != null;
        assert this.contextId != null;
        assert this.data != null;
    }

    /**
     * Get the user that made these changes.
     * 
     * @return the user; never null
     * @see SecurityContext#getUserName()
     */
    public String getUserName() {
        return this.userName;
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
     * Get the {@link ExecutionContext#getId() identifier} of the {@link ExecutionContext} where these changes originated. This
     * identifier may be useful in preventing feedbacks.
     * 
     * @return the context identifier; never null
     */
    public String getContextId() {
        return contextId;
    }

    /**
     * Get the list of changes.
     * 
     * @return the immutable list of change requests; never null and never empty
     */
    public List<ChangeRequest> getChangeRequests() {
        return changeRequests;
    }

    /**
     * Get the data associated with these changes.
     * 
     * @return the immutable data; never null but possibly empty
     */
    public Map<String, String> getData() {
        return data;
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
            if (!this.getContextId().equals(that.getContextId())) return false;
            if (!this.getSourceName().equals(that.getSourceName())) return false;
            if (!this.getTimestamp().equals(that.getTimestamp())) return false;
            if (!this.getUserName().equals(that.getUserName())) return false;
            if (!this.getData().equals(that.getData())) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (processId.length() != 0) {
            return getTimestamp() + " @" + getUserName() + " [" + getSourceName() + "] - " + changeRequests.size() + " events";
        }
        return getTimestamp() + " @" + getUserName() + " #" + getProcessId() + " [" + getSourceName() + "] - "
               + changeRequests.size() + " events";
    }
}
