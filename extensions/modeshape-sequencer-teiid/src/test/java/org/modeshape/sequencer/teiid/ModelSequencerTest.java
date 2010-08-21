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
package org.modeshape.sequencer.teiid;

import org.junit.Test;
import org.modeshape.graph.sequencer.AbstractStreamSequencerTest;
import org.modeshape.graph.sequencer.StreamSequencer;

public class ModelSequencerTest extends AbstractStreamSequencerTest {

    @Override
    protected StreamSequencer createSequencer() {
        return new ModelSequencer();
    }

    @Test
    public void shouldSequenceOldBooksPhysicalRelationalModelForOracle() throws Exception {
        print = true;
        sequence("model/old/BooksO.xmi");
        assertNoProblems();
        printOutput();
    }

    @Test
    public void shouldSequenceOldBooksPhysicalRelationalModelForSqlServer() throws Exception {
        sequence("model/old/BooksS.xmi");
        assertNoProblems();
        printOutput();
    }

    @Test
    public void shouldSequenceNewBooksPhysicalRelationalModelForSourceA() throws Exception {
        print = true;
        sequence("model/books/Books_SourceA.xmi");
        assertNoProblems();
        printOutput();
    }

    @Test
    public void shouldSequenceNewBooksPhysicalRelationalModelForSourceB() throws Exception {
        print = true;
        sequence("model/books/Books_SourceB.xmi");
        assertNoProblems();
        printOutput();
    }

    @Test
    public void shouldSequencePartsSupplierPhysicalRelationalModelForSourceA() throws Exception {
        print = true;
        sequence("model/parts/PartsSupplier_SourceA.xmi");
        assertNoProblems();
        printOutput();
    }

    @Test
    public void shouldSequencePartsSupplierPhysicalRelationalModelForSourceB() throws Exception {
        print = true;
        sequence("model/parts/PartSupplier_SourceB.xmi");
        assertNoProblems();
        printOutput();
    }

    @Test
    public void shouldSequencePartsSupplierVirtualRelationalModel() throws Exception {
        print = true;
        sequence("model/parts/PartsVirtual.xmi");
        assertNoProblems();
        printOutput();
    }
}
