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
package org.modeshape.jcr.index.elasticsearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import javax.jcr.query.qom.And;
import javax.jcr.query.qom.BindVariableValue;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.LowerCase;
import javax.jcr.query.qom.NodeLocalName;
import javax.jcr.query.qom.NodeName;
import javax.jcr.query.qom.Not;
import javax.jcr.query.qom.PropertyExistence;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.StaticOperand;
import javax.jcr.query.qom.UpperCase;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.ModeShapeLexicon;
import org.modeshape.jcr.api.query.qom.Between;
import org.modeshape.jcr.api.query.qom.SetCriteria;
import org.modeshape.jcr.index.elasticsearch.client.EsRequest;
import org.modeshape.jcr.index.elasticsearch.query.AndQuery;
import org.modeshape.jcr.index.elasticsearch.query.BoolQuery;
import org.modeshape.jcr.index.elasticsearch.query.ExistsQuery;
import org.modeshape.jcr.index.elasticsearch.query.MatchAllQuery;
import org.modeshape.jcr.index.elasticsearch.query.MatchQuery;
import org.modeshape.jcr.index.elasticsearch.query.NotQuery;
import org.modeshape.jcr.index.elasticsearch.query.OrQuery;
import org.modeshape.jcr.index.elasticsearch.query.Query;
import org.modeshape.jcr.index.elasticsearch.query.RangeQuery;
import org.modeshape.jcr.index.elasticsearch.query.StringQuery;
import org.modeshape.jcr.index.elasticsearch.query.TermsQuery;
import org.modeshape.jcr.index.elasticsearch.query.WildcardQuery;
import org.modeshape.jcr.query.model.Comparison;
import org.modeshape.jcr.query.model.FullTextSearch;
import org.modeshape.jcr.query.model.Length;
import org.modeshape.jcr.query.model.Literal;
import org.modeshape.jcr.query.model.NodeDepth;
import org.modeshape.jcr.query.model.NodePath;
import org.modeshape.jcr.query.model.Or;
import org.modeshape.jcr.value.ValueFactories;

/**
 *
 * @author kulikov
 */
@SuppressWarnings("unchecked")
public class Operations {

    private final ValueFactories valueFactories;
    private final EsIndexColumns columns;
    
    /**
     * 
     * @param columns 
     */
    public Operations(ValueFactories valueFactories, EsIndexColumns columns) {
        this.valueFactories = valueFactories;
        this.columns = columns;
    }

    public EsRequest createQuery(Collection<Constraint> constraints, Map<String, Object> variables) {
        BoolQuery query = new BoolQuery();
        if (constraints.isEmpty()) {
            query.must(new MatchAllQuery());
        }
        
        for (Constraint c : constraints) {
            query = query.must(build(c, variables));
        }
        
        EsRequest res = new EsRequest();
        res.put("query", query.build());
        return res;
    }
    
