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

import java.io.BufferedInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.console.AeshConsoleCallback;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.terminal.Color;
import org.jboss.aesh.terminal.TerminalCharacter;
import org.jboss.aesh.terminal.TerminalColor;

/**
 * Command processor wraps shell console.
 * 
 * @author kulikov
 */
public class ShellCommandProcessor implements Command {
    
    //ssh session
    private final ShellSession session;
    
    //ssh server std streams
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    
    private ExitCallback callback;
    private Environment environment;
    
    //shell  console
    private Console console;
    
    public ShellCommandProcessor(ShellSession session) {
        this.session = session;
    }
    
    @Override
    public void setInputStream(InputStream in) {  
        this.in = new BufferedInputStream(in) {
            @Override
            public int read(byte[] buff, int offset, int len) throws IOException {
                int res = super.read(buff, offset, len);
                for (int i = 0; i < res; i++) {
                    if (buff[i] == '\r') {
                        buff[i] = '\n'; 
                    }
                }
                return res;
            }
        };
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.out = new FilterOutputStream(out){
            @Override
            public void write(int b) throws IOException {
                if (b == '\n') {
                    super.write('\r');
                }
                super.write(b);
            }
        };
    }

    @Override
    public void setErrorStream(OutputStream out) {
        this.err = new FilterOutputStream(out){
            @Override
            public void write(int b) throws IOException {
                if (b == '\n') {
                    super.write('\r');
                }
                super.write(b);
            }
        };
    }

    @Override
    public void setExitCallback(ExitCallback ec) {
        this.callback = ec;
        session.setExitCallback(callback);
    }

    @Override
    public void start(Environment e) throws IOException {
        this.environment = e;
        //create shell console and assign std streams
        console = new Console(new SettingsBuilder()
                .readInputrc(false)
                .logging(false)
                .ansi(true)               
                .inputStream(in)
                .outputStream(new PrintStream(out))
                .outputStreamError(new PrintStream(err))
                .create());
        
        console.setPrompt(prompt("[modeshape]$ "));
        console.setCompletionEnabled(true);
        
        console.setConsoleCallback(new AeshConsoleCallback() {
            @Override
            public int execute(ConsoleOperation co) throws InterruptedException {
                String cmd = co.getBuffer();
                try {
                    //execute command using active interpreter
                    String res = session.getInterpreter().execute(session, cmd);
                    
                    //update prompt 
                    console.setPrompt(prompt(session.getInterpreter().prompt()));
                    
                    //print response and flush output
                    console.getShell().out().println(res);
                    console.getShell().out().flush();
                } catch (Exception e) {
                    //print error message and flush stream
                    console.getShell().err().println(e.getMessage());
                    console.getShell().err().flush();
                }
                
                return 1;
            }
             
        });
        
        console.addCompletion(new Completion() {
            @Override
            public void complete(CompleteOperation co) {
                co.addCompletionCandidates(session.getInterpreter().completions(co.getBuffer()));
            }
        });
        
        console.start();
    }

    @Override
    public void destroy() {
    }

    public void setPrompt(String prompt) {
        console.setPrompt(prompt(prompt));
    }
    
    private Prompt prompt(String prompt) {
        List<TerminalCharacter> chars = new ArrayList<>();
        for (int i = 0; i < prompt.length(); i++) {
            chars.add(new TerminalCharacter(prompt.charAt(i), new TerminalColor(Color.DEFAULT, Color.DEFAULT)));
        }
        return new Prompt(chars);
    }
    
    protected void exit() {
        this.callback.onExit(0);
    }
    
}
