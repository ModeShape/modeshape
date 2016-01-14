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
package org.modeshape.jcr.cache;

import java.util.Map;
import java.util.Set;
import org.modeshape.jcr.JcrI18n;

/**
 * An exception signalling that a set of nodes could not be deleted because there are other nodes that contain JCR REFERENCE
 * properties pointing to these nodes, preventing their deletion.
 */
public class ReferentialIntegrityException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Map<String, Set<String>> referrersByRemovedNode;

    public ReferentialIntegrityException(Map<String, Set<String>> referrersByRemovedNode) {
        assert referrersByRemovedNode != null && !referrersByRemovedNode.isEmpty();
        this.referrersByRemovedNode = referrersByRemovedNode;
    }

    @Override
    public String getMessage() {
        return JcrI18n.referentialIntegrityException.text(referrersByRemovedNode);
    }
}