    public Query build(Constraint constraint, Map<String, Object> variables) {
        if (constraint instanceof Between) {
            Between between = (Between) constraint;
            String field = (String)operand(between.getOperand()).apply(between.getOperand(), variables);
            Object low = staticOperand(between.getLowerBound()).apply(field, between.getLowerBound(), variables);
            Object high = staticOperand(between.getUpperBound()).apply(field, between.getUpperBound(), variables);
            
            return new RangeQuery(field).from(low).to(high)
                    .includeLower(between.isLowerBoundIncluded())
                    .includeUpper(between.isUpperBoundIncluded());
        }
        
        if (constraint instanceof Or) {
            Or or = (Or)constraint;
            return new OrQuery(
                    build(or.getConstraint1(), variables),
                    build(or.getConstraint2(), variables));
        }

        if (constraint instanceof And) {
            And and = (And)constraint;
            return new AndQuery(
                    build(and.getConstraint1(), variables),
                    build(and.getConstraint2(), variables));
        }

        if (constraint instanceof Not) {
            Not not = (Not)constraint;
            return new NotQuery(build(not.getConstraint(), variables));
        }

        if (constraint instanceof Comparison) {
            Comparison comp = (Comparison)constraint;
            String field = (String) operand(comp.getOperand1()).apply(comp.getOperand1(), variables);
            Object value = staticOperand(comp.getOperand2()).apply(field, comp.getOperand2(), variables);
            switch (comp.operator()) {
                case EQUAL_TO:
                    return new MatchQuery(field, value);
                case GREATER_THAN:
                    return new RangeQuery(field).gt(value);
                case GREATER_THAN_OR_EQUAL_TO:
                    return new RangeQuery(field).gte(value);
                case LESS_THAN:
                    return new RangeQuery(field).lt(value);
                case LESS_THAN_OR_EQUAL_TO:
                    return new RangeQuery(field).lte(value);
                case NOT_EQUAL_TO:
                    return new NotQuery(new MatchQuery(field, value));
                case LIKE:
                    String queryString = (String)value;                    
                    
                    if (!queryString.contains("%")) {
                        return new StringQuery(queryString);
                    }
                    
                    String[] terms = queryString.split(" ");
                    Query[] termFilters = new Query[terms.length];
                    
                    for (int i = 0; i < termFilters.length; i++) {
                        termFilters[i] = terms[i].contains("%") ?
                                new WildcardQuery(field, terms[i].replaceAll("%", "*")) :
                                new StringQuery(terms[i]);
                    }
                    
                    if (termFilters.length == 1) {
                        return termFilters[0];
                    }
                    
                    Query builder = new AndQuery(termFilters[0],termFilters[1]);
                    for (int i = 2; i < termFilters.length; i++) {
                        builder = new AndQuery(builder, termFilters[i]);
                    }
                    
                    return builder;
            }
        }

        if (constraint instanceof SetCriteria) {
            SetCriteria setCriteria = (SetCriteria)constraint;
            
            String field = (String) operand(setCriteria.getOperand()).apply(setCriteria.getOperand(), variables);
            ArrayList<Object> list = new ArrayList<Object>();
            for (StaticOperand so : setCriteria.getValues()) {
                Object vals = staticOperand(so).apply(field, so, variables);
                if (vals instanceof Object[]) {
                    list.addAll(Arrays.asList((Object[]) vals));
                } else if (vals != null) {
                    list.add(vals);
                }
            }
            
            Object[] set = new Object[list.size()];
            list.toArray(set);
            return new TermsQuery(field, set);
        }
        

        if (constraint instanceof PropertyExistence) {
            PropertyExistence pe = (PropertyExistence)constraint;
            return new ExistsQuery(pe.getPropertyName());
        }
        
        if (constraint instanceof FullTextSearch) {
            FullTextSearch fts = (FullTextSearch) constraint;
            return new StringQuery(fts.fullTextSearchExpression());
        }
        
        return null;
    }
    
    private abstract static class OperandBuilder<T extends DynamicOperand> {
        public abstract Object apply(T operand, Map<String, Object> variables);
    }
    

    private static abstract class StaticOperandBuilder<T extends StaticOperand> {
        public abstract Object apply(String field, T operand, Map<String, Object> variables);
    }
    
    private OperandBuilder operand(DynamicOperand op) {
        if (op instanceof PropertyValue) {
            return propertyValueBuilder;
        } 
        
        if (op instanceof NodePath) {
            return pathBuilder;
        } 
        
        if (op instanceof NodeDepth) {
            return depthBuilder;
        } 
        
        if (op instanceof NodeName) {
            return nodeNameBuilder;
        } 
        
        if (op instanceof LowerCase) {
            return lowerCaseBuilder;
        }
        
        if (op instanceof UpperCase) {
            return upperCaseBuilder;
        }

        if (op instanceof NodeLocalName) {
            return nodeLocalNameBuilder;
        }

        if (op instanceof Length) {
            return lengthBuilder;
        }
        
        return null;
    }
 
