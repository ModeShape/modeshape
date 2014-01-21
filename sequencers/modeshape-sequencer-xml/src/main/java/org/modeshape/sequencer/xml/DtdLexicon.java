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


import static org.modeshape.sequencer.xml.DtdLexicon.Namespace.PREFIX;

/**
 * Lexicon of names for XML DTD concepts.
 */
public class DtdLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/dtd/1.0";
        public static final String PREFIX = "modedtd";
    }

    public static final String NAME = PREFIX + ":name";
    public static final String PUBLIC_ID = PREFIX + ":publicId";
    public static final String SYSTEM_ID = PREFIX + ":systemId";
    public static final String VALUE = PREFIX + ":value";
    public static final String ENTITY = PREFIX + ":entity";
}
