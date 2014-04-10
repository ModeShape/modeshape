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

package org.modeshape.jcr.spi.federation;

/**
 * A specialized abstract {@link Connector} class that is support both reads and writes. In addition, this class has a {@code readonly}
 * flag allowing clients to configure a writable connector (external source) as read-only. In this case, all of the write operations
 * will throw an exception.

* @author Horia Chiorean (hchiorea@redhat.com)
 */
public abstract class WritableConnector extends Connector {

    /**
     * A flag which indicates whether a connector allows both write & read operations, or only read operations. If a connector
     * is read-only, any attempt to write content (add/update/delete etc) will result in an exception being raised.
     */
    private boolean readonly = false;

    @Override
    public boolean isReadonly() {
        return readonly;
    }
}
