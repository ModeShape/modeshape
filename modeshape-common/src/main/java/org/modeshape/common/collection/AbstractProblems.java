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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.modeshape.common.i18n.I18n;

/**
 * A list of problems for some execution context. The problems will be {@link #iterator() returned} in the order in which they
 * were encountered (although this cannot be guaranteed in contexts involving multiple threads or processes).
 */
public abstract class AbstractProblems implements Problems {
    private static final long serialVersionUID = 1L;

    protected static final List<Problem> EMPTY_PROBLEMS = Collections.emptyList();

    public void addError( I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, Problem.DEFAULT_CODE, message, params, null, null, null));
    }

    public void addError( Throwable throwable,
                          I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, Problem.DEFAULT_CODE, message, params, null, null, throwable));
    }

    public void addError( String resource,
                          String location,
                          I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, Problem.DEFAULT_CODE, message, params, resource, location, null));
    }

    public void addError( Throwable throwable,
                          String resource,
                          String location,
                          I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, Problem.DEFAULT_CODE, message, params, resource, location, throwable));
    }

    public void addError( int code,
                          I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, code, message, params, null, null, null));
    }

    public void addError( Throwable throwable,
                          int code,
                          I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, code, message, params, null, null, throwable));
    }

    public void addError( int code,
                          String resource,
                          String location,
                          I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, code, message, params, resource, location, null));
    }

    public void addError( Throwable throwable,
                          int code,
                          String resource,
                          String location,
                          I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, code, message, params, resource, location, throwable));
    }

    public void addWarning( I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, Problem.DEFAULT_CODE, message, params, null, null, null));
    }

    public void addWarning( Throwable throwable,
                            I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, Problem.DEFAULT_CODE, message, params, null, null, throwable));
    }

    public void addWarning( String resource,
                            String location,
                            I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, Problem.DEFAULT_CODE, message, params, resource, location, null));
    }

    public void addWarning( Throwable throwable,
                            String resource,
                            String location,
                            I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, Problem.DEFAULT_CODE, message, params, resource, location, throwable));
    }

    public void addWarning( int code,
                            I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, code, message, params, null, null, null));
    }

    public void addWarning( Throwable throwable,
                            int code,
                            I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, code, message, params, null, null, throwable));
    }

    public void addWarning( int code,
                            String resource,
                            String location,
                            I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, code, message, params, resource, location, null));
    }

    public void addWarning( Throwable throwable,
                            int code,
                            String resource,
                            String location,
                            I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, code, message, params, resource, location, throwable));
    }

    public void addInfo( I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, Problem.DEFAULT_CODE, message, params, null, null, null));
    }

    public void addInfo( Throwable throwable,
                         I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, Problem.DEFAULT_CODE, message, params, null, null, throwable));
    }

    public void addInfo( String resource,
                         String location,
                         I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, Problem.DEFAULT_CODE, message, params, resource, location, null));
    }

    public void addInfo( Throwable throwable,
                         String resource,
                         String location,
                         I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, Problem.DEFAULT_CODE, message, params, resource, location, throwable));
    }

    public void addInfo( int code,
                         I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, code, message, params, null, null, null));
    }

    public void addInfo( Throwable throwable,
                         int code,
                         I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, code, message, params, null, null, throwable));
    }

    public void addInfo( int code,
                         String resource,
                         String location,
                         I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, code, message, params, resource, location, null));
    }

    public void addInfo( Throwable throwable,
                         int code,
                         String resource,
                         String location,
                         I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, code, message, params, resource, location, throwable));
    }

    public boolean hasProblems() {
        return getProblems().size() > 0;
    }

    public boolean hasErrors() {
        for (Problem problem : this.getProblems()) {
            if (problem.getStatus() == Problem.Status.ERROR) return true;
        }
        return false;
    }

    public boolean hasWarnings() {
        for (Problem problem : this.getProblems()) {
            if (problem.getStatus() == Problem.Status.WARNING) return true;
        }
        return false;
    }

    public boolean hasInfo() {
        for (Problem problem : this.getProblems()) {
            if (problem.getStatus() == Problem.Status.INFO) return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return getProblems().isEmpty();
    }

    public int size() {
        return getProblems().size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.Problems#iterator()
     */
    public Iterator<Problem> iterator() {
        return getProblems().iterator();
    }

    protected abstract void addProblem( Problem problem );

    protected abstract List<Problem> getProblems();

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Iterator<Problem> iter = getProblems().iterator();
        if (iter.hasNext()) {
            sb.append(iter.next());
            while (iter.hasNext()) {
                sb.append("\n");
                sb.append(iter.next());
            }
        }
        return sb.toString();
    }
}
