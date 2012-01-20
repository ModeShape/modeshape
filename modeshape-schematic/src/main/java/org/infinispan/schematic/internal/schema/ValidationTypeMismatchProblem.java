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
package org.infinispan.schematic.internal.schema;

import org.infinispan.schematic.SchemaLibrary.MismatchedTypeProblem;
import org.infinispan.schematic.SchemaLibrary.ProblemType;
import org.infinispan.schematic.document.Path;

public final class ValidationTypeMismatchProblem extends ValidationProblem implements MismatchedTypeProblem {
    private final Object actualValue;
    private final Object convertedValue;

    public ValidationTypeMismatchProblem( ProblemType type,
                                          Path path,
                                          Object actualValue,
                                          Object convertedValue,
                                          String reason,
                                          Throwable cause ) {
        super(type, path, reason, cause);
        this.actualValue = actualValue;
        this.convertedValue = convertedValue;
    }

    @Override
    public Object getActualValue() {
        return actualValue;
    }

    @Override
    public Object getConvertedValue() {
        return convertedValue;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
