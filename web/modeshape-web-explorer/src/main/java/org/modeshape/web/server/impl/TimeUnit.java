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

import java.io.Serializable;
import org.modeshape.jcr.api.monitor.Window;

/**
 * Local time units 
 * 
 * @author kulikov
 */
public class TimeUnit implements Serializable {
    private static final TimeUnit UNITS[] = {
        new TimeUnit(Window.PREVIOUS_60_SECONDS, "Previous 60 seconds"),
        new TimeUnit(Window.PREVIOUS_60_MINUTES, "Previous 60 minutes"),
        new TimeUnit(Window.PREVIOUS_24_HOURS, "Previous 24 hours"),
        new TimeUnit(Window.PREVIOUS_7_DAYS, "Previous 7 days"),
        new TimeUnit(Window.PREVIOUS_52_WEEKS, "Previous 52 weeks"),
    }; 
    
    private Window window;
    private String title;
    
    public TimeUnit() {
    }
    
    public TimeUnit(Window window, String title) {
        this.window = window;
        this.title = title;
    }
    
    public Window window() {
        return window;
    }
    
    public String title() {
        return title;
    }
    
    public static String[] names() {
        String[] names = new String[UNITS.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = UNITS[i].title();
        }
        return names;
    }
    
    public static TimeUnit find( String name ) {
        for (int i = 0; i < UNITS.length; i++) {
            if (UNITS[i].title().equals(name)) {
                return UNITS[i];
            }
        }
        return null;
    }
    
}
