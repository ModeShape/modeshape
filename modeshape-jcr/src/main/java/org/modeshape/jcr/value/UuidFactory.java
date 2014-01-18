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
package org.modeshape.jcr.value;

import java.util.UUID;
import org.modeshape.common.annotation.ThreadSafe;

/**
 * A factory for creating {@link UUID UUID instances}. This interface extends the {@link ValueFactory} generic interface and adds
 * specific methods for creating UUIDs.
 */
@ThreadSafe
public interface UuidFactory extends ValueFactory<UUID> {

    @Override
    UuidFactory with( ValueFactories valueFactories );

    /**
     * Create a new random UUID.
     * 
     * @return the new randomly generated UUID
     */
    UUID create();

}
