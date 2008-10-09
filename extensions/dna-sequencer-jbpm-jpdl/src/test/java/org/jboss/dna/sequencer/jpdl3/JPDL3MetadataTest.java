/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.sequencer.jpdl3;

import java.io.InputStream;
import java.util.List;
import static org.junit.Assert.*;
import org.jboss.dna.sequencer.jpdl3.JPDL3EndStateMetadata;
import org.jboss.dna.sequencer.jpdl3.JPDL3Metadata;
import org.jboss.dna.sequencer.jpdl3.JPDL3StartStateMetadata;
import org.jboss.dna.sequencer.jpdl3.JPDL3TransitionMetadata;
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
    public void shouldHaveStartState() {
        JPDL3StartStateMetadata jPDL3StartStateMetadata = metadata.getStartStateMetadata();
        assertNotNull(jPDL3StartStateMetadata);
        assertEquals("S0", jPDL3StartStateMetadata.getName());
        //Transitions
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

    public static JPDL3Metadata createJPDLMetadata() {
        InputStream stream = ClassLoaderUtil.getStream("processdefinition.xml");
        JPDL3Metadata metadata = JPDL3Metadata.instance(stream);
        assertNotNull(metadata);
        return metadata;
    }
}
