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
package org.jboss.dna.spi;

import java.security.AccessControlContext;
import java.security.AccessController;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

/**
 * A factory for creating {@link ExecutionContext} instances. Each execution context is affiliated with a JAAS {@link Subject},
 * and thus the factory methods take the same parameters that the JAAS {@link LoginContext} take.
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
public interface ExecutionContextFactory {

    /**
     * Creates an {@link ExecutionContext} using a snapshot of the {@link AccessControlContext access control context} obtained
     * from the current calling context.
     * 
     * @return the execution context; never <code>null</code>.
     * @see AccessController#getContext()
     */
    ExecutionContext create();

    /**
     * Creates an {@link ExecutionContext} using the supplied {@link AccessControlContext access control context}.
     * 
     * @param accessControlContext An access control context.
     * @return the execution context; never <code>null</code>.
     * @throws IllegalArgumentException if <code>accessControlContext</code> is <code>null</code>.
     */
    ExecutionContext create( AccessControlContext accessControlContext );

    /**
     * Create an {@link ExecutionContext} for the supplied {@link LoginContext}.
     * 
     * @param loginContext the JAAS login context
     * @return the execution context
     * @throws IllegalArgumentException if the <code>loginContext</code> is null
     */
    ExecutionContext create( LoginContext loginContext );

    /**
     * @param name the name of the JAAS login context
     * @return the execution context
     * @throws IllegalArgumentException if the <code>name</code> is null
     * @throws LoginException if there <code>name</code> is invalid (or there is no login context named "other"), or if the
     *         default callback handler JAAS property was not set or could not be loaded
     */
    ExecutionContext create( String name ) throws LoginException;

    /**
     * @param name the name of the JAAS login context
     * @param subject the subject to authenticate
     * @return the execution context
     * @throws LoginException if there <code>name</code> is invalid (or there is no login context named "other"), if the default
     *         callback handler JAAS property was not set or could not be loaded, or if the <code>subject</code> is null or
     *         unknown
     */
    ExecutionContext create( String name,
                             Subject subject ) throws LoginException;

    /**
     * @param name the name of the JAAS login context
     * @param callbackHandler the callback handler that will be used by {@link LoginModule}s to communicate with the user.
     * @return the execution context
     * @throws LoginException if there <code>name</code> is invalid (or there is no login context named "other"), or if the
     *         <code>callbackHandler</code> is null
     */
    ExecutionContext create( String name,
                             CallbackHandler callbackHandler ) throws LoginException;

    /**
     * @param name the name of the JAAS login context
     * @param subject the subject to authenticate
     * @param callbackHandler the callback handler that will be used by {@link LoginModule}s to communicate with the user.
     * @return the execution context
     * @throws LoginException if there <code>name</code> is invalid (or there is no login context named "other"), if the default
     *         callback handler JAAS property was not set or could not be loaded, if the <code>subject</code> is null or unknown,
     *         or if the <code>callbackHandler</code> is null
     */
    ExecutionContext create( String name,
                             Subject subject,
                             CallbackHandler callbackHandler ) throws LoginException;

}
