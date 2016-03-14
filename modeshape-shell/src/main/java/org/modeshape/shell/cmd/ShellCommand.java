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
package org.modeshape.shell.cmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.modeshape.common.i18n.I18n;
import org.modeshape.shell.ShellSession;
import org.modeshape.shell.ShellI18n;

/**
 * Basic class for shell command.
 * 
 * @author kulikov
 */
public abstract class ShellCommand {
    public final static String SILENCE = "";
    public final static String EOL = System.getProperty("line.separator");
    public final static String TAB3 = "\t\t\t";
    
    //name of the command
    private final String name;
    private String fqn;
    
    //set of child commands; 
    //child command starts with name of this command
    private final HashMap<String, ShellCommand> childCommands = new HashMap<>();
    
    //options and arguments of this command
    //arguments are separated by whitespace and options are highlighed by -key
    private final HashMap<String, String> options = new HashMap<>();    
    private final ArrayList<String> arguments = new ArrayList<>();

    //message displayed on help request
    private final I18n helpMessage;
    
    /**
     * Creates new instance of this command.
     * 
     * @param name 
     */
    public ShellCommand( String name ) {
        this.name = this.convertCamelCase(name);
        this.fqn = this.convertCamelCase(name);
        this.helpMessage = ShellI18n.unknownCommand;
    }

    /**
     * Creates new command instance.
     * 
     * @param name the name of the command
     * @param helpMessage help message.
     */
    public ShellCommand( String name, I18n helpMessage ) {
        this.name = this.convertCamelCase(name);
        this.fqn = this.convertCamelCase(name);
        this.helpMessage = helpMessage;
    }
    
    /**
     * Gets the name of this command.
     * 
     * @return command name irrespective of parent
     */
    public String name() {
        return name;
    }
   
    /**
     * The whole command name including name of parent commands.
     * 
     * @return 
     */
    protected String fqn() {
        return fqn;
    }
    
    /**
     * List of arguments detected by the command.
     * 
     * @return 
     */
    protected List<String> arguments() {
        return this.arguments;
    }
    
    /**
     * List of options detected by the command.
     * 
     * @return 
     */
    protected Map<String, String> options() {
        return this.options;
    }
    
    /**
     * Adds given command to the list of child commands.
     * 
     * @param command 
     */
    protected void addChild(ShellCommand command) {
        childCommands.put(command.name, command);
    }
    
    /**
     * Builds subcommands.
     * 
     * @return map between command name command instance.
     */
    public Map<String, ShellCommand> buildCommands() {
        final HashMap<String, ShellCommand> map = new HashMap<>();
        map.put(this.name, this);
        for (ShellCommand c : childCommands.values()) {
            Map<String, ShellCommand> children = c.buildCommands();
            for (String cname : children.keySet()) {
                ShellCommand cmd = children.get(cname);
                cmd.fqn = this.name + " " + cname;
                map.put(cmd.fqn, cmd);
            }
        }        
        return Collections.unmodifiableMap(map);
    }
    
    /**
     * Parse command line.
     * 
     * @param cmd 
     */
    public void prepare(String cmd) {
        parseOptions(cmd);
        parseArgs(cmd);
    }
    
    public void prepare(List<String> args, Map<String, String> options) {
        this.arguments.clear();
        this.options.clear();
        
        this.arguments.addAll(args);
        this.options.putAll(options);
    }

    private void parseOptions(String cmd) {
        options.clear();
        
        //determine all option entries
        String[] tokens = cmd.trim().split(" ");
        for (String token : tokens) {
            //is this an option key
            if (token.startsWith("-")) {
                options.put(token, CommandParser.getOptionValue(token, cmd));
            }
        }
    }
    
    private void parseArgs(String cmd) {
        int i = cmd.indexOf(fqn) + fqn.length() + 1;
        String args = i < cmd.length() ? cmd.substring(i) : "";
        for (String key : options.keySet()) {
            args = args.replace(key, "");
            args = args.replace(options.get(key), "");
        }
        
        args = args.trim();
        
        int pos = 0;
        arguments.clear();
        
        while (pos < args.length()) {
            int pos1 = pos + 1;
            
            if (args.charAt(pos) == '\'') {
                pos1 = args.indexOf('\'', pos1);
                pos++;
            } else if (args.charAt(pos) == '"') {
                pos++;
                pos1 = args.indexOf('"', pos1);
            } else {
                pos1 = args.indexOf(' ', pos1);
            }
            
            if (pos1 == -1) pos1 = args.length();
            arguments.add(args.substring(pos, pos1).trim());
            pos = pos1;
        }
    }
    
    /**
     * Gets value of the given option.
     * 
     * @param option option's key
     * @return option's value as text.
     */
    public String optionValue(String option) {
        return options.get(option);
    }
    
    /**
     * Gets argument with given index.
     * 
     * @param i index value
     * @return argument value as text or null if index out of range.
     */
    public String args(int i) {
        return i < arguments.size() ? arguments.get(i) : null;
    }
    
    /**
     * Executes command.
     * 
     * @param session
     * @return
     * @throws Exception 
     */
    public String perform(ShellSession session) throws Exception {
        if (options.containsKey("-help") || options.containsKey("--help")) {
            return help();
        }
        
        //execute action if this command has no childs
        if (childCommands.isEmpty()) {
            return exec(session);
        }
        
        //otherwise return list of child commands
        //as completion of this command
        return complete();
    }
    
    
    private String complete() {
        StringBuilder builder = new StringBuilder();
        
        ArrayList<String> cnames = new ArrayList<>();
        cnames.addAll(childCommands.keySet());
        Collections.sort(cnames);
        
        for (String cname : cnames) {
            builder.append(this.name).append(" ").append(cname).append(" ");
        }
        
        return builder.toString();
    }
    
    /**
     * This command action implementation.
     * 
     * @param shell
     * @return
     * @throws Exception 
     */
    public String exec(ShellSession session) throws Exception {
        return SILENCE;
    }
    
    /**
     * Help message.
     * 
     * @return 
     */
    public String help() {
        if (!this.childCommands.isEmpty()) {
            String opts = "";
            for (ShellCommand cmd : childCommands.values()) {
                opts = opts.length() > 0 ? opts + "|" + cmd.name : opts + cmd.name;
            }
            return "Usage: " + this.fqn + " " + opts;
        }
        return helpMessage.text();
    }

    public String help(Object... args) {
        return helpMessage.text(args);
    }
    
    protected boolean allArgsSpecified(Object... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) return false;
        }
        return true;
    }
    
    private String convertCamelCase(String name) {
        String s = name.toLowerCase();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) == s.charAt(i)) {
                builder.append(name.charAt(i));
            } else {
                builder.append('-');
                builder.append(s.charAt(i));
            }
        }
        return builder.toString();
    }
}
