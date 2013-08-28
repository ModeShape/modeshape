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
package org.modeshape.jcr.value.basic;

import java.util.UUID;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Reference;

/**
 * A {@link Reference} implementation that uses a single {@link UUID} as the pointer.
 * //TODO author=Horia Chiorean date=5/18/12 description=Decide if this reference implementation is still needed
 */
@Immutable
public class UuidReference implements Reference {

    private static final long serialVersionUID = 2299467578161645109L;
    private/*final*/UUID uuid;
    private/*final*/boolean isWeak;

    public UuidReference( UUID uuid ) {
        this.uuid = uuid;
        this.isWeak = false;
    }

    public UuidReference( UUID uuid,
                          boolean weak ) {
        this.uuid = uuid;
        this.isWeak = weak;
    }

    /**
     * @return uuid
     */
    public UUID getUuid() {
        return this.uuid;
    }

    @Override
    public String getString() {
        return this.uuid.toString();
    }

    @Override
    public String getString( TextEncoder encoder ) {
        if (encoder == null) encoder = Path.DEFAULT_ENCODER;
        return encoder.encode(getString());
    }

    @Override
    public boolean isWeak() {
        return isWeak;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public boolean isForeign() {
        return false;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public int compareTo( Reference that ) {
        if (this == that) return 0;
        if (this.isWeak()) {
            if (!that.isWeak()) return -1;
        } else {
            if (that.isWeak()) return 1;
        }
        if (that instanceof UuidReference) {
            return this.uuid.compareTo(((UuidReference)that).getUuid());
        }
        return this.getString().compareTo(that.getString());
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof UuidReference) {
            UuidReference that = (UuidReference)obj;
            return this.isWeak() == that.isWeak() && this.uuid.equals(that.getUuid());
        }
        if (obj instanceof Reference) {
            Reference that = (Reference)obj;
            return this.isWeak() == that.isWeak() && this.getString().equals(that.getString());
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return this.uuid.toString();
    }
}
