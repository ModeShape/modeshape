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
package org.modeshape.common.logging.log4j;

import org.modeshape.common.logging.Logger;
import org.modeshape.common.logging.LogFactory;

/**
 * A {@link LogFactory} extension which creates a {@link Log4jLoggerImpl} instance to use for logging
 *
 * @author Horia Chiorean
 */
public final class Log4jLoggerFactory extends LogFactory {

    @Override
    protected Logger getLogger( String name ) {
        return new Log4jLoggerImpl(name);
    }
}
