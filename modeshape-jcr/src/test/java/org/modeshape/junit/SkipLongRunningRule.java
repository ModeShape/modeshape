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

package org.modeshape.junit;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * JUnit rule that inspects the presence of the {@link SkipLongRunning} annotation either on a test method or on a test suite.
 * If it finds the annotation, it will only run the test method/suite if the system property {@code skipLongRunningTests}
 * has the value {@code true}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class SkipLongRunningRule implements TestRule {

    private static final Statement EMPTY_STATEMENT = new EmptyStatement();
    private static final String SKIP_LONG_RUNNING_PROPERTY = "skipLongRunningTests";

    @Override
    public Statement apply( Statement base,
                            Description description ) {
        SkipLongRunning ignoreIfLongRunning = description.getAnnotation(SkipLongRunning.class);
        if (ignoreIfLongRunning == null) {
            if (description.isTest()) {
                //search for the annotation on the test suite class
                Class<?> testClass = description.getTestClass();
                if (!testClass.isAnnotationPresent(SkipLongRunning.class)) {
                    return base;
                }
            } else {
                return base;
            }
        }

        String skipLongRunning = System.getProperty(SKIP_LONG_RUNNING_PROPERTY);
        if (skipLongRunning != null && skipLongRunning.equalsIgnoreCase(Boolean.TRUE.toString())) {
            return EMPTY_STATEMENT;
        }

        return base;
    }


    private static class EmptyStatement extends Statement {
        @Override
        public void evaluate() throws Throwable {
            //do nothing, this is the equivalent of an empty test
        }
    }
}
