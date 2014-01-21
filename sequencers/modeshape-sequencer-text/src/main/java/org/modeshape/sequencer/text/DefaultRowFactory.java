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
package org.modeshape.sequencer.text;

import static org.modeshape.sequencer.text.TextSequencerLexicon.COLUMN;
import static org.modeshape.sequencer.text.TextSequencerLexicon.DATA;
import static org.modeshape.sequencer.text.TextSequencerLexicon.ROW;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.NotThreadSafe;

/**
 * A default implementation of the {@link RowFactory} class. This class records rows in the following subgraph:
 * 
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
    public void recordRow( Node outputNode,
                           String[] columns ) throws RepositoryException {
        Node rowNode = outputNode.addNode(ROW);

        for (String column : columns) {
            Node columnNode = rowNode.addNode(COLUMN);
            columnNode.addMixin(COLUMN);
            columnNode.setProperty(DATA, column);
        }
    }
}
