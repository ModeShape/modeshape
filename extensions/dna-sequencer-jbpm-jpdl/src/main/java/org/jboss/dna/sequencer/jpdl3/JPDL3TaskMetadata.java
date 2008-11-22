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

/**
 * @author sp
 *
 */
public class JPDL3TaskMetadata {
    
    /**
     * The name.
     */
    private String name="";
    
    /**
     * The dueDate.
     */
    private String dueDate="";

    /**
     * The swimlane.
     */
    private String swimlane="";

    /**
     * @param name
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param dueDate
     */
    public void setDueDate( String dueDate ) {
        this.dueDate = dueDate;
    }

    /**
     * @return dueDate
     */
    public String getDueDate() {
        return dueDate;
    }

    /**
     * @return swimlane.
     */
    public String getSwimlane() {
        return this.swimlane;
    }

    /**
     * @param swimlane Sets swimlane to the specified value.
     */
    public void setSwimlane( String swimlane ) {
        this.swimlane = swimlane;
    }

}
