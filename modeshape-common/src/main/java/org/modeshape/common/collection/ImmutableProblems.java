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

import java.util.EnumSet;
import java.util.Iterator;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.Problem.Status;
import org.modeshape.common.function.Consumer;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.logging.Logger;
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
        return ReadOnlyIterator.around(delegate.iterator());
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
    public void apply( Consumer<Problem> consumer ) {
        delegate.apply(consumer);
    }

    @Override
    public void apply( EnumSet<Status> statuses,
                       Consumer<Problem> consumer ) {
        delegate.apply(statuses, consumer);
    }

    @Override
    public void apply( Status status,
                       Consumer<Problem> consumer ) {
        delegate.apply(status, consumer);
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
