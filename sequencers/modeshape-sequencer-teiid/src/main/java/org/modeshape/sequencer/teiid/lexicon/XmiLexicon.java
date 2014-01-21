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

import static org.modeshape.sequencer.teiid.lexicon.XmiLexicon.Namespace.PREFIX;

/**
 * Constants associated with the XMI namespace used in reading XMI models and writing JCR nodes.
 */
public interface XmiLexicon {

    /**
     * The URI and prefix constants of the XMI namespace.
     */
    public interface Namespace {
        String PREFIX = "xmi";
        String URI = "http://www.omg.org/XMI";
    }

    /**
     * Constants associated with the XMI namespace that identify XMI model identifiers.
     */
    public interface ModelId {
        String UUID = "uuid";
        String XMI_TAG = "XMI";
    }

    /**
     * JCR identifiers relating to the XMI namespace.
     */
    public interface JcrId {
        String MODEL = PREFIX + ":model";
        String REFERENCEABLE = PREFIX + ":referenceable";
        String UUID = PREFIX + ':' + ModelId.UUID;
        String VERSION = PREFIX + ":version";
        String XMI = PREFIX + ":xmi";
    }
}
