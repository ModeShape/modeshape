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
package org.modeshape.sequencer.text;

import static org.modeshape.sequencer.text.TextSequencerLexicon.Namespace.PREFIX;

/**
 * The namespace and property names used within a {@link AbstractTextSequencer} to store internal information.
 */
public class TextSequencerLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/sequencer/text/1.0";
        public static final String PREFIX = "text";
    }

    public static final String COLUMN = PREFIX + ":column";
    public static final String DATA = PREFIX + ":data";
    public static final String ROW = PREFIX + ":row";
}
