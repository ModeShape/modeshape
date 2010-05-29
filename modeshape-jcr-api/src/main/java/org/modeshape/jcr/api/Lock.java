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
package org.modeshape.jcr.api;

import javax.jcr.RepositoryException;

/**
 * Placeholder for JCR 2.0 Lock interface
 * 
 */
public interface Lock extends javax.jcr.lock.Lock {

    /**
     * Returns the number of seconds remaining until this locks times out. If the lock has already timed out, a negative value is
     * returned. If the number of seconds remaining is infinite or unknown, <code>Long.MAX_VALUE</code> is returned.
     * 
     * @return the number of seconds remaining until this lock times out.
     * @throws RepositoryException if an error occurs.
     * @since JCR 2.0
     */
    public long getSecondsRemaining() throws RepositoryException;

    /**
     * Returns <code>true</code> if the current session is the owner of this lock, either because it is session-scoped and bound
     * to this session or open-scoped and this session currently holds the token for this lock. Returns <code>false</code>
     * otherwise.
     * 
     * @return a <code>boolean</code>.
     * @since JCR 2.0
     */
    public boolean isLockOwningSession();

}
