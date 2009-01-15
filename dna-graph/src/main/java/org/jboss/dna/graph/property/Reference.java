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
package org.jboss.dna.graph.property;

import java.io.Serializable;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextEncoder;

/**
 * A representation of a reference to another node. Node references may not necessarily resolve to an existing node.
 * @author Randall Hauch
 */
@Immutable
public interface Reference extends Comparable<Reference>, Serializable {

    /**
     * Get the string form of the Reference. The {@link Path#DEFAULT_ENCODER default encoder} is used to encode characters in the
     * reference.
     * @return the encoded string
     * @see #getString(TextEncoder)
     */
    public String getString();

    /**
     * Get the encoded string form of the Reference, using the supplied encoder to encode characters in the reference.
     * @param encoder the encoder to use, or null if the {@link Path#DEFAULT_ENCODER default encoder} should be used
     * @return the encoded string
     * @see #getString()
     */
    public String getString( TextEncoder encoder );

}
