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
package org.modeshape.shell.cmd.config;

import java.io.InputStream;
import java.util.ArrayList;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Json;
import org.modeshape.schematic.document.ParsingException;
import org.modeshape.shell.cmd.ShellCommand;

/**
 * This class is used for dynamic building configuration commands using schema.
 *
 * @author kulikov
 */
public abstract class SubCommand extends ShellCommand {

    private final static String SCHEMA_PATH = "org/modeshape/jcr/repository-config-schema.json";
    private final SubCommand parent;
    private final String help;
    private final String paramName;
    protected final String type;
    protected final String defaultValue;
    
    public SubCommand(SubCommand parent, String name, String type, String defaultValue, String help) {
        super(name);
        this.paramName = name;
        this.parent = parent;
        this.type = type;
        this.help = help;
        this.defaultValue = defaultValue;
    }

    protected Document schema() {
        Document doc = null;
        try {
            InputStream schemaFileStream = getClass().getClassLoader().getResourceAsStream(SCHEMA_PATH);
            doc = Json.read(schemaFileStream);
            doc = doc.getDocument("properties");
            return doc;
        } catch (ParsingException e) {
        }        
        return doc;
    }
    
    @Override
    public String help() {
        return help != null && help.length() > 0 ? help : super.help();
    }

    protected abstract SubCommand create(SubCommand parent, String name, 
            String type, String defaultValue, String help);
    
    protected void loadSubcommands(SubCommand parent, Document doc) {
        for (Document.Field f : doc.fields()) {
            Document schema = f.getValueAsDocument();

            String t = schema.getString("type");
            String description = schema.getString("description");
            String defValue = defaultValue(schema);

            SubCommand ccmd = create(parent, f.getName(), type, 
                    defValue, description);

            if (t != null && t.equals("object")) {
                Document d = schema.getDocument("properties");
                if (d != null) {
                    loadSubcommands(ccmd, d);
                }
            }
            
            parent.addChild(ccmd);
        }
    }
    
    /**
     * Determines default value of the parameter described by the given schema.
     * 
     * @param schema
     * @return 
     */
    private String defaultValue(Document schema) {
        Object obj = schema.get("default");
        return obj != null ? obj.toString() : "not specified";
    }
    
    protected ArrayList<String> path() {
        ArrayList<String> list = new ArrayList<>();
        list.add(this.paramName);
        SubCommand p = parent;
        while (p != null && p.parent != null) {
            list.add(0, p.paramName);
            p = p.parent;
        }
        return list;
    }
    
}
