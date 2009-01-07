/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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

/**
 * Metdata class represent a swimlane in a jpdl xml file.
 * 
 * @author Serge Pagop
 */
public class JPDL3SwimlaneMetadata {

    /**
     * The name.
     */
    private String name = "";

    /**
     * The JPDL3AssignmentMetadata
     */
    private JPDL3AssignmentMetadata assignment;

    /**
     * The actor id expression.
     */
    private String actorIdExpression = "";

    /**
     * The pooledActorsExpression.
     */
    private String pooledActorsExpression = "";

    /**
     * Get the name of the specific swimlane.
     * 
     * @return name of the swimlane.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the name of the swimlane.
     * 
     * @param name - the name of the swimlane.
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * Get the delegated instance, the assignment.
     * 
     * @return assignment - the delegated instance.
     */
    public JPDL3AssignmentMetadata getAssignment() {
        return this.assignment;
    }

    /**
     * Set the delegated instance, the assignment.
     * 
     * @param assignment - the delegated instance.
     */
    public void setAssignment( JPDL3AssignmentMetadata assignment ) {
        this.assignment = assignment;
    }

    /**
     * @param actorIdExpression
     */
    public void setActorIdExpression( String actorIdExpression ) {
        this.actorIdExpression = actorIdExpression;
    }

    /**
     * @return actorIdExpression
     */
    public String getActorIdExpression() {
        return actorIdExpression;
    }

    /**
     * @param pooledActorsExpression
     */
    public void setPooledActorsExpression( String pooledActorsExpression ) {
        this.pooledActorsExpression = pooledActorsExpression;
    }

    /**
     * @return pooledActorsExpression
     */
    public String getPooledActorsExpression() {
        return pooledActorsExpression;
    }

}
