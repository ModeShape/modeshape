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
