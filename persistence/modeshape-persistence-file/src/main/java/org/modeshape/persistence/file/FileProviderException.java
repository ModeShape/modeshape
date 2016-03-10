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
package org.modeshape.persistence.file;

import org.modeshape.common.i18n.I18n;

/**
 * {@link RuntimeException} which is thrown in exception cases by the FS db provider.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public class FileProviderException extends RuntimeException {

    protected FileProviderException( Throwable cause ) {
        super(cause);
    } 
    
    protected FileProviderException( String message ) {
        super(message);
    }

    protected FileProviderException( I18n msgResource, Object... params ) {
        super(msgResource.text(params));
    }
}
