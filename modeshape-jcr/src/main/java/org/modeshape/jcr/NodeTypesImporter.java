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

package org.modeshape.jcr;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeDefinition;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.logging.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class which performs the import of the optional, repository configured node-types
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class NodeTypesImporter {

    private static final Logger LOGGER = Logger.getLogger(NodeTypesImporter.class);

    private final JcrRepository.RunningState runningState;
    private final List<String> nodeTypesFiles;

    public NodeTypesImporter( List<String> nodeTypesFiles,
                              JcrRepository.RunningState runningState ) {
        this.nodeTypesFiles = nodeTypesFiles;
        this.runningState = runningState;
    }

    void importNodeTypes() throws RepositoryException {
        if (nodeTypesFiles.isEmpty()) {
            return;
        }

        List<NodeTypeDefinition> nodeTypeDefinitions = new ArrayList<NodeTypeDefinition>();
        for (String cndFile : nodeTypesFiles) {
            nodeTypeDefinitions.addAll(parseCNDFile(cndFile));
        }

        if (nodeTypeDefinitions.isEmpty()) {
            return;
        }

        runningState.nodeTypeManager().registerNodeTypes(nodeTypeDefinitions, false, false, true);
    }

    private List<NodeTypeDefinition> parseCNDFile( String cndFile ) {
        InputStream cndFileStream = runningState.environment().getClassLoader(NodeTypesImporter.class.getClassLoader())
                                                .getResourceAsStream(cndFile);

        if (cndFileStream == null) {
            LOGGER.warn(JcrI18n.cannotLoadCndFile, cndFile);
            return Collections.emptyList();
        }

        CndImporter cndImporter = new CndImporter(runningState.context(), true);
        Problems importProblems = new SimpleProblems();
        try {
            cndImporter.importFrom(cndFileStream, importProblems, cndFile);
            if (importProblems.hasErrors()) {
                importProblems.writeTo(LOGGER);
                return Collections.emptyList();
            } else {
                return cndImporter.getNodeTypeDefinitions();
            }
        } catch (IOException e) {
            LOGGER.error(e, JcrI18n.errorReadingCndFile, cndFile);
            return Collections.emptyList();
        }
    }
}
