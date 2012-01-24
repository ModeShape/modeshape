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
package org.infinispan.schematic;

import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.JsonSchema.Type;
import org.infinispan.schematic.document.Path;

/**
 * A library of JSON Schema documents. Because JSON Schemas are in fact JSON documents, this library is also a DocumentLibrary.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
public interface SchemaLibrary extends DocumentLibrary {

    /**
     * Validate the supplied document against the JSON Schema with the supplied URI.
     * 
     * @param document the document to be validated; may not be null
     * @param schemaUri the URI of the JSON Schema that should be used to validate the document; may not be null
     * @return the results of the validation; never null
     */
    public Results validate( Document document,
                             String schemaUri );

    /**
     * Look for fields within the document (including nested documents) whose values are not of the expected type for the given
     * schema, but whose values can be converted into the expected type.
     * <p>
     * This is often useful when the results from {@link #validate(Document, String)} contain only
     * {@link Results#hasOnlyTypeMismatchErrors() mismatched type errors}. In such a case, the document can be converted and the
     * resulting document will likely satisfy the schema.
     * </p>
     * 
     * @param document the document to be validated; may not be null
     * @param results the results from the {@link #validate(Document, String)} call; may not be null
     * @return the converted document, or the same input <code>document</code> if the
     */
    public Document convertValues( Document document,
                                   Results results );

    /**
     * Look for fields within the document (including nested documents) whose values are not of the expected type for the given
     * schema, but whose values can be converted into the expected type.
     * <p>
     * This method is similar to {@link #convertValues(Document, Results)}, except that this method automatically runs a JSON
     * Schema validation to obtain the results. If you've already {@link #validate(Document, String) validated} the document, then
     * instead of calling this method (which would validate a second time) try calling {@link #convertValues(Document, Results)}.
     * </p>
     * 
     * @param document the document to be validated; may not be null
     * @param schemaUri the URI of the JSON Schema that should be used to validate the document; may not be null
     * @return the converted document, or the same input <code>document</code> if the
     */
    public Document convertValues( Document document,
                                   String schemaUri );

    /**
     * The results from a {@link SchemaLibrary#validate(Document, String) validation}.
     * 
     * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
     * @since 5.1
     */
    public interface Results extends Iterable<Problem> {
        /**
         * Determine if these results contain at least one error or warning. Calling this method is equivalent to (but more
         * efficient than) calling:
         * 
         * <pre>
         * hasErrors() || hasWarnings()
         * </pre>
         * 
         * or
         * 
         * <pre>
         * problemCount() != 0
         * </pre>
         * 
         * or
         * 
         * <pre>
         * errorCount() != 0 || warningCount() != 0
         * </pre>
         * 
         * @return <code>true</code> if there is at least one error or warning, or <code>false</code> if there are no errors or
         *         warnings
         */
        boolean hasProblems();

        /**
         * Determine if these results contain at least one error.
         * 
         * @return <code>true</code> if there is at least one error, or <code>false</code> if there are no errors
         */
        boolean hasErrors();

        /**
         * Determine if these results contain at least one warning.
         * 
         * @return <code>true</code> if there is at least one warning, or <code>false</code> if there are no errors
         */
        boolean hasWarnings();

        /**
         * Determine if these results contain only errors that are {@link MismatchedTypeProblem mismatched fields}, where the
         * value of a field has a type that does not match but can be converted to the type defined in the schema.
         * <p>
         * All type mismatch errors are considered {@link #hasErrors() errors}, but not all errors are type mismatch errors.
         * </p>
         * <p>
         * If this method return true, then consider calling {@link SchemaLibrary#convertValues(Document, Results)} to convert the
         * mismatched values and then revalidating.
         * </p>
         * 
         * @return <code>true</code> if there is at least one mismatched type error, or <code>false</code> if there are no errors
         * @see SchemaLibrary#convertValues(Document, Results)
         */
        boolean hasOnlyTypeMismatchErrors();

        /**
         * Determine the number of errors within these results.
         * 
         * @return the number of errors; always 0 or a positive number
         */
        int errorCount();

        /**
         * Determine the number of warnings within these results.
         * 
         * @return the number of warnings; always 0 or a positive number
         */
        int warningCount();

        /**
         * Determine the number of problems (that is, errors and warnings) within these results.
         * 
         * @return the number of errors and warnings; always 0 or a positive number
         */
        int problemCount();

    }

    public enum ProblemType {
        /** The problem type signaling a validation error. */
        ERROR,

        /** The problem type signaling a validation warning. */
        WARNING;
    }

    public interface Problem {
        /**
         * Get the type of problem.
         * 
         * @return the type; never null
         */
        ProblemType getType();

        /**
         * The path to the field about which this problem applies.
         * 
         * @return the path; never null
         */
        Path getPath();

        /**
         * Get the message describing the problem.
         * 
         * @return the message; never null
         */
        String getReason();

        /**
         * Get the exception that was the cause of this problem, if there was an exception.
         * 
         * @return the exception; may be null
         */
        Throwable getCause();
    }

    /**
     * A special type of problem where a field value was not of the expected type, but where the field value could be converted to
     * the expected type
     */
    public interface MismatchedTypeProblem extends Problem {
        /**
         * Get the actual field value.
         * 
         * @return the actual field value
         */
        Object getActualValue();

        /**
         * Get the converted field value that would satisfy the type expected by the schema.
         * 
         * @return the converted field value
         */
        Object getConvertedValue();

        Type getActualType();

        Type getExpectedType();
    }

}
