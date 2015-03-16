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

import org.modeshape.jcr.api.monitor.ValueMetric;

/**
 *
 * @author kulikov
 */
public class MsValueMetric {

    private final static MsValueMetric[] METRICS = {
        new MsValueMetric(ValueMetric.SESSION_COUNT, "Session count"),
        new MsValueMetric(ValueMetric.QUERY_COUNT, "Query count"),
        new MsValueMetric(ValueMetric.WORKSPACE_COUNT, "Workspace count"),
        new MsValueMetric(ValueMetric.LISTENER_COUNT, "Listener count"),
        new MsValueMetric(ValueMetric.EVENT_QUEUE_SIZE, "Event queue size"),
        new MsValueMetric(ValueMetric.EVENT_COUNT, "Event count"),
        new MsValueMetric(ValueMetric.SESSION_SCOPED_LOCK_COUNT, "Session scoped lock count"),
        new MsValueMetric(ValueMetric.OPEN_SCOPED_LOCK_COUNT, "Open scoped lock count"),
        new MsValueMetric(ValueMetric.SESSION_SAVES, "Session saves"),
        new MsValueMetric(ValueMetric.NODE_CHANGES, "Node changes"),
        new MsValueMetric(ValueMetric.EVENT_QUEUE_SIZE, "Event Queue size"),
        new MsValueMetric(ValueMetric.SEQUENCED_COUNT, "Sequenced count")
    };
    
    private ValueMetric metric;
    private String title;

    public MsValueMetric(ValueMetric metric, String title) {
        this.metric = metric;
        this.title = title;
    }

    public ValueMetric metric() {
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
    
    public static MsValueMetric find( String name ) {
        for (int i = 0; i < METRICS.length; i++) {
            if (METRICS[i].title().equals(name)) {
                return METRICS[i];
            }
        }
        return null;
    }
    
}
