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
package org.modeshape.jdbc;

/**
 * Specialized implementation for ModeShape-specific features.
 */
public class ModeShapeMetaData extends JcrMetaData {

    public ModeShapeMetaData( JcrConnection connection ) {
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
