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
import org.jboss.dna.connector.federation.contribution.Contribution;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public class OneContributionMergePlan extends MergePlan {

    private static final long serialVersionUID = 1L;
    private final Contribution contribution;

    /**
     * @param contribution the contribution for this merge plan
     */
    /*package*/OneContributionMergePlan( Contribution contribution ) {
        assert contribution != null;
        this.contribution = contribution;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.merge.MergePlan#getContributionCount()
     */
    @Override
    public int getContributionCount() {
        return 1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.merge.MergePlan#getContributionFrom(java.lang.String)
     */
    @Override
    public Contribution getContributionFrom( String sourceName ) {
        return isSource(sourceName) ? contribution : null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Contribution> iterator() {
        return new Iterator<Contribution>() {
            private boolean next = true;

            public boolean hasNext() {
                return next;
            }

            @SuppressWarnings( "synthetic-access" )
            public Contribution next() {
                if (next) {
                    next = false;
                    return contribution;
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
        return contribution.getSourceName().equals(sourceName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return contribution.hashCode();
    }

}
