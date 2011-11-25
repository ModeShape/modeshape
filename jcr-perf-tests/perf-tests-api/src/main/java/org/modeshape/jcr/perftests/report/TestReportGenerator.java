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
