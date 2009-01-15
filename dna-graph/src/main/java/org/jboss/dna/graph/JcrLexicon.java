/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph;

import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.basic.BasicName;

/**
 * @author Randall Hauch
 */
public class JcrLexicon {

    public static class Namespace {
        public static final String URI = "http://www.jcp.org/jcr/1.0";
        public static final String PREFIX = "jcr";
    }

    public static final Name UUID = new BasicName(Namespace.URI, "uuid");
    public static final Name NAME = new BasicName(Namespace.URI, "name");
    public static final Name PRIMARY_TYPE = new BasicName(Namespace.URI, "primaryType");
    public static final Name MIXIN_TYPES = new BasicName(Namespace.URI, "mixinTypes");
    public static final Name CONTENT = new BasicName(Namespace.URI, "content");
    public static final Name CREATED = new BasicName(Namespace.URI, "created");
    public static final Name ENCODED = new BasicName(Namespace.URI, "encoded");
    public static final Name MIMETYPE = new BasicName(Namespace.URI, "mimeType");
    public static final Name DATA = new BasicName(Namespace.URI, "data");
    public static final Name LAST_MODIFIED = new BasicName(Namespace.URI, "lastModified");
}
