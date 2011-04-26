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
package org.modeshape.sequencer.xsd;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.modeshape.graph.property.Path;

/**
 * A simple registry that keeps track of the {@link UUID} and {@link Path} for entities, keyed by their namespace and name.
 */
public class NamespaceEntityResolver {

    private final Map<String, Map<String, Target>> targetByNameByNamespace = new HashMap<String, Map<String, Target>>();

    private static final Target NullTarget = new Target(null, null);

    public void register( String namespace,
                          String name,
                          Path path,
                          UUID uuid ) {
        Map<String, Target> forNamespace = targetByNameByNamespace.get(namespace);
        if (forNamespace == null) {
            forNamespace = new HashMap<String, NamespaceEntityResolver.Target>();
            targetByNameByNamespace.put(namespace, forNamespace);
        }
        Target existing = forNamespace.put(name, new Target(path, uuid));
        assert existing == null || (existing.uuid.equals(uuid) && existing.path.equals(path));
    }

    public UUID lookup( String namespace,
                        String name ) {
        return doLookup(namespace, name).uuid;
    }

    protected Target doLookup( String namespace,
                               String name ) {
        Map<String, Target> forNamespace = targetByNameByNamespace.get(namespace);
        if (forNamespace == null) return NullTarget;
        Target target = forNamespace.get(name);
        return target != null ? target : NullTarget;
    }

    protected static final class Target {
        protected final Path path;
        protected final UUID uuid;

        protected Target( Path path,
                          UUID uuid ) {
            this.path = path;
            this.uuid = uuid;
        }
    }

}
