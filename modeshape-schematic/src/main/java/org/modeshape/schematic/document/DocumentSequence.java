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
package org.modeshape.schematic.document;

/**
 * An abstraction of a sequence of 0 or more {@link Document} instances.
 */
public interface DocumentSequence {
    /**
     * Get the next document.
     * 
     * @return the next document, or null if there are no more
     * @throws ParsingException if there was a problem reading from the stream
     */
    Document nextDocument() throws ParsingException;
}
