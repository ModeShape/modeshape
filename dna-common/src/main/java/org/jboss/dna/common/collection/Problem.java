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
package org.jboss.dna.common.collection;

import net.jcip.annotations.Immutable;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.ArgCheck;

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

    public Problem( Status status, int code, I18n message, Object... params ) {
        this(status, code, message, params, null, null, null);
    }

    public Problem( Status status, int code, I18n message, Object[] params, String resource, String location, Throwable throwable ) {
        ArgCheck.isNotNull(status, "status");
        ArgCheck.isNotNull(message, "message");
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

}
