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
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.Problem.Status;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;

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

    @Override
    public void addError( I18n message,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addError( String resource,
                          String location,
                          I18n message,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addError( int code,
                          I18n message,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addError( int code,
                          String resource,
                          String location,
                          I18n message,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addError( Throwable throwable,
                          I18n message,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addError( Throwable throwable,
                          String resource,
                          String location,
                          I18n message,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addError( Throwable throwable,
                          int code,
                          I18n message,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addError( Throwable throwable,
                          int code,
                          String resource,
                          String location,
                          I18n message,
                          Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addInfo( I18n message,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addInfo( String resource,
                         String location,
                         I18n message,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addInfo( int code,
                         I18n message,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addInfo( int code,
                         String resource,
                         String location,
                         I18n message,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addInfo( Throwable throwable,
                         I18n message,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addInfo( Throwable throwable,
                         String resource,
                         String location,
                         I18n message,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addInfo( Throwable throwable,
                         int code,
                         I18n message,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addInfo( Throwable throwable,
                         int code,
                         String resource,
                         String location,
                         I18n message,
                         Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addWarning( I18n message,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addWarning( String resource,
                            String location,
                            I18n message,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addWarning( int code,
                            I18n message,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addWarning( int code,
                            String resource,
                            String location,
                            I18n message,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addWarning( Throwable throwable,
                            I18n message,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addWarning( Throwable throwable,
                            String resource,
                            String location,
                            I18n message,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addWarning( Throwable throwable,
                            int code,
                            I18n message,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addWarning( Throwable throwable,
                            int code,
                            String resource,
                            String location,
                            I18n message,
                            Object... params ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAll( Iterable<Problem> problems ) {
        if (problems != null && problems != this && problems != delegate) this.delegate.addAll(problems);
    }

    @Override
    public boolean hasErrors() {
        return delegate.hasErrors();
    }

    @Override
    public boolean hasInfo() {
        return delegate.hasInfo();
    }

    @Override
    public boolean hasProblems() {
        return delegate.hasProblems();
    }

    @Override
    public boolean hasWarnings() {
        return delegate.hasWarnings();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public int errorCount() {
        return delegate.errorCount();
    }

    @Override
    public int problemCount() {
        return delegate.problemCount();
    }

    @Override
    public int warningCount() {
        return delegate.warningCount();
    }

    @Override
    public int infoCount() {
        return delegate.infoCount();
    }

    @Override
    public Iterator<Problem> iterator() {
        return new ReadOnlyIterator<Problem>(delegate.iterator());
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public void writeTo( Logger logger ) {
        delegate.writeTo(logger);
    }

    @Override
    public void writeTo( Logger logger,
                         Status firstStatus,
                         Status... additionalStatuses ) {
        delegate.writeTo(logger, firstStatus, additionalStatuses);
    }
}
