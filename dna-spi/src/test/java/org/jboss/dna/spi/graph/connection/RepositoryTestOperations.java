/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.spi.graph.connection;

import org.jboss.dna.common.util.Logger;

/**
 * @author Randall Hauch
 */
public class RepositoryTestOperations {

    /**
     * Return an operation factory that produces {@link RepositoryOperation} instances that each call
     * {@link RepositoryConnection#execute(ExecutionEnvironment, org.jboss.dna.spi.graph.commands.GraphCommand...)} the supplied
     * number of times, intermixed with random math operations and {@link Thread#yield() yielding}.
     * @param env the environment
     * @param callsPerOperation the number of <code>load</code> calls per RepositoryOperation
     * @return the factory
     */
    public static RepositoryOperation.Factory<Integer> createMultipleLoadOperationFactory( final ExecutionEnvironment env, final int callsPerOperation ) {
        return new RepositoryOperation.Factory<Integer>() {

            public RepositoryOperation<Integer> create() {
                return new CallLoadMultipleTimes(env, callsPerOperation);
            }
        };
    }

    public static class CallLoadMultipleTimes implements RepositoryOperation<Integer> {

        private final int count;
        private final ExecutionEnvironment env;

        public CallLoadMultipleTimes( ExecutionEnvironment env, int count ) {
            this.count = count;
            this.env = env;
        }

        /**
         * {@inheritDoc}
         */
        public String getName() {
            return Thread.currentThread().getName() + "-CallLoadMultipleTimes";
        }

        /**
         * {@inheritDoc}
         */
        public Integer run( RepositoryConnection connection ) throws InterruptedException {
            Logger.getLogger(RepositoryTestOperations.class).debug("Running {} operation", this.getClass().getSimpleName());
            int total = count;
            for (int i = 0; i != count; ++i) {
                // Add two random numbers ...
                int int1 = random(this.hashCode() ^ (int)System.nanoTime() * i);
                if (i % 2 == 0) {
                    Thread.yield();
                }
                connection.execute(env);
                int int2 = random(this.hashCode() ^ (int)System.nanoTime() + i);
                total += Math.min(Math.abs(Math.max(int1, int2) + int1 * int2 / 3), count);
            }
            Logger.getLogger(RepositoryTestOperations.class).debug("Finishing {} operation", this.getClass().getSimpleName());
            return total < count ? total : count; // should really always return count
        }
    }

    /**
     * A "random-enough" number generator that is cheap and that has no synchronization issues (like some other random number
     * generators).
     * <p>
     * This was taken from <a href="http://wwww.jcip.org">Java Concurrency In Practice</a> (page 253).
     * </p>
     * @param seed the seed, typically based on a hash code and nanoTime
     * @return a number that is "random enough"
     */
    public static int random( int seed ) {
        seed ^= (seed << 6);
        seed ^= (seed >>> 21);
        seed ^= (seed << 7);
        return seed;
    }

}
