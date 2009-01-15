/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
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
