/*
 *
 */
package org.jboss.dna.common.text;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * Encoder that escapes characters that are not allowed in JCR names. The mapping defined in Section 3.6.3 of the JSR-283 public
 * review document: <table cellspacing="0" cellpadding="1" border="1">
 * <tr>
 * <th>Non-JCR character<br/>(Unicode code point)</th>
 * <th>Private use<br/>Unicode code point </th>
 * </tr>
 * <tr>
 * <td>* (U+002A)</td>
 * <td> U+F02A </td>
 * </tr>
 * <tr>
 * <td>/ (U+002F)</td>
 * <td> U+F02F </td>
 * </tr>
 * <tr>
 * <td>: (U+003A)</td>
 * <td> U+F03A </td>
 * </tr>
 * <tr>
 * <td>[ (U+005B)</td>
 * <td> U+F05B </td>
 * </tr>
 * <tr>
 * <td>] (U+005D)</td>
 * <td> U+F05D </td>
 * </tr>
 * <tr>
 * <td>| (U+007C)</td>
 * <td> U+F07C </td>
 * </tr>
 * </table>
 * </p>
 * @author Randall Hauch
 */
public class Jsr283Encoder implements ITextEncoder {

    /**
     * {@inheritDoc}
     */
    public String encode( String publicName ) {
        if (publicName == null) return null;
        StringBuilder sb = new StringBuilder();
        CharacterIterator iter = new StringCharacterIterator(publicName);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            char mapped = c;
            if (c == '*') {
                mapped = '\uF02A';
            } else if (c == '/') {
                mapped = '\uF02F';
            } else if (c == ':') {
                mapped = '\uF03A';
            } else if (c == '[') {
                mapped = '\uF05B';
            } else if (c == ']') {
                mapped = '\uF05D';
            } else if (c == '|') {
                mapped = '\uF07C';
            }
            sb.append(mapped);
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String decode( String jcrNodeName ) {
        if (jcrNodeName == null) return null;
        StringBuilder sb = new StringBuilder();
        CharacterIterator iter = new StringCharacterIterator(jcrNodeName);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            char mapped = c;
            if (c == '\uF02A') {
                mapped = '*';
            } else if (c == '\uF02F') {
                mapped = '/';
            } else if (c == '\uF03A') {
                mapped = ':';
            } else if (c == '\uF05B') {
                mapped = '[';
            } else if (c == '\uF05D') {
                mapped = ']';
            } else if (c == '\uF07C') {
                mapped = '|';
            }
            sb.append(mapped);

        }
        return sb.toString();
    }

}
