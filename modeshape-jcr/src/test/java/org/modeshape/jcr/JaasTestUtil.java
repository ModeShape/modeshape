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
package org.modeshape.jcr;

import org.picketbox.config.PicketBoxConfiguration;
import org.picketbox.factories.SecurityFactory;

/**
 * A simple utility for test cases to initialize the PicketBox JAAS implementation.
 */
public class JaasTestUtil {

    public static void initJaas( String picketBoxConfigurationFile ) {
        SecurityFactory.prepare();
        try {
            PicketBoxConfiguration idtrustConfig = new PicketBoxConfiguration();
            idtrustConfig.load(picketBoxConfigurationFile);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static void releaseJaas() {
        // don't release in our test cases ...
        // SecurityFactory.release();
    }

}
