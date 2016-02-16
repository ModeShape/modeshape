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
package org.modeshape.schematic.document;

import java.util.Date;
import org.modeshape.schematic.annotation.Immutable;

/**
 * A {@link Bson.Type#TIMESTAMP timestamp} value for use within a {@link Document BSON object}. <b>Note that using {@link Date} is
 * recommended for most applications and use cases, as they are more generally suited for representing instants in time.</b>
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
@Immutable
public final class Timestamp {

    private final Date time;
    private final int inc;

    public Timestamp() {
        this.time = null;
        this.inc = 0;
    }

    public Timestamp( Date date ) {
        this((int)(date.getTime() / 1000L), 0);
    }

    public Timestamp( int timeInSeconds,
                      int inc ) {
        this.time = new Date(timeInSeconds * 1000L);
        this.inc = inc;
    }

    public int getTime() {
        return time == null ? 0 : (int)(time.getTime() / 1000L);
    }

    public int getInc() {
        return inc;
    }

    public Date getAsDate() {
        return new Date(time.getTime()); // make defensive copy
    }

    @Override
    public int hashCode() {
        return getTime(); // note that Date.hashCode() calls Date.getTime(), so our approach works great
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Timestamp) {
            Timestamp that = (Timestamp)obj;
            return this.getInc() == that.getInc() && this.getTime() == that.getTime();
        }
        return false;
    }

    @Override
    public String toString() {
        return "TS(" + time + ':' + inc + ')';
    }
}
