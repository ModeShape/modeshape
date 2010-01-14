package org.modeshape.common.text;

import net.jcip.annotations.Immutable;

/**
 * A class that represents the position of a particular character in terms of the lines and columns of a character sequence.
 */
@Immutable
public final class Position {
	/**
	 * The position is used when there is no content.
	 */
	public final static Position EMPTY_CONTENT_POSITION = new Position(-1, 1, 0);
	
    private final int line;
    private final int column;
    private final int indexInContent;

    public Position( int indexInContent, 
    				 int line,
                     int column ) {
    	this.indexInContent = indexInContent < 0 ? -1:indexInContent;
        this.line = line;
        this.column = column;
        
        assert this.indexInContent >= -1;
        assert this.line > 0;
        assert this.column >= 0;
        // make sure that negative index means an EMPTY_CONTENT_POSITION
        assert this.indexInContent < 0 ? this.line == 1 && this.column == 0: true;
    }
    
    /**
     * Get the 0-based index of this position in the content character array.
     * 
     * @return the index; never negative except for the first position in an empty content.
     */
    public int getIndexInContent() {
        return indexInContent;
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
        return indexInContent;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "" + indexInContent + ':' + line + ':' + column;
    }
    
    /**
     * Return a new position that is the addition of this position and that supplied.
     * 
     * @param position the position to add to this object; may not be null
     * @return the combined position
     */
    public Position add(Position position) {
    	if( this.getIndexInContent() < 0 ) {
    		return position.getIndexInContent() < 0 ? EMPTY_CONTENT_POSITION:position;
    	}
    	
    	if( position.getIndexInContent() < 0 ) {
    		return this;
    	}

        int index = this.getIndexInContent() + position.getIndexInContent();
        int line = position.getLine() + this.getLine() - 1;
        int column = this.getLine() == 1 ? this.getColumn() + position.getColumn() : this.getColumn();
        
        return new Position(index, line, column);
    }
}