    private StaticOperandBuilder staticOperand(StaticOperand op) {
        if (op instanceof Literal) {
            return literalBuilder;
        }
        
        if (op instanceof BindVariableValue) {
            return bindVariableBuilder;
        }
        
        return null;
    }
    
    private final OperandBuilder propertyValueBuilder = new OperandBuilder<PropertyValue>() {
        @Override
        public String apply(PropertyValue operand, Map<String, Object> variables) {
            return operand.getPropertyName();
        }
    };

    private final StaticOperandBuilder literalBuilder = new StaticOperandBuilder<Literal>() {
        @Override
        public Object apply(String field, Literal op, Map<String, Object> variables) {
            EsIndexColumn col = columns.column(field);
            
            if (op.value() instanceof Object[]) {
                return col.columnValue((Object[])col.cast((Object[])op.value()));
            }
                        
            return col.columnValue(col.cast(op.value()));
        }
    };

    private final StaticOperandBuilder bindVariableBuilder = new StaticOperandBuilder<BindVariableValue>() {
        @Override
        public Object apply(String field, BindVariableValue op, Map<String, Object> variables) {
            EsIndexColumn col = columns.column(field);
            Object value = variables.get(op.getBindVariableName());
            
            if (value instanceof StaticOperand) {
                return staticOperand((StaticOperand)value).apply(field, (StaticOperand)value, variables);
            }
            
            if (value instanceof DynamicOperand) {
                return operand((DynamicOperand)value).apply((DynamicOperand)value, variables);
            }
            
            if (value instanceof Object[]) {
                return col.cast((Object[]) value);
            }
            
            if (value instanceof Collection) {
                return col.cast((Collection) value);
            }
            
            return col.cast(variables.get(op.getBindVariableName()));
        }
    };

    private final OperandBuilder lowerCaseBuilder = new OperandBuilder<LowerCase>() {
        @Override
        public Object apply(LowerCase op, Map<String, Object> variables) {
            return EsIndexColumn.LOWERCASE_PREFIX + 
                    ((String)operand(op.getOperand()).apply(op.getOperand(), variables));
        }
    };

    private final OperandBuilder upperCaseBuilder = new OperandBuilder<UpperCase>() {
        @Override
        public Object apply(UpperCase op, Map<String, Object> variables) {
            return EsIndexColumn.UPPERCASE_PREFIX + 
                    ((String)operand(op.getOperand()).apply(op.getOperand(), variables));
        }
    };
    
    private final OperandBuilder nodeLocalNameBuilder = new OperandBuilder<NodeLocalName>() {
        @Override
        public String apply(NodeLocalName op, Map<String, Object> variables) {
            return valueFactories.getStringFactory().create(ModeShapeLexicon.LOCALNAME);
        }
    };


    private final OperandBuilder nodeNameBuilder = new OperandBuilder<NodeName>() {
        @Override
        public String apply(NodeName op, Map<String, Object> variables) {
            return valueFactories.getStringFactory().create(JcrLexicon.NAME);
        }
    };

    private final OperandBuilder lengthBuilder = new OperandBuilder<Length>() {
        @Override
        public String apply(Length op, Map<String, Object> variables) {
            return EsIndexColumn.LENGTH_PREFIX + op.getPropertyValue().getPropertyName();
        }
    };

    private final OperandBuilder pathBuilder = new OperandBuilder<NodePath>() {
        @Override
        public String apply(NodePath op, Map<String, Object> variables) {
            return valueFactories.getStringFactory().create(JcrLexicon.PATH);
        }
    };

    private final OperandBuilder depthBuilder = new OperandBuilder<NodeDepth>() {
        @Override
        public String apply(NodeDepth op, Map<String, Object> variables) {
            return valueFactories.getStringFactory().create(ModeShapeLexicon.DEPTH);
        }
    };
    
    
    
}
