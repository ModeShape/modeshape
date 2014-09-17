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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import org.modeshape.common.collection.Problem.Status;
import org.modeshape.common.function.Consumer;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.logging.Logger.Level;

/**
 * A list of problems for some execution context. The problems will be {@link #iterator() returned} in the order in which they
 * were encountered (although this cannot be guaranteed in contexts involving multiple threads or processes).
 */
public abstract class AbstractProblems implements Problems {
    private static final long serialVersionUID = 1L;

    protected static final List<Problem> EMPTY_PROBLEMS = Collections.emptyList();

    @Override
    public void addError( I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, Problem.DEFAULT_CODE, message, params, null, null, null));
    }

    @Override
    public void addError( Throwable throwable,
                          I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, Problem.DEFAULT_CODE, message, params, null, null, throwable));
    }

    @Override
    public void addError( String resource,
                          String location,
                          I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, Problem.DEFAULT_CODE, message, params, resource, location, null));
    }

    @Override
    public void addError( Throwable throwable,
                          String resource,
                          String location,
                          I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, Problem.DEFAULT_CODE, message, params, resource, location, throwable));
    }

    @Override
    public void addError( int code,
                          I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, code, message, params, null, null, null));
    }

    @Override
    public void addError( Throwable throwable,
                          int code,
                          I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, code, message, params, null, null, throwable));
    }

    @Override
    public void addError( int code,
                          String resource,
                          String location,
                          I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, code, message, params, resource, location, null));
    }

    @Override
    public void addError( Throwable throwable,
                          int code,
                          String resource,
                          String location,
                          I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, code, message, params, resource, location, throwable));
    }

    @Override
    public void addWarning( I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, Problem.DEFAULT_CODE, message, params, null, null, null));
    }

    @Override
    public void addWarning( Throwable throwable,
                            I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, Problem.DEFAULT_CODE, message, params, null, null, throwable));
    }

    @Override
    public void addWarning( String resource,
                            String location,
                            I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, Problem.DEFAULT_CODE, message, params, resource, location, null));
    }

    @Override
    public void addWarning( Throwable throwable,
                            String resource,
                            String location,
                            I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, Problem.DEFAULT_CODE, message, params, resource, location, throwable));
    }

    @Override
    public void addWarning( int code,
                            I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, code, message, params, null, null, null));
    }

    @Override
    public void addWarning( Throwable throwable,
                            int code,
                            I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, code, message, params, null, null, throwable));
    }

    @Override
    public void addWarning( int code,
                            String resource,
                            String location,
                            I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, code, message, params, resource, location, null));
    }

    @Override
    public void addWarning( Throwable throwable,
                            int code,
                            String resource,
                            String location,
                            I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, code, message, params, resource, location, throwable));
    }

    @Override
    public void addInfo( I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, Problem.DEFAULT_CODE, message, params, null, null, null));
    }

    @Override
    public void addInfo( Throwable throwable,
                         I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, Problem.DEFAULT_CODE, message, params, null, null, throwable));
    }

    @Override
    public void addInfo( String resource,
                         String location,
                         I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, Problem.DEFAULT_CODE, message, params, resource, location, null));
    }

    @Override
    public void addInfo( Throwable throwable,
                         String resource,
                         String location,
                         I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, Problem.DEFAULT_CODE, message, params, resource, location, throwable));
    }

    @Override
    public void addInfo( int code,
                         I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, code, message, params, null, null, null));
    }

    @Override
    public void addInfo( Throwable throwable,
                         int code,
                         I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, code, message, params, null, null, throwable));
    }

    @Override
    public void addInfo( int code,
                         String resource,
                         String location,
                         I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, code, message, params, resource, location, null));
    }

    @Override
    public void addInfo( Throwable throwable,
                         int code,
                         String resource,
                         String location,
                         I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, code, message, params, resource, location, throwable));
    }

    @Override
    public boolean hasProblems() {
        return getProblems().size() > 0;
    }

    @Override
    public boolean hasErrors() {
        for (Problem problem : this.getProblems()) {
            if (problem.getStatus() == Problem.Status.ERROR) return true;
        }
        return false;
    }

    @Override
    public boolean hasWarnings() {
        for (Problem problem : this.getProblems()) {
            if (problem.getStatus() == Problem.Status.WARNING) return true;
        }
        return false;
    }

    @Override
    public boolean hasInfo() {
        for (Problem problem : this.getProblems()) {
            if (problem.getStatus() == Problem.Status.INFO) return true;
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return getProblems().isEmpty();
    }

    @Override
    public int errorCount() {
        int count = 0;
        for (Problem problem : getProblems()) {
            if (problem.getStatus() == Problem.Status.ERROR) ++count;
        }
        return count;
    }

    @Override
    public int problemCount() {
        return getProblems().size();
    }

    @Override
    public int warningCount() {
        int count = 0;
        for (Problem problem : getProblems()) {
            if (problem.getStatus() == Problem.Status.WARNING) ++count;
        }
        return count;
    }

    @Override
    public int infoCount() {
        int count = 0;
        for (Problem problem : getProblems()) {
            if (problem.getStatus() == Problem.Status.INFO) ++count;
        }
        return count;
    }

    @Override
    public int size() {
        return getProblems().size();
    }

    @Override
    public Iterator<Problem> iterator() {
        return getProblems().iterator();
    }

    protected abstract void addProblem( Problem problem );

    protected abstract List<Problem> getProblems();

    @Override
    public void writeTo( Logger logger ) {
        if (hasProblems()) {
            for (Problem problem : this) {
                Level level = logLevelFor(problem.getStatus());
                logger.log(level, problem.getMessage(), problem.getParameters());
            }
        }
    }

    @Override
    public void writeTo( Logger logger,
                         Status firstStatus,
                         Status... additionalStatuses ) {
        EnumSet<Status> stats = EnumSet.of(firstStatus, additionalStatuses);
        if (hasProblems()) {
            for (Problem problem : this) {
                Status status = problem.getStatus();
                if (!stats.contains(status)) continue;
                Level level = logLevelFor(status);
                logger.log(level, problem.getMessage(), problem.getParameters());
            }
        }
    }

    protected final Level logLevelFor( Status status ) {
        switch (status) {
            case ERROR:
                return Level.ERROR;
            case WARNING:
                return Level.WARNING;
            case INFO:
                return Level.INFO;
        }
        assert false : "Should not happen";
        return Level.INFO;
    }

    @Override
    public void apply( Consumer<Problem> consumer ) {
        if (consumer != null) {
            for (Problem problem : getProblems()) {
                if (problem != null) consumer.accept(problem);
            }
        }
    }

    @Override
    public void apply( Status status,
                       Consumer<Problem> consumer ) {
        if (status != null && consumer != null) {
            for (Problem problem : getProblems()) {
                if (problem != null && problem.getStatus() == status) consumer.accept(problem);
            }
        }
    }

    @Override
    public void apply( EnumSet<Status> statuses,
                       Consumer<Problem> consumer ) {
        if (statuses != null && consumer != null) {
            for (Problem problem : getProblems()) {
                if (problem != null && statuses.contains(problem.getStatus())) consumer.accept(problem);
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final Consumer<Problem> consumer = new Consumer<Problem>() {
            private boolean first = true;

            @Override
            public void accept( Problem problem ) {
                if (first) first = false;
                else sb.append("\n");
                sb.append(problem);
            }
        };
        // Do these in this order, not all at once ...
        apply(Status.ERROR, consumer);
        apply(Status.WARNING, consumer);
        apply(Status.INFO, consumer);
        return sb.toString();
    }
}
