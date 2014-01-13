/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
