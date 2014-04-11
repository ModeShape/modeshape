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

/**
 * A {@link Supplier} for {@link AutoCloseable} objects that itself can be closed without regard for whether the {@link #get()}
 * method has not yet been called.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 * @param <T> the type of object to be supplied
 */
public interface CloseableSupplier<T extends AutoCloseable> extends AutoCloseable, Supplier<T> {

    /**
     * 
     */
    @Override
    public void close();
}
