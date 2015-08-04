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
package org.modeshape.web.shared;

import java.io.Serializable;
import java.util.HashMap;

/**
 *
 * @author kulikov
 */
public class Acl implements Serializable {
    private static final long serialVersionUID = 1L;
    private HashMap<String, Policy> policies = new HashMap<String,Policy>();
    
    public String[] principals() {
        String[] principals = new String[policies.size()];
        policies.keySet().toArray(principals);
        return principals;
    }
    
    public void addPolicy(Policy policy) {
        policies.put(policy.getPrincipal(), policy);
    }
    
    public void removePolicy( String principal ) {
        policies.remove(principal);
    }
    
    public Policy getPolicy(String principal){
        return policies.get(principal);
    }
    
    public void modify(String principal, JcrPermission permission, boolean enabled) {
        Policy policy = policies.get(principal);
        policy.modify(permission, enabled);
    }
}
