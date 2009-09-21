package org.jboss.dna.common.text;

import net.jcip.annotations.Immutable;

/**
 * A class that represents the position of a particular character in terms of the lines and columns of a character sequence.
 */
@Immutable
public final class Position {
    private final int line;
    private final int column;

    public Position( int line,
                     int column ) {
        this.line = line;
        this.column = column;
    }

    /**
     * Get the 1-based column number of the character.
     * 
     * @return the column number; always positive
     */
    public int getColumn() {
        return column;
    }

    /**
     * Get the 1-based line number of the character.
     * 
     * @return the line number; always positive
     */
    public int getLine() {
        return line;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return line;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "" + line + ':' + column;
    }
}
