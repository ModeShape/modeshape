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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;

/**
 * A test class for {@link JcrUtils}.
 */
public final class JcrUtilsTest {

    private static final String PUBLIC_NAME = "a|b]c[d:e/f*g";
    private static final String JCR_NAME = "a" + '\uF07C' + 'b' + '\uF05D' + 'c' + '\uF05B' + 'd' + '\uF03A' + 'e' + '\uF02F'
                                           + 'f' + '\uF02A' + 'g';

    private JcrUtils utils;

    @Before
    public void createUtils() {
        this.utils = new JcrUtils();
    }

    @Test
    @FixFor( "MODE-1956" )
    public void shouldDecodeNameWithUnicodeSubstitutionCharacters() {
        assertThat(this.utils.decode(JCR_NAME), is(PUBLIC_NAME));
    }

    @Test
    @FixFor( "MODE-1956" )
    public void shouldEncodeNameWithIllegalCharacters() {
        assertThat(this.utils.encode(PUBLIC_NAME), is(JCR_NAME));
    }

}
