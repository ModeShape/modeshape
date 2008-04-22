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

package org.jboss.dna.common.util;

import org.jboss.dna.common.CommonI18n;
import org.jboss.dna.common.util.LogContext;
import org.jboss.dna.common.util.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LogContextTest {

    private Logger logger;

    @Before
    public void beforeEach() {
        logger = Logger.getLogger(LoggerTest.class);
    }

    @After
    public void afterEach() {
        LogContext.clear();
    }

    @Test
    public void shouldAcceptValidKeys() {
        LogContext.set("username", "jsmith");
        logger.info(CommonI18n.passthrough, "tracking activity for username");
        logger.info(CommonI18n.passthrough, "A second log message");
    }

}
