/*
 * JBoss, Home of Professional Open Source
 * Copyright [2011], Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.perftests.init;

import org.modeshape.jcr.perftests.AbstractPerformanceTestSuite;
import org.modeshape.jcr.perftests.SuiteConfiguration;

/**
 * Test which opens a {@link OpenAuthenticatedSessionTestSuite#SESSIONS_COUNT} number of authenticated sessions on a repository.
 * The credentials used by this test are those provided to the runner configuration {@see org.modeshape.jcr.perftests.RunnerConfiguration}
 *
 * @author Horia Chiorean
 */
public class OpenAuthenticatedSessionTestSuite extends AbstractPerformanceTestSuite {

   private static final int SESSIONS_COUNT = 10;

    public OpenAuthenticatedSessionTestSuite( SuiteConfiguration suiteConfiguration ) {
        super(suiteConfiguration);
    }

    @Override
    protected void runTest() throws Exception {
        for (int i = 0; i < SESSIONS_COUNT; i++) {
            newSession();
        }
    }
}
