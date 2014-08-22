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
package org.modeshape.jcr.api.index;

import org.modeshape.jcr.api.Problems;

/**
 * Exception used when one or more index definitions is invalid.
 */
public class InvalidIndexDefinitionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Problems problems;

    /**
     * Constructs a new instance of this class.
     * 
     * @param problems the validation problems for the index definitions(s)
     */
    public InvalidIndexDefinitionException( Problems problems ) {
        super(problems.toString());
        assert problems != null;
        this.problems = problems;
    }

    /**
     * Constructs a new instance of this class.
     * 
     * @param problems the validation problems for the index definitions(s)
     * @param message the message
     */
    public InvalidIndexDefinitionException( Problems problems,
                                             String message ) {
        super(message);
        assert problems != null;
        this.problems = problems;
    }

    /**
     * Constructs a new instance of this class.
     * 
     * @param problems the validation problems for the index definitions(s)
     * @param cause the cause of this exception
     */
    public InvalidIndexDefinitionException( Problems problems,
                                             Throwable cause ) {
        super(problems.toString(), cause);
        assert problems != null;
        this.problems = problems;
    }

    /**
     * Constructs a new instance of this class.
     * 
     * @param problems the validation problems for the index definitions(s)
     * @param message the message
     * @param cause the cause of this exception
     */
    public InvalidIndexDefinitionException( Problems problems,
                                             String message,
                                             Throwable cause ) {
        super(message, cause);
        assert problems != null;
        this.problems = problems;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * Get the problems associated with the index definition(s).
     * 
     * @return the problems; never null
     */
    public Problems getProblems() {
        return problems;
    }
}
