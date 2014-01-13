/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.connector.cmis;

import java.util.ArrayList;
import javax.jcr.nodetype.NodeType;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;

/**
 * Maps relations between JCR node types and CMIS object types.
 * 
 * @author kulikov
 */
@SuppressWarnings( "synthetic-access" )
public class Nodes {
    private final static String[] map = new String[] {BaseTypeId.CMIS_FOLDER.value() + " = " + NodeType.NT_FOLDER,
        BaseTypeId.CMIS_DOCUMENT.value() + " = " + NodeType.NT_FILE};

    private ArrayList<Relation> list = new ArrayList<Relation>();

    /**
     * Gets the name of the given property in JCR domain.
     * 
     * @param cmisName the name of the given property in CMIS domain.
     * @return the name of the given property in JCR domain.
     */
    public String findJcrName( String cmisName ) {
        for (Relation aList : list) {
            if (aList.cmisName.equals(cmisName)) {
                return aList.jcrName;
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
    public String findCmisName( String jcrName ) {
        for (Relation aList : list) {
            if (aList.jcrName.equals(jcrName)) {
                return aList.cmisName;
            }
        }
        return jcrName;
    }

    public Nodes() {
        for (String aMap : map) {
            String[] tokens = aMap.split("=");
            list.add(new Relation(tokens[0].trim(), tokens[1].trim()));
        }
    }

    private class Relation {
        private String jcrName;
        private String cmisName;

        private Relation( String cmisName,
                          String jcrName ) {
            this.cmisName = cmisName;
            this.jcrName = jcrName;
        }
    }

}
