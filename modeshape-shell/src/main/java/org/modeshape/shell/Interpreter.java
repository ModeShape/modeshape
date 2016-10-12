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
package org.modeshape.shell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.modeshape.shell.cmd.ShellCommand;

/**
 * Interpreter of the shell commands.
 * 
 * Interpreter provides basic implementation of the process of command 
 * execution, prompt displayed in console and calculates command completions.
 * 
 * 
 * @author kulikov
 */
public class Interpreter {
    //prompt 
    private final String prompt;
    //map between command name and command instance.
    private final HashMap<String, ShellCommand> commandsMap = new HashMap<>();

    /**
     * Creates new instance of the interpreter.
     * 
     * @param prompt console prompt
     * @param commands list of commands this interpreter will execute.
     */
    public Interpreter(String prompt, ArrayList<ShellCommand> commands) {
        this.prompt = prompt;
        for (ShellCommand c : commands) {
            commandsMap.putAll(c.buildCommands());
        }
    }

    /**
     * Creates new instance of the interpreter.
     * 
     * @param prompt console prompt
     * @param commands list of commands this interpreter will execute.
     */
    public Interpreter(String prompt, ShellCommand[] commands) {
        this.prompt = prompt;
        for (ShellCommand c : commands) {
            commandsMap.putAll(c.buildCommands());
        }
    }
    
    /**
     * Prompt related to this interpreter.
     * 
     * @return 
     */
    public String prompt() {
        return prompt;
    }
    
    /**
     * Computes possible commands started from the given text.
     * 
     * @param prefix text given from user input 
     * @return possible commands.
     */
    public List<String> completions(String prefix) {
        ArrayList<String> list = new ArrayList<>();
        for (String s : commandsMap.keySet()) {
            if (s.startsWith(prefix)) list.add(s);
        }
        return list;
    }
    
    /**
     * Executes given command line.
     * 
     * @param session the current shell session.
     * @param cmd command line
     * @return execution response
     * @throws Exception if exception happend during command execution or parsing.
     */
    public String execute(ShellSession session, String cmd) throws Exception {
        //find command and parse arguments
        ShellCommand command = lookup(cmd);
        command.prepare(cmd);

        //delegate execution to command instance
        return command.perform(session);
    }

    
    /**
     * Searches command instance related to the given user input.
     * 
     * @param cmdLine user's input
     * @return  command instance or null if not found.
     */
    public ShellCommand lookup(String cmdLine) {
        String[] tokens = cmdLine.split(" ");
        
        int i = 0;
        ShellCommand command = null;
        while (i < tokens.length && commandsMap.containsKey(key(tokens, i))) {
            command = commandsMap.get(key(tokens, i));
            i++;
        }
        
        return command;
    }
    
    private String key(String[] tokens, int len) {
        String k = "";
        for (int i = 0; i < len + 1; i++) {
            k += " " + tokens[i];
        }
        return k.trim();
    }
    
}
