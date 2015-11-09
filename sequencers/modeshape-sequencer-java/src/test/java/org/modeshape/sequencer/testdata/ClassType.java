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
package org.modeshape.sequencer.testdata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is a class type test class.
 *
 * @author Schting Schtang
 * @param <T> a map
 * @since 2.0
 */
@SuppressWarnings( {"serial", "unused"} )
public abstract class ClassType<T extends HashMap<String, ?>> extends ArrayList<T> implements Serializable {

    /**
     * A constant.
     */
    public static final String CONSTANT = "constant"; // a line comment

    static {
        System.out.println(System.currentTimeMillis());
    }

    public Object twoString = new Object() {
        @Override
        public String toString() {
            return (super.toString() + super.toString());
        }
    };

    Number number = 1;

    private T[] t;

    /**
     * @return the identifier (never <code>null</code>)
     */
    abstract String getId();
   
    @SafeVarargs
    public final void set(@SuppressWarnings( "unchecked" ) T... t) throws Exception {
        if (this.t == t) {
            throw new Exception("Blah blah blah");
        }

        this.t = t;
    }

    public synchronized T[] get() {
        return this.t;
    }

    /**
     * Performs a shutdown.
     *
     * @param waitTime the time to wait in milliseconds before shutdown
     */
    @Deprecated
    public <U extends Number> void shutdown( final U waitTime ) {
        this.number = waitTime;
    }

    /**
     * The nested class.
     */
    class Blah implements Comparable<T> {

        private final T stuff;

        /**
         * Constructs a blah.
         *
         * @param stuff the stuff
         */
        public Blah( T stuff ) {
            this.stuff = stuff;
        }

        /**
         * {@inheritDoc}
         *
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo( T that ) {
            return 0;
        }

        /**
         * @return stuff
         */
        public T getStuff() {
            return stuff;
        }

    }

}
