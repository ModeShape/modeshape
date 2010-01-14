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
package org.modeshape.common.text;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.BitSet;
import net.jcip.annotations.Immutable;

/**
 * An {@link TextEncoder encoder} and {@link TextDecoder decoder} for XML element and attribute names.
 * <p>
 * Any UTF-16 unicode character that is not a valid XML name character according to the <a
 * href="http://www.w3.org/TR/REC-xml/#sec-common-syn">World Wide Web Consortium (W3C) Extensible Markup Language (XML) 1.0
 * (Fourth Edition) Recommendation</a> is escaped as <code>_xHHHH_</code>, where <code>HHHH</code> stands for the four-digit
 * hexadecimal UTF-16 unicode value for the character in the most significant bit first order. For example, the name "Customer_ID"
 * is encoded as "Customer_x0020_ID".
 * </p>
 * <p>
 * Decoding transforms every <code>_xHHHH_</code> encoding sequences back into the UTF-16 character. Note that
 * {@link #decode(String) decoding} can be safely done on any XML name, even if the name does not contain any encoded sequences.
 * </p>
 */
@Immutable
public class XmlNameEncoder implements TextDecoder, TextEncoder {

    private static final BitSet XML_NAME_ALLOWED_CHARACTERS = new BitSet(2 ^ 16);

