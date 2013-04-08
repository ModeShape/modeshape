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
package org.modeshape.connector;

import java.util.ArrayList;

/**
 * Maps relations between JCR node types and CMIS object types.
 * 
 * @author kulikov
 */
public class Nodes {
    private final static String[] map = new String[] {
        "cmis:folder = folder",
        "cmis:document = file"
    };
    
    private ArrayList<Relation> list = new ArrayList();
    
    /**
     * Gets the name of the given property in JCR domain.
     * 
     * @param cmisName the name of the given property in CMIS domain.
     * @return the name of the given property in JCR domain.
     */
    public String findJcrName(String cmisName) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).cmisName.equals(cmisName)) {
                return list.get(i).jcrName;
            }
        }
        return cmisName;
    }

    /**
     * Gets the name of the given property in CMIS domain.
     * 
     * @param jcrName the name of the given property in JCR domain.
     * @return the name of the given property in CMIS domain.
     */
    public String findCmisName(String jcrName) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).jcrName.equals(jcrName)) {
                return list.get(i).cmisName;
            }
        }
        return jcrName;
    }
    
    public Nodes() {
        for (int i = 0; i < map.length; i++) {
           String[] tokens = map[i].split("=");
           list.add(new Relation(tokens[0].trim(), tokens[1].trim()));
        }
    }
    
    private class Relation {
        private String jcrName;
        private String cmisName;

        private Relation(String cmisName, String jcrName) {
            this.cmisName = cmisName;
            this.jcrName = jcrName;
        }
    }
    
}
