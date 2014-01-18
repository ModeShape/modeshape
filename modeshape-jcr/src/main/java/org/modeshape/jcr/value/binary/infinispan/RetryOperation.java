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
package org.modeshape.jcr.value.binary.infinispan;

import java.io.IOException;
import org.modeshape.common.logging.Logger;

/**
 * Infinispan operation can fail e.g. due view changes of the cluster (transactions fails in these cases for example). In such
 * cases it make sense to repeat the operation to avoid unnecessary errors reported to the user.
 */
abstract class RetryOperation {

    private static final Logger LOGGER = Logger.getLogger(RetryOperation.class);

    private static final int RETRIES = 3;
    private static final long SLEEP_BETWEEN_RETRIES = 2000L;


    public boolean doTry() throws IOException {
        boolean sleepInterrupted = false;
        int failures = 0;
        IOException lastException = null;
        while (!sleepInterrupted && failures <= RETRIES) {
            try {
                return call();
            } catch (IOException ex) {
                lastException = ex;
            } catch (Exception ex) {
                lastException = new IOException(ex);
            }
            LOGGER.debug(lastException, "Failed to execute cache operation.");
            failures++;
            if (failures <= RETRIES) {
                try {
                    Thread.sleep(SLEEP_BETWEEN_RETRIES);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    sleepInterrupted = true;
                    LOGGER.debug("Retry interrupted. ");
                }
            }
        }
        assert lastException != null;
        throw lastException;
    }

    protected abstract boolean call() throws IOException;
}
