/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.example.dna.repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.dna.common.util.StringUtil;

/**
 * @author Randall Hauch
 */
public class ConsoleInput implements UserInterface {

    protected static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    private final RepositoryClient repositoryClient;
    private final Map<Integer, String> selectionToSourceName = new HashMap<Integer, String>();

    public ConsoleInput( final RepositoryClient client,
                         final String[] args ) {
        this.repositoryClient = client;
        try {
            System.out.println();
            System.out.print("Starting repositories ... ");
            client.startRepositories();
            System.out.println("done.");
            System.out.println();

            System.out.println(getMenu());
            Thread eventThread = new Thread(new Runnable() {

                private boolean quit = false;

                @SuppressWarnings( "synthetic-access" )
                public void run() {
                    try {
                        while (!quit) {
                            System.out.print(">");
                            try {
                                String input = in.readLine();
                                if (input.length() != 1) {
                                    System.out.println("Please enter a valid option.");
                                    continue;
                                }

                                char option = input.charAt(0);
                                switch (option) {
                                    case '?':
                                    case 'h':
                                        System.out.println(getMenu());
                                        break;
                                    case 'q':
                                        quit = true;
                                        break;
                                    default:
                                        try {
                                            int selection = Integer.parseInt("" + option);
                                            String sourceName = selectionToSourceName.get(selection);
                                            navigate(sourceName);
                                        } catch (NumberFormatException e) {
                                            System.out.println("Invalid option.");
                                            break;
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
                    } finally {
                        try {
                            // Terminate ...
                            System.out.println();
                            System.out.print("done.\nShutting down repositories and services ... ");
                            client.shutdown();
                            System.out.print("done.");
                            System.out.println();
                            System.out.println();
                        } catch (Exception err) {
                            System.out.println("Error shutting down sequencing service and repository: "
                                               + err.getLocalizedMessage());
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

    protected String getMenu() {
        selectionToSourceName.clear();
        StringBuilder buffer = new StringBuilder();
        buffer.append("-----------------------------------\n");
        buffer.append("Menu:\n");
        buffer.append("\n");
        buffer.append(" Select a repository to view:\n");
        int selection = 1;
        for (String sourceName : this.repositoryClient.getNamesOfRepositories()) {
            selectionToSourceName.put(selection, sourceName);
            buffer.append("  " + selection + ") " + sourceName + "\n");
        }
        buffer.append(" or\n");
        buffer.append("  ?) Show this menu\n");
        buffer.append("  q) Quit");
        return buffer.toString();
    }

    protected void navigate( String sourceName ) {
        String currentPath = "/";
        while (true) {

            // Ask for the command ...
            System.out.print("> ");
            try {
                String input = in.readLine().trim();
                if (input.length() == 0) {
                    continue;
                }

                if ("?".equals(input) || "help".equals(input) || "h".equals(input)) {
                    System.out.println("  Enter a command:");
                    System.out.println("      pwd          print the current node's path");
                    System.out.println("      ls [path]    to list the details of the node at the specified absolute or relative path");
                    System.out.println("                   (or the current path if none is supplied)");
                    System.out.println("      cd path      to change to the node at the specified absolute or relative path");
                    System.out.println("      exit         to exit this repository and return to the main menu");
                    System.out.println("  and press return:");
                } else if ("pwd".equals(input)) {
                    System.out.println(" " + currentPath);
                } else if ("exit".equals(input)) {
                    return;
                } else if (input.startsWith("ls")) {
                    input = input.substring("ls".length()).trim();
                    String path = currentPath;
                    if (input.length() != 0) path = input;
                    displayNode(sourceName, path);
                } else if (input.startsWith("cd ")) {
                    input = input.substring("cd ".length()).trim();
                    if (input.length() == 0) continue;
                    // Check to see if the new path exists ...
                    if (!repositoryClient.getNodeInfo(sourceName, input, null, null)) {
                        System.out.println(" \"" + input + "\" does not exist");
                    } else {
                        currentPath = input;
                    }
                }
            } catch (Throwable e) {
                displayError(" processing your command", e);
            }
        }

    }

    protected void displayNode( String sourceName,
                                String path ) {
        Map<String, Object[]> properties = new HashMap<String, Object[]>();
        List<String> children = new ArrayList<String>();
        try {
            repositoryClient.getNodeInfo(sourceName, path, properties, children);
        } catch (Throwable t) {
            displayError(" displaying node \"" + path + "\"", t);
        }

        System.out.println("  Path:" + path);
        System.out.println("  Properties:");
        int maxLength = 0;
        for (String propertyName : properties.keySet()) {
            maxLength = Math.max(maxLength, propertyName.length());
        }
        for (Map.Entry<String, Object[]> property : properties.entrySet()) {
            String name = property.getKey();
            name = StringUtil.justifyLeft(name, maxLength, ' ');
            Object[] values = property.getValue();
            String valueStr = StringUtil.readableString(values);
            if (values.length == 1) StringUtil.readableString(values[0]);
            System.out.println("     " + name + " = " + valueStr);
        }
        System.out.println("  Children:");
        for (String childName : children) {
            System.out.println("     " + childName);
        }
        System.out.println();
    }

    protected void displayError( String activity,
                                 Throwable t ) {
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
     * @see org.jboss.example.dna.repository.UserInterface#getLocationOfRepositoryFiles()
     */
    public String getLocationOfRepositoryFiles() {
        return new File("").getAbsolutePath();
    }
}
