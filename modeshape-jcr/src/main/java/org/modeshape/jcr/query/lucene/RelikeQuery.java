package org.modeshape.jcr.query.lucene;

import java.io.IOException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.regex.Pattern;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FilteredTermEnum;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.ToStringUtils;

/**
 * A Lucene {@link Query} implementation that matches some value by document property as like pattern.
 * 
 * @see RelikeTermEnum
 */
public class RelikeQuery extends MultiTermQuery {

    private static final long serialVersionUID = 1L;

    private final String relikeValue;
    private final Term term;

    public RelikeQuery( String fieldName,
                        String relikeValue ) {
        assert relikeValue != null;

        term = new Term(fieldName);

        this.relikeValue = relikeValue;
    }

    @Override
    protected FilteredTermEnum getEnum( IndexReader reader ) throws IOException {
        return new RelikeTermEnum(reader, getTerm(), relikeValue);
    }

    public Term getTerm() {
        return term;
    }

    @Override
    public String toString( String field ) {
        StringBuilder buffer = new StringBuilder();
        if (!term.field().equals(field)) {
            buffer.append(term.field());
            buffer.append(":");
        }
        buffer.append(term.text());
        buffer.append(ToStringUtils.boost(getBoost()));
        return buffer.toString();
    }

    @Override
    public boolean equals( Object o ) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        final RelikeQuery that = (RelikeQuery)o;

        return relikeValue.equals(that.relikeValue);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + relikeValue.hashCode();
        return result;
    }

    private class RelikeTermEnum extends FilteredTermEnum {

        private String field;
        private boolean endEnum = false;
        private final String relikeValue;

        public RelikeTermEnum( IndexReader reader,
                               Term term,
                               String relikeValue ) throws IOException {
            super();

            this.field = term.field();
            this.relikeValue = relikeValue;

            setEnum(reader.terms(term));
        }

        @Override
        protected final boolean termCompare( Term term ) {
            if (field == term.field()) {
                return like(relikeValue, term.text());
            }
            endEnum = true;
            return false;
        }

        @Override
        public final float difference() {
            return 1.0f;
        }

        @Override
        public final boolean endEnum() {
            return endEnum;
        }

        @Override
        public void close() throws IOException {
            super.close();
            field = null;
        }
    }

    public static boolean like( String value,
                                String pattern ) {
        CompareType cmpType = getCompareType(pattern);
        switch (cmpType) {
            case EQ: {
                return value.equals(pattern);
            }
            case ENDS_WITH: {
                return value.endsWith(pattern.substring(1));
            }
            case STARTS_WITH: {
                return value.startsWith(pattern.substring(0, pattern.length() - 1));
            }
            case REGEXP:
            default: {
                Pattern p = Pattern.compile(toRegularExpression(pattern));
                return p.matcher(value).matches();
            }
        }
    }

    private static enum CompareType {
        EQ,
        STARTS_WITH,
        ENDS_WITH,
        REGEXP
    }

    /**
     * Determine the compare type given the expression. This method returns:
     * <ul>
     * <li>CompareType.EQ if expression does not contains '_' or '%';</li>
     * <li>CompareType.ENDS_WITH if expression has only one '%' at start and does not contains '_';</li>
     * <li>CompareType.STARTS_WITH if expression has only one '%' at end and does not contains '_';</li>
     * <li>CompareType.REGEXP otherwise</li>
     * </ul>
     * 
     * @param expression the expression for which the {@link CompareType} is to be found
     * @return the compare type; never null;
     */
    private static CompareType getCompareType( String expression ) {
        CompareType result = CompareType.EQ;

        CharacterIterator iter = new StringCharacterIterator(expression);
        final int fistIndex = 0;
        final int lastIndex = expression.length() - 1;
        boolean skipNext = false;
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            if (skipNext) {
                skipNext = false;
                continue;
            }
            if (c == '_') {
                return CompareType.REGEXP;
            }
            if (c == '%') {
                if (result != CompareType.EQ) {
                    // pattern like '%abcdfe%' -> only regexp can handle this;
                    return CompareType.REGEXP;
                }

                int index = iter.getIndex();
                if (index == fistIndex) {
                    result = CompareType.ENDS_WITH;
                } else if (index == lastIndex) {
                    result = CompareType.STARTS_WITH;
                } else {
                    return CompareType.REGEXP;
                }
            }

            if (c == '\\') skipNext = true;
        }

        return result;
    }

    private static String toRegularExpression( String likeExpression ) {
        // Replace all '\x' with 'x' ...
        String result = likeExpression.replaceAll("\\\\(.)", "$1");
        // Escape characters used as metacharacters in regular expressions, including
        // '[', '^', '\', '$', '.', '|', '+', '*', '?', '(', and ')'
        result = result.replaceAll("([$.|+()\\*\\?\\[\\\\^\\\\\\\\])", "\\\\$1");
        // Replace '%'->'[.]*' and '_'->'[.]
        result = result.replace("%", ".*").replace("_", ".");
        return result;
    }
}
