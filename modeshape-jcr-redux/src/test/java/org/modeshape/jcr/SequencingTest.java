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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import javax.jcr.Node;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Json;
import org.junit.Test;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;

/**
 * Tests of various sequencing configurations.
 */
public class SequencingTest extends SingleUseAbstractTest {

    protected void startRepositoryWithConfiguration( String configContent ) throws Exception {
        Document doc = Json.read(configContent);
        startRepositoryWithConfiguration(doc);
    }

    protected void startRepositoryWithConfiguration( Document doc ) throws Exception {
        config = new RepositoryConfiguration(doc, REPO_NAME, cm);
        repository = new JcrRepository(config);
        repository.start();
        session = repository.login();
    }

    protected void addSequencer( EditableDocument doc,
                                 String name,
                                 String desc,
                                 String classname,
                                 String... pathExpressions ) {
        EditableDocument sequencing = doc.getOrCreateDocument(FieldName.SEQUENCING);
        EditableArray sequencers = sequencing.getOrCreateArray(FieldName.SEQUENCERS);
        // Create the sequencer doc ...
        EditableDocument sequencer = Schematic.newDocument();
        sequencer.set(FieldName.NAME, name);
        sequencer.set(FieldName.DESCRIPTION, desc);
        sequencer.set(FieldName.CLASSNAME, classname);
        if (pathExpressions.length == 1) {
            sequencer.set(FieldName.PATH_EXPRESSION, pathExpressions[0]);
        } else {
            sequencer.set(FieldName.PATH_EXPRESSIONS, pathExpressions);
        }
        sequencers.add(sequencer);
    }

    @Test
    public void shouldStartRepositoryWithNoSequencers() throws Exception {
        startRepositoryWithConfiguration("{}");
    }

    @Test
    public void shouldStartRepositoryWithOneSequencer() throws Exception {
        EditableDocument doc = Schematic.newDocument();
        addSequencer(doc, "seq1", "desc of seq1", TestSequencer.class.getName(), "/foo[@bar]");
        startRepositoryWithConfiguration(doc);

        // Now use a session to add a '/foo' node with a 'bar' property ...
        Node foo = session.getRootNode().addNode("foo");
        foo.setProperty("bar", "value of bar");
        foo.setProperty("baz", "value of baz");
        session.save();

        boolean found = false;
        for (int i = 0; i != 10; ++i) {
            if (TestSequencer.COUNTER.get() == 1) {
                found = true;
                break;
            }
            Thread.sleep(100L);
        }
        assertThat("Failed to sequence '/foo' even after waiting 1 sec", found, is(true));
    }

    /**
     * Sequencer path expressions are matching expressions, and therefore we cannot verify that they actually represent paths. So,
     * even though this is an valid path expression, it won't match any real paths.
     * 
     * @throws Exception
     */
    @Test
    public void shouldCreateStartRepositoryWithValidButUnusableSequencerPathExpression() throws Exception {
        EditableDocument doc = Schematic.newDocument();
        addSequencer(doc, "seq1", "desc of seq1", TestSequencer.class.getName(), "## valid but unusable");
        startRepositoryWithConfiguration(doc);
    }
}
