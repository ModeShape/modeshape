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
package org.modeshape.sequencer.teiid.lexicon;


/**
 * Constants associated with the XSI namespace used in reading XMI models and writing JCR nodes.
 */
public interface XsiLexicon {

    /**
     * The URI and prefix constants of the XSI namespace.
     */
    public interface Namespace {
        String PREFIX = "xsi";
        String URI = "http://www.w3.org/2001/XMLSchema-instance";
    }

    /**
     * Model identifiers relating to the XSI namespace.
     */
    public interface ModelId {
        String TYPE = "type";
    }
}
