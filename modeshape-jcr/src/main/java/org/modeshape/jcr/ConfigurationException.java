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
package org.modeshape.jcr;

import org.modeshape.common.collection.Problems;

/**
 * An exception signalling errors in a {@link RepositoryConfiguration configuration}.
 */
public class ConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Problems results;


    /**
     * @param message the message
     */
    public ConfigurationException( String message ) {
        super(message);
        this.results = null;
    }

    /**
     * @param results the validation results for the configuration
     */
    public ConfigurationException( Problems results ) {
        super();
        this.results = results;
    }

    /**
     * @param results the validation results for the configuration
     * @param message the message
     */
    public ConfigurationException( Problems results,
                                   String message ) {
        super(message);
        this.results = results;
    }

    /**
     * @param results the validation results for the configuration
     * @param cause the cause of this exception
     */
    public ConfigurationException( Problems results,
                                   Throwable cause ) {
        super(cause);
        this.results = results;
    }

    /**
     * @param results the validation results for the configuration
     * @param message the message
     * @param cause the cause of this exception
     */
    public ConfigurationException( Problems results,
                                   String message,
                                   Throwable cause ) {
        super(message, cause);
        this.results = results;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * @return results
     */
    public Problems getProblems() {
        return results;
    }
}
