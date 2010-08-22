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
package org.modeshape.graph;

import net.jcip.annotations.Immutable;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;

/**
 * A lexicon of names used within ModeShape.
 */
@Immutable
public class ModeShapeLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/1.0";
        public static final String PREFIX = "mode";
    }

    public static final Name UUID = new BasicName(Namespace.URI, "uuid");
    public static final Name MERGE_PLAN = new BasicName(Namespace.URI, "mergePlan");
    public static final Name CLASSNAME = new BasicName(Namespace.URI, "classname");
    public static final Name CLASSPATH = new BasicName(Namespace.URI, "classpath");
    public static final Name NAMESPACES = new BasicName(Namespace.URI, "namespaces");
    public static final Name PROJECTION_RULES = new BasicName(Namespace.URI, "projectionRules");
    public static final Name READ_ONLY = new BasicName(Namespace.URI, "readOnly");
    public static final Name RESOURCE = new BasicName(Namespace.URI, "resource");
    public static final Name ROOT = new BasicName(Namespace.URI, "root");
    public static final Name TIME_TO_EXPIRE = new BasicName(Namespace.URI, "timeToExpire");
    public static final Name URI = new BasicName(Namespace.URI, "uri");
    /**
     * @deprecated Use {@link #URI} instead.
     */
    @Deprecated
    public static final Name NAMESPACE_URI = URI;

    public static final Name WORKSPACES = new BasicName(Namespace.URI, "workspaces");
    public static final Name SOURCE_NAME = new BasicName(Namespace.URI, "source");
    public static final Name WORKSPACE_NAME = new BasicName(Namespace.URI, "workspaceName");
    public static final Name DEFAULT_WORKSPACE_NAME = new BasicName(Namespace.URI, "defaultWorkspaceName");
    public static final Name PROJECTION = new BasicName(Namespace.URI, "projection");
    public static final Name PROJECTIONS = new BasicName(Namespace.URI, "projections");

    public static final Name HASHED = new BasicName(Namespace.URI, "hashed");
    public static final Name SHA1 = new BasicName(Namespace.URI, "sha1");

}
