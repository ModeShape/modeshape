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
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

class RemoveIndexStorage extends AbstractModeShapeRemoveStepHandler {

    static final RemoveIndexStorage INSTANCE = new RemoveIndexStorage();

    private RemoveIndexStorage() {
    }

    @Override
    List<ServiceName> servicesToRemove( OperationContext context,
                                        ModelNode operation,
                                        ModelNode model ) throws OperationFailedException {
        String repositoryName = repositoryName(operation);
        List<ServiceName> servicesToRemove = new ArrayList<ServiceName>();
        servicesToRemove.add(ModeShapeServiceNames.indexStorageServiceName(repositoryName));

        // Now see if we need to remove the path service ...
        String relativeTo = ModelAttributes.RELATIVE_TO.resolveModelAttribute(context, model).asString();
        if (relativeTo.equalsIgnoreCase(ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE)) {
            servicesToRemove.add(ModeShapeServiceNames.indexStorageDirectoryServiceName(repositoryName));
        }

        String sourceRelativeTo = ModelAttributes.SOURCE_RELATIVE_TO.resolveModelAttribute(context, model).asString();
        if (sourceRelativeTo.equalsIgnoreCase(ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE)) {
            servicesToRemove.add(ModeShapeServiceNames.indexSourceStorageDirectoryServiceName(repositoryName));
        }

        return servicesToRemove;
    }
}
