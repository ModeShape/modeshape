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
package org.modeshape.web.server.impl;

import org.modeshape.jcr.api.monitor.DurationMetric;

/**
 *
 * @author kulikov
 */
public class MsDurationMetric {
    private final static MsDurationMetric[] METRICS = {
        new MsDurationMetric(DurationMetric.QUERY_EXECUTION_TIME, "Query execution time"),
        new MsDurationMetric(DurationMetric.SEQUENCER_EXECUTION_TIME, "Sequencer execution time"),
        new MsDurationMetric(DurationMetric.SESSION_LIFETIME, "Session Life time")
    };
    
    private DurationMetric metric;
    private String title;

    public MsDurationMetric(DurationMetric metric, String title) {
        this.metric = metric;
        this.title = title;
    }

    public DurationMetric metric() {
        return metric;
    }

    public String title() {
        return title;
    }
    
    public static String[] getNames() {
        String[] names = new String[METRICS.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = METRICS[i].title();
        }
        return names;
    }
    
    public static MsDurationMetric find( String name ) {
        for (int i = 0; i < METRICS.length; i++) {
            if (METRICS[i].title().equals(name)) {
                return METRICS[i];
            }
        }
        return null;
    }
    
}
