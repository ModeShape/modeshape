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
package org.modeshape.common.collection;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;

/**
 * An immutable representation of a problem, with a status, code, internationalized and parameterized message, values for the
 * parameters, information about the resource and location, and an optional exception. The use of internationalized messages
 * allows for automatic localization of the messages (and substitution of the parameter values) via the
 * {@link #getMessageString()} method.
 */
@Immutable
public class Problem {

    public static final int DEFAULT_CODE = 0;

    public enum Status {
        ERROR,
        WARNING,
        INFO;

        public Logger.Level getLogLevel() {
            switch (this) {
                case ERROR:
                    return Logger.Level.ERROR;
                case WARNING:
                    return Logger.Level.WARNING;
                case INFO:
                default:
                    return Logger.Level.INFO;
            }
        }
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

    public int getCode() {
        return this.code;
    }

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

    public I18n getMessage() {
        return this.message;
    }

    public Object[] getParameters() {
        return this.parameters;
    }

    public String getResource() {
        return this.resource;
    }

    public Status getStatus() {
        return this.status;
    }

    public Throwable getThrowable() {
        return this.throwable;
    }

    @Override
    public int hashCode() {
        return HashCode.compute(status, code, message, resource, location);
    }

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
