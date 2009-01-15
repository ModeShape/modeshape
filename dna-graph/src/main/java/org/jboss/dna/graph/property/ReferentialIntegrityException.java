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
package org.jboss.dna.graph.property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.dna.graph.Location;

/**
 * @author Randall Hauch
 */
public class ReferentialIntegrityException extends RuntimeException {

    /**
     */
    private static final long serialVersionUID = -3703984046286975978L;

    private final Map<Location, List<Reference>> invalidReferences;

    /**
     * @param location the location of the node containing the bad reference(s)
     * @param invalidReferences the invalid references
     */
    public ReferentialIntegrityException( Location location,
                                          Reference... invalidReferences ) {
        this.invalidReferences = new HashMap<Location, List<Reference>>();
        List<Reference> invalidRefList = null;
        if (invalidReferences == null || invalidReferences.length == 0) {
            invalidRefList = Collections.emptyList();
        } else if (invalidReferences.length == 1) {
            invalidRefList = Collections.singletonList(invalidReferences[0]);
        } else {
            invalidRefList = new ArrayList<Reference>();
            for (Reference ref : invalidReferences) {
                invalidRefList.add(ref);
            }
        }
        this.invalidReferences.put(location, invalidRefList);
    }

    /**
     * @param invalidReferences the map of locations to invalid references
     */
    public ReferentialIntegrityException( Map<Location, List<Reference>> invalidReferences ) {
        this.invalidReferences = invalidReferences;
    }

    /**
     * @param invalidReferences the map of locations to invalid references
     * @param message
     */
    public ReferentialIntegrityException( Map<Location, List<Reference>> invalidReferences,
                                          String message ) {
        super(message);
        this.invalidReferences = invalidReferences;
    }

    /**
     * @param invalidReferences the map of locations to invalid references
     * @param cause
     */
    public ReferentialIntegrityException( Map<Location, List<Reference>> invalidReferences,
                                          Throwable cause ) {
        super(cause);
        this.invalidReferences = invalidReferences;
    }

    /**
     * @param invalidReferences the map of locations to invalid references
     * @param message
     * @param cause
     */
    public ReferentialIntegrityException( Map<Location, List<Reference>> invalidReferences,
                                          String message,
                                          Throwable cause ) {
        super(message, cause);
        this.invalidReferences = invalidReferences;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * @return invalidReferences
     */
    public Map<Location, List<Reference>> getInvalidReferences() {
        return invalidReferences;
    }
}
