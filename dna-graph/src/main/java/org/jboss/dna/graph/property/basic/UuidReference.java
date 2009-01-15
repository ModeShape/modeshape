/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.graph.property.basic;

import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Reference;

/**
 * @author Randall Hauch
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
