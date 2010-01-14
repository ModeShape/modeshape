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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.node.EndState;
import org.jbpm.graph.node.StartState;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.instantiation.Delegation;
import org.jbpm.taskmgmt.def.Swimlane;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;
import static org.modeshape.sequencer.jpdl3.JPDL3MetadataConstants.*;

/**
 * The jBPM Process definition language meta data.
 * 
 * @author Serge Pagop
 */
public class JPDL3Metadata {

    /**
     * The process definition name.
     */
    private String pdName;

    /**
     * The start node of the process definition.
     */
    private JPDL3StartStateMetadata jPDL3StartStateMetadata;

    /**
     * The end node of the process definition.
     */
    private JPDL3EndStateMetadata jPDL3EndStateMetadata;

    /**
     * The swimlanes of the process definitions
     */
    List<JPDL3SwimlaneMetadata> swimlanes = new ArrayList<JPDL3SwimlaneMetadata>();

    /**
     * The task nodes of the process definitions.
     */
    private List<JPDL3TaskNodeMetadata> taskNodes = new ArrayList<JPDL3TaskNodeMetadata>();

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
        List<JPDL3SwimlaneMetadata> swimlaneContainer = new ArrayList<JPDL3SwimlaneMetadata>();
        List<JPDL3TaskNodeMetadata> taskNodeContainer = new ArrayList<JPDL3TaskNodeMetadata>();

        if (processDefinition != null) {
            JPDL3Metadata jplMetadata = new JPDL3Metadata();
            if (processDefinition.getName() != null) {
                jplMetadata.setPdName(processDefinition.getName());
            }

            TaskMgmtDefinition taskMgmtDefinition = processDefinition.getTaskMgmtDefinition();
            if (taskMgmtDefinition != null) {
                // Get the swimlanes of the process definition, if there is one.
                Map<String, Swimlane> mapOfSwimlanes = taskMgmtDefinition.getSwimlanes();
                Set<String> swimlaneKeys = mapOfSwimlanes.keySet();
                for (String swimlaneKey : swimlaneKeys) {
                    Swimlane swimlane = mapOfSwimlanes.get(swimlaneKey);
                    JPDL3SwimlaneMetadata jPDL3SwimlaneMetadata = new JPDL3SwimlaneMetadata();
                    jPDL3SwimlaneMetadata.setName(swimlane.getName());
                    if (swimlane.getActorIdExpression() != null) jPDL3SwimlaneMetadata.setActorIdExpression(swimlane.getActorIdExpression());
                    if (swimlane.getPooledActorsExpression() != null) jPDL3SwimlaneMetadata.setPooledActorsExpression(swimlane.getPooledActorsExpression());
                    Delegation delegation = swimlane.getAssignmentDelegation();
                    if (delegation != null) {
                        JPDL3AssignmentMetadata jPDL3AssignmentMetadata = new JPDL3AssignmentMetadata();
                        // full qualified class name.
                        jPDL3AssignmentMetadata.setFqClassName(delegation.getClassName());
                        // config type
                        if (delegation.getConfigType() != null) jPDL3AssignmentMetadata.setConfigType(delegation.getConfigType());
                        // expression assignment
                        if (EXPRESSION_ASSIGNMENT_HANLDER_DELEGATION_CN.equals(delegation.getClassName())) jPDL3AssignmentMetadata.setExpression(delegation.getConfiguration());
                        jPDL3SwimlaneMetadata.setAssignment(jPDL3AssignmentMetadata);
                    }
                    swimlaneContainer.add(jPDL3SwimlaneMetadata);
                    // with expression
                }
            }
            jplMetadata.setSwimlanes(swimlaneContainer);

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
                
                // TaskNode
                if (node instanceof TaskNode) {
                    TaskNode taskNode = (TaskNode)node;
                    JPDL3TaskNodeMetadata jPDL3TaskNodeMetadata = new JPDL3TaskNodeMetadata();
                    
                    if(taskNode.getName() != null) {
                        jPDL3TaskNodeMetadata.setName(taskNode.getName()); 
                    }
                    
                    Map<String, Task> tasks = taskNode.getTasksMap();
                    List<JPDL3TaskMetadata> taskList = new ArrayList<JPDL3TaskMetadata>();
                    
                    if(!tasks.isEmpty()) {
                        Set<String> keys = tasks.keySet();
                        for (String key : keys) {
                            Task task = tasks.get(key);
                            JPDL3TaskMetadata jPDL3TaskMetadata = new JPDL3TaskMetadata();
                            if(task.getName() != null)
                                jPDL3TaskMetadata.setName(task.getName());
                            if(task.getDueDate() != null)
                                jPDL3TaskMetadata.setDueDate(task.getDueDate());
                            taskList.add(jPDL3TaskMetadata);
                            
                            if(task.getSwimlane() != null) {
                                Swimlane swimlane = task.getSwimlane();
                                jPDL3TaskMetadata.setSwimlane(swimlane.getName());
                            }
                        }
                    }
                    jPDL3TaskNodeMetadata.setTasks(taskList);
                    
                    // transitions
                    List<JPDL3TransitionMetadata> transitions = new ArrayList<JPDL3TransitionMetadata>();
                    for (Transition transition : (List<Transition>)taskNode.getLeavingTransitions()) {
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
                    jPDL3TaskNodeMetadata.setTransitions(transitions);
                    
                    taskNodeContainer.add(jPDL3TaskNodeMetadata);
                    jplMetadata.setTaskNodes(taskNodeContainer);
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
        return this.jPDL3StartStateMetadata;
    }

    /**
     * @return the jPDL3EndStateMetadata.
     */
    public JPDL3EndStateMetadata getEndStateMetadata() {
        return this.jPDL3EndStateMetadata;
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

    /**
     * Get a list of all swimlane of the process definition
     * 
     * @return a list of all swimlane of the process definition. this can also be a empty list.
     */
    public List<JPDL3SwimlaneMetadata> getSwimlanes() {
        return this.swimlanes;
    }

    /**
     * Set a list with some swimlanes for the process definition.
     * 
     * @param swimlanes - the swimlanes.
     */
    public void setSwimlanes( List<JPDL3SwimlaneMetadata> swimlanes ) {
        this.swimlanes = swimlanes;
    }

    /**
     * @return the task nodes
     */
    public List<JPDL3TaskNodeMetadata> getTaskNodes() {
        return this.taskNodes;
    }

    /**
     * @param taskNodes Sets taskNodes to the specified value.
     */
    public void setTaskNodes( List<JPDL3TaskNodeMetadata> taskNodes ) {
        this.taskNodes = taskNodes;
    }
}
