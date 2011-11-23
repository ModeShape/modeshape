/*
 *
 *  * ModeShape (http://www.modeshape.org)
 *  * See the COPYRIGHT.txt file distributed with this work for information
 *  * regarding copyright ownership.  Some portions may be licensed
 *  * to Red Hat, Inc. under one or more contributor license agreements.
 *  * See the AUTHORS.txt file in the distribution for a full listing of
 *  * individual contributors.
 *  *
 *  * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 *  * is licensed to you under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * ModeShape is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package org.modeshape;

import org.junit.Test;
import org.modeshape.jcr.JcrRepositoryFactory;
import org.modeshape.jcr.perftests.PerformanceTestSuiteRunner;
import org.modeshape.jcr.perftests.RunnerConfiguration;
import org.modeshape.jcr.perftests.read.BigFileReadTestSuite;
import org.modeshape.jcr.perftests.read.ConcurrentReadTestSuite;
import org.modeshape.jcr.perftests.read.SmallFileReadTestSuite;
import org.modeshape.jcr.perftests.write.ConcurrentReadWriteTestSuite;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Runs the performance tests against a Modeshape 2.x repo.
 *
 * @author Horia Chiorean
 */
public class ModeShape2xPerformanceTest {

    @Test
    public void testModeShapeInMemory() throws Exception {
        Map<String, URL> parameters = new HashMap<String, URL>();
        parameters.put(JcrRepositoryFactory.URL, getClass().getClassLoader().getResource("configRepository.xml"));
        //TODO author=Horia Chiorean date=11/22/11 description=some tests excluded because of various problems
        RunnerConfiguration runnerConfig = new RunnerConfiguration().addTestsToExclude(
                ConcurrentReadTestSuite.class.getSimpleName(),//deadlock
                ConcurrentReadWriteTestSuite.class.getSimpleName(),//deadlock
                BigFileReadTestSuite.class.getSimpleName(), //binary incompatibility
                SmallFileReadTestSuite.class.getSimpleName()); //binary incompatibility
        new PerformanceTestSuiteRunner(runnerConfig).runPerformanceTests(parameters, null);
    }
}
