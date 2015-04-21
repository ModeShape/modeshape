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

package org.modeshape.jcr.clustering;

import org.modeshape.common.i18n.I18n;

/**
 * Class which holds i18n messages for {@link ClusteringService}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ClusteringI18n {
    public static I18n channelNotConnected;
    public static I18n errorSendingMessage;
    public static I18n errorReceivingMessage;
    public static I18n memberOfClusterIsSuspect;
    public static I18n channelConfigurationError;

    private ClusteringI18n() {
    }

    static {
        try {
            I18n.initialize(ClusteringI18n.class);
        } catch (final Exception err) {
            // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
            System.err.println(err);
        }
    }
}
