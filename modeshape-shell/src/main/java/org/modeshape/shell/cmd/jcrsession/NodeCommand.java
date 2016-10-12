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
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;
import org.h2.util.IOUtils;
import org.modeshape.shell.ShellSession;
import org.modeshape.shell.ShellI18n;

/**
 *
 * @author kulikov
 */
public class NodeCommand extends ShellCommand {

    public NodeCommand() {
        super("node");
        addChild(new AddCommand());
        addChild(new ShowCommand());
        addChild(new SetCommand());
        addChild(new UploadCommand());
        addChild(new DownloadCommand());
    }

    private class AddCommand extends ShellCommand {

        public AddCommand() {
            super("add");
            addChild(new AddMixinTypeCommand());
            addChild(new AddNodeCommand());
        }
    }

    private class AddMixinTypeCommand extends ShellCommand {

        public AddMixinTypeCommand() {
            super("mixin", ShellI18n.nodeAddMixinHelp);
        }

        @Override
        public String exec(ShellSession shell) throws Exception {
            String value = this.args(0);
            
            if (value == null) {
                return help();
            }
            
            String path = shell.getPath();
            Node node = shell.jcrSession().getNode(path);
            node.addMixin(value);
            
            return SILENCE;
        }

    }

    private class AddNodeCommand extends ShellCommand {

        public AddNodeCommand() {
            super("node", ShellI18n.nodeAddNodeHelp);
        }

        @Override
        public String exec(ShellSession shell) throws Exception {
            String relPath = this.args(0);
            String primaryType = this.optionValue("--primary-type");

            //relative path is mandatory
            if (relPath == null) {
                return help();
            }

            if (relPath.startsWith("/")) {
                throw new IllegalArgumentException("path must be relative");
            }
            
            String path = shell.getPath();
            Node node = shell.jcrSession().getNode(path);

            if (primaryType != null) {
                node.addNode(relPath, primaryType);
            } else {
                node.addNode(relPath);
            }
            
            return SILENCE;
        }

    }

    private class ShowCommand extends ShellCommand {

        public ShowCommand() {
            super("show");
            addChild(new ShowIndexCommand());
            addChild(new ShowIdentifierCommand());
            addChild(new ShowMixinTypesCommand());
            addChild(new ShowNodesCommand());
            addChild(new ShowPropertiesCommand());
            addChild(new ShowPrimaryTypeCommand());
            addChild(new ShowPropertyCommand());
            addChild(new ShowReferencesCommand());
        }
    }

    private class ShowIndexCommand extends ShellCommand {

        public ShowIndexCommand() {
            super("index", ShellI18n.nodeShowIndexHelp);
        }

        @Override
        public String exec(ShellSession shell) throws Exception {
            String path = shell.getPath();
            Node node = shell.jcrSession().getNode(path);
            return Integer.toString(node.getIndex());
        }

    }

    private class ShowIdentifierCommand extends ShellCommand {

        public ShowIdentifierCommand() {
            super("identifier", ShellI18n.nodeShowIdentifierHelp);
        }

        @Override
        public String exec(ShellSession shell) throws Exception {
            String path = shell.getPath();
            Node node = shell.jcrSession().getNode(path);
            return Integer.toString(node.getIndex());
        }

    }

    private class ShowPrimaryTypeCommand extends ShellCommand {

        public ShowPrimaryTypeCommand() {
            super("primary-type", ShellI18n.nodeShowPrimaryTypeHelp);
        }

        @Override
        public String exec(ShellSession shell) throws Exception {
            String path = shell.getPath();
            Node node = shell.jcrSession().getNode(path);
            return node.getPrimaryNodeType().getName();
        }

    }

    private class ShowMixinTypesCommand extends ShellCommand {

        public ShowMixinTypesCommand() {
            super("mixins", ShellI18n.nodeShowMixinsHelp);
        }

        @Override
        public String exec(ShellSession shell) throws Exception {
            String path = shell.getPath();
            Node node = shell.jcrSession().getNode(path);

            NodeType[] list = node.getMixinNodeTypes();
            if (list.length == 0) {
                return "<empty set>";
            }
            
            StringBuilder builder = new StringBuilder();
            for (NodeType t : list) {
                builder.append(t.getName()).append(EOL);
            }
            return builder.toString();
        }

    }

    private class ShowNodesCommand extends ShellCommand {

        public ShowNodesCommand() {
            super("nodes", ShellI18n.nodeShowNodesHelp);
        }

        @Override
        public String exec(ShellSession shell) throws Exception {
            String path = shell.getPath();
            Node node = shell.jcrSession().getNode(path);

            NodeIterator list = node.getNodes();
            if (!list.hasNext()) {
                return "<empty set>";
            }
            
            StringBuilder builder = new StringBuilder();
            while (list.hasNext()) {
                builder.append(list.nextNode().getName()).append(EOL);
            }
            return builder.toString();
        }

    }

