/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.example.repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.security.auth.callback.CallbackHandler;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.JaasSecurityContext;

/**
 * The {@link UserInterface} implementation that uses the console.
 */
public class ConsoleInput implements UserInterface {

    protected static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    private final RepositoryClient repositoryClient;
    private final Map<Integer, String> selectionToSourceName = new HashMap<Integer, String>();

    /**
     * Construct the console input and prompt for user input to interact with the RepositoryClient.
     * 
     * @param client the client that should be used; may not be null
     * @param args the command-line arguments; may not be null but may be empty
     */
    public ConsoleInput( final RepositoryClient client,
                         final String[] args ) {
        assert client != null;
        this.repositoryClient = client;
        for (String arg : args) {
            arg = arg.trim().toLowerCase();
            if (arg.equals("--help")) {
                System.out.println();
                System.out.println("Usage:   run.sh [options]");
                System.out.println();
                System.out.println("Options:");
                System.out.println("  --api=value    Specify which API should be used to obtain the content.");
                System.out.println("                 The 'value' must be either 'jcr' or 'dna', and defaults");
                System.out.println("                 to 'jcr'.");
                System.out.println("  --jaas         Specify that JAAS should be used to authenticate the user.");
                System.out.println("  --jaas=name    With no 'name', use JAAS with an application context");
                System.out.println("                 named \"" + RepositoryClient.JAAS_LOGIN_CONTEXT_NAME + "\".");
                System.out.println("                 If another application context is to be used, then specify");
                System.out.println("                 the name.");
                System.out.println("  --help         Print these instructions and exit.");
                System.out.println();
                return;
            }
        }
        try {
            Thread eventThread = new Thread(new Runnable() {

                private boolean quit = false;

                @SuppressWarnings( "synthetic-access" )
                public void run() {
                    try {
                        System.out.println();
                        System.out.print("Starting repositories ... ");
                        client.startRepositories();
                        System.out.println("done.");
                        System.out.println();
                        displayMainMenu();

                        while (!quit) {
                            System.out.print(">");
                            try {
                                String input = in.readLine().trim();
                                if ("?".equals(input) || "h".equals(input)) displayMainMenu();
                                else if ("q".equals(input)) quit = true;
                                else {
                                    try {
                                        int selection = Integer.parseInt(input);
                                        String sourceName = selectionToSourceName.get(selection);
                                        displayNavigationMenu(sourceName);
                                        displayMainMenu();
                                    } catch (NumberFormatException e) {
                                        System.out.println("Invalid option.");
                                        displayMainMenu();
                                    }
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid integer " + e.getMessage());
                            } catch (IllegalArgumentException e) {
                                System.out.println(e.getMessage());
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception err) {
                        System.out.println("Error: " + err.getLocalizedMessage());
                        err.printStackTrace(System.err);
                    } finally {
                        try {
                            // Terminate ...
                            System.out.println();
                            System.out.print("done.\nShutting down repositories ... ");
                            client.shutdown();
                            System.out.print("done.");
                            System.out.println();
                            System.out.println();
                        } catch (Exception err) {
                            System.out.println("Error shutting down repository: " + err.getLocalizedMessage());
                            err.printStackTrace(System.err);
                        }
                    }
                }
            });

            eventThread.start();
        } catch (Exception err) {
            System.out.println("Error: " + err.getLocalizedMessage());
            err.printStackTrace(System.err);
        }
    }

    /**
     * Generate the main menu for the console-based application.
     */
    protected void displayMainMenu() {
        selectionToSourceName.clear();
        System.out.println("-----------------------------------");
        System.out.println("Menu:");
        System.out.println();
        System.out.println("Select a repository to view:");
        int selection = 1;
        for (String sourceName : repositoryClient.getNamesOfRepositories()) {
            selectionToSourceName.put(selection, sourceName);
            System.out.println(StringUtil.justifyRight("" + selection++, 3, ' ') + ") " + sourceName);
        }
        System.out.println("or");
        System.out.println("  ?) Show this menu");
        System.out.println("  q) Quit");
    }

    /**
     * Display the menu for navigating the source with the supplied name. This method returns as soon as the user exits the
     * source.
     * 
     * @param sourceName the source to be navigated; may not be null
     */
    protected void displayNavigationMenu( String sourceName ) {
        assert sourceName != null;
        String currentPath = "/";
        System.out.println();
        System.out.println("Entering the \"" + sourceName + "\" repository.");
        displayNavigationHelp();
        while (true) {
            try {
                // Print the prompt and read the input command ...
                System.out.print(sourceName + "> ");
                String input = in.readLine().trim();

                // Process the command ...
                if (input.length() == 0) continue;
                if ("?".equals(input) || "help".equals(input) || "h".equals(input)) {
                    displayNavigationHelp();
                } else if ("pwd".equals(input)) {
                    System.out.println(currentPath);
                } else if ("exit".equals(input)) {
                    return;
                } else if (input.startsWith("ls") || input.startsWith("ll")) {
                    input = input.substring(2).trim();
                    String path = repositoryClient.buildPath(currentPath, input);
                    displayNode(sourceName, path);
                } else if (input.startsWith("cd ")) {
                    input = input.substring("cd ".length()).trim();
                    if (input.length() == 0) continue;
                    // Change the current path to the new location
                    String oldPath = currentPath;
                    currentPath = repositoryClient.buildPath(currentPath, input);
                    // If the current path does not exist, then go back to the previous path ...
                    if (!repositoryClient.getNodeInfo(sourceName, currentPath, null, null)) {
                        System.out.println("\"" + currentPath + "\" does not exist");
                        currentPath = oldPath;
                    } else {
                        System.out.println(currentPath);
                    }
                }
            } catch (Throwable e) {
                displayError(" processing your command", e);
            }
        }
    }

    protected void displayNavigationHelp() {
        System.out.println();
        System.out.println("Enter one of the following commands followed by RETURN:");
        System.out.println("      pwd          print the current node's path");
        System.out.println("      ls [path]    to list the details of the node at the specified absolute or relative path");
        System.out.println("                   (or the current path if none is supplied)");
        System.out.println("      cd path      to change to the node at the specified absolute or relative path");
        System.out.println("      exit         to exit this repository and return to the main menu");
        System.out.println();
    }

    /**
     * Display the node with the given path found in the supplied source.
     * 
     * @param sourceName the name of the source; may not be null
     * @param path the path to the node; may not be null
     */
    protected void displayNode( String sourceName,
                                String path ) {
        assert sourceName != null;
        assert path != null;

        // Retrieve the node information from the client ...
        Map<String, Object[]> properties = new HashMap<String, Object[]>();
        List<String> children = new ArrayList<String>();
        try {
            repositoryClient.getNodeInfo(sourceName, path, properties, children);
        } catch (Throwable t) {
            displayError(" displaying node \"" + path + "\"", t);
        }

        // Print the './' and '../' options ...
        System.out.println(" ./");
        System.out.println(" ../");

        // Display the children ...
        for (String childName : children) {
            System.out.println(" " + childName + "/");
        }
        // Determine the maximum length of the properties so that we can left-justify the values
        int maxLength = 5;
        for (String propertyName : properties.keySet()) {
            maxLength = Math.max(maxLength, propertyName.length());
        }
        // Display the properties ...
        for (Map.Entry<String, Object[]> property : properties.entrySet()) {
            String name = StringUtil.justifyLeft(property.getKey(), maxLength, ' ');
            Object[] values = property.getValue();
            String valueStr = values.length == 1 ? values[0].toString() : Arrays.asList(values).toString();
            System.out.println(" " + name + " = " + valueStr);
        }
    }

    /**
     * Display the supplied error that happened during the activity.
     * 
     * @param activity the activity; may not be null but may be empty
     * @param t the exception; may not be null
     */
    public void displayError( String activity,
                              Throwable t ) {
        assert activity != null;
        assert t != null;
        System.err.println();
        System.err.println("There has been an error" + activity);
        System.err.println("  " + t.getMessage());
        t.printStackTrace(System.err);
        System.err.println();
        System.err.println("Press any key to continue:");
        try {
            in.readLine();
        } catch (IOException err) {
            err.printStackTrace(System.err);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.example.repository.UserInterface#getLocationOfRepositoryFiles()
     */
    public String getLocationOfRepositoryFiles() {
        return new File("").getAbsolutePath();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.example.repository.UserInterface#getRepositoryConfiguration()
     */
    public File getRepositoryConfiguration() {
        return new File("configRepository.xml");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.example.repository.UserInterface#getCallbackHandler()
     */
    public CallbackHandler getCallbackHandler() {
        return new JaasSecurityContext.UserPasswordCallbackHandler("jsmith", "secret".toCharArray());
    }

}
