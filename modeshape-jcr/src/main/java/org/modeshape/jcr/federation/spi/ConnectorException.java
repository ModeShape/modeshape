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

package org.modeshape.jcr.federation.spi;

import org.modeshape.common.i18n.I18n;

/**
 * Exception class that can be thrown either by {@link Connector} implementations or in other exceptional cases involving federation operations.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ConnectorException extends RuntimeException {

    public ConnectorException( I18n i18nText,
                               Object... arguments ) {
        this(i18nText.text(arguments));
    }

    public ConnectorException( Throwable cause ) {
        super(cause);
    }

    public ConnectorException( String message ) {
        super(message);
    }

    public ConnectorException( String message,
                               Throwable cause ) {
        super(message, cause);
    }
}
