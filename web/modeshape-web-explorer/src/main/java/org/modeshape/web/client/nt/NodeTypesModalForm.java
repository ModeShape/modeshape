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
package org.modeshape.web.client.nt;

import org.modeshape.web.client.Console;
import org.modeshape.web.shared.ModalForm;

/**
 * Displays available node types.
 * 
 * @author kulikov
 */
public class NodeTypesModalForm extends ModalForm {
    public NodeTypesModalForm(final Console console) {
        super(console, 800, 550, "Node Types", new NodeTypesForm(console));
    }    
}
