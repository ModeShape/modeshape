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
import java.util.ArrayList;
import java.util.List;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.node.EndState;
import org.jbpm.graph.node.StartState;

/**
 * The jBPM Process definition language meta data.
 * 
 * @author Serge Pagop
 */
public class JPDL3Metadata {
    private String pdName;
    private JPDL3StartStateMetadata jPDL3StartStateMetadata;
    private JPDL3EndStateMetadata jPDL3EndStateMetadata;

    
    private JPDL3Metadata() {
        // prevent construction
    }

    /**
     * Create an instance of {@link JPDL3Metadata} with all data of a specific jpdl xml document.
     * 
     * @param stream - the {@link InputStream}, that represents a stream of jpdl.
     * @return a object of {@link JPDL3Metadata}.
     */
    @SuppressWarnings( {"unchecked", "cast"} )
    public static JPDL3Metadata instance( InputStream stream ) {
        ProcessDefinition processDefinition = ProcessDefinition.parseXmlInputStream(stream);
        if (processDefinition != null) {
            JPDL3Metadata jplMetadata = new JPDL3Metadata();
            if (processDefinition.getName() != null) {
                jplMetadata.setPdName(processDefinition.getName());
            }
            List<Node> nodes = (List<Node>)processDefinition.getNodes();
            for (Node node : nodes) {
                if (node instanceof StartState) {
                    StartState startState = (StartState)node;
                    JPDL3StartStateMetadata jPDL3StartStateMetadata = new JPDL3StartStateMetadata();
                    if (startState.getName() != null) {
                        jPDL3StartStateMetadata.setName(startState.getName());
                    }
                    List<JPDL3TransitionMetadata> transitions = new ArrayList<JPDL3TransitionMetadata>();
                    for (Transition transition : (List<Transition>)startState.getLeavingTransitions()) {
                        JPDL3TransitionMetadata jPDL3TransitionMetadata = new JPDL3TransitionMetadata();
                        if (transition.getName() != null) {
                            jPDL3TransitionMetadata.setName(transition.getName());
                        }
                        Node toNode = transition.getTo();
                        if (toNode != null) {
                            jPDL3TransitionMetadata.setTo(toNode.getName());
                        }
                        transitions.add(jPDL3TransitionMetadata);
                    }
                    jPDL3StartStateMetadata.setTransitions(transitions);
                    jplMetadata.setStartStateMetadata(jPDL3StartStateMetadata);
                }

                if (node instanceof EndState) {
                    EndState endState = (EndState)node;
                    JPDL3EndStateMetadata jPDL3EndStateMetadata = new JPDL3EndStateMetadata();
                    if (endState.getName() != null) {
                        jPDL3EndStateMetadata.setName(endState.getName());
                    }
                    jplMetadata.setEndStateMetadata(jPDL3EndStateMetadata);
                }
            }
            return jplMetadata;
        }
        return null;
    }

    /**
     * Get the name of process definition.
     * 
     * @return the name of the process definition.
     */
    public String getPdName() {
        return pdName;
    }

    /**
     * Set the name of process definition.
     * 
     * @param pdName - the name of process definition.
     */
    public void setPdName( String pdName ) {
        this.pdName = pdName;
    }

    /**
     * @return the jPDL3StartStateMetadata.
     */
    public JPDL3StartStateMetadata getStartStateMetadata() {
        return jPDL3StartStateMetadata;
    }
    
    /**
     * 
     * @return the jPDL3EndStateMetadata.
     */
    public JPDL3EndStateMetadata getEndStateMetadata() {
        return jPDL3EndStateMetadata;
    }
    
    
    /**
     * @param jPDL3StartStateMetadata the jPDL3StartStateMetadata to set
     */
    public void setStartStateMetadata( JPDL3StartStateMetadata jPDL3StartStateMetadata ) {
        this.jPDL3StartStateMetadata = jPDL3StartStateMetadata;
    }

    /**
     * @param jPDL3EndStateMetadata the jPDL3EndStateMetadata to set
     */
    public void setEndStateMetadata( JPDL3EndStateMetadata jPDL3EndStateMetadata ) {
        this.jPDL3EndStateMetadata = jPDL3EndStateMetadata;
    }
}
