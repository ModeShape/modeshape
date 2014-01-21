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

package org.modeshape.jcr.clustering;

import java.io.Serializable;

/**
 * Interface allowing participants in a cluster to communicate to each other using different messages.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 *
 * @see ClusteringService#sendMessage(java.io.Serializable)
 * @param <T> the payload type that this consumer expects
 */
public abstract class MessageConsumer<T extends Serializable> {

    private final Class<T> payloadType;

    protected MessageConsumer( Class<T> payloadType ) {
        this.payloadType = payloadType;
    }

    /**
     * Returns the type of the payload;
     *
     * @return a {@link Class} instance never null;
     */
    public Class<T> getPayloadType() {
        return payloadType;
    }

    /**
     * Consumes a payload of the given type.
     *
     * @param payload a {@link Serializable} payload.
     */
    public abstract void consume(T payload);
}
