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
import org.modeshape.shell.ShellSession;
import org.modeshape.shell.ShellI18n;

/**
 *
 * @author kulikov
 */
public class WorkspaceCommand extends ShellCommand {
    
    public WorkspaceCommand() {
        super("workspace");
        addChild(new CloneCmd());
        addChild(new CopyCmd());
        addChild(new CreateCmd());
        addChild(new DeleteCmd());
        addChild(new ImportCmd());
        addChild(new ShowCmd());
    }
    
    private class CloneCmd extends ShellCommand {

        public CloneCmd() {
            super("clone", ShellI18n.workspaceCloneHelp);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            String srcWorkspace = args(0);
            String srcPath = args(1);
            String dstPath = args(2);
            
            assert srcWorkspace != null : "Source workspace not specified";
            assert srcPath != null : "Source path not specified";
            assert dstPath != null : "Destination not specified";
            
            assert srcPath.startsWith("/") : srcPath + " is not a valid absolute path";
            assert dstPath.startsWith("/") : dstPath + " is not a valid absolute path";
            
            boolean removExisting = optionValue("-removeExisting") != null;
            
            session.jcrSession().getWorkspace().clone(dstPath, dstPath, dstPath, removExisting);
            return SILENCE;
        }

    }

    private class CopyCmd extends ShellCommand {

        public CopyCmd() {
            super("copy", ShellI18n.workspaceCopyHelp);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            String srcWorkspace = optionValue("-w");
            String srcPath = optionValue("-s");
            String dstPath = optionValue("-d");
            
            assert srcPath != null : "Source path not specified";
            assert dstPath != null : "Destination not specified";
            
            assert srcPath.startsWith("/") : srcPath + " is not a valid absolute path";
            assert dstPath.startsWith("/") : dstPath + " is not a valid absolute path";

            if (srcWorkspace == null) {
                session.jcrSession().getWorkspace().copy(srcPath, dstPath);
            } else {
                session.jcrSession().getWorkspace().copy(srcWorkspace, srcPath, dstPath);
            }
            return SILENCE;
        }

    }

    private class CreateCmd extends ShellCommand {

        public CreateCmd() {
            super("create", ShellI18n.workspaceCreateHelp);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            String aname = args(0);
            String srcWorkspace = optionValue("-clone");
            
            assert aname != null : "New name not specified";

            if (srcWorkspace == null) {
                session.jcrSession().getWorkspace().createWorkspace(aname);
            } else {
                session.jcrSession().getWorkspace().createWorkspace(aname, srcWorkspace);
            }
            return SILENCE;
        }

    }

    private class DeleteCmd extends ShellCommand {

        public DeleteCmd() {
            super("delete", ShellI18n.workspaceDeleteHelp);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            String aname = args(0);
            assert aname != null : "Workspace name not specified";
            session.jcrSession().getWorkspace().deleteWorkspace(aname);
            return SILENCE;
        }

    }

    private class ImportCmd extends ShellCommand {

        public ImportCmd() {
            super("import", ShellI18n.workspaceImport);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            return "";//session.interpreter().onCommand(session, "session import", arguments(), options());
        }

    }

    private class ShowCmd extends ShellCommand {

        public ShowCmd() {
            super("show");
            addChild(new ShowNameCmd());
            addChild(new ShowNamesCmd());
        }
        
    }

    private class ShowNameCmd extends ShellCommand {

        public ShowNameCmd() {
            super("name", ShellI18n.workspaceShowName);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            return session.jcrSession().getWorkspace().getName();
        }

    }

    private class ShowNamesCmd extends ShellCommand {

        public ShowNamesCmd() {
            super("names", ShellI18n.workspaceShowNames);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            StringBuilder builder = new StringBuilder();
            String[] list = session.jcrSession().getWorkspace().getAccessibleWorkspaceNames();
            for (String n : list) {
                builder.append(n).append("\n");
            }
            return builder.toString();
        }

    }
    
}

