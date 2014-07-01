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

package org.modeshape.common.function;

import java.util.Objects;

/**
 * A simple predicate that takes a single argument.
 * 
 * @param <T> the type of input for the {@link #test(Object)} method
 * @author Randall Hauch (rhauch@redhat.com)
 */
public abstract class Predicate<T> {

    public abstract boolean test( T input );

    /**
     * Obtain a new predicate that performs the logical AND of this predicate and the supplied predicate.
     * 
     * @param other the other predicate
     * @return the composed predicate; never null
     */
    public Predicate<T> and( final Predicate<T> other ) {
        if (other == null || other == this) return this;
        return new Predicate<T>() {
            @Override
            public boolean test( T input ) {
                return Predicate.this.test(input) && other.test(input);
            }
        };
    }

    /**
     * Obtain a new predicate that performs the logical OR of this predicate and the supplied predicate.
     * 
     * @param other the other predicate
     * @return the composed predicate; never null
     */
    public Predicate<T> or( final Predicate<T> other ) {
        if (other == null || other == this) return this;
        return new Predicate<T>() {
            @Override
            public boolean test( T input ) {
                return Predicate.this.test(input) || other.test(input);
            }
        };
    }

    /**
     * Obtain a new predicate that performs the logical NOT of this predicate.
     * 
     * @return the composed predicate; never null
     */
    public Predicate<T> negate() {
        return new Predicate<T>() {
            @Override
            public boolean test( T input ) {
                return !Predicate.this.test(input);
            }

            @Override
            public Predicate<T> negate() {
                return Predicate.this;
            }
        };
    }

    /**
     * Return a predicate that is never satisfied.
     * 
     * @return the predicate; never null
     */
    public static <T> Predicate<T> never() {
        return new Predicate<T>() {
            @Override
            public boolean test( T input ) {
                return false;
            }
        };
    }

    /**
     * Return a predicate that is always satisfied.
     * 
     * @return the predicate; never null
     */
    public static <T> Predicate<T> always() {
        return new Predicate<T>() {
            @Override
            public boolean test( T input ) {
                return true;
            }
        };
    }

    /**
     * Return a predicate that is equivalent to {@link Objects#equals(Object, Object)} called with the supplied object and the
     * object passed as a parameter to the {@link #test(Object)} method.
     * 
     * @param obj the object to compare the test parameters
     * @return the new predicate; never null
     */
    public static <T> Predicate<T> isEqual( final Object obj ) {
        if (obj == null) {
            return new Predicate<T>() {
                @Override
                public boolean test( T input ) {
                    return input == null;
                }
            };
        }
        return new Predicate<T>() {
            @Override
            public boolean test( T input ) {
                return Objects.equals(input, obj);
            }
        };
    }
}
