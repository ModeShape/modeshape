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

import java.util.ArrayList;
import java.util.List;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

class RemoveBinaryStorage extends AbstractModeShapeRemoveStepHandler {

    static final RemoveBinaryStorage INSTANCE = new RemoveBinaryStorage();

    private RemoveBinaryStorage() {
    }

    @Override
    List<ServiceName> servicesToRemove( ModelNode operation,
                                        ModelNode model ) {
        String repositoryName = repositoryName(operation);
        List<ServiceName> servicesToRemove = new ArrayList<ServiceName>();
        servicesToRemove.add(ModeShapeServiceNames.binaryStorageServiceName(repositoryName));

        //see if we need to remove the path service ...
        if (model.has(ModelKeys.RELATIVE_TO)
                && model.get(ModelKeys.RELATIVE_TO).asString().contains(ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE)) {
            // The binaries were stored in the data directory, so we need to remove the path service ...
            ServiceName dirServiceName = ModeShapeServiceNames.binaryStorageDirectoryServiceName(repositoryName);
            servicesToRemove.add(dirServiceName);
        }

        return servicesToRemove;
    }
}
