package org.modeshape.jcr.query.process;

import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.query.QueryResults;
import org.modeshape.jcr.query.model.*;

import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.StaticOperand;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper class to avoid entering queries with empty strings into Lucene<br/>
 * See issue <a href="https://issues.jboss.org/browse/MODE-2174">MODE-2174</a>
 * @author: vadym.karko
 * @since: 3/31/14 1:47 PM
 */
public class LuceneQueryDistributor
{
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

	List<Constraint> luceneConstraints = null;
	List<Constraint> postConstraints = null;
	QueryResults.Columns columns;


	public LuceneQueryDistributor(QueryResults.Columns columns)
	{
		this.columns = columns;
	}

	/**
	 * Distributes constraint directly in Lucene or post process.<br/>
	 * Add constraint in prepared constraints (<code>luceneConstraints</code>) if it has no empty strings,
	 * otherwise in post processing constraints (<code>postConstraints</code>)
	 * @param constraint constraint that should be distributed
	 */
	public void distribute(Constraint constraint)
	{
		switch (isQueryContainsEmpty(constraint))
		{
			case EQUALS:	// ... WHERE x = ''
			case NULL:		// ... WHERE x IS NULL
			case IN: 		// ... WHERE x IN ('')
			case OR: 		// ... OR ...
				addPostConstraints(constraint);
			break;

			case LENGTH:	// ... WHERE LENGTH(x) = 0  <=> ... WHERE x = ''
				Length length = (Length)((Comparison) constraint).getOperand1();

				Constraint emptyString = new Comparison(
						new PropertyValue(length.selectorName(), length.getPropertyValue().getPropertyName()),
						Operator.EQUAL_TO,
						new Literal("")
				);

				addPostConstraints(emptyString);
			break;

			default: addLuceneConstraints(constraint); break;
		}
	}


	/**
	 * Checks if query has empty string, and if so, where exactly in query
	 * @param constraint constraint that should be checked
	 * @return {@link ContainsEmpty#NOWHERE} if empty string not found
	 * @see ContainsEmpty
	 */
	private ContainsEmpty isQueryContainsEmpty(Constraint constraint)
	{
		if (constraint instanceof Comparison)
		{
			Comparison comparison = (Comparison)constraint;
			String value = comparison.getOperand2().toString();

			// ... WHERE x = ''
			if ("''".equals(value)) return ContainsEmpty.EQUALS;

			// ... WHERE LENGTH(x) = 0
			if (QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO.equals(comparison.getOperator()) &&
				"CAST('0' AS LONG)".equals(value)) return ContainsEmpty.LENGTH;
		}
		// ... WHERE x IS NOT NULL
		else if (constraint instanceof Not)
		{
			Not not = (Not)constraint;

			return isQueryContainsEmpty(not.getConstraint());
		}
		// ... WHERE x IS NULL
		else if (constraint instanceof PropertyExistence)
		{
			PropertyExistence property = (PropertyExistence)constraint;
			String column = columns.getColumnTypeForProperty(property.getSelectorName(), property.getPropertyName());

			if ("STRING".equals(column)) return ContainsEmpty.NULL;
		}
		// ... WHERE x IN ('')
		else if (constraint instanceof SetCriteria)
		{
			for (StaticOperand operand : ((SetCriteria) constraint).getValues())
			{
				if ((operand instanceof Literal) &&	("".equals(((Literal) operand).value().toString()))) return ContainsEmpty.IN;
			}
		}
		// ... OR ...
		else if (constraint instanceof Or)
		{
			Or or = (Or)constraint;
			Constraint left = or.getConstraint1();
			Constraint right = or.getConstraint2();

			return ((isQueryContainsEmpty(left) != ContainsEmpty.NOWHERE) ||
					(isQueryContainsEmpty(right) != ContainsEmpty.NOWHERE)) ? ContainsEmpty.OR : ContainsEmpty.NOWHERE;
		}

		return ContainsEmpty.NOWHERE;
	}

	/**
	 * Pushes constraint into Lucene constraint list
	 * @param constraint
	 */
	private void addLuceneConstraints(Constraint constraint)
	{
		if (luceneConstraints == null) luceneConstraints = new ArrayList<Constraint>();
		luceneConstraints.add(constraint);
	}

	/**
	 * Pushes constraint into post constraint list
	 * @param constraint
	 */
	private void addPostConstraints(Constraint constraint)
	{
		if (postConstraints == null) postConstraints = new ArrayList<Constraint>();
		postConstraints.add(constraint);
	}

	/**
	 * @return Returns constraint list that should be used in direct Lucene search
	 */
	public List<Constraint> getLuceneConstraints()
	{
		return luceneConstraints != null ? luceneConstraints : Collections.<Constraint>emptyList();
	}

	/**
	 * @return Returns constraint list that should be used in post Lucene search
	 */
	public List<Constraint> getPostConstraints()
	{
		return postConstraints != null ? postConstraints : Collections.<Constraint>emptyList();
	}
}