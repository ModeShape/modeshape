package org.jboss.dna.sequencer.text;

import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Path.Segment;
import org.jboss.dna.graph.sequencer.SequencerOutput;
import org.jboss.dna.graph.sequencer.StreamSequencerContext;

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
