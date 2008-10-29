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
package org.jboss.dna.sequencer.xml;

import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.basic.BasicName;

/**
 * @author Randall Hauch
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
