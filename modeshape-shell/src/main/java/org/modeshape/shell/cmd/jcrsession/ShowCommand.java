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
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.nodetype.NodeType;
import org.modeshape.jcr.api.PropertyType;
import org.modeshape.shell.ShellSession;

/**
 *
 * @author kulikov
 */
public class ShowCommand extends ShellCommand {

    public ShowCommand() {
        super("show");
        addChild(new ShowChilds());
        addChild(new ShowProperties());
        addChild(new ShowProperty());
        addChild(new ShowType());
    }

    private void printPropertyDefinition(StringBuilder builder, String name, boolean v) {
        builder.append(name);
        builder.append("\t\t\t");
        builder.append(booleanValue(v));
        builder.append("\n");
    }

    private String booleanValue(boolean a) {
        return a ? "yes" : "no";
    }

    @Override
    public String help() {
        return "usage: list OPTIONS";
    }

    @Override
    public String exec(ShellSession session) throws Exception {
        return "";
    }

    private class ShowChilds extends ShellCommand {

        public ShowChilds() {
            super("nodes");
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            Node node = session.jcrSession().getNode(session.getPath());
            NodeIterator it = node.getNodes();

            StringBuilder builder = new StringBuilder();
            while (it.hasNext()) {
                Node child = it.nextNode();
                builder.append(child.getName().trim())
                        .append("\t\t\t")
                        .append(child.getPrimaryNodeType().getName().trim())
                        .append("\n");
            }
            return builder.toString();
        }

        @Override
        public String help() {
            return "usage: show nodes";
        }
    }

    private class ShowProperties extends ShellCommand {

        public ShowProperties() {
            super("properties");
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            StringBuilder builder = new StringBuilder();
            Node node = session.jcrSession().getNode(session.getPath());
            PropertyIterator it = node.getProperties();
            while (it.hasNext()) {
                Property p = it.nextProperty();
                builder.append(p.getName());
                builder.append("\t\t\t");
                builder.append(PropertyType.nameFromValue(p.getType()));
                builder.append("\n");
            }
            return builder.toString();
        }

        @Override
        public String help() {
            return "usage: show properties";
        }
    }
    
    private class ShowProperty extends ShellCommand {

        public ShowProperty() {
            super("property");
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            StringBuilder builder = new StringBuilder();
            Node node = session.jcrSession().getNode(session.getPath());
            Property p = node.getProperty(this.args(1));

            printPropertyDefinition(builder, "auto created", p.getDefinition().isAutoCreated());
            printPropertyDefinition(builder, "multiple", p.getDefinition().isMultiple());
            printPropertyDefinition(builder, "mandatory", p.getDefinition().isMandatory());
            printPropertyDefinition(builder, "protected", p.getDefinition().isProtected());
            printPropertyDefinition(builder, "allow full text search", p.getDefinition().isFullTextSearchable());
            printPropertyDefinition(builder, "allow query order", p.getDefinition().isQueryOrderable());

            return builder.toString();
        }

        @Override
        public String help() {
            return "usage: show properties";
        }
    }

    private class ShowType extends ShellCommand {

        public ShowType() {
            super("type");
            addChild(new ShowPrimaryType());
            addChild(new ShowMixinType());
        }

        @Override
        public String help() {
            return "usage: show type primary| mixin";
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            return "";
        }
    }

    private class ShowPrimaryType extends ShellCommand {

        public ShowPrimaryType() {
            super("primary");
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            Node node = session.jcrSession().getNode(session.getPath());
            return node.getPrimaryNodeType().getName();
        }

        @Override
        public String help() {
            return "usage: show type primary| mixin";
        }
    }
    
    private class ShowMixinType extends ShellCommand {

        public ShowMixinType() {
            super("mixin");
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            final StringBuilder builder = new StringBuilder();
            Node node = session.jcrSession().getNode(session.getPath());
            NodeType[] mixin = node.getMixinNodeTypes();
            if (mixin.length > 0) {
                for (NodeType t : mixin) {
                    builder.append(t.getName());
                    builder.append("\n");
                }
            } else {
                builder.append("[not assigned]");
            }
            return builder.toString();
        }

        @Override
        public String help() {
            return "usage: show type primary| mixin";
        }
    }
    
}
