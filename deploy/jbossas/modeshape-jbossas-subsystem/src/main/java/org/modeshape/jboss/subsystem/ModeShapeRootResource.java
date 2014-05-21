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

import java.util.List;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class ModeShapeRootResource extends SimpleResourceDefinition {
    protected final static ModeShapeRootResource INSTANCE = new ModeShapeRootResource();

    /**
     * Set the default ModeShape RBAC access as follows:
     *  Addressing is not sensitive: any management user can address (i.e. read the configuration)
     *  Read is sensitive: only Auditor, Administrator, SuperUser can read attributes, resources etc.
     *  Write is sensitive: only Administrator and SuperUser can write
     */
    protected static final SensitivityClassification MODESHAPE_SECURITY =
            new SensitivityClassification(ModeShapeExtension.SUBSYSTEM_NAME, "modeshape-access-control", false, true, true);
    protected static final SensitiveTargetAccessConstraintDefinition MODESHAPE_SECURITY_DEF =
            new SensitiveTargetAccessConstraintDefinition(MODESHAPE_SECURITY);


    private ModeShapeRootResource() {
        super(ModeShapeExtension.SUBSYSTEM_PATH, ModeShapeExtension.getResourceDescriptionResolver(),
              AddModeShapeSubsystem.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerOperations( ManagementResourceRegistration resourceRegistration ) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION,
                                                      GenericSubsystemDescribeHandler.INSTANCE,
                                                      false);
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return MODESHAPE_SECURITY_DEF.wrapAsList();
    }
}
