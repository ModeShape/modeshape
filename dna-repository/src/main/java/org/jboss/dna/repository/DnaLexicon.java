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
package org.jboss.dna.repository;

import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.basic.BasicName;

/**
 * A lexicon of names used within JBoss DNA.
 */
@Immutable
public class DnaLexicon extends org.jboss.dna.graph.DnaLexicon {

    public static final Name SOURCES = new BasicName(Namespace.URI, "sources");
    public static final Name SOURCE = new BasicName(Namespace.URI, "source");
    public static final Name READABLE_NAME = new BasicName(Namespace.URI, "readableName");
    public static final Name DESCRIPTION = new BasicName(Namespace.URI, "description");
    public static final Name SEQUENCERS = new BasicName(Namespace.URI, "sequencers");
    public static final Name SEQUENCER = new BasicName(Namespace.URI, "sequencer");
    public static final Name PATH_EXPRESSION = new BasicName(Namespace.URI, "pathExpression");
    public static final Name JNDI_NAME = new BasicName(Namespace.URI, "jndiName");
    public static final Name MIME_TYPE_DETECTORS = new BasicName(Namespace.URI, "mimeTypeDetectors");
    public static final Name MIME_TYPE_DETECTOR = new BasicName(Namespace.URI, "mimeTypeDetector");
    public static final Name OPTIONS = new BasicName(Namespace.URI, "options");
    public static final Name VALUE = new BasicName(Namespace.URI, "value");
    public static final Name RETRY_LIMIT = new BasicName(Namespace.URI, "retryLimit");
    public static final Name DEFAULT_CACHE_POLICY = new BasicName(Namespace.URI, "defaultCachePolicy");
}
