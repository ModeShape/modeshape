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
package org.modeshape.jcr;

import org.modeshape.common.collection.Problems;

/**
 * An exception signalling errors in a {@link RepositoryConfiguration configuration}.
 */
public class ConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Problems results;

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
