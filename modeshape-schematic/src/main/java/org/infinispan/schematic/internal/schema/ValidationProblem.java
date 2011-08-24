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

import org.infinispan.schematic.SchemaLibrary.Problem;
import org.infinispan.schematic.SchemaLibrary.ProblemType;
import org.infinispan.schematic.document.Path;

public final class ValidationProblem implements Problem {
   private final ProblemType type;
   private final Path path;
   private final String reason;
   private final Throwable cause;

   public ValidationProblem(ProblemType type, Path path, String reason, Throwable cause) {
      this.type = type;
      this.path = path;
      this.reason = reason;
      this.cause = cause;
   }

   @Override
   public ProblemType getType() {
      return type;
   }

   @Override
   public Path getPath() {
      return path;
   }

   @Override
   public String getReason() {
      return reason;
   }

   @Override
   public Throwable getCause() {
      return cause;
   }

   @Override
   public String toString() {
      return "" + type + " at " + path + ": " + reason;
   }
}