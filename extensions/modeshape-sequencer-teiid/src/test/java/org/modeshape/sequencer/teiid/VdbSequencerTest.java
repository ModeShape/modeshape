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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.sequencer.AbstractStreamSequencerTest;
import org.modeshape.graph.sequencer.StreamSequencer;

/**
 * 
 */
public class VdbSequencerTest extends AbstractStreamSequencerTest {

    @Override
    protected StreamSequencer createSequencer() {
        return new VdbSequencer();
    }

    @Test
    public void shouldSequenceVdbForQuickEmployees() throws Exception {
        // print = true;
        sequence("vdb/qe.vdb");
        assertNoProblems();
        printOutput();
    }

    @Test
    public void shouldSequenceVdbForQuickEmployeesWithVersionSpecifiedInFileName() throws Exception {
        // print = true;
        sequence("vdb/qe.2.vdb");
        assertNoProblems();
        printOutput();
    }

    @Test
    public void shouldSequenceVdbForPartsFromXml() throws Exception {
        // print = true;
        sequence("vdb/PartsFromXml.vdb");
        assertNoProblems();
        printOutput();
    }

    @Test
    public void shouldSequenceVdbForYahooUdfTest() throws Exception {
        // print = true;
        sequence("vdb/YahooUdfTest.vdb");
        assertNoProblems();
        printOutput();
    }

    @Test
    public void shouldExtractVersionInformation() {
        assertVersionInfo("something", "something", 1);
        assertVersionInfo("something.else", "something.else", 1);
        assertVersionInfo("something else", "something else", 1);
        assertVersionInfo("something.", "something", 1);
        assertVersionInfo("something.1", "something", 1);
        assertVersionInfo("something.12", "something", 12);
        assertVersionInfo("something.123", "something", 123);
        assertVersionInfo("something.4", "something", 4);
        assertVersionInfo("something.-4", "something", 4);
        assertVersionInfo("something.+4", "something", 4);
        assertVersionInfo("something. 4", "something", 4);
        assertVersionInfo("something.  4", "something", 4);
        assertVersionInfo("something.  -4", "something", 4);
        assertVersionInfo("something.  -1234  ", "something", 1234);
    }

    protected void assertVersionInfo( String fileNameWithoutExtension,
                                      String expectedName,
                                      int expectedVersion ) {
        AtomicInteger actual = new AtomicInteger(1);
        Name name = VdbSequencer.extractVersionInfomation(context, fileNameWithoutExtension, actual);
        assertThat(name, is(context.getValueFactories().getNameFactory().create(expectedName)));
        assertThat(actual.get(), is(expectedVersion));
    }
}
