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
package org.modeshape.web.jcr.rest.client.domain;

/**
 * The IModeShapeObject class defines a ModeShape business object.
 */
public interface IModeShapeObject {

    /**
     * @return the object name (never <code>null</code>)
     */
    String getName();

    /**
     * @return a description suitable for use in a tooltip (never <code>null</code>)
     */
    String getShortDescription();

}
