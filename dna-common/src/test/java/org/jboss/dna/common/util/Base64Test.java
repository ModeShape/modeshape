/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.common.util;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class Base64Test {

    // =========================================================================
    // H E L P E R M E T H O D S
    // =========================================================================

    // =========================================================================
    // T E S T C A S E S
    // =========================================================================

    @Test
    public void testBasicExamples() {
        // Make up some source objects
        byte[] originalBytes = {(byte)-2, (byte)-1, (byte)0, (byte)1, (byte)2};

        // Display original array
        System.out.println("\n\nOriginal array: ");
        for (int i = 0; i < originalBytes.length; i++)
            System.out.print(originalBytes[i] + " ");
        System.out.println();

        // Encode serialized bytes
        String encBytes = Base64.encodeBytes(originalBytes);

        // Print encoded bytes
        System.out.println("Bytes, encoded ( " + encBytes.getBytes().length + " bytes):\n" + encBytes);

        // Decode bytes
        byte[] decBytes = Base64.decode(encBytes);

        // Display decoded bytes
        System.out.println("Encoded Bytes -> decoded: ");
        for (int i = 0; i < decBytes.length; i++)
            System.out.print(decBytes[i] + " ");
        System.out.println();
    }

    @Test( expected = NullPointerException.class )
    public void testEncodeNullByteArray() {
        Base64.encodeBytes(null);
    }

    @Test
    public void testEncodeEmptyByteArray() {
        String result = Base64.encodeBytes(new byte[] {});
        assertThat(result, is(notNullValue()));
        assertThat(result.length(), is(0));
    }

}
