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
package org.modeshape.shell.cmd.jcrsession;

import org.modeshape.shell.cmd.ShellCommand;
import java.security.Principal;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import org.modeshape.jcr.security.SimplePrincipal;
import org.modeshape.shell.ShellSession;
import org.modeshape.shell.ShellI18n;

/**
 *
 * @author kulikov
 */
public class AclCommand extends ShellCommand {

    public AclCommand() {
        super("acl");
        addChild(new ShowCommand());
        addChild(new SetCommand());
        addChild(new RemoveCommand());
    }
    
    private class ShowCommand extends ShellCommand {

        public ShowCommand() {
            super("show", ShellI18n.aclShowHelp);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            String path = session.getPath();
            AccessControlManager acm = session.jcrSession().getAccessControlManager();
            AccessControlPolicy[] policies = acm.getPolicies(path);
            if (policies == null || policies.length == 0) {
                return "<Empty set>";
            }
            
            StringBuilder builder = new StringBuilder();
            for (AccessControlPolicy p : policies) {
                AccessControlList acl = (AccessControlList)p;
                AccessControlEntry[] entries = acl.getAccessControlEntries();
                for (AccessControlEntry en : entries) {
                    builder.append(en.getPrincipal());
                    builder.append("\t{");
                    
                    Privilege[] perms = en.getPrivileges();
                    for (int i = 0; i < perms.length; i++) {
                        if (i > 0) builder.append(", ");
                        builder.append(perms[i]);
                    }
                }           
            }
            return builder.toString();
        }

    }
    
    private class SetCommand extends ShellCommand {
        public SetCommand() {
            super("set", ShellI18n.aclSetHelp);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            String principal = optionValue("--principal");
            String perms = optionValue("--permissions");
            
            if (principal == null || perms == null) {
                return help();
            }
            
            String path = session.getPath();
            
            
            AccessControlManager acm = session.jcrSession().getAccessControlManager();
            AccessControlPolicy[] policies = acm.getPolicies(path);
            
            AccessControlList acl;
            if (policies != null && policies.length > 0) {
                acl = (AccessControlList) policies[0];
            } else {
                AccessControlPolicyIterator it = acm.getApplicablePolicies(path);
                acl = (AccessControlList) it.nextAccessControlPolicy();
            }
            
            acl.addAccessControlEntry(principal(principal), privileges(acm, perms));
            acm.setPolicy(path, acl);
            
            return SILENCE;
        }

    }

    private class RemoveCommand extends ShellCommand {

        public RemoveCommand() {
            super("remove", ShellI18n.aclRemoveHelp);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            String path = session.getPath();
            AccessControlManager acm = session.jcrSession().getAccessControlManager();
            AccessControlPolicy[] policies = acm.getPolicies(path);

            
            if (policies == null || policies.length == 0) {
                return SILENCE;
            }
            
            String principal = this.optionValue("--principal");
            if (principal == null) {
                acm.removePolicy(path, policies[0]);
                return SILENCE;
            }
            
            AccessControlList acl = (AccessControlList) policies[0];
            AccessControlEntry entry = null;
            
            for (AccessControlEntry en : acl.getAccessControlEntries()) {
                if (en.getPrincipal().getName().equals(principal)) {
                    entry = en;
                    break;
                }
            }
            
            if (entry != null) {
                acl.removeAccessControlEntry(entry);
            }

            acm.setPolicy(path, acl);
            return SILENCE;
        }
    }
    
    private Principal principal(String name) {
        return SimplePrincipal.newInstance(name);
    }
    
    private Privilege[] privileges(AccessControlManager acm, String list) throws AccessControlException, RepositoryException {
        String tokens[] = list.split(",");
        Privilege privileges[] = new Privilege[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            privileges[i] = acm.privilegeFromName(tokens[i]);
        }
        return privileges;
    }
}
