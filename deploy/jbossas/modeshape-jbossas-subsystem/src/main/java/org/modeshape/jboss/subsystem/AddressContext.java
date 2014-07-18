/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jboss.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;

/**
 * A wrapper class over a {@link org.jboss.dmr.ModelNode} representing an active operation.
 * Operations include adding/removing resources which are part of the ModeShape subsystem.
 * This class can be used to retrieve ModeShape & server related information based on the {@link org.jboss.as.controller.PathAddress}
 * of the address of the current operation. The structure of the {@link org.jboss.as.controller.PathAddress} varies based on the server mode (STANDALONE/DOMAIN)
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class AddressContext {
    private final String repositoryName;
    private final PathAddress pathAddress;

    private AddressContext( ModelNode operation ) {
        if (operation == null) {
            throw new IllegalArgumentException("The operation node cannot be null");
        }
        final ModelNode address = operation.require(OP_ADDR);
        this.pathAddress = PathAddress.pathAddress(address);
        String repositoryName = null;
        for (PathElement element : pathAddress) {
            if (element.getKey().equalsIgnoreCase(ModelKeys.REPOSITORY)) {
                repositoryName = element.getValue();
                break;
            }
        }
        this.repositoryName = repositoryName;
    }

    /**
     * Returns the name of the ModeShape repository, based on the address of the wrapped operation.
     *
     * @return a {@link String} which is either the name of the repository or {@code null} if there is no repository segement
     * in the address for the current operation.
     */
    public String repositoryName() {
        return repositoryName;
    }

    /**
     * Returns the value of the last path element from the address of wrapped operation.
     *
     * @return the value of the last path element, never {@code null}
     */
    public String lastPathElementValue() {
        return this.pathAddress.getLastElement().getValue();
    }

    /**
     * Creates a new context instance which wraps the given operation. The operation is expected to have a
     * {@link org.jboss.as.controller.descriptions.ModelDescriptionConstants#OP_ADDR} attribute.
     *
     * @param operation a {@link org.jboss.dmr.ModelNode} instance, never null
     * @return an {@link AddressContext instance}, never {@code null}
     */
    public static AddressContext forOperation( ModelNode operation ) {
        return new AddressContext(operation);
    }
}
