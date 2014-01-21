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
package org.modeshape.sequencer.sramp;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple registry that keeps track of the identifiers and paths for entities, keyed by their namespace and name.
 */
public final class NamespaceEntityResolver {

    private final Map<String, Map<String, String>> identifierByNameByNamespace = new HashMap<String, Map<String, String>>();

    public void register( String namespace,
                          String name,
                          String identifier ) {
        Map<String, String> forNamespace = identifierByNameByNamespace.get(namespace);
        if (forNamespace == null) {
            forNamespace = new HashMap<String, String>();
            identifierByNameByNamespace.put(namespace, forNamespace);
        }
        forNamespace.put(name, identifier);
    }

    public String lookupIdentifier( String namespace,
                                    String name ) {
        Map<String, String> forNamespace = identifierByNameByNamespace.get(namespace);
        if (forNamespace == null) {
            return null;
        }
        return forNamespace.get(name);
    }
}
