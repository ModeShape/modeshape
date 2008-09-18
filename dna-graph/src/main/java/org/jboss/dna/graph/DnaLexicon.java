/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.basic.BasicName;

/**
 * @author Randall Hauch
 */
public class DnaLexicon {

    public static class Namespace {
        public static final String URI = "http://www.jboss.org/dna";
        public static final String PREFIX = "dna";
    }

    public static final Name UUID = new BasicName(Namespace.URI, "uuid");
    public static final Name MERGE_PLAN = new BasicName(Namespace.URI, "mergePlan");
    public static final Name CLASSNAME = new BasicName(Namespace.URI, "classname");
    public static final Name CLASSPATH = new BasicName(Namespace.URI, "classpath");
    public static final Name PROJECTION_RULES = new BasicName(Namespace.URI, "projectionRules");
    public static final Name TIME_TO_EXPIRE = new BasicName(Namespace.URI, "timeToExpire");
}
