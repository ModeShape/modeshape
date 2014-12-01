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
package org.modeshape.web.client.peditor;

import org.modeshape.web.shared.JcrNode;

/**
 *
 * @author kulikov
 * @param <T> the value type
 */
public interface ValueEditor<T> {
    /**
     * Assigns value of the given property of the given node to the editor.
     * 
     * @param node the node representation
     * @param name property name
     * @param value property value
     */
    public void setValue(JcrNode node, String name, T value);
}
