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
package org.modeshape.schematic.internal.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.modeshape.schematic.SchemaLibrary;
import org.modeshape.schematic.annotation.NotThreadSafe;
import org.modeshape.schematic.document.JsonSchema.Type;
import org.modeshape.schematic.document.Path;

/**
 * Basic implementation of {@link SchemaLibrary.Results} to which problems can be added.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
@NotThreadSafe
public class ValidationResult implements SchemaLibrary.Results, Problems {

    private final List<SchemaLibrary.Problem> problems = new ArrayList<>();
    private int successes;

    @Override
    public Iterator<SchemaLibrary.Problem> iterator() {
        return Collections.unmodifiableList(problems).iterator();
    }

    @Override
    public boolean hasProblems() {
        return !problems.isEmpty();
    }

    @Override
    public boolean hasErrors() {
        for (SchemaLibrary.Problem problem : problems) {
            if (problem.getType() == SchemaLibrary.ProblemType.ERROR) return true;
        }
        return false;
    }

    @Override
    public boolean hasWarnings() {
        for (SchemaLibrary.Problem problem : problems) {
            if (problem.getType() == SchemaLibrary.ProblemType.WARNING) return true;
        }
        return false;
    }

    @Override
    public boolean hasOnlyTypeMismatchErrors() {
        boolean foundMismatch = false;
        for (SchemaLibrary.Problem problem : problems) {
            if (problem.getType() == SchemaLibrary.ProblemType.ERROR) {
                if (problem instanceof SchemaLibrary.MismatchedTypeProblem) foundMismatch = true;
                else return false;
            }
        }
        return foundMismatch;
    }

    @Override
    public int errorCount() {
        int result = 0;
        for (SchemaLibrary.Problem problem : problems) {
            if (problem.getType() == SchemaLibrary.ProblemType.ERROR) ++result;
        }
        return result;
    }

    @Override
    public int warningCount() {
        int result = 0;
        for (SchemaLibrary.Problem problem : problems) {
            if (problem.getType() == SchemaLibrary.ProblemType.WARNING) ++result;
        }
        return result;
    }

    @Override
    public int problemCount() {
        return problems.size();
    }

    public int successCount() {
        return successes;
    }

    @Override
    public void recordSuccess() {
        ++successes;
    }

    @Override
    public void recordError( Path path,
                             String reason ) {
        problems.add(new ValidationProblem(SchemaLibrary.ProblemType.ERROR, path, reason, null));
    }

    @Override
    public void recordError( Path path,
                             String reason,
                             Throwable cause ) {
        problems.add(new ValidationProblem(SchemaLibrary.ProblemType.ERROR, path, reason, cause));
    }

    @Override
    public void recordWarning( Path path,
                               String reason ) {
        problems.add(new ValidationProblem(SchemaLibrary.ProblemType.WARNING, path, reason, null));
    }

    @Override
    public void recordTypeMismatch( Path path,
                                    String reason,
                                    Type actualType,
                                    Object actualValue,
                                    Type requiredType,
                                    Object convertedValue ) {
        problems.add(new ValidationTypeMismatchProblem(SchemaLibrary.ProblemType.ERROR, path, actualValue, actualType, requiredType,
                                                       convertedValue, reason, null));
    }

    public void recordIn( Problems otherProblems ) {
        if (successes == 0 && problems.isEmpty()) return;

        for (SchemaLibrary.Problem problem : problems) {
            for (int i = 0; i != successes; ++i) {
                otherProblems.recordSuccess();
            }
            switch (problem.getType()) {
                case ERROR:
                    if (problem instanceof SchemaLibrary.MismatchedTypeProblem) {
                        SchemaLibrary.MismatchedTypeProblem mismatch = (SchemaLibrary.MismatchedTypeProblem)problem;
                        otherProblems.recordTypeMismatch(mismatch.getPath(),
                                                         mismatch.getReason(),
                                                         mismatch.getActualType(),
                                                         mismatch.getActualValue(),
                                                         mismatch.getExpectedType(),
                                                         mismatch.getConvertedValue());
                    } else {
                        otherProblems.recordError(problem.getPath(), problem.getReason(), problem.getCause());
                    }
                    break;
                case WARNING:
                    otherProblems.recordWarning(problem.getPath(), problem.getReason());
                    break;
            }
        }
    }

    public void add( SchemaLibrary.Problem problem ) {
        if (problem != null) {
            problems.add(problem);
        }
    }

    public void addAll( Iterable<SchemaLibrary.Problem> results ) {
        if (results != null) {
            for (SchemaLibrary.Problem problem : results) {
                problems.add(problem);
            }
        }
    }

    public void addAll( Iterator<SchemaLibrary.Results> iter ) {
        if (iter != null) {
            while (iter.hasNext()) {
                addAll(iter.next());
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (SchemaLibrary.Problem problem : problems) {
            sb.append(problem);
            sb.append('\n');
        }
        return sb.toString();
    }

}
