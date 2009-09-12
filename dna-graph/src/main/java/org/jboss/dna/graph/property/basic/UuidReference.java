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
package org.jboss.dna.graph.property.basic;

import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Reference;

/**
 * A {@link Reference} implementation that uses a single {@link UUID} as the pointer.
 */
@Immutable
public class UuidReference implements Reference {

    /**
     */
    private static final long serialVersionUID = 2299467578161645109L;
    private UUID uuid;

    public UuidReference( UUID uuid ) {
        this.uuid = uuid;
    }

    /**
     * @return uuid
     */
    public UUID getUuid() {
        return this.uuid;
    }

    /**
     * {@inheritDoc}
     */
    public String getString() {
        return this.uuid.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getString( TextEncoder encoder ) {
        if (encoder == null) encoder = Path.DEFAULT_ENCODER;
        return encoder.encode(getString());
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( Reference that ) {
        if (this == that) return 0;
        if (that instanceof UuidReference) {
            return this.uuid.compareTo(((UuidReference)that).getUuid());
        }
        return this.getString().compareTo(that.getString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof UuidReference) {
            return this.uuid.equals(((UuidReference)obj).getUuid());
        }
        if (obj instanceof Reference) {
            return this.getString().equals(((Reference)obj).getString());
        }
        return super.equals(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.uuid.toString();
    }

}
