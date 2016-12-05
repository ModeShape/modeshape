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
package org.modeshape.jcr.perftests.report;

import org.modeshape.jcr.perftests.TestData;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

/**
 * Base class which should be extended by classes which generate test reports based on a
 * {@link org.modeshape.jcr.perftests.TestData} object
 *
 * @author Horia Chiorean
 */
public abstract class TestReportGenerator {

    /** the time unit to which test data values should be converted */
    protected final TimeUnit timeUnit;

    /** the name of the report file which will be written by default in the root of the current directory, from the point of view of the classloader */
    protected final String reportFileName;

    public TestReportGenerator( String reportFileName, TimeUnit timeUnit ) {
        this.timeUnit = timeUnit;
        this.reportFileName = reportFileName;
    }

    /**
     * Generates a report based on the provided test data
     *
     * @param testData a <code>TestData</code> instance which must be non-null.
     * @throws Exception if anything goes wrong during the report generation.
     */
    public abstract void generateReport( TestData testData ) throws Exception;

    protected File getReportDir() throws URISyntaxException {
        File reportDir = new File(getClass().getClassLoader().getResource(".").toURI());
        if (!reportDir.exists() || !reportDir.isDirectory()) {
            throw new IllegalStateException("Cannot locate target folder for performance report");
        }
        return reportDir;
    }

    protected File getReportFile() throws URISyntaxException {
        return new File(getReportDir(), reportFileName);
    }
}
