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
package org.modeshape.sequencer.jpdl3;

import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import java.util.List;
import org.jbpm.util.ClassLoaderUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Serge Pagop
 */
public class JPDL3MetadataTest {
    JPDL3Metadata metadata;

    @Before
    public void beforeEach() {
        metadata = createJPDLMetadata();
    }

    @After
    public void afterEach() {

    }

    @Test
    public void shouldHaveName() {
        assertEquals("Pd-Name", metadata.getPdName());
    }

    @Test
    public void shouldHaveOneOrMoreSwimlaneNode() {
        List<JPDL3SwimlaneMetadata> swimlanes = metadata.getSwimlanes();
        assertThat(swimlanes.isEmpty(), is(false));
        for (JPDL3SwimlaneMetadata swimlane : swimlanes) {
            if (swimlane.getName().equals("SL1")) {
                assertThat(swimlane.getAssignment(), is(notNullValue()));
                assertThat(swimlane.getAssignment().getFqClassName(), is("com.sample.assigned.Task1Handler"));
                assertThat(swimlane.getAssignment().getConfigType(), is("constructor"));
            } else if (swimlane.getName().equals("SL2")) {
                assertThat(swimlane.getAssignment(), is(notNullValue()));
                assertNotNull(swimlane.getAssignment());
                assertThat(swimlane.getAssignment().getFqClassName(), is("com.sample.assigned.Task2Handler"));
            } else if (swimlane.getName().equals("SL3")) {
                assertThat(swimlane.getAssignment(), is(notNullValue()));
                assertThat(swimlane.getAssignment().getFqClassName(),
                           is("org.jbpm.identity.assignment.ExpressionAssignmentHandler"));
                assertThat(swimlane.getAssignment().getExpression(), is("<expression>group(group1)</expression>"));
            } else if (swimlane.getName().equals("SL4")) {
                assertThat(swimlane.getAssignment(), is(nullValue()));
                assertThat(swimlane.getActorIdExpression(), is("bobthebuilder"));
            } else if (swimlane.getName().equals("SL5")) {
                assertThat(swimlane.getAssignment(), is(nullValue()));
                assertThat(swimlane.getPooledActorsExpression(), is("hippies,hells angles"));
            }
        }
    }

    @Test
    public void shouldHaveStartState() {
        JPDL3StartStateMetadata jPDL3StartStateMetadata = metadata.getStartStateMetadata();
        assertNotNull(jPDL3StartStateMetadata);
        assertEquals("S0", jPDL3StartStateMetadata.getName());
        // Transitions
        List<JPDL3TransitionMetadata> transitions = jPDL3StartStateMetadata.getTransitions();
        for (JPDL3TransitionMetadata jPDL3TransitionMetadata : transitions) {
            assertEquals("Tr01_S01", jPDL3TransitionMetadata.getName());
            assertEquals("Phase01", jPDL3TransitionMetadata.getTo());
        }
    }

    @Test
    public void shouldHaveEndState() {
        JPDL3EndStateMetadata jPDL3EndStateMetadata = metadata.getEndStateMetadata();
        assertNotNull(jPDL3EndStateMetadata);
        assertEquals("S1", jPDL3EndStateMetadata.getName());
    }

    @Test
    public void shouldHaveATaskNode() {
        List<JPDL3TaskNodeMetadata> taskNodes = metadata.getTaskNodes();
        assertThat(taskNodes.size() > 0, is(true));
        for (JPDL3TaskNodeMetadata jPDL3TaskNodeMetadata : taskNodes) {
            
            // task node 1
            if (jPDL3TaskNodeMetadata.getName().equals("Phase01")) {
                List<JPDL3TaskMetadata> tasks = jPDL3TaskNodeMetadata.getTasks();
                assertThat(tasks.size() == 2, is(true));
                for (JPDL3TaskMetadata jPDL3TaskMetadata : tasks) {
                    if (jPDL3TaskMetadata.getName().equals("Task01_Phase01")) {
                        String swimlane = jPDL3TaskMetadata.getSwimlane();
                        assertThat(swimlane, is("SL1"));
                    }
                }
            }
            
            List<JPDL3TransitionMetadata> transitions = jPDL3TaskNodeMetadata.getTransitions();
            
            for (JPDL3TransitionMetadata transitionMetadata : transitions) {
                String transitionName = transitionMetadata.getName();
                if (transitionName.equals("Tr01_Phase01")) {
                    assertThat(transitionMetadata.getTo(), is("Phase02"));
                } else if (transitionName.equals("Tr01_Phase02")) {
                    assertThat(transitionMetadata.getTo(), is("Phase03"));
                } else if (transitionName.equals("Tr01_Phase03")) {
                    assertThat(transitionMetadata.getTo(), is("Phase04"));
                } else if (transitionName.equals("Tr01_Phase04")) {
                    assertThat(transitionMetadata.getTo(), is("Phase05"));
                } else if (transitionName.equals("Tr01_Phase05")) {
                    assertThat(transitionMetadata.getTo(), is("Phase06"));
                } else if (transitionName.equals("Tr01_Phase06")) {
                    assertThat(transitionMetadata.getTo(), is("S1"));
                }

            }
        }
    }

    public static JPDL3Metadata createJPDLMetadata() {
        InputStream stream = ClassLoaderUtil.getStream("processdefinition.xml");
        JPDL3Metadata metadata = JPDL3Metadata.instance(stream);
        assertNotNull(metadata);
        return metadata;
    }
}
