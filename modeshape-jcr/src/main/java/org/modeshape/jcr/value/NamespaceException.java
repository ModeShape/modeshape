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
package org.modeshape.jcr.value;

import org.modeshape.common.annotation.Immutable;

/**
 * A runtime exception denoting that a namespace was invalid or not found.
 */
@Immutable
public class NamespaceException extends RuntimeException {

    private static final long serialVersionUID = 1300642242538881207L;

    public NamespaceException() {
    }

    public NamespaceException( String message ) {
        super(message);

    }

    public NamespaceException( Throwable cause ) {
        super(cause);

    }

    public NamespaceException( String message,
                               Throwable cause ) {
        super(message, cause);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
