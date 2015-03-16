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
package org.modeshape.web.shared;

import java.io.Serializable;

/**
 *
 * @author kulikov
 */
public class Stats implements Serializable {
    
    private String time;
    private double min, max, avg;
    
    public Stats() {
    }
    
    public Stats(double min, double max, double avg) {
        this.min = min;
        this.max = max;
        this.avg = avg;
    }

    public String time() {
        return time;
    }
    
    public double min() {
        return min;
    }
    
    public double max() {
        return max;
    }
    
    public double avg() {
        return avg;
    }
}
