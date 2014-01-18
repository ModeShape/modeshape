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

import java.io.Serializable;
import org.modeshape.common.annotation.Immutable;

/**
 * A qualified name consisting of a namespace and a local name.
 */
@Immutable
public interface Name extends Comparable<Name>, Serializable, Readable {

    /**
     * Get the local name part of this qualified name.
     * 
     * @return the local name; never null
     */
    String getLocalName();

    /**
     * Get the URI for the namespace used in this qualified name.
     * 
     * @return the URI; never null but possibly empty
     */
    String getNamespaceUri();
}
