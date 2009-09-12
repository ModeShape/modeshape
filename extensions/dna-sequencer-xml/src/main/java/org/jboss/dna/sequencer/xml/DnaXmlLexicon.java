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
package org.jboss.dna.sequencer.xml;

import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.basic.BasicName;

/**
 * Lexicon of names for XML concepts.
 */
public class DnaXmlLexicon {

    public static class Namespace {
        public static final String URI = "http://www.jboss.org/dna/xml/1.0";
        public static final String PREFIX = "dnaxml";
    }

    public static final Name CDATA = new BasicName(Namespace.URI, "cData");
    public static final Name CDATA_CONTENT = new BasicName(Namespace.URI, "cDataContent");
    public static final Name COMMENT = new BasicName(Namespace.URI, "comment");
    public static final Name COMMENT_CONTENT = new BasicName(Namespace.URI, "commentContent");
    public static final Name DOCUMENT = new BasicName(Namespace.URI, "document");
    public static final Name ELEMENT_CONTENT = new BasicName(Namespace.URI, "elementContent");
    public static final Name PROCESSING_INSTRUCTION = new BasicName(Namespace.URI, "processingInstruction");
    public static final Name PROCESSING_INSTRUCTION_CONTENT = new BasicName(Namespace.URI, "processingInstructionContent");
    public static final Name TARGET = new BasicName(Namespace.URI, "target");
}
