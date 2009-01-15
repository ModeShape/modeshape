/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.federation.merge;

import java.util.Iterator;
import java.util.NoSuchElementException;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.connector.federation.contribution.Contribution;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public class FiveContributionMergePlan extends MergePlan {

    private static final long serialVersionUID = 1L;
    private final Contribution contribution1;
    private final Contribution contribution2;
    private final Contribution contribution3;
    private final Contribution contribution4;
    private final Contribution contribution5;

    /**
     * @param contribution1 the first contribution for this merge plan
     * @param contribution2 the second contribution for this merge plan
     * @param contribution3 the third contribution for this merge plan
     * @param contribution4 the fourth contribution for this merge plan
     * @param contribution5 the fifth contribution for this merge plan
     */
    /*package*/FiveContributionMergePlan( Contribution contribution1,
                                           Contribution contribution2,
                                           Contribution contribution3,
                                           Contribution contribution4,
                                           Contribution contribution5 ) {
        assert contribution1 != null;
        assert contribution2 != null;
        assert contribution3 != null;
        assert contribution4 != null;
        assert contribution5 != null;
        this.contribution1 = contribution1;
        this.contribution2 = contribution2;
        this.contribution3 = contribution3;
        this.contribution4 = contribution4;
        this.contribution5 = contribution5;
        assert checkEachContributionIsFromDistinctSource();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.merge.MergePlan#getContributionCount()
     */
    @Override
    public int getContributionCount() {
        return 5;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.merge.MergePlan#getContributionFrom(java.lang.String)
     */
    @Override
    public Contribution getContributionFrom( String sourceName ) {
        if (contribution1.getSourceName().equals(sourceName)) return contribution1;
        if (contribution2.getSourceName().equals(sourceName)) return contribution2;
        if (contribution3.getSourceName().equals(sourceName)) return contribution3;
        if (contribution4.getSourceName().equals(sourceName)) return contribution4;
        if (contribution5.getSourceName().equals(sourceName)) return contribution5;
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Contribution> iterator() {
        return new Iterator<Contribution>() {
            private int next = 5;

            public boolean hasNext() {
                return next > 0;
            }

            @SuppressWarnings( "synthetic-access" )
            public Contribution next() {
                if (next == 5) {
                    next = 4;
                    return contribution1;
                }
                if (next == 4) {
                    next = 3;
                    return contribution2;
                }
                if (next == 3) {
                    next = 2;
                    return contribution3;
                }
                if (next == 2) {
                    next = 1;
                    return contribution4;
                }
                if (next == 1) {
                    next = 0;
                    return contribution5;
                }
                throw new NoSuchElementException();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.merge.MergePlan#isSource(java.lang.String)
     */
    @Override
    public boolean isSource( String sourceName ) {
        if (contribution1.getSourceName().equals(sourceName)) return true;
        if (contribution2.getSourceName().equals(sourceName)) return true;
        if (contribution3.getSourceName().equals(sourceName)) return true;
        if (contribution4.getSourceName().equals(sourceName)) return true;
        if (contribution5.getSourceName().equals(sourceName)) return true;
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(contribution1, contribution2, contribution3, contribution4, contribution5);
    }

}
