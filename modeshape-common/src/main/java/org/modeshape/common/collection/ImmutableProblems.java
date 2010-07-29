/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.common.collection;

import java.util.Iterator;
import net.jcip.annotations.Immutable;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;

/**
 * An immutable wrapper for a mutable {@link Problems}.
 */
@Immutable
public class ImmutableProblems implements Problems {

    private static final long serialVersionUID = 1L;
    private final Problems delegate;

    public ImmutableProblems( Problems delegate ) {
        CheckArg.isNotNull(delegate, "delegate");
        this.delegate = delegate;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#addError(org.modeshape.common.i18n.I18n, java.lang.Object[])
     */
    public void addError( I18n message,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#addError(java.lang.String, java.lang.String, org.modeshape.common.i18n.I18n,
     *      java.lang.Object[])
     */
    public void addError( String resource,
                          String location,
                          I18n message,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#addError(int, org.modeshape.common.i18n.I18n, java.lang.Object[])
     */
    public void addError( int code,
                          I18n message,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#addError(int, String, String, I18n, Object...)
     */
    public void addError( int code,
                          String resource,
                          String location,
                          I18n message,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#addError(java.lang.Throwable, org.modeshape.common.i18n.I18n,
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
     * @see org.modeshape.common.collection.Problems#addError(java.lang.Throwable, java.lang.String, java.lang.String,
     *      org.modeshape.common.i18n.I18n,java.lang.Object[])
     */
    public void addError( Throwable throwable,
                          String resource,
                          String location,
                          I18n message,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#addError(java.lang.Throwable, int, org.modeshape.common.i18n.I18n,
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
     * @see org.modeshape.common.collection.Problems#addError(java.lang.Throwable, int, java.lang.String, java.lang.String,
     *      org.modeshape.common.i18n.I18n, java.lang.Object[])
     */
    public void addError( Throwable throwable,
                          int code,
                          String resource,
                          String location,
                          I18n message,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#addInfo(org.modeshape.common.i18n.I18n, java.lang.Object[])
     */
    public void addInfo( I18n message,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#addInfo(java.lang.String, java.lang.String, org.modeshape.common.i18n.I18n,
     *      java.lang.Object[])
     */
    public void addInfo( String resource,
                         String location,
                         I18n message,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#addInfo(int, org.modeshape.common.i18n.I18n, java.lang.Object[])
     */
    public void addInfo( int code,
                         I18n message,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#addInfo(int, java.lang.String, java.lang.String,
     *      org.modeshape.common.i18n.I18n, java.lang.Object[])
     */
    public void addInfo( int code,
                         String resource,
                         String location,
                         I18n message,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#addInfo(java.lang.Throwable, org.modeshape.common.i18n.I18n,
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
     * @see org.modeshape.common.collection.Problems#addInfo(java.lang.Throwable, java.lang.String, java.lang.String,
     *      org.modeshape.common.i18n.I18n, java.lang.Object[])
     */
    public void addInfo( Throwable throwable,
                         String resource,
                         String location,
                         I18n message,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#addInfo(java.lang.Throwable, int, org.modeshape.common.i18n.I18n,
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
     * @see org.modeshape.common.collection.Problems#addInfo(java.lang.Throwable, int, java.lang.String, java.lang.String,
     *      org.modeshape.common.i18n.I18n, java.lang.Object[])
     */
    public void addInfo( Throwable throwable,
                         int code,
                         String resource,
                         String location,
                         I18n message,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#addWarning(org.modeshape.common.i18n.I18n, java.lang.Object[])
     */
    public void addWarning( I18n message,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#addWarning(java.lang.String, java.lang.String,
     *      org.modeshape.common.i18n.I18n, java.lang.Object[])
     */
    public void addWarning( String resource,
                            String location,
                            I18n message,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#addWarning(int, org.modeshape.common.i18n.I18n, java.lang.Object[])
     */
    public void addWarning( int code,
                            I18n message,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#addWarning(int, java.lang.String, java.lang.String,
     *      org.modeshape.common.i18n.I18n, java.lang.Object[])
     */
    public void addWarning( int code,
                            String resource,
                            String location,
                            I18n message,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#addWarning(java.lang.Throwable, org.modeshape.common.i18n.I18n,
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
     * @see org.modeshape.common.collection.Problems#addWarning(java.lang.Throwable, java.lang.String, java.lang.String ,
     *      org.modeshape.common.i18n.I18n,java.lang.Object[])
     */
    public void addWarning( Throwable throwable,
                            String resource,
                            String location,
                            I18n message,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#addWarning(java.lang.Throwable, int, org.modeshape.common.i18n.I18n,
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
     * @see org.modeshape.common.collection.Problems#addWarning(java.lang.Throwable, int, java.lang.String, java.lang.String,
     *      org.modeshape.common.i18n.I18n,java.lang.Object[])
     */
    public void addWarning( Throwable throwable,
                            int code,
                            String resource,
                            String location,
                            I18n message,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#addAll(java.lang.Iterable)
     */
    public void addAll( Iterable<Problem> problems ) {
        if (problems != null && problems != this && problems != delegate) this.delegate.addAll(problems);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#hasErrors()
     */
    public boolean hasErrors() {
        return delegate.hasErrors();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#hasInfo()
     */
    public boolean hasInfo() {
        return delegate.hasInfo();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#hasProblems()
     */
    public boolean hasProblems() {
        return delegate.hasProblems();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#hasWarnings()
     */
    public boolean hasWarnings() {
        return delegate.hasWarnings();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#isEmpty()
     */
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#iterator()
     */
    public Iterator<Problem> iterator() {
        return new ReadOnlyIterator<Problem>(delegate.iterator());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#size()
     */
    public int size() {
        return delegate.size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return delegate.toString();
    }
}
