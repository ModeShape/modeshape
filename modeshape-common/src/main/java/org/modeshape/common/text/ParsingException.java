package org.modeshape.common.text;

/**
 * An exception representing a problem during parsing of text.
 */
public class ParsingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Position position;

    /**
     * @param position the position of the error; never null
     */
    public ParsingException( Position position ) {
        super();
        this.position = position;
    }

    /**
     * @param position the position of the error; never null
     * @param message the message
     * @param cause the underlying cause
     */
    public ParsingException( Position position,
                             String message,
                             Throwable cause ) {
        super(message, cause);
        this.position = position;
    }

    /**
     * @param position the position of the error; never null
     * @param message the message
     */
    public ParsingException( Position position,
                             String message ) {
        super(message);
        this.position = position;
    }

    /**
     * @return position
     */
    public Position getPosition() {
        return position;
    }
}
