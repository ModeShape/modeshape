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
public class DnaLexicon {

    public static class Namespace {
        public static final String URI = "http://www.jboss.org/dna/1.0";
        public static final String PREFIX = "dna";
    }

    public static final Name UUID = new BasicName(Namespace.URI, "uuid");
    public static final Name MERGE_PLAN = new BasicName(Namespace.URI, "mergePlan");
    public static final Name CLASSNAME = new BasicName(Namespace.URI, "classname");
    public static final Name CLASSPATH = new BasicName(Namespace.URI, "classpath");
    public static final Name PROJECTION_RULES = new BasicName(Namespace.URI, "projectionRules");
    public static final Name TIME_TO_EXPIRE = new BasicName(Namespace.URI, "timeToExpire");
}
