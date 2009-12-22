package org.jboss.dna.sequencer.text;

import java.util.Arrays;
import org.jboss.dna.common.util.CheckArg;

/**
 * An text sequencer implementation that uses a {@link #setColumnStartPositions(String) list of column numbers} to split incoming
 * rows into fixed-width columns. By default, this class treats each row as a single column. There is an implicit column start
 * index of 0 for the first column.
 * 
 * @see AbstractTextSequencer
 */
public class FixedWidthTextSequencer extends AbstractTextSequencer {

    private int[] columnStartPositions = new int[] {};

    /**
     * Set the column start positions. The column start positions are 0-based. Everything before the first start position is
     * treated as the first column.
     * <p>
     * As an example, if the column start positions were {3, 6, 15} and the incoming stream was:
     * 
     * <pre>
     *           1         2
     * 012345678901234567890
     * supercallifragilistic
     * expialidocious
     * </pre>
     * This sequencer would return the following rows:
     * <pre>
     * row 1: "sup", "erc", "allifragi", "listic"
     * row 2: "exp:, "ial", "idocious"
     * </pre>
     * 
     * Note that there are only three columns returned in the second row, as there were not enough characters to reach the third
     * start position.
     * </p>
     * 
     * @param columnStartPositions the column startPositions; may not be null
     */
    public void setColumnStartPositions( int[] columnStartPositions ) {
        CheckArg.isNotNull(columnStartPositions, "columnStartPositions");

        this.columnStartPositions = columnStartPositions;
        Arrays.sort(columnStartPositions);
    }

    /**
     * Set the column start positions from a list of column start positions concatenated into a single, comma-delimited string.
     * 
     * @param commaDelimitedColumnStartPositions a list of column start positions concatenated into a single, comma-delimited
     *        string; may not be null
     * @see #setColumnStartPositions(int[])
     */
    public void setColumnStartPositions( String commaDelimitedColumnStartPositions ) {
        CheckArg.isNotNull(commaDelimitedColumnStartPositions, "commaDelimitedColumnStartPositions");

        String[] stringStartPositions = commaDelimitedColumnStartPositions.split(",");
        int[] columnStartPositions = new int[stringStartPositions.length];

        for (int i = 0; i < stringStartPositions.length; i++) {
            columnStartPositions[i] = Integer.valueOf(stringStartPositions[i]);
        }

        setColumnStartPositions(columnStartPositions);
    }

    @Override
    protected String[] parseLine( String line ) {
        assert line != null;

        int columnCount = columnStartPositions.length + 1;
        int currentPos = 0;

        String[] columns = new String[columnCount];

        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            int endPos = columnIndex >= columnCount - 1 ? Integer.MAX_VALUE : columnStartPositions[columnIndex];
            String chunk = parseColumn(line, currentPos, endPos);

            // The line was shorter than expected
            if (chunk == null) {
                assert columnIndex > 0 : "parseColumn failed to return the first column in string " + line;

                String[] truncatedColumns = new String[columnIndex - 1];
                System.arraycopy(columns, 0, truncatedColumns, 0, columnIndex - 1);
                return truncatedColumns;
            }
            columns[columnIndex] = chunk;
            currentPos = endPos;
        }

        return columns;
    }

    private String parseColumn( String line,
                                int startPos,
                                int endPos ) {
        int length = line.length();

        if (length < startPos) {
            return null;
        }

        if (length < endPos) {
            return line.substring(startPos);
        }

        return line.substring(startPos, endPos);
    }

}
