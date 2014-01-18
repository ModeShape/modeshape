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
package org.modeshape.sequencer.teiid.xmi;

/**
 * Each XMI part has a name and may have a namepspace URI and prefix.
 */
public interface XmiPart {

    /**
     * @return the XMI part name (never <code>null</code> or empty)
     */
    String getName();

    /**
     * @return the namespace prefixed XMI part name (never <code>null</code> or empty)
     */
    String getQName();

    /**
     * @return the namespace prefix (can be <code>null</code> or empty)
     */
    String getNamespacePrefix();

    /**
     * @return the namespace URI (can be <code>null</code> or empty)
     */
    String getNamespaceUri();

    /**
     * @return the value (can be <code>null</code> or empty)
     */
    String getValue();
}
