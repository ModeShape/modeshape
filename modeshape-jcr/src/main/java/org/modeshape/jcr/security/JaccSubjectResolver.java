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
package org.modeshape.jcr.security;

import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.security.JaasProvider.SubjectResolver;

/**
 * A class that can resolve the current JAAS {@link Subject} using the JACC API. Note that the JACC API is assumed provided; if
 * not, loading this class will fail.
 * <p>
 * See <a href="https://issues.jboss.org/browse/MODE-1270">MODE-1270</a> for details.
 * </p>
 */
public class JaccSubjectResolver implements SubjectResolver {

    /** The JACC PolicyContext key for the current Subject */
    private static final String SUBJECT_CONTEXT_KEY = "javax.security.auth.Subject.container";

    private static volatile boolean reportedWarning = false;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.security.JaasProvider.SubjectResolver#resolveSubject()
     */
    @Override
    public Subject resolveSubject() {
        try {
            return (Subject)PolicyContext.getContext(SUBJECT_CONTEXT_KEY);
        } catch (Throwable e) {
            if (!reportedWarning) {
                reportedWarning = true;
                Logger.getLogger(getClass())
                      .debug(e,
                             "Failed to find the Subject at the 'javax.security.auth.Subject.container' key in the JACC PolicyContext. "
                             + "Check configuration and J2EE container's suggested approach for getting the JAAS Subject.");
            }
            return null;
        }
    }

}
