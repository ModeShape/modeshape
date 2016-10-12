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

/**
 * Utility class from command line parsing.
 * 
 * @author kulikov
 */
public class CommandParser {
    public static String commandName(String cmd) {
        int len = cmd.indexOf(' ');
        return len > 0 ? cmd.substring(0, cmd.indexOf(' ')) : cmd;
    }
    
    public static String getOptionValue(String option, String cmd) {
        int pos = cmd.indexOf(option);
        
        //no such key at all
        if (pos < 0) {
            return null;
        }
        
        //shift to the end of key position
        pos += option.length();
        
        //ignore leadeing whitespaces
        while (pos < cmd.length()) {
            if (cmd.charAt(pos) == ' ') {
                pos++;
            } else {
                break;
            }
        }
        
        
        int eos = pos + 1;
        
        //end of line
        if (eos >= cmd.length()) {
            return "";
        }
        
        //quoted text?
        if (cmd.charAt(pos) == '\'') {
            eos = cmd.indexOf('\'', eos);
            pos++;
        } else if (cmd.charAt(pos) == '"') {
            eos = cmd.indexOf('"', eos);
            pos++;
        } else {
            eos = cmd.indexOf(' ', eos);
        }
        
        eos = eos < 0 ? cmd.length() : eos;        
        return cmd.substring(pos, eos).trim();
    }
    
}
