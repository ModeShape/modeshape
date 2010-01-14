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

import net.jcip.annotations.Immutable;

/**
 * An encoder implementation that does nothing. This is useful when a {@link TextEncoder encoder} is optional but the code is
 * easier to write when there is always an encoder.
 */
@Immutable
public class NoOpEncoder implements TextEncoder, TextDecoder {

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
