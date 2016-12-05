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
package org.modeshape.jcr.query.process;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.StaticOperand;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.query.QueryResults;
import org.modeshape.jcr.query.model.Comparison;
import org.modeshape.jcr.query.model.Constraint;
import org.modeshape.jcr.query.model.Length;
import org.modeshape.jcr.query.model.Literal;
import org.modeshape.jcr.query.model.Not;
import org.modeshape.jcr.query.model.Or;
import org.modeshape.jcr.query.model.PropertyExistence;
import org.modeshape.jcr.query.model.PropertyValue;
import org.modeshape.jcr.query.model.SetCriteria;


/**
 * Helper class to avoid entering queries with empty strings into Lucene<br/>
 * See issue <a href="https://issues.jboss.org/browse/MODE-2174">MODE-2174</a>
 * @author: vadym.karko
 * @since: 3/31/14 1:47 PM
 */
public class LuceneQueryDistributor {
    /**
     * Define places in query according to position in query
     * <table>
     *     <tr><td><b>EQUALS</b></td><td><code>SELECT &#8230 WHERE x = ''</code></td></tr>
     *     <tr><td><b>NULL</b></td><td><code>SELECT &#8230 WHERE x IS NULL</code></td></tr>
     *     <tr><td><b>LENGTH</b></td><td><code>SELECT &#8230 WHERE LENGTH(x) = 0</code></td></tr>
     *     <tr><td><b>IN</b></td><td><code>SELECT &#8230 WHERE x IN (&#8230, '', &#8230)</code></td></tr>
     *     <tr><td><b>OR</b></td><td><code>SELECT &#8230 WHERE x = '' OR &#8230</code></td></tr>
     *     <tr><td><b>NOWHERE</b></td><td> none of mentioned above</td></tr>
     * </table>
     */
    private enum ContainsEmpty {EQUALS, NULL, LENGTH, IN, OR, NOWHERE}

    private final List<Constraint> luceneConstraints;
    private final List<Constraint> postConstraints;
    private final QueryResults.Columns columns;


    public LuceneQueryDistributor(QueryResults.Columns columns) {
        this.columns = columns;
        luceneConstraints = new ArrayList<Constraint>();
        postConstraints = new ArrayList<Constraint>();
    }

    /**
     * Distributes constraint directly in Lucene or post process.<br/>
     * Add constraint in prepared constraints (<code>luceneConstraints</code>) if it has no empty strings,
     * otherwise in post processing constraints (<code>postConstraints</code>)
     * @param constraint constraint that should be distributed
     */
    public void distribute(Constraint constraint) {
        switch (isQueryContainsEmpty(constraint)) {
            case EQUALS:    // ... WHERE x = ''
            case NULL:      // ... WHERE x IS NULL
            case IN:        // ... WHERE x IN ('')
            case OR:        // ... OR ...
                postConstraints.add(constraint);
            break;

            case LENGTH:
                Operator operator = ((Comparison) constraint).operator();
                Length length = (Length)((Comparison) constraint).getOperand1();

                switch (operator) {
                    // ... WHERE LENGTH(x) = 0  <=> ... WHERE x = ''
                    case EQUAL_TO:
                        Constraint emptyString = new Comparison(
                                new PropertyValue(length.selectorName(), length.getPropertyValue().getPropertyName()),
                                Operator.EQUAL_TO,
                                new Literal("")
                        );

                        postConstraints.add(emptyString);
                    break;

                    // ... WHERE LENGTH(x) >= 0  <=> ... WHERE x IS NOT NULL
                    case GREATER_THAN_OR_EQUAL_TO:
                        PropertyExistence existence = new PropertyExistence(
                                length.selectorName(),
                                length.getPropertyValue().getPropertyName()
                        );

                        postConstraints.add(existence);
                    break;

                    default: postConstraints.add(constraint); break;
                }
            break;

            default: luceneConstraints.add(constraint); break;
        }
    }


    /**
     * Checks if query has empty string, and if so, where exactly in query
     * @param constraint constraint that should be checked
     * @return {@link ContainsEmpty#NOWHERE} if empty string not found
     * @see ContainsEmpty
     */
    private ContainsEmpty isQueryContainsEmpty(Constraint constraint) {
        if (constraint instanceof Comparison) {
            Comparison comparison = (Comparison)constraint;
            String value = comparison.getOperand2().toString();

            // ... WHERE x = ''
            if ("''".equals(value)) return ContainsEmpty.EQUALS;

            // ... WHERE LENGTH(x) = 0
            if ((QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO.equals(comparison.getOperator()) ||
                 QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN.equals(comparison.getOperator()) ||
                 QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO.equals(comparison.getOperator())) &&
                "CAST('0' AS LONG)".equals(value)) return ContainsEmpty.LENGTH;
        }
        // ... WHERE x IS NOT NULL
        else if (constraint instanceof Not) {
            Not not = (Not)constraint;

            return isQueryContainsEmpty(not.getConstraint());
        }
        // ... WHERE x IS NULL
        else if (constraint instanceof PropertyExistence) {
            PropertyExistence property = (PropertyExistence)constraint;
            String column = columns.getColumnTypeForProperty(property.getSelectorName(), property.getPropertyName());

            if ("STRING".equals(column)) return ContainsEmpty.NULL;
        }
        // ... WHERE x IN ('')
        else if (constraint instanceof SetCriteria) {
            for (StaticOperand operand : ((SetCriteria) constraint).getValues()) {
                if ((operand instanceof Literal) &&
                    ("".equals(((Literal) operand).value().toString()))) return ContainsEmpty.IN;
            }
        }
        // ... OR ...
        else if (constraint instanceof Or) {
            Or or = (Or)constraint;
            Constraint left = or.getConstraint1();
            Constraint right = or.getConstraint2();

            return ((isQueryContainsEmpty(left) != ContainsEmpty.NOWHERE) ||
                    (isQueryContainsEmpty(right) != ContainsEmpty.NOWHERE)) ? ContainsEmpty.OR : ContainsEmpty.NOWHERE;
        }

        return ContainsEmpty.NOWHERE;
    }

    /**
     * @return Returns constraint list that should be used in direct Lucene search
     */
    public List<Constraint> getLuceneConstraints() {
        return luceneConstraints;
    }

    /**
     * @return Returns constraint list that should be used in post Lucene search
     */
    public List<Constraint> getPostConstraints() {
        return postConstraints;
    }
}