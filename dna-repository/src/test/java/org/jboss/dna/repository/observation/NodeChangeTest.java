/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.repository.observation;

import java.util.HashSet;
import java.util.Set;
import javax.jcr.observation.Event;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class NodeChangeTest {

    private String validRepositoryWorkspaceName;
    private String validAbsolutePath;
    private int validEventTypes;
    private Set<String> validModifiedProperties;
    private Set<String> validRemovedProperties;
    private NodeChange nodeChange;

    @Before
    public void beforeEach() {
        validRepositoryWorkspaceName = "repositoryX";
        validAbsolutePath = "/a/b/c/d";
        validEventTypes = Event.NODE_ADDED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;
        validModifiedProperties = new HashSet<String>();
        validRemovedProperties = new HashSet<String>();
        validModifiedProperties.add("jcr:name");
        validModifiedProperties.add("jcr:title");
        validRemovedProperties.add("jcr:mime");
        nodeChange = new NodeChange("", validRepositoryWorkspaceName, validAbsolutePath, validEventTypes, validModifiedProperties,
                                    validRemovedProperties);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldHaveUnmodifiableSetOfModifiedPropertyNames() {
        nodeChange.getModifiedProperties().clear();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldHaveUnmodifiableSetOfRemovedPropertyNames() {
        nodeChange.getRemovedProperties().clear();
    }
}
