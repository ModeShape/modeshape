/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.infinispan.schematic.document;

import org.infinispan.marshall.SerializeWith;

/**
 * A {@link Bson.Type#JAVASCRIPT_WITH_SCOPE JavaScript code with scope} value for use within a {@link Document BSON Object}.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
@Immutable
@SerializeWith( Code.Externalizer.class )
public final class CodeWithScope extends Code {

    private final Document scope;

    public CodeWithScope( String code,
                          Document scope ) {
        super(code);
        this.scope = scope;
        assert this.scope != null;
    }

    public Document getScope() {
        return scope;
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof CodeWithScope) {
            CodeWithScope that = (CodeWithScope)obj;
            return this.getCode().equals(that.getCode()) && this.getScope().equals(that.getScope());
        }
        return false;
    }

    @Override
    public String toString() {
        return "CodeWithScope (" + getCode() + ':' + getScope() + ')';
    }

}
