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
import java.net.URL;
import javax.jcr.PropertyType;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import org.modeshape.jcr.JcrNodeTypeManager;
import org.modeshape.shell.ShellSession;
import org.modeshape.shell.ShellI18n;

/**
 *
 * @author kulikov
 */
public class TypeCommand extends ShellCommand {

    public TypeCommand() {
        super("ntm");
        addChild(new ShowCommand());
        addChild(new RegisterCommand());
        addChild(new UnregisterCommand());
    }

    private class RegisterCommand extends ShellCommand {

        public RegisterCommand() {
            super("register", ShellI18n.typeRegisterHelp);
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            String url = optionValue("--url");
            if (url == null || url.length() == 0) {
                return help();
            }

            boolean allowUpdates = optionValue("--allow-updates") != null;
            JcrNodeTypeManager ntm = (JcrNodeTypeManager) session.jcrSession().getWorkspace().getNodeTypeManager();
            ntm.registerNodeTypes(new URL(url), allowUpdates);
            
            return SILENCE;
        }

    }

    private class UnregisterCommand extends ShellCommand {

        public UnregisterCommand() {
            super("unregister", ShellI18n.typeUnregisterHelp);
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            String name = optionValue("--type-name");
            if (name == null || name.length() == 0) {
                return help();
            }

            NodeTypeManager ntm = session.jcrSession().getWorkspace().getNodeTypeManager();
            ntm.unregisterNodeType(name);
            return SILENCE;
        }

    }

    private class ShowCommand extends ShellCommand {

        public ShowCommand() {
            super("show");
            addChild(new ShowTypesCommand());
            addChild(new ShowPropertyDefsCommand());
            addChild(new ShowChildNodeDefsCommand());
        }

    }

    private class ShowTypesCommand extends ShellCommand {

        public ShowTypesCommand() {
            super("types", ShellI18n.typeShowTypesHelp);
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            final String option = args(0);

            if (option == null) {
                return help();
            }

            final NodeTypeManager ntm = session.jcrSession().getWorkspace().getNodeTypeManager();
            final NodeTypeIterator it;
            switch (option.toLowerCase()) {
                case "all":
                    it = ntm.getAllNodeTypes();
                    break;
                case "primary":
                    it = ntm.getPrimaryNodeTypes();
                    break;
                case "mixin":
                    it = ntm.getMixinNodeTypes();
                    break;
                default:
                    return help();
            }

            StringBuilder builder = new StringBuilder();
            while (it.hasNext()) {
                NodeType type = it.nextNodeType();
                builder.append(type.getName());
                builder.append(EOL);
            }
            return builder.toString();
        }

    }

    private class ShowPropertyDefsCommand extends ShellCommand {

        public ShowPropertyDefsCommand() {
            super("property-definitions", ShellI18n.typeShowPropertyDefsHelp);
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            String name = optionValue("--type-name");
            if (name == null || name.length() == 0) {
                return help();
            }

            NodeTypeManager ntm = session.jcrSession().getWorkspace().getNodeTypeManager();
            NodeType t = ntm.getNodeType(name);

            PropertyDefinition[] defs = t.getPropertyDefinitions();
            StringBuilder builder = new StringBuilder();
            for (PropertyDefinition d : defs) {
                builder.append(d.getName());
                builder.append("\t");
                builder.append(PropertyType.nameFromValue(d.getRequiredType()));

                if (d.isAutoCreated()) {
                    builder.append("\tauto-created");
                }
                if (d.isMultiple()) {
                    builder.append("\tmultiple");
                }
                if (d.isMandatory()) {
                    builder.append("\tmandatory");
                }
                if (d.isProtected()) {
                    builder.append("\tprotected");
                }
                if (d.isFullTextSearchable()) {
                    builder.append("\tfull-text-search");
                }

                builder.append(EOL);
            }
            return builder.toString();
        }

    }

    private class ShowChildNodeDefsCommand extends ShellCommand {

        public ShowChildNodeDefsCommand() {
            super("node-definitions", ShellI18n.typeShowNodeDefsHelp);
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            String name = optionValue("--type-name");
            if (name == null || name.length() == 0) {
                return help();
            }

            NodeTypeManager ntm = session.jcrSession().getWorkspace().getNodeTypeManager();
            NodeType t = ntm.getNodeType(name);

            NodeDefinition[] defs = t.getChildNodeDefinitions();
            StringBuilder builder = new StringBuilder();
            for (NodeDefinition d : defs) {
                builder.append(d.getName());
                builder.append("\t");
                builder.append(d.getDefaultPrimaryType());

                if (d.isAutoCreated()) {
                    builder.append("\tauto-created");
                }
                if (d.isMandatory()) {
                    builder.append("\tmandatory");
                }
                if (d.isProtected()) {
                    builder.append("\tprotected");
                }

                builder.append(EOL);
            }
            return builder.toString();
        }

    }
}
