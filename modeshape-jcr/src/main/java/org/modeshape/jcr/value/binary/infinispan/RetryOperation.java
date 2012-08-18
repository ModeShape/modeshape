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
package org.modeshape.jcr.value.binary.infinispan;

import java.io.IOException;
import org.modeshape.common.logging.Logger;

/**
 * Infinispan operation can fail e.g. due view changes of the cluster (transactions fails in these cases for example). In such
 * cases it make sense to repeat the operation to avoid unnecessary errors reported to the user.
 */
abstract class RetryOperation {

    private final Logger logger;

    private static final int RETRIES = 3;
    private static final long SLEEP_BETWEEN_RETRIES = 2000L;

    public RetryOperation() {
        logger = Logger.getLogger(getClass());
    }

    public void doTry() throws IOException {
        boolean sleepInterrupted = false;
        int failures = 0;
        IOException lastException = null;
        while (!sleepInterrupted && failures <= RETRIES) {
            try {
                call();
                return;
            } catch (IOException ex) {
                lastException = ex;
            } catch (Exception ex) {
                lastException = new IOException(ex);
            }
            // todo detailed info about operation
            logger.debug(lastException, "Failed to execute cache operation.");
            failures++;
            if (failures <= RETRIES) {
                try {
                    Thread.sleep(SLEEP_BETWEEN_RETRIES);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    sleepInterrupted = true;
                    logger.debug("Retry interrupted. ");
                }
            }
        }
        assert lastException != null;
        throw lastException;
    }

    protected abstract void call() throws IOException;
}
