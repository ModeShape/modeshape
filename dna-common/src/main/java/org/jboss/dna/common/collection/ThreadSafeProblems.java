/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.common.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.ThreadSafe;

/**
 * A thread-safe {@link Problems} collection. The problems will be {@link #iterator() returned} in the order in which they were
 * encountered.
 */
@ThreadSafe
public class ThreadSafeProblems extends AbstractProblems {
    private static final long serialVersionUID = 1L;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final List<Problem> problems = new LinkedList<Problem>();

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.AbstractProblems#hasErrors()
     */
    @Override
    public boolean hasErrors() {
        try {
            lock.readLock().lock();
            return super.hasErrors();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.AbstractProblems#hasProblems()
     */
    @Override
    public boolean hasProblems() {
        try {
            lock.readLock().lock();
            return super.hasProblems();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.AbstractProblems#hasInfo()
     */
    @Override
    public boolean hasInfo() {
        try {
            lock.readLock().lock();
            return super.hasInfo();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.AbstractProblems#hasWarnings()
     */
    @Override
    public boolean hasWarnings() {
        try {
            lock.readLock().lock();
            return super.hasWarnings();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.AbstractProblems#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        try {
            lock.readLock().lock();
            return super.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.AbstractProblems#size()
     */
    @Override
    public int size() {
        try {
            lock.readLock().lock();
            return super.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.AbstractProblems#addProblem(Problem)
     */
    @Override
    protected void addProblem( Problem problem ) {
        try {
            lock.writeLock().lock();
            problems.add(problem);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.AbstractProblems#getProblems()
     */
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
