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
package org.jboss.dna.common.jdbc.model.api;

/**
 * Provides RDBMS supported best row identifier pseudo types as enumeration set.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public enum ColumnPseudoType {

    UNKNOWN(0), // Indicates that the column may or may not be a pseudo column.
    NOT_PSEUDO(1), // Indicates that the column is NOT a pseudo column.
    PSEUDO(2); // Indicates that the column is a pseudo column.

    private final int type;

    ColumnPseudoType( int type ) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name();
    }
}
