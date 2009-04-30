/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
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
 * Database metadata method call related exception
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class DatabaseMetaDataMethodException extends Exception {
    // ~ Instance fields ------------------------------------------------------------------
    private static final long serialVersionUID = 5001714254060693493L;

    public static final String METHOD_FAILED = "Database Metadata method failed";

    /* database metadata method name that caused exception */
    private String methodName;

    // ~ Constructors ---------------------------------------------------------------------

    /**
     * Default constructor
     */
    public DatabaseMetaDataMethodException() {
        this(METHOD_FAILED, null, null);
    }

    /**
     * Constructor
     * 
     * @param message the explanation of exception
     * @param methodName the name of method that caused exception
     */
    public DatabaseMetaDataMethodException( String message,
                                            String methodName ) {
        this(message, methodName, null);
    }

    /**
     * Constructor
     * 
     * @param message the explanation of exception
     * @param methodName the name of method that caused exception
     * @param ex the exception that causes problem
     */
    public DatabaseMetaDataMethodException( String message,
                                            String methodName,
                                            Throwable ex ) {
        super(message, ex);

        // set method name
        setMethodName(methodName);
    }

    /**
     * Constructor
     * 
     * @param ex the exception that causes problem
     */
    public DatabaseMetaDataMethodException( Throwable ex ) {
        this(METHOD_FAILED, null, ex);
    }

    // ~ Methods --------------------------------------------------------------------------

    /**
     * Returns name of method that caused exception
     * 
     * @return name of method that caused exception
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Sets the name of method that caused exception
     * 
     * @param methodName the name of method that caused exception
     */
    public void setMethodName( String methodName ) {
        this.methodName = methodName;
    }
}
