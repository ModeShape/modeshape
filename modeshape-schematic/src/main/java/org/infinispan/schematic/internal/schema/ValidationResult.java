/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.infinispan.schematic.internal.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.infinispan.schematic.SchemaLibrary.Problem;
import org.infinispan.schematic.SchemaLibrary.ProblemType;
import org.infinispan.schematic.SchemaLibrary.Results;
import org.infinispan.schematic.document.NotThreadSafe;
import org.infinispan.schematic.document.Path;

/**
 * Basic implementation of {@link Results} to which problems can be added.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
@NotThreadSafe
public class ValidationResult implements Results, Problems {

    private final List<Problem> problems = new ArrayList<Problem>();

    @Override
    public Iterator<Problem> iterator() {
        return Collections.unmodifiableList(problems).iterator();
    }

    @Override
    public boolean hasProblems() {
        return !problems.isEmpty();
    }

    @Override
    public boolean hasErrors() {
        for (Problem problem : problems) {
            if (problem.getType() == ProblemType.ERROR) return true;
        }
        return false;
    }

    @Override
    public boolean hasWarnings() {
        for (Problem problem : problems) {
            if (problem.getType() == ProblemType.WARNING) return true;
        }
        return false;
    }

    @Override
    public int errorCount() {
        int result = 0;
        for (Problem problem : problems) {
            if (problem.getType() == ProblemType.ERROR) ++result;
        }
        return result;
    }

    @Override
    public int warningCount() {
        int result = 0;
        for (Problem problem : problems) {
            if (problem.getType() == ProblemType.WARNING) ++result;
        }
        return result;
    }

    @Override
    public int problemCount() {
        return problems.size();
    }

    @Override
    public void recordError( Path path,
                             String reason ) {
        problems.add(new ValidationProblem(ProblemType.ERROR, path, reason, null));
    }

    @Override
    public void recordError( Path path,
                             String reason,
                             Throwable cause ) {
        problems.add(new ValidationProblem(ProblemType.ERROR, path, reason, cause));
    }

    @Override
    public void recordWarning( Path path,
                               String reason ) {
        problems.add(new ValidationProblem(ProblemType.WARNING, path, reason, null));
    }

    public void add( Problem problem ) {
        if (problem != null) {
            problems.add(problem);
        }
    }

    public void addAll( Iterable<Problem> results ) {
        if (results != null) {
            for (Problem problem : results) {
                problems.add(problem);
            }
        }
    }

    public void addAll( Iterator<Results> iter ) {
        if (iter != null) {
            while (iter.hasNext()) {
                addAll(iter.next());
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Problem problem : problems) {
            sb.append(problem);
            sb.append('\n');
        }
        return sb.toString();
    }

}
