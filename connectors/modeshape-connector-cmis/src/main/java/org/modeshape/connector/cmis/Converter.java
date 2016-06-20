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
import java.util.Collections;
import java.util.List;
import javax.jcr.security.Privilege;
import org.modeshape.jcr.security.SimplePrincipal;

/**
 * Utility class for conversion CMIS/JCR permissions.
 * 
 * @author kulikov
 */
public class Converter {
    private final static String CMIS_ANYONE = "anyone";
    
    public static String jcrPrincipal(String cmisPrincipal) {
        return cmisPrincipal.equalsIgnoreCase(CMIS_ANYONE) ? 
                SimplePrincipal.EVERYONE.getName() : cmisPrincipal;
    }
    
    /*
     * Translates CMIS permission to a set of JCR privileges.
     * 
     * @param cmisPermission
     * @return 
     */
    public static String[] jcrPermissions( String cmisPermission ) {
        switch (cmisPermission) {
            case "cmis:read" :
                return new String[] {
                    Privilege.JCR_READ,
                    Privilege.JCR_READ_ACCESS_CONTROL
                };
            case "cmis:write" : 
                return new String[] {
                    Privilege.JCR_WRITE,
                    Privilege.JCR_READ_ACCESS_CONTROL,
                    Privilege.JCR_MODIFY_ACCESS_CONTROL,
                    Privilege.JCR_NODE_TYPE_MANAGEMENT,
                    Privilege.JCR_RETENTION_MANAGEMENT,
                    Privilege.JCR_VERSION_MANAGEMENT
                };
            case "cmis:all" :
                return new String[] {Privilege.JCR_ALL};
            default :
                    return null;
        }
    }
    
    public static String[] jcrPermissions( List<String> cmisPermissions ) {
        ArrayList<String> list = new ArrayList<>();   
        for (String permission : cmisPermissions) {
            Collections.addAll(list, jcrPermissions(permission));
        }
        String[] res = new String[list.size()];
        list.toArray(res);
        return res;
    }
    
}
