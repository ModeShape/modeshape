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
package org.jboss.dna.services.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.jboss.dna.common.util.StringUtil;
import net.jcip.annotations.NotThreadSafe;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class Problems implements Iterable<Problem> {

    private List<Problem> problems;

    public Problems() {
    }

    public void addError( String message, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.ERROR, Problem.DEFAULT_CODE, message));
    }

    public void addError( Throwable throwable, String message, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.ERROR, Problem.DEFAULT_CODE, message, null, null, throwable));
    }

    public void addError( String message, String resource, String location, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.ERROR, Problem.DEFAULT_CODE, message, resource, location));
    }

    public void addError( Throwable throwable, String message, String resource, String location, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.ERROR, Problem.DEFAULT_CODE, message, resource, location, throwable));
    }

    public void addError( int code, String message, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.ERROR, code, message));
    }

    public void addError( Throwable throwable, int code, String message, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.ERROR, code, message, null, null, throwable));
    }

    public void addError( int code, String message, String resource, String location, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.ERROR, code, message, resource, location));
    }

    public void addError( Throwable throwable, int code, String message, String resource, String location, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.ERROR, code, message, resource, location, throwable));
    }

    public void addWarning( String message, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.WARNING, Problem.DEFAULT_CODE, message));
    }

    public void addWarning( Throwable throwable, String message, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.WARNING, Problem.DEFAULT_CODE, message, null, null, throwable));
    }

    public void addWarning( String message, String resource, String location, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.WARNING, Problem.DEFAULT_CODE, message, resource, location));
    }

    public void addWarning( Throwable throwable, String message, String resource, String location, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.WARNING, Problem.DEFAULT_CODE, message, resource, location, throwable));
    }

    public void addWarning( int code, String message, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.WARNING, code, message));
    }

    public void addWarning( Throwable throwable, int code, String message, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.WARNING, code, message, null, null, throwable));
    }

    public void addWarning( int code, String message, String resource, String location, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.WARNING, code, message, resource, location));
    }

    public void addWarning( Throwable throwable, int code, String message, String resource, String location, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.WARNING, code, message, resource, location, throwable));
    }

    public void addInfo( String message, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.INFO, Problem.DEFAULT_CODE, message));
    }

    public void addInfo( Throwable throwable, String message, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.INFO, Problem.DEFAULT_CODE, message, null, null, throwable));
    }

    public void addInfo( String message, String resource, String location, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.INFO, Problem.DEFAULT_CODE, message, resource, location));
    }

    public void addInfo( Throwable throwable, String message, String resource, String location, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.INFO, Problem.DEFAULT_CODE, message, resource, location, throwable));
    }

    public void addInfo( int code, String message, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.INFO, code, message));
    }

    public void addInfo( Throwable throwable, int code, String message, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.INFO, code, message, null, null, throwable));
    }

    public void addInfo( int code, String message, String resource, String location, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.INFO, code, message, resource, location));
    }

    public void addInfo( Throwable throwable, int code, String message, String resource, String location, Object... params ) {
        message = params != null && params.length > 0 ? StringUtil.createString(message, params) : message;
        addProblem(new Problem(Problem.Status.INFO, code, message, resource, location, throwable));
    }

    public boolean hasProblems() {
        return this.problems != null && this.problems.size() > 0;
    }

    public boolean hasErrors() {
        if (this.problems == null) return false;
        for (Problem problem : this.problems) {
            if (problem.getStatus() == Problem.Status.ERROR) return true;
        }
        return false;
    }

    public boolean hasWarnings() {
        if (this.problems == null) return false;
        for (Problem problem : this.problems) {
            if (problem.getStatus() == Problem.Status.WARNING) return true;
        }
        return false;
    }

    public boolean hasInfo() {
        if (this.problems == null) return false;
        for (Problem problem : this.problems) {
            if (problem.getStatus() == Problem.Status.INFO) return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return this.problems == null || this.problems.isEmpty();
    }

    public int size() {
        if (this.problems == null) return 0;
        return this.problems.size();
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Problem> iterator() {
        return problems.iterator();
    }

    protected void addProblem( Problem problem ) {
        if (problem == null) return;
        if (problems == null) problems = new LinkedList<Problem>();
        problems.add(problem);
    }

}
