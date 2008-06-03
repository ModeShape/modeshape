/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.common.text;

/**
 * Interface for components that can encode text. This is the counterpart to {@link TextDecoder}.
 * 
 * @author Randall Hauch
 * @see TextDecoder
 */
public interface TextEncoder {

    /**
     * Returns the encoded version of a string.
     * 
     * @param text the text with characters that are to be encoded.
     * @return the text with the characters encoded as required, or null if the supplied text is null
     * @see TextDecoder#decode(String)
     */
    public String encode( String text );

}
