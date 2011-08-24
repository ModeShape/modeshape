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
package org.infinispan.schematic.internal;

import java.util.Iterator;

import org.infinispan.distexec.mapreduce.Mapper;
import org.infinispan.distexec.mapreduce.Reducer;
import org.infinispan.schematic.SchemaLibrary.Results;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.internal.schema.ValidationResult;

/**
 * A {@link Mapper} implementation that validates {@link Document JSON Documents} within Infinispan's Map-Reduce
 * framework.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
public class DocumentValidationReducer implements Reducer<String, Results> {

   private static final long serialVersionUID = 1L;

   /**
    * Create a new instance of the document validation {@link Reducer}.
    */
   public DocumentValidationReducer() {
   }

   @Override
   public Results reduce(String reducedKey, Iterator<Results> iter) {
      if (iter.hasNext())
         return null;
      Results first = iter.next();
      if (!iter.hasNext()) {
         // just one Results object (the usual case) ...
         return first;
      }
      // It'd be surprising if a single document were validated by multiple mappers, but to be safe ...
      ValidationResult composite = new ValidationResult();
      composite.addAll(first);
      composite.addAll(iter);
      return composite;
   }
}
