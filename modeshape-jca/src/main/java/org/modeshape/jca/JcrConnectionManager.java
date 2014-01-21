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
package org.modeshape.jca;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

/**
 * Provides the implementation of the ConnectionManager interface for the Modeshape JCA Resource Adapter.
 * 
 * @author kulikov
 */
public class JcrConnectionManager implements ConnectionManager {
    private static final long serialVersionUID = 1L;

    @Override
    public Object allocateConnection( ManagedConnectionFactory mcf,
                                      ConnectionRequestInfo cri ) throws ResourceException {
        ManagedConnection mc = mcf.createManagedConnection(null, cri);
        return mc.getConnection(null, cri);
    }

}
