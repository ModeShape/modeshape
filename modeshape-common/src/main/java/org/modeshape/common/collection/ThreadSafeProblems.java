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
package org.modeshape.common.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.modeshape.common.annotation.ThreadSafe;

/**
 * A thread-safe {@link Problems} collection. The problems will be {@link #iterator() returned} in the order in which they were
 * encountered.
 */
@ThreadSafe
public class ThreadSafeProblems extends AbstractProblems {
    private static final long serialVersionUID = 1L;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final List<Problem> problems = new LinkedList<Problem>();

    @Override
    public boolean hasErrors() {
        try {
            lock.readLock().lock();
            return super.hasErrors();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean hasProblems() {
        try {
            lock.readLock().lock();
            return super.hasProblems();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean hasInfo() {
        try {
            lock.readLock().lock();
            return super.hasInfo();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean hasWarnings() {
        try {
            lock.readLock().lock();
            return super.hasWarnings();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        try {
            lock.readLock().lock();
            return super.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        try {
            lock.readLock().lock();
            return super.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    protected void addProblem( Problem problem ) {
        try {
            lock.writeLock().lock();
            problems.add(problem);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void addAll( Iterable<Problem> problems ) {
        if (problems == this) return;
        try {
            lock.writeLock().lock();
            if (problems != null) {
                for (Problem problem : problems) {
                    this.problems.add(problem);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected List<Problem> getProblems() {
        // Return an unmodifiable copy ...
        try {
            lock.readLock().lock();
            return Collections.unmodifiableList(new ArrayList<Problem>(this.problems));
        } finally {
            lock.readLock().unlock();
        }
    }
}
