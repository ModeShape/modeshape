/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.sequencer.xml;

import static org.modeshape.sequencer.xml.XmlLexicon.Namespace.PREFIX;

/**
 * Lexicon of names for XML concepts.
 */
public class XmlLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/xml/1.0";
        public static final String PREFIX = "modexml";
    }

    public static final String CDATA = PREFIX + ":cData";
    public static final String CDATA_CONTENT = PREFIX + ":cDataContent";
    public static final String COMMENT = PREFIX + ":comment";
    public static final String COMMENT_CONTENT = PREFIX + ":commentContent";
    public static final String DOCUMENT = PREFIX + ":document";
    public static final String ELEMENT = PREFIX + ":element";
    public static final String ELEMENT_CONTENT = PREFIX + ":elementContent";
    public static final String PROCESSING_INSTRUCTION = PREFIX + ":processingInstruction";
    public static final String PROCESSING_INSTRUCTION_CONTENT = PREFIX + ":processingInstructionContent";
    public static final String TARGET = PREFIX + ":target";
}
