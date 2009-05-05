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

import java.util.Iterator;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.CheckArg;

/**
 * An immutable wrapper for a mutable {@link Problems}.
 */
@Immutable
public class ImmutableProblems implements Problems {

    private final Problems delegate;

    public ImmutableProblems( Problems delegate ) {
        CheckArg.isNotNull(delegate, "delegate");
        this.delegate = delegate;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addError(org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void addError( I18n message,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addError(org.jboss.dna.common.i18n.I18n, java.lang.String, java.lang.String,
     *      java.lang.Object[])
     */
    public void addError( I18n message,
                          String resource,
                          String location,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addError(int, org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void addError( int code,
                          I18n message,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addError(int, org.jboss.dna.common.i18n.I18n, java.lang.String,
     *      java.lang.String, java.lang.Object[])
     */
    public void addError( int code,
                          I18n message,
                          String resource,
                          String location,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addError(java.lang.Throwable, org.jboss.dna.common.i18n.I18n,
     *      java.lang.Object[])
     */
    public void addError( Throwable throwable,
                          I18n message,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addError(java.lang.Throwable, org.jboss.dna.common.i18n.I18n,
     *      java.lang.String, java.lang.String, java.lang.Object[])
     */
    public void addError( Throwable throwable,
                          I18n message,
                          String resource,
                          String location,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addError(java.lang.Throwable, int, org.jboss.dna.common.i18n.I18n,
     *      java.lang.Object[])
     */
    public void addError( Throwable throwable,
                          int code,
                          I18n message,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addError(java.lang.Throwable, int, org.jboss.dna.common.i18n.I18n,
     *      java.lang.String, java.lang.String, java.lang.Object[])
     */
    public void addError( Throwable throwable,
                          int code,
                          I18n message,
                          String resource,
                          String location,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addInfo(org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void addInfo( I18n message,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addInfo(org.jboss.dna.common.i18n.I18n, java.lang.String, java.lang.String,
     *      java.lang.Object[])
     */
    public void addInfo( I18n message,
                         String resource,
                         String location,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addInfo(int, org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void addInfo( int code,
                         I18n message,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addInfo(int, org.jboss.dna.common.i18n.I18n, java.lang.String,
     *      java.lang.String, java.lang.Object[])
     */
    public void addInfo( int code,
                         I18n message,
                         String resource,
                         String location,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addInfo(java.lang.Throwable, org.jboss.dna.common.i18n.I18n,
     *      java.lang.Object[])
     */
    public void addInfo( Throwable throwable,
                         I18n message,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addInfo(java.lang.Throwable, org.jboss.dna.common.i18n.I18n,
     *      java.lang.String, java.lang.String, java.lang.Object[])
     */
    public void addInfo( Throwable throwable,
                         I18n message,
                         String resource,
                         String location,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addInfo(java.lang.Throwable, int, org.jboss.dna.common.i18n.I18n,
     *      java.lang.Object[])
     */
    public void addInfo( Throwable throwable,
                         int code,
                         I18n message,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addInfo(java.lang.Throwable, int, org.jboss.dna.common.i18n.I18n,
     *      java.lang.String, java.lang.String, java.lang.Object[])
     */
    public void addInfo( Throwable throwable,
                         int code,
                         I18n message,
                         String resource,
                         String location,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addWarning(org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void addWarning( I18n message,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addWarning(org.jboss.dna.common.i18n.I18n, java.lang.String,
     *      java.lang.String, java.lang.Object[])
     */
    public void addWarning( I18n message,
                            String resource,
                            String location,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addWarning(int, org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void addWarning( int code,
                            I18n message,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addWarning(int, org.jboss.dna.common.i18n.I18n, java.lang.String,
     *      java.lang.String, java.lang.Object[])
     */
    public void addWarning( int code,
                            I18n message,
                            String resource,
                            String location,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addWarning(java.lang.Throwable, org.jboss.dna.common.i18n.I18n,
     *      java.lang.Object[])
     */
    public void addWarning( Throwable throwable,
                            I18n message,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addWarning(java.lang.Throwable, org.jboss.dna.common.i18n.I18n,
     *      java.lang.String, java.lang.String, java.lang.Object[])
     */
    public void addWarning( Throwable throwable,
                            I18n message,
                            String resource,
                            String location,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addWarning(java.lang.Throwable, int, org.jboss.dna.common.i18n.I18n,
     *      java.lang.Object[])
     */
    public void addWarning( Throwable throwable,
                            int code,
                            I18n message,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#addWarning(java.lang.Throwable, int, org.jboss.dna.common.i18n.I18n,
     *      java.lang.String, java.lang.String, java.lang.Object[])
     */
    public void addWarning( Throwable throwable,
                            int code,
                            I18n message,
                            String resource,
                            String location,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#hasErrors()
     */
    public boolean hasErrors() {
        return delegate.hasErrors();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#hasInfo()
     */
    public boolean hasInfo() {
        return delegate.hasInfo();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#hasProblems()
     */
    public boolean hasProblems() {
        return delegate.hasProblems();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#hasWarnings()
     */
    public boolean hasWarnings() {
        return delegate.hasWarnings();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#isEmpty()
     */
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#iterator()
     */
    public Iterator<Problem> iterator() {
        return new ReadOnlyIterator<Problem>(delegate.iterator());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#size()
     */
    public int size() {
        return delegate.size();
    }

}