    static {
        // Initialize the unescaped bitset ...

        // XML Names may contain: Letter | Digit | '.' | '-' | '_' | ':' | CombiningChar | Extender
        XML_NAME_ALLOWED_CHARACTERS.set('.');
        XML_NAME_ALLOWED_CHARACTERS.set('-');
        XML_NAME_ALLOWED_CHARACTERS.set('_');
        XML_NAME_ALLOWED_CHARACTERS.set(':');

        // XML Base Character Set
        XML_NAME_ALLOWED_CHARACTERS.set('\u0041', '\u005A' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0061', '\u007A' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u00C0', '\u00D6' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u00D8', '\u00F6' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u00F8', '\u00FF' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0100', '\u0131' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0134', '\u013E' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0141', '\u0148' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u014A', '\u017E' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0180', '\u01C3' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u01CD', '\u01F0' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u01F4', '\u01F5' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u01FA', '\u0217' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0250', '\u02A8' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u02BB', '\u02C1' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0386');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0388', '\u038A' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u038C');
        XML_NAME_ALLOWED_CHARACTERS.set('\u038E', '\u03A1' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u03A3', '\u03CE' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u03D0', '\u03D6' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u03DA');
        XML_NAME_ALLOWED_CHARACTERS.set('\u03DC');
        XML_NAME_ALLOWED_CHARACTERS.set('\u03DE');
        XML_NAME_ALLOWED_CHARACTERS.set('\u03E0');
        XML_NAME_ALLOWED_CHARACTERS.set('\u03E2', '\u03F3' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0401', '\u040C' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u040E', '\u044F' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0451', '\u045C' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u045E', '\u0481' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0490', '\u04C4' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u04C7', '\u04C8' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u04CB', '\u04CC' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u04D0', '\u04EB' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u04EE', '\u04F5' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u04F8', '\u04F9' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0531', '\u0556' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0559');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0561', '\u0586' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u05D0', '\u05EA' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u05F0', '\u05F2' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0621', '\u063A' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0641', '\u064A' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0671', '\u06B7' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u06BA', '\u06BE' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u06C0', '\u06CE' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u06D0', '\u06D3' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u06D5');
        XML_NAME_ALLOWED_CHARACTERS.set('\u06E5', '\u06E6' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0905', '\u0939' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u093D');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0958', '\u0961' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0985', '\u098C' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u098F', '\u0990' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0993', '\u09A8' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u09AA', '\u09B0' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u09B2');
        XML_NAME_ALLOWED_CHARACTERS.set('\u09B6', '\u09B9' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u09DC', '\u09DD' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u09DF', '\u09E1' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u09F0', '\u09F1' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A05', '\u0A0A' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A0F', '\u0A10' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A13', '\u0A28' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A2A', '\u0A30' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A32', '\u0A33' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A35', '\u0A36' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A38', '\u0A39' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A59', '\u0A5C' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A5E');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A72', '\u0A74' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A85', '\u0A8B' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A8D');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A8F', '\u0A91' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A93', '\u0AA8' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0AAA', '\u0AB0' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0AB2', '\u0AB3' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0AB5', '\u0AB9' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0ABD');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0AE0');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B05', '\u0B0C' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B0F', '\u0B10' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B13', '\u0B28' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B2A', '\u0B30' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B32', '\u0B33' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B36', '\u0B39' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B3D');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B5C', '\u0B5D' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B5F', '\u0B61' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B85', '\u0B8A' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B8E', '\u0B90' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B92', '\u0B95' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B99', '\u0B9A' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B9C');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B9E', '\u0B9F' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0BA3', '\u0BA4' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0BA8', '\u0BAA' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0BAE', '\u0BB5' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0BB7', '\u0BB9' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0C05', '\u0C0C' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0C0E', '\u0C10' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0C12', '\u0C28' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0C2A', '\u0C33' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0C35', '\u0C39' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0C60', '\u0C61' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0C85', '\u0C8C' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0C8E', '\u0C90' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0C92', '\u0CA8' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0CAA', '\u0CB3' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0CB5', '\u0CB9' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0CDE');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0CE0', '\u0CE1' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0D05', '\u0D0C' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0D0E', '\u0D10' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0D12', '\u0D28' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0D2A', '\u0D39' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0D60', '\u0D61' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0E01', '\u0E2E' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0E30');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0E32', '\u0E33' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0E40', '\u0E45' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0E81', '\u0E82' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0E84');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0E87', '\u0E88' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0E8A');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0E8D');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0E94', '\u0E97' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0E99', '\u0E9F' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0EA1', '\u0EA3' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0EA5');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0EA7');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0EAA', '\u0EAB' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0EAD', '\u0EAE' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0EB0');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0EB2', '\u0EB3' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0EBD');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0EC0', '\u0EC4' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0F40', '\u0F47' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0F49', '\u0F69' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u10A0', '\u10C5' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u10D0', '\u10F6' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1100');
        XML_NAME_ALLOWED_CHARACTERS.set('\u1102', '\u1103' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1105', '\u1107' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1109');
        XML_NAME_ALLOWED_CHARACTERS.set('\u110B', '\u110C' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u110E', '\u1112' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u113C');
        XML_NAME_ALLOWED_CHARACTERS.set('\u113E');
        XML_NAME_ALLOWED_CHARACTERS.set('\u1140');
        XML_NAME_ALLOWED_CHARACTERS.set('\u114C');
        XML_NAME_ALLOWED_CHARACTERS.set('\u114E');
        XML_NAME_ALLOWED_CHARACTERS.set('\u1150');
        XML_NAME_ALLOWED_CHARACTERS.set('\u1154', '\u1155' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1159');
        XML_NAME_ALLOWED_CHARACTERS.set('\u115F', '\u1161' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1163');
        XML_NAME_ALLOWED_CHARACTERS.set('\u1165');
        XML_NAME_ALLOWED_CHARACTERS.set('\u1167');
        XML_NAME_ALLOWED_CHARACTERS.set('\u1169');
        XML_NAME_ALLOWED_CHARACTERS.set('\u116D', '\u116E' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1172', '\u1173' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1175');
        XML_NAME_ALLOWED_CHARACTERS.set('\u119E');
        XML_NAME_ALLOWED_CHARACTERS.set('\u11A8');
        XML_NAME_ALLOWED_CHARACTERS.set('\u11AB');
        XML_NAME_ALLOWED_CHARACTERS.set('\u11AE', '\u11AF' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u11B7', '\u11B8' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u11BA');
        XML_NAME_ALLOWED_CHARACTERS.set('\u11BC', '\u11C2' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u11EB');
        XML_NAME_ALLOWED_CHARACTERS.set('\u11F0');
        XML_NAME_ALLOWED_CHARACTERS.set('\u11F9');
        XML_NAME_ALLOWED_CHARACTERS.set('\u1E00', '\u1E9B' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1EA0', '\u1EF9' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1F00', '\u1F15' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1F18', '\u1F1D' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1F20', '\u1F45' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1F48', '\u1F4D' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1F50', '\u1F57' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1F59');
        XML_NAME_ALLOWED_CHARACTERS.set('\u1F5B');
        XML_NAME_ALLOWED_CHARACTERS.set('\u1F5D');
        XML_NAME_ALLOWED_CHARACTERS.set('\u1F5F', '\u1F7D' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1F80', '\u1FB4' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1FB6', '\u1FBC' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1FBE');
        XML_NAME_ALLOWED_CHARACTERS.set('\u1FC2', '\u1FC4' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1FC6', '\u1FCC' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1FD0', '\u1FD3' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1FD6', '\u1FDB' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1FE0', '\u1FEC' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1FF2', '\u1FF4' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u1FF6', '\u1FFC' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u2126');
        XML_NAME_ALLOWED_CHARACTERS.set('\u212A', '\u212B' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u212E');
        XML_NAME_ALLOWED_CHARACTERS.set('\u2180', '\u2182' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u3041', '\u3094' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u30A1', '\u30FA' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u3105', '\u312C' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\uAC00', '\uD7A3' + 1);

        // XML Ideograph Character Set

        XML_NAME_ALLOWED_CHARACTERS.set('\u4E00', '\u9FA5' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u3007');
        XML_NAME_ALLOWED_CHARACTERS.set('\u3021', '\u3029' + 1);

        // XML Combining Character Set

        XML_NAME_ALLOWED_CHARACTERS.set('\u0300', '\u0345' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0360', '\u0361' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0483', '\u0486' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0591', '\u05A1' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u05A3', '\u05B9' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u05BB', '\u05BD' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u05BF');
        XML_NAME_ALLOWED_CHARACTERS.set('\u05C1', '\u05C2' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u05C4');
        XML_NAME_ALLOWED_CHARACTERS.set('\u064B', '\u0652' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0670');
        XML_NAME_ALLOWED_CHARACTERS.set('\u06D6', '\u06DC' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u06DD', '\u06DF' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u06E0', '\u06E4' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u06E7', '\u06E8' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u06EA', '\u06ED' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0901', '\u0903' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u093C');
        XML_NAME_ALLOWED_CHARACTERS.set('\u093E', '\u094C' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u094D');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0951', '\u0954' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0962', '\u0963' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0981', '\u0983' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u09BC');
        XML_NAME_ALLOWED_CHARACTERS.set('\u09BE');
        XML_NAME_ALLOWED_CHARACTERS.set('\u09BF');
        XML_NAME_ALLOWED_CHARACTERS.set('\u09C0', '\u09C4' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u09C7', '\u09C8' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u09CB', '\u09CD' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u09D7');
        XML_NAME_ALLOWED_CHARACTERS.set('\u09E2', '\u09E3' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A02');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A3C');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A3E');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A3F');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A40', '\u0A42' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A47', '\u0A48' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A4B', '\u0A4D' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A70', '\u0A71' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A81', '\u0A83' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0ABC');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0ABE', '\u0AC5' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0AC7', '\u0AC9' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0ACB', '\u0ACD' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B01', '\u0B03' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B3C');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B3E', '\u0B43' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B47', '\u0B48' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B4B', '\u0B4D' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B56', '\u0B57' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B82', '\u0B83' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0BBE', '\u0BC2' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0BC6', '\u0BC8' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0BCA', '\u0BCD' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0BD7');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0C01', '\u0C03' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0C3E', '\u0C44' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0C46', '\u0C48' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0C4A', '\u0C4D' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0C55', '\u0C56' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0C82', '\u0C83' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0CBE', '\u0CC4' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0CC6', '\u0CC8' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0CCA', '\u0CCD' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0CD5', '\u0CD6' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0D02', '\u0D03' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0D3E', '\u0D43' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0D46', '\u0D48' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0D4A', '\u0D4D' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0D57');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0E31');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0E34', '\u0E3A' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0E47', '\u0E4E' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0EB1');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0EB4', '\u0EB9' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0EBB', '\u0EBC' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0EC8', '\u0ECD' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0F18', '\u0F19' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0F35');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0F37');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0F39');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0F3E');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0F3F');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0F71', '\u0F84' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0F86', '\u0F8B' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0F90', '\u0F95' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0F97');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0F99', '\u0FAD' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0FB1', '\u0FB7' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0FB9');
        XML_NAME_ALLOWED_CHARACTERS.set('\u20D0', '\u20DC' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u20E1');
        XML_NAME_ALLOWED_CHARACTERS.set('\u302A', '\u302F' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u3099');
        XML_NAME_ALLOWED_CHARACTERS.set('\u309A');

        // XML Digits
        XML_NAME_ALLOWED_CHARACTERS.set('\u0030', '\u0039' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0660', '\u0669' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u06F0', '\u06F9' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0966', '\u096F' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u09E6', '\u09EF' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0A66', '\u0A6F' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0AE6', '\u0AEF' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0B66', '\u0B6F' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0BE7', '\u0BEF' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0C66', '\u0C6F' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0CE6', '\u0CEF' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0D66', '\u0D6F' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0E50', '\u0E59' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0ED0', '\u0ED9' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u0F20', '\u0F29' + 1);

        // XML Extenders
        XML_NAME_ALLOWED_CHARACTERS.set('\u00B7');
        XML_NAME_ALLOWED_CHARACTERS.set('\u02D0');
        XML_NAME_ALLOWED_CHARACTERS.set('\u02D1');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0387');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0640');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0E46');
        XML_NAME_ALLOWED_CHARACTERS.set('\u0EC6');
        XML_NAME_ALLOWED_CHARACTERS.set('\u3005');
        XML_NAME_ALLOWED_CHARACTERS.set('\u3031', '\u3035' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u309D', '\u309E' + 1);
        XML_NAME_ALLOWED_CHARACTERS.set('\u30FC', '\u30FE' + 1);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.text.TextDecoder#decode(java.lang.String)
     */
    public String decode( String encodedText ) {
        if (encodedText == null) return null;
        if (encodedText.length() < 7) {
            // Not big enough to have an encoded sequence
            return encodedText;
        }
        StringBuilder sb = new StringBuilder();
        char[] digits = new char[4];
        CharacterIterator iter = new StringCharacterIterator(encodedText);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            if (c == '_') {
                // Read the next character, if there is one ...
                char next = iter.next();
                if (next == CharacterIterator.DONE) {
                    sb.append(c);
                    break;
                }
                // If the next character is not 'x', then these are just regular characters ...
                if (next != 'x') {
                    sb.append(c).append(next);
                    continue;
                }
                // Read the next 4 characters (digits) and another '_' character ...
                digits[0] = iter.next();
                if (digits[0] == CharacterIterator.DONE) {
                    sb.append(c).append(next);
                    break;
                }
                digits[1] = iter.next();
                if (digits[1] == CharacterIterator.DONE) {
                    sb.append(c).append(next).append(digits, 0, 1);
                    break;
                }
                digits[2] = iter.next();
                if (digits[2] == CharacterIterator.DONE) {
                    sb.append(c).append(next).append(digits, 0, 2);
                    break;
                }
                digits[3] = iter.next();
                if (digits[3] == CharacterIterator.DONE) {
                    sb.append(c).append(next).append(digits, 0, 3);
                    break;
                }
                char underscore = iter.next();
                if (underscore != '_') { // includes DONE
                    sb.append(c).append(next).append(digits, 0, 4);
                    if (underscore == CharacterIterator.DONE) break;
                    sb.append(underscore);
                    continue;
                }
                // We've read all 4 digits, including the trailing '_'
                // Now parse into the resulting character
                try {
                    sb.appendCodePoint(Integer.parseInt(new String(digits), 16));
                } catch (NumberFormatException e) {
                    // code was not hexadecimal, so just write out the characters as is ...
                    sb.append(c).append(next).append(digits).append(underscore);
                }
            } else {
                // Just append other characters ...
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.text.TextEncoder#encode(java.lang.String)
     */
    public String encode( String text ) {
        if (text == null) return null;
        if (text.length() == 0) return text;
        StringBuilder sb = new StringBuilder();
        String hex = null;
        CharacterIterator iter = new StringCharacterIterator(text);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            if (c == '_') {
                // Read the next character (if there is one) ...
                char next = iter.next();
                if (next == CharacterIterator.DONE) {
                    sb.append(c);
                    break;
                }
                // If the next character is not 'x', then these are just regular characters ...
                if (next != 'x') {
                    sb.append(c).append(next);
                    continue;
                }
                // The next character is 'x', so write out the '_' character in encoded form ...
                sb.append("_x005f_");
                // And then write out the next character ...
                sb.append(next);
            } else if (XML_NAME_ALLOWED_CHARACTERS.get(c)) {
                // Legal characters for an XML Name ...
                sb.append(c);
            } else {
                // All other characters must be escaped with '_xHHHH_' where 'HHHH' is the hex string for the code point
                hex = Integer.toHexString(c);
                // The hex string excludes the leading '0's, so check the character values so we know how many to prepend
                if (c >= '\u0000' && c <= '\u000f') {
                    sb.append("_x000").append(hex);
                } else if (c >= '\u0010' && c <= '\u00ff') {
                    sb.append("_x00").append(hex);
                } else if (c >= '\u0100' && c <= '\u0fff') {
                    sb.append("_x0").append(hex);
                } else {
                    sb.append("_x").append(hex);
                }
                sb.append('_');
            }
        }
        return sb.toString();
    }

}
