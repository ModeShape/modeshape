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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.jcr.Credentials;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.SimpleCredentials;
import org.modeshape.shell.ShellSession;
import org.modeshape.shell.ShellI18n;

/**
 *
 * @author kulikov
 */
public class SessionCommand extends ShellCommand {
    public SessionCommand() {
        super("session");
        addChild(new RefreshCmd());
        addChild(new ExitCmd());
        addChild(new SaveCmd());
        addChild(new StatusCmd());
        addChild(new ImpersonateCmd());
        addChild(new MoveCmd());
        addChild(new RemoveCmd());
        addChild(new ImportCmd());
        addChild(new ExportCmd());
        addChild(new ShowCmd());
    }
    
    private class RefreshCmd extends ShellCommand {

        public RefreshCmd() {
            super("refresh", ShellI18n.sessionRefreshHelp);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            boolean keepChanges = false;
            
            String s = this.optionValue("--keep-changes");
            if (s != null) keepChanges = Boolean.valueOf(s);
            
            session.jcrSession().refresh(keepChanges);
            return SILENCE;
        }
                
    }
    
    private class ExitCmd extends ShellCommand {

        public ExitCmd() {
            super("exit", ShellI18n.sessionExitHelp);
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            session.jcrSession().logout();
            session.setJcrSession(null);
            return SILENCE;
        }
        
    }

    private class SaveCmd extends ShellCommand {

        public SaveCmd() {
            super("save", ShellI18n.sessionExitHelp);
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            session.jcrSession().save();
            return SILENCE;
        }
    }

    private class StatusCmd extends ShellCommand {

        public StatusCmd() {
            super("status", ShellI18n.sessionStatusHelp);
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            return (session.jcrSession() != null && session.jcrSession().isLive()) ?
                    "alive" : "not alive";
        }
        
    }

    private class ImpersonateCmd extends ShellCommand {

        public ImpersonateCmd() {
            super("impersonate", ShellI18n.sessionImpersonateHelp);
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            String uname = this.optionValue("--user");
            String passwd = this.optionValue("--password");
            
            if (uname == null) {
                session.jcrSession().impersonate(null);
            } else if (passwd == null) {
                Credentials creds = new SimpleCredentials(uname, null);
                session.jcrSession().impersonate(creds);
            } else {
                Credentials creds = new SimpleCredentials(uname, passwd.toCharArray());
                session.jcrSession().impersonate(creds);
            }
            return "";
        }
        
    }

    
    private class MoveCmd extends ShellCommand {

        public MoveCmd() {
            super("move", ShellI18n.sessionMoveHelp);
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            String src = this.args(0);
            String dst = this.args(1);
            
            if (src == null) {
                throw new IllegalArgumentException("source path should be specified");
            }

            if (dst == null) {
                throw new IllegalArgumentException("destination path should be specified");
            }
            
            session.jcrSession().move(src, dst);
            return SILENCE;
        }
        
    }

    private class RemoveCmd extends ShellCommand {

        public RemoveCmd() {
            super("remove", ShellI18n.sessionRemoveHelp);
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            String src = this.args(0);
            
            if (src == null) {
                throw new IllegalArgumentException("path should be specified");
            }
            
            session.jcrSession().removeItem(src);
            return SILENCE;
        }
        
    }

    private class ImportCmd extends ShellCommand {

        public ImportCmd() {
            super("import", ShellI18n.sessionImportHelp);
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            String sourceFile = this.args(0);            
            FileInputStream fin = new FileInputStream(sourceFile);
            
            String parent = this.optionValue("--path");
            String b = this.optionValue("--behaviour");
            
            int i = ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;
            switch (b) {
                case "create-new":
                    i = ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;
                    break;
                case "remove-existing":
                    i = ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING;
                    break;
                case "replace-existing":
                    i = ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING;
                    break;
                case "collision-throw":
                    i = ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW;
                    break;
            }
            
            session.jcrSession().importXML(parent, fin, i);
            return "";
        }
        
    }

    private class ExportCmd extends ShellCommand {

        public ExportCmd() {
            super("export");
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            String path = this.args(0);
            String dst = this.optionValue("--destination-file");

            
            FileOutputStream fout = new FileOutputStream(dst);
            
            String v = this.optionValue("--view");
            boolean skipBinary = this.optionValue("--skipBinary") != null;
            boolean noRecurse = this.optionValue("--noRecurse") != null;
            
            switch (v) {
                case "system":
                    session.jcrSession().exportSystemView(path, fout, skipBinary, noRecurse);
                    break;
                case "document":
                    session.jcrSession().exportDocumentView(path, fout, skipBinary, noRecurse);
                    break;
            }
            return SILENCE;
        }
        
    }

    private class ShowCmd extends ShellCommand {

        public ShowCmd() {
            super("show");
            addChild(new ShowAttributeNamesCmd());
            addChild(new ShowPrefixesCmd());
            addChild(new ShowAttributeCmd());
            addChild(new ShowPrefixCmd());
            addChild(new ShowUriCmd());
        }

    }

    private class ShowAttributeNamesCmd extends ShellCommand {

        public ShowAttributeNamesCmd() {
            super("attributes", ShellI18n.sessionShowAttributesHelp);
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            StringBuilder builder = new StringBuilder();
            String[] names = session.jcrSession().getAttributeNames();
            for (String name : names) {
                builder.append(name).append(EOL);
            }
            return builder.toString();
        }
        
    }

    private class ShowPrefixesCmd extends ShellCommand {

        public ShowPrefixesCmd() {
            super("prefixes", ShellI18n.sessionShowPrefixesHelp);
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            StringBuilder builder = new StringBuilder();
            String[] names = session.jcrSession().getNamespacePrefixes();
            for (String name : names) {
                builder.append(name).append(EOL);
            }
            return builder.toString();
        }
        
    }
    
    private class ShowAttributeCmd extends ShellCommand {

        public ShowAttributeCmd() {
            super("attribute", ShellI18n.sessionShowAttributeHelp);
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            String aname = this.args(0);
            assert aname != null : "Attribute name expected";
            
            Object value = session.jcrSession().getAttribute(aname);
            return value == null ? "null" : value.toString();
        }
        
    }

    private class ShowPrefixCmd extends ShellCommand {

        public ShowPrefixCmd() {
            super("prefix", ShellI18n.sessionShowPrefixHelp);
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            String aname = this.args(0);
            assert aname != null : "uri expected";
            
            Object value = session.jcrSession().getNamespacePrefix(aname);
            return value == null ? "null" : value.toString();
        }
        
    }

    private class ShowUriCmd extends ShellCommand {

        public ShowUriCmd() {
            super("uri", ShellI18n.sessionShowUriHelp);
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            String aname = this.args(0);
            assert aname != null : "Prefix name expected";
            
            Object value = session.jcrSession().getNamespaceURI(aname);
            return value == null ? "null" : value.toString();
        }
        
    }
    
}
