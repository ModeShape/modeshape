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
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import org.modeshape.shell.ShellSession;
import org.modeshape.shell.ShellI18n;

/**
 *
 * @author kulikov
 */
public class VersionCommand extends ShellCommand {

    public VersionCommand() {
        super("version");
        addChild(new CheckinCommand());
        addChild(new CheckoutCommand());
        addChild(new MergeCommand());
        addChild(new RestoreCommand());
        addChild(new ShowCommand());
    }
    
    private class CheckinCommand extends ShellCommand {

        public CheckinCommand() {
            super("checkin", ShellI18n.versionCheckinHelp);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            String path = session.getPath();
            VersionManager vm = session.jcrSession().getWorkspace().getVersionManager();
            Version v = vm.checkin(path);
            return v.getName();
        }
    }
    
    private class CheckoutCommand extends ShellCommand {

        public CheckoutCommand() {
            super("checkout", ShellI18n.versionCheckoutHelp);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            String path = session.getPath();
            session.jcrSession().getWorkspace().getVersionManager().checkout(path);
            return SILENCE;
        }

    }

    private class MergeCommand extends ShellCommand {

        public MergeCommand() {
            super("merge", ShellI18n.versionMergeHelp);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            String path = session.getPath();
            String workspace = session.jcrSession().getWorkspace().getName();
            
            boolean bestEffort = optionValue("--best-effort") != null && 
                    Boolean.valueOf(optionValue("--best-effort"));
            boolean shallow = optionValue("--shallow") != null && 
                    Boolean.valueOf(optionValue("--shallow"));
            
            VersionManager vm = session.jcrSession().getWorkspace().getVersionManager();
            NodeIterator it;
            if (optionValue("--shallow") != null) {
                it = vm.merge(path, workspace, bestEffort, shallow);
            } else {
                it = vm.merge(path, workspace, bestEffort);
            }
            
            StringBuilder builder = new StringBuilder();
            while (it.hasNext()) {
                Node n = it.nextNode();
                builder.append("Failure to merge ")
                       .append(n.getPath())
                       .append(EOL); 
            }
            return builder.toString();
        }

    }

    
    private class RestoreCommand extends ShellCommand {

        public RestoreCommand() {
            super("restore", ShellI18n.versionRestoreHelp);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            String path = session.getPath();
            
            boolean removeExisting = optionValue("--remove-existing") != null && 
                    Boolean.valueOf(optionValue("---remove-existing"));
            String versionName = args(0);
            
            VersionManager vm = session.jcrSession().getWorkspace().getVersionManager();
            vm.restore(path, versionName, removeExisting);
            return SILENCE;
        }

    }

    private class ShowCommand extends ShellCommand {

        public ShowCommand() {
            super("show");
            addChild(new ShowBaseVersionCommand());
            addChild(new ShowVersionHistoryCommand());
            addChild(new ShowIsCheckedoutCommand());
        }
        
    }

    private class ShowBaseVersionCommand extends ShellCommand {

        public ShowBaseVersionCommand() {
            super("base-version", ShellI18n.versionShowBaseVersion);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            String path = session.getPath();
            Node node = session.jcrSession().getNode(path);
            
            if (!node.isNodeType("mix:versionable")) {
                return "Node is not versionable";
            }
            
            VersionManager vm = session.jcrSession().getWorkspace().getVersionManager();
            return vm.getBaseVersion(path).getName();
        }

    }

    private class ShowVersionHistoryCommand extends ShellCommand {

        public ShowVersionHistoryCommand() {
            super("history", ShellI18n.versionShowHistoryHelp);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            String path = session.getPath();
            Node node = session.jcrSession().getNode(path);
            
            if (!node.isNodeType("mix:versionable")) {
                return "Node is not versionable";
            }
            
            VersionManager vm = session.jcrSession().getWorkspace().getVersionManager();
            VersionHistory h = vm.getVersionHistory(path);
            
            StringBuilder builder = new StringBuilder();
            VersionIterator it = h.getAllVersions();
            
            if (it.getSize() == 0) {
                return "<Up-to date>";
            }
            
            while (it.hasNext()) {
                Version v = it.nextVersion();
                builder.append(v.getName());
                builder.append(TAB3);
                builder.append(v.getCreated().getTime());
                builder.append(EOL);
            }
            
            return builder.toString();
        }

    }

    private class ShowIsCheckedoutCommand extends ShellCommand {

        public ShowIsCheckedoutCommand() {
            super("status", ShellI18n.versionShowStatusHelp);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            String path = session.getPath();
            Node node = session.jcrSession().getNode(path);
            
            if (!node.isNodeType("mix:versionable")) {
                return "Node is not versionable";
            }
            
            VersionManager vm = session.jcrSession().getWorkspace().getVersionManager();
            return vm.isCheckedOut(path) ? "checked out" : "not checked out";
        }

    }
    
}
