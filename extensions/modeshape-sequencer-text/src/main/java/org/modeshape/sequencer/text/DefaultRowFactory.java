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
package org.modeshape.sequencer.text;

import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencerContext;

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
    
    private int rowNumber = 1;

    public void recordRow( StreamSequencerContext context,
                        SequencerOutput output,
                        String[] columns ) {

        int columnNumber = 1;
        PathFactory pathFactory = context.getValueFactories().getPathFactory();

        Segment rowSegment = pathFactory.createSegment(TextSequencerLexicon.ROW, rowNumber);
        output.setProperty(pathFactory.createRelativePath(rowSegment), JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED);

        for (String column : columns) {
            Segment columnSegment = pathFactory.createSegment(TextSequencerLexicon.COLUMN, columnNumber++);

            output.setProperty(pathFactory.createRelativePath(rowSegment, columnSegment),
                               JcrLexicon.PRIMARY_TYPE,
                               JcrNtLexicon.UNSTRUCTURED);
            output.setProperty(pathFactory.createRelativePath(rowSegment, columnSegment),
                               JcrLexicon.MIXIN_TYPES,
                               TextSequencerLexicon.COLUMN);
            output.setProperty(pathFactory.createRelativePath(rowSegment, columnSegment), TextSequencerLexicon.DATA, column);
        }

        rowNumber++;
    }

}
