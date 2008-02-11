/*
 *
 */
package org.jboss.dna.common.text;

/**
 * An encoder implementation that does nothing. This is useful when a {@link ITextEncoder encoder} is optional but the code is
 * easier to write when there is always an encoder.
 * @author Randall Hauch
 */
public class NoOpEncoder implements ITextEncoder {

    private static final NoOpEncoder INSTANCE = new NoOpEncoder();

    public static final NoOpEncoder getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    public String encode( String text ) {
        return text;
    }

    /**
     * {@inheritDoc}
     */
    public String decode( String encodedText ) {
        return encodedText;
    }
}
