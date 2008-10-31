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
package org.jboss.dna.common.monitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Logger;
import org.slf4j.Marker;

/**
 * @author jverhaeg
 */
@Immutable
public final class CapturedActivityInfo {

    private final Logger.Level type;
    private final String taskName;
    private final Marker marker;
    private final Throwable throwable;
    private final String message;

    CapturedActivityInfo( Logger.Level type,
                          I18n taskName,
                          Object[] taskNameParameters,
                          final Marker marker,
                          Throwable throwable,
                          I18n message,
                          Object[] messageParameters,
                          Locale locale ) {
        assert type != null;
        assert taskName != null || taskNameParameters == null || taskNameParameters.length == 0;
        assert message != null || messageParameters == null || messageParameters.length == 0;
        this.type = type;
        this.taskName = taskName == null ? "" : taskName.text(locale, taskNameParameters);
        this.marker = marker == null ? null : new ImmutableMarker(marker);
        this.throwable = throwable;
        this.message = message == null ? "" : message.text(locale, messageParameters);
    }

    /**
     * Returns the identifying marker associated with this information.
     * 
     * @return the marker
     */
    public Marker getMarker() {
        return marker;
    }

    /**
     * Returns the message associated with this information localized to the {@link ActivityMonitor#getStatus(Locale) supplied
     * locale}.
     * 
     * @return the message
     */
    public String getMessage() {
        if (message == null) {
            if (throwable == null) return null;
            return throwable.getMessage();
        }
        return message;
    }

    /**
     * Returns the name of the task, localized to the {@link ActivityMonitor#getStatus(Locale) supplied locale}, that was being
     * performed when this information was captured.
     * 
     * @return the task name
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * Returns the throwable associated with this information.
     * 
     * @return the throwable
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * @return <code>true</code> if this information represents an error.
     */
    public boolean isError() {
        return (type == Logger.Level.ERROR);
    }

    /**
     * @return <code>true</code> if this information represents a warning.
     */
    public boolean isWarning() {
        return (type == Logger.Level.WARNING);
    }

    @Immutable
    private final class ImmutableMarker implements Marker {

        private static final long serialVersionUID = 1L;

        private final String name;
        private final List<ImmutableMarker> children = new ArrayList<ImmutableMarker>();

        ImmutableMarker( Marker marker ) {
            name = marker.getName();
            for (Iterator<?> iter = marker.iterator(); iter.hasNext();) {
                children.add(new ImmutableMarker((Marker)iter.next()));
            }
        }

        public void add( Marker child ) {
            throw new UnsupportedOperationException();
        }

        public boolean contains( Marker that ) {
            CheckArg.isNotNull(that, "that");
            if (this == that) return true;
            for (ImmutableMarker marker : children) {
                if (marker.contains(that)) {
                    return true;
                }
            }
            return false;
        }

        public boolean contains( String name ) {
            CheckArg.isNotNull(name, "name");
            if (this.name.equals(name)) return true;
            for (ImmutableMarker marker : children) {
                if (marker.contains(name)) {
                    return true;
                }
            }
            return false;
        }

        public String getName() {
            return name;
        }

        public boolean hasChildren() {
            return !children.isEmpty();
        }

        public Iterator<?> iterator() {
            return children.iterator();
        }

        public boolean remove( Marker child ) {
            throw new UnsupportedOperationException();
        }
    }
}