    private class ShowPropertiesCommand extends ShellCommand {

        public ShowPropertiesCommand() {
            super("properties", ShellI18n.nodeShowPropertiesHelp);
        }

        @Override
        public String exec(ShellSession shell) throws Exception {
            String path = shell.getPath();
            Node node = shell.jcrSession().getNode(path);

            PropertyIterator list = node.getProperties();
            StringBuilder builder = new StringBuilder();
            while (list.hasNext()) {
                builder.append(list.nextProperty().getName()).append(EOL);
            }
            return builder.toString();
        }
    }

    private class ShowPropertyCommand extends ShellCommand {

        public ShowPropertyCommand() {
            super("property", ShellI18n.nodeShowPropertyHelp);
        }

        @Override
        public String exec(ShellSession shell) throws Exception {
            String pname = args(0);
            
            if (pname == null) {
                return help();
            }
            
            String path = shell.getPath();
            Node node = shell.jcrSession().getNode(path);

            Property p = node.getProperty(pname);
            return p != null ? p.getString() : "not assigned";
        }

    }

    private class ShowReferencesCommand extends ShellCommand {

        public ShowReferencesCommand() {
            super("references", ShellI18n.nodeShowReferencesHelp);
        }

        @Override
        public String exec(ShellSession shell) throws Exception {
            String path = shell.getPath();
            Node node = shell.jcrSession().getNode(path);

            StringBuilder builder = new StringBuilder();
            PropertyIterator list = node.getReferences();
            while (list.hasNext()) {
                builder.append(list.nextProperty().getName()).append(EOL);
            }
            return builder.toString();
        }

    }

    private class SetCommand extends ShellCommand {
        public SetCommand() {
            super("set");
            addChild(new SetPrimaryTypeCommand());
            addChild(new SetPropertyCommand());
        }
    }

    private class SetPrimaryTypeCommand extends ShellCommand {

        public SetPrimaryTypeCommand() {
            super("primary-type", ShellI18n.nodeSetPrimaryTypeHelp);
        }
        
        @Override
        public String exec(ShellSession shell) throws Exception {
            String typeName = args(0);
            
            if (typeName == null) {
                return help();
            }
            
            String path = shell.getPath();
            Node node = shell.jcrSession().getNode(path);
            node.setPrimaryType(typeName);
            
            return "";
        }
    }
    
    private class SetPropertyCommand extends ShellCommand {

        public SetPropertyCommand() {
            super("property-value", ShellI18n.nodeSetPropertyValueHelp);
        }

        @Override
        public String exec(ShellSession shell) throws Exception {
            String pname = optionValue("--name");
            String pvalue = optionValue("--value");
            String pvalues = optionValue("--values");

            if (pname == null) {
                return help();
            }
            
            if (pvalues == null && pvalue == null) {
                return help();
            }
            
            String path = shell.getPath();
            Node node = shell.jcrSession().getNode(path);
            
            ValueFactory factory = shell.jcrSession().getValueFactory();
            
            if (pvalue != null) {
                node.setProperty(pname, factory.createValue(pvalue));
            } else {
                String[] tokens = pvalues.split(",");
                Value[] values = new Value[tokens.length];
                for (int i = 0; i < values.length; i++) {
                    values[i] = factory.createValue(tokens[i]);
                }
                node.setProperty(path, values);
            }

            return SILENCE;
        }

    }

    private class UploadCommand extends ShellCommand {

        public UploadCommand() {
            super("upload", ShellI18n.nodeUploadHelp);
        }

        @Override
        public String exec(ShellSession shell) throws Exception {
            String path = shell.getPath();

            String pname = optionValue("--property-name");
            String fname = optionValue("--file");

            if (fname == null || pname == null) {
                return help();
            }
            
            try (FileInputStream fin = new FileInputStream(fname)) {
                ValueFactory valueFactory = shell.jcrSession().getValueFactory();

                Binary binaryValue = valueFactory.createBinary(fin);
                shell.jcrSession().getNode(path).setProperty(pname, binaryValue);
            }
            return SILENCE;
        }

    }

    private class DownloadCommand extends ShellCommand {

        public DownloadCommand() {
            super("download", ShellI18n.nodeDownloadHelp);
        }

        @Override
        public String exec(ShellSession shell) throws Exception {
            String path = shell.getPath();

            String pname = optionValue("--name");
            String fname = optionValue("--file");

            if (fname == null || pname == null) {
                return help();
            }

            FileOutputStream fout = new FileOutputStream(fname);
            try {
                Binary binaryValue = shell.jcrSession().getNode(path).getProperty(pname).getBinary();
                IOUtils.copy(binaryValue.getStream(), fout);
            } finally {
                fout.flush();
                fout.close();
            }
            return SILENCE;
        }

    }
}
