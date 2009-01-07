/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.connector.federation.merge;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.connector.federation.contribution.Contribution;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public class MultipleContributionMergePlan extends MergePlan {

    private static final long serialVersionUID = 1L;
    private final List<Contribution> contributions = new CopyOnWriteArrayList<Contribution>();

    /**
     * @param contributions the contributions for this merge plan
     */
    /*package*/MultipleContributionMergePlan( Contribution... contributions ) {
        assert contributions != null;
        for (int i = 0; i != contributions.length; ++i) {
            assert contributions[i] != null;
            this.contributions.add(contributions[i]);
        }
        assert checkEachContributionIsFromDistinctSource();
    }

    /**
     * @param contributions the contributions for this merge plan
     */
    /*package*/MultipleContributionMergePlan( Iterable<Contribution> contributions ) {
        assert contributions != null;
        for (Contribution contribution : contributions) {
            assert contribution != null;
            this.contributions.add(contribution);
        }
        assert checkEachContributionIsFromDistinctSource();
    }

    /**
     * @param contributions the contributions for this merge plan
     */
    /*package*/MultipleContributionMergePlan( Iterator<Contribution> contributions ) {
        assert contributions != null;
        while (contributions.hasNext()) {
            Contribution contribution = contributions.next();
            assert contribution != null;
            this.contributions.add(contribution);
        }
        assert checkEachContributionIsFromDistinctSource();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.merge.MergePlan#getContributionCount()
     */
    @Override
    public int getContributionCount() {
        return contributions.size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.merge.MergePlan#getContributionFrom(java.lang.String)
     */
    @Override
    public Contribution getContributionFrom( String sourceName ) {
        for (Contribution contribution : contributions) {
            if (contribution.getSourceName().equals(sourceName)) return contribution;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Contribution> iterator() {
        final Iterator<Contribution> iterator = this.contributions.iterator();
        return new Iterator<Contribution>() {
            public boolean hasNext() {
                return iterator.hasNext();
            }

            public Contribution next() {
                return iterator.next();
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
        for (Contribution contribution : contributions) {
            if (contribution.getSourceName().equals(sourceName)) return true;
        }
        return false;
    }

    /**
     * @param contribution
     */
    public void addContribution( Contribution contribution ) {
        this.contributions.add(contribution);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return contributions.hashCode();
    }

}
