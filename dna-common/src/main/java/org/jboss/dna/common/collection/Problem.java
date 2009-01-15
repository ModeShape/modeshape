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
package org.jboss.dna.common.collection;

import net.jcip.annotations.Immutable;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.HashCode;

/**
 * @author Randall Hauch
 */
@Immutable
public class Problem {

    public static final int DEFAULT_CODE = 0;

    public enum Status {
        ERROR,
        WARNING,
        INFO;
    }

    private final Status status;
    private final I18n message;
    private final Object[] parameters;
    private final Throwable throwable;
    private final int code;
    private final String resource;
    private final String location;

    public Problem( Status status,
                    int code,
                    I18n message,
                    Object[] params,
                    String resource,
                    String location,
                    Throwable throwable ) {
        CheckArg.isNotNull(status, "status");
        CheckArg.isNotNull(message, "message");
        this.status = status;
        this.code = code;
        this.message = message;
        this.parameters = params;
        this.resource = resource != null ? resource.trim() : null;
        this.location = location != null ? location.trim() : null;
        this.throwable = throwable;
    }

    /**
     * @return code
     */
    public int getCode() {
        return this.code;
    }

    /**
     * @return location
     */
    public String getLocation() {
        return this.location;
    }

    /**
     * Get the message written in the current locale.
     * 
     * @return the message
     */
    public String getMessageString() {
        return this.message.text(this.parameters);
    }

    /**
     * @return message
     */
    public I18n getMessage() {
        return this.message;
    }

    public Object[] getParameters() {
        return this.parameters;
    }

    /**
     * @return resource
     */
    public String getResource() {
        return this.resource;
    }

    /**
     * @return status
     */
    public Status getStatus() {
        return this.status;
    }

    /**
     * @return throwable
     */
    public Throwable getThrowable() {
        return this.throwable;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(status, code, message, resource, location);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Problem) {
            Problem that = (Problem)obj;
            if (this.getStatus() != that.getStatus()) return false;
            if (this.getCode() != that.getCode()) return false;
            if (!this.getMessage().equals(that.getMessage())) return false;
            if (!this.getParameters().equals(that.getParameters())) return false;

            String thisResource = this.getResource();
            String thatResource = that.getResource();
            if (thisResource != thatResource) {
                if (thisResource == null || !thisResource.equals(thatResource)) return false;
            }

            String thisLocation = this.getLocation();
            String thatLocation = that.getLocation();
            if (thisLocation != thatLocation) {
                if (thisLocation == null || !thisLocation.equals(thatLocation)) return false;
            }

            Throwable thisThrowable = this.getThrowable();
            Throwable thatThrowable = that.getThrowable();
            if (thisThrowable != thatThrowable) {
                if (thisThrowable == null || !thisThrowable.equals(thatThrowable)) return false;
            }
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
        StringBuilder sb = new StringBuilder();
        sb.append(this.getStatus()).append(": ");
        if (this.getCode() != DEFAULT_CODE) {
            sb.append("(").append(this.getCode()).append(") ");
        }
        sb.append(this.getMessageString());
        if (this.getResource() != null) {
            sb.append(" Resource=\"").append(this.getResource()).append("\"");
        }
        if (this.getLocation() != null) {
            sb.append(" At \"").append(this.getLocation()).append("\"");
        }
        if (this.getThrowable() != null) {
            sb.append(" (threw ").append(this.getThrowable().getLocalizedMessage()).append(")");
        }
        return sb.toString();
    }

}
