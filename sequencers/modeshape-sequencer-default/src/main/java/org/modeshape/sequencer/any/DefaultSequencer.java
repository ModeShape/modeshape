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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.sequencer.any;

import static org.modeshape.sequencer.any.DefaultMetadataLexicon.METADATA_NODE;

import java.io.IOException;
import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.mimetype.MimeTypeConstants;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.sequencer.sramp.AbstractSrampSequencer;
import org.modeshape.sequencer.sramp.SrampLexicon;

/**
 * A sequencer that processes the binary content of a file.
 * <p>
 * This sequencer produces data that corresponds to the following structure:
 * <ul>
 * <li><strong>mp3:metadata</strong> node of type <code>default:metadata</code>
 * <ul>
 * <li><strong>s-ramp:contentSize</strong> - optional string property for size of the file</li>
 * <li><strong>s-ramp:contentType</strong> - optional string property for file mime-type</li>
 * </ul>
 * </p>
 */
public class DefaultSequencer extends AbstractSrampSequencer {

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        super.initialize(registry, nodeTypeManager);
        super.registerNodeTypes("any.cnd", nodeTypeManager, true);
        registerDefaultMimeTypes(MimeTypeConstants.OCTET_STREAM);
    }

    @Override
    public boolean execute( Property inputProperty,
                            Node outputNode,
                            Context context ) throws Exception {
        Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");
        try {
            DefaultMetadata metadata = DefaultMetadata.instance(binaryValue.getStream());
            Node sequencedNode = outputNode;
            if (outputNode.isNew()) {
                outputNode.setPrimaryType(METADATA_NODE);
            } else {
                sequencedNode = outputNode.addNode(METADATA_NODE, METADATA_NODE);
            }

            sequencedNode.setProperty(SrampLexicon.CONTENT_SIZE, metadata.getContentSize());
            sequencedNode.setProperty(SrampLexicon.CONTENT_TYPE, MimeTypeConstants.OCTET_STREAM);

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            getLogger().error("Cannot sequence default content ", e);
            return false;
        }
    }
}
