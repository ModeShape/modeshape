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

package org.modeshape.sequencer.wsdl;

import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.sequencer.sramp.AbstractSrampSequencer;
import static org.modeshape.sequencer.wsdl.WsdlLexicon.WSDL_DOCUMENT;
import org.modeshape.sequencer.xsd.XsdSequencer;
import java.io.IOException;

/**
 * A sequencer that processes and extract the schema object model from XML Schema Document files.
 */
public class WsdlSequencer extends AbstractSrampSequencer {

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        super.initialize(registry, nodeTypeManager);
        registerNodeTypes(XsdSequencer.class.getResourceAsStream("xsd.cnd"), nodeTypeManager, true);
        registerNodeTypes("wsdl.cnd", nodeTypeManager, true);
    }

    @Override
    public boolean execute( Property inputProperty,
                            Node outputNode,
                            Context context ) throws Exception {
        Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");

        if (outputNode.isNew()) {
            outputNode.setPrimaryType(WSDL_DOCUMENT);
        } else {
            outputNode = outputNode.addNode(WSDL_DOCUMENT, WSDL_DOCUMENT);
        }

        String baseUri = inputProperty.getParent().getPath();
        new Wsdl11Reader(context, baseUri).read(binaryValue.getStream(), outputNode);
        return true;
    }
}
