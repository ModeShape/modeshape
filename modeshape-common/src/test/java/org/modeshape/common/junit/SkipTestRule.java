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

package org.modeshape.common.junit;

import java.lang.annotation.Annotation;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.modeshape.common.util.StringUtil;

/**
 * JUnit rule that inspects the presence of the {@link SkipLongRunning} annotation either on a test method or on a test suite. If
 * it finds the annotation, it will only run the test method/suite if the system property {@code skipLongRunningTests} has the
 * value {@code true}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class SkipTestRule implements TestRule {

    @Override
    public Statement apply( Statement base,
                            Description description ) {
        SkipLongRunning skipLongRunningAnnotation = hasAnnotation(description, SkipLongRunning.class);
        if (skipLongRunningAnnotation != null) {
            boolean skipLongRunning = Boolean.valueOf(System.getProperty(SkipLongRunning.SKIP_LONG_RUNNING_PROPERTY));
            if (skipLongRunning) {
                return emptyStatement(skipLongRunningAnnotation.value(), description);
            }
        }

        SkipOnOS skipOnOSAnnotation = hasAnnotation(description, SkipOnOS.class);
        if (skipOnOSAnnotation != null) {
            String[] oses = skipOnOSAnnotation.value();
            String osName = System.getProperty("os.name");
            if (!StringUtil.isBlank(osName)) {
                for (String os : oses) {
                    if (osName.toLowerCase().startsWith(os.toLowerCase())) {
                        return emptyStatement(skipOnOSAnnotation.description(), description);
                    }
                }
            }
        }

        return base;
    }

    private <T extends Annotation> T hasAnnotation( Description description, Class<T> annotationClass ) {
        T annotation = description.getAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        } else if (description.isTest() && description.getTestClass().isAnnotationPresent(annotationClass)) {
            return description.getTestClass().getAnnotation(annotationClass);
        }
        return null;
    }

    private static Statement emptyStatement( final String reason, final Description description ) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                StringBuilder messageBuilder = new StringBuilder(description.testCount());
                messageBuilder.append("Skipped ").append(description.toString());
                if (!StringUtil.isBlank(reason)) {
                    messageBuilder.append(" because: ").append(reason);
                }
                System.out.println(messageBuilder.toString());
            }
        };
    }
}
