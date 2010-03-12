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
package org.modeshape.jcr;

import net.jcip.annotations.Immutable;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;

/**
 * A lexicon of names used within ModeShape.
 */
@Immutable
public class ModeShapeLexicon extends org.modeshape.repository.ModeShapeLexicon {

    public static final Name BASE = new BasicName(Namespace.URI, "base");
    public static final Name EXPIRATION_DATE = new BasicName(Namespace.URI, "expirationDate");
    public static final Name IS_HELD_BY_SESSION = new BasicName(Namespace.URI, "isHeldBySession");
    public static final Name IS_SESSION_SCOPED = new BasicName(Namespace.URI, "isSessionScoped");    
    public static final Name LOCK = new BasicName(Namespace.URI, "lock");    
    public static final Name LOCKED_UUID = new BasicName(Namespace.URI, "lockedUuid");
    public static final Name LOCKING_SESSION = new BasicName(Namespace.URI, "lockingSession");
    public static final Name LOCKS = new BasicName(Namespace.URI, "locks");    
    public static final Name NAMESPACE = new BasicName(Namespace.URI, "namespace");
    public static final Name NODE_TYPES = new BasicName(Namespace.URI, "nodeTypes");
    public static final Name REPOSITORIES = new BasicName(Namespace.URI, "repositories");
    public static final Name SYSTEM = new BasicName(Namespace.URI, "system");
    public static final Name URI = new BasicName(Namespace.URI, "uri");
    public static final Name VERSION_STORAGE = new BasicName(Namespace.URI, "versionStorage");
    public static final Name WORKSPACE = new BasicName(Namespace.URI, "workspace");    
}
