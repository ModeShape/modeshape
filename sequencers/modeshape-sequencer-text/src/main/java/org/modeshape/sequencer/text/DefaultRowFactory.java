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
package org.modeshape.sequencer.text;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.NotThreadSafe;
import static org.modeshape.jcr.api.JcrConstants.NT_UNSTRUCTURED;
import static org.modeshape.sequencer.text.TextSequencerLexicon.*;


/**
 * A default implementation of the {@link RowFactory} class. This class records rows in the following subgraph:
 * <pre>
 * &lt;graph root&gt;
 *     + text:row[1] (jcr:primaryType = nt:unstructured)
 *     |   + text:column[1] (jcr:primaryType = nt:unstructured, jcr:mixinTypes = text:column, text:data = <column1 data>)
 *     |   + ...
 *     |   + text:column[n] (jcr:primaryType = nt:unstructured, jcr:mixinTypes = text:column, text:data = <columnN data>)
 *     + ...
 *     + text:row[m] (jcr:primaryType = nt:unstructured)
 *         + text:column[1] (jcr:primaryType = nt:unstructured, jcr:mixinTypes = text:column, text:data = <column1 data>)
 *         + ...
 *         + text:column[n] (jcr:primaryType = nt:unstructured, jcr:mixinTypes = text:column, text:data = <columnN data>)
 * </pre>
 */
@NotThreadSafe
public class DefaultRowFactory implements RowFactory {

    @Override
    public void recordRow( Node outputNode, String[] columns ) throws RepositoryException {
        Node rowNode = outputNode.addNode(ROW);

        for (String column : columns) {
            Node columnNode = rowNode.addNode(COLUMN);
            columnNode.addMixin(COLUMN);
            columnNode.setProperty(DATA, column);
        }
    }
}
