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

package org.modeshape.jcr.spi.federation;

import org.modeshape.common.i18n.I18n;

/**
 * Exception class that can be thrown either by {@link Connector} implementations or in other exceptional cases involving
 * federation operations.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ConnectorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

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
