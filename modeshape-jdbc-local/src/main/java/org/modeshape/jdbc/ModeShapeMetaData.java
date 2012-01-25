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
package org.modeshape.jdbc;


/**
 * Specialized implementation for ModeShape-specific features.
 */
public class ModeShapeMetaData extends JcrMetaData {

    public ModeShapeMetaData( JcrConnection connection) {
        super(connection);
    }

    /**
     * {@inheritDoc}
     * <p>
     * ModeShape does support <code>FULL OUTER JOIN</code>, so this method returns true when this driver connects to a ModeShape
     * JCR repository.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#supportsFullOuterJoins()
     */
    @Override
    public boolean supportsFullOuterJoins() {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * ModeShape does support <code>UNION</code>, so this method returns true when this driver connects to a ModeShape JCR
     * repository.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#supportsUnion()
     */
    @Override
    public boolean supportsUnion() {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * ModeShape does support <code>UNION ALL</code>, so this method returns true when this driver connects to a ModeShape JCR
     * repository.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#supportsUnionAll()
     */
    @Override
    public boolean supportsUnionAll() {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * ModeShape uses arithmetic operations in criteria, and in the current implementation if one operand is null then the
     * operations returns the other. Therefore, 'null + X = X', so this method returns <code>false</code>.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#nullPlusNonNullIsNull()
     */
    @Override
    public boolean nullPlusNonNullIsNull() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * ModeShape definitely uses sort order. Therefore, this method always returns <code>false</code>.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#nullsAreSortedAtEnd()
     */
    @Override
    public boolean nullsAreSortedAtEnd() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * ModeShape definitely uses sort order. Therefore, this method always returns <code>false</code>.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#nullsAreSortedAtStart()
     */
    @Override
    public boolean nullsAreSortedAtStart() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * ModeShape sorts null values to be lower than non-null values. Therefore, this method returns <code>false</code>.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#nullsAreSortedHigh()
     */
    @Override
    public boolean nullsAreSortedHigh() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * ModeShape sorts null values to be lower than non-null values. Therefore, this method returns <code>true</code>.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#nullsAreSortedLow()
     */
    @Override
    public boolean nullsAreSortedLow() {
        return true;
    }

}
