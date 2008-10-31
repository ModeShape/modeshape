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

import net.jcip.annotations.Immutable;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.Logger;
import org.slf4j.Marker;

/**
 * @author jverhaeg
 */
@Immutable
class UnlocalizedActivityInfo {

    final Logger.Level type;
    final I18n taskName;
    final Object[] taskNameParameters;
    final Marker marker;
    final Throwable throwable;
    final I18n message;
    final Object[] messageParameters;

    UnlocalizedActivityInfo( Logger.Level type,
                             I18n taskName,
                             Object[] taskNameParameters,
                             Marker marker,
                             Throwable throwable,
                             I18n message,
                             Object[] messageParameters ) {
        assert type != null;
        this.type = type;
        this.taskName = taskName;
        this.taskNameParameters = taskNameParameters;
        this.marker = marker;
        this.throwable = throwable;
        this.message = message;
        this.messageParameters = messageParameters;
    }
}
