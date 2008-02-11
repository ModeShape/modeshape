/*
 *
 */
package org.jboss.dna.common.text;

/**
 * Interface for components that can encode and unencode text.
 * @author Randall Hauch
 */
public interface ITextEncoder {

    /**
     * Returns the encoded version of a string.
     * @param text the text with characters that are to be encoded.
     * @return the text with the characters encoded as required, or null if the supplied text is null
     * @see #decode(String)
     */
    public String encode( String text );

    /**
     * Return the decoded version of an encoded string
     * @param encodedText the encoded text
     * @return the unecoded form of the text, or null if the supplied node name is also null
     * @see #encode(String)
     */
    public String decode( String encodedText );

}
