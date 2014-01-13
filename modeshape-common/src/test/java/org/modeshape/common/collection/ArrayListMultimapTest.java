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
package org.modeshape.common.collection;

public class ArrayListMultimapTest extends AbstractMultimapTest {

    @Override
    protected <K, V> Multimap<K, V> createMultimap() {
        return ArrayListMultimap.create();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.AbstractMultimapTest#valuesAllowDuplicates()
     */
    @Override
    protected boolean valuesAllowDuplicates() {
        return true;
    }
}
