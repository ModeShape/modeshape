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
package org.modeshape.example.sequencer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import org.modeshape.repository.sequencer.SequencingService;

/**
 * The {@link UserInterface} implementation that uses the console to interact with a user.
 */
public class ConsoleInput implements UserInterface {

    protected static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    public ConsoleInput( final SequencingClient client ) {
        try {
            System.out.println();
            System.out.print("Starting ModeShape repository and sequencing service ... ");
            client.startRepository();
            System.out.println("done.");
            System.out.println();

            System.out.println(getMenu());
            Thread eventThread = new Thread(new Runnable() {

                private boolean quit = false;

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
                                    case 'u':
                                        client.uploadFile();
                                        break;
                                    case 's':
                                        client.search();
                                        break;
                                    case 'm':
                                    case '?':
                                    case 'h':
                                        System.out.println(getMenu());
                                        break;
                                    case 'd':
                                        System.out.println(getStatistics(client.getStatistics()));
                                        break;
                                    case 'q':
                                        quit = true;
                                        break;
                                    default:
                                        System.out.println("Invalid option.");
                                        break;
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
                            System.out.print("Shutting down repository ... ");
                            client.shutdownRepository();
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
        StringBuilder buffer = new StringBuilder();
        buffer.append("-----------------------------------\n");
        buffer.append("Menu:\n");
        buffer.append("\n");
        buffer.append("u) Upload a file to the repository\n");
        buffer.append("s) Search the repository using extracted metadata\n");
        buffer.append("\n");
        buffer.append("d) Display statistics\n");
        buffer.append("\n");
        buffer.append("?) Show this menu\n");
        buffer.append("q) Quit");
        return buffer.toString();
    }

    /**
     * {@inheritDoc}
     */
    public URL getFileToUpload() throws IllegalArgumentException, IOException {
        System.out.println("Please enter the file to upload:");
        String path = in.readLine();
        File file = new File(path);
        if (!file.exists()) {
            throw new IllegalArgumentException("The file \"" + file.getAbsolutePath() + "\" does not exist.");
        }
        if (!file.canRead()) {
            throw new IllegalArgumentException("Unable to read \"" + file.getAbsolutePath() + "\".");
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("Please specify a file.  The file \"" + file.getAbsolutePath()
                                               + "\" is a directory.");
        }
        return file.toURI().toURL();
    }

    public String getRepositoryPath( String defaultPath ) throws IllegalArgumentException, IOException {
        if (defaultPath != null) defaultPath = defaultPath.trim();
        if (defaultPath.length() == 0) defaultPath = null;
        String displayDefaultPath = defaultPath == null ? "" : " [" + defaultPath.trim() + "]";
        System.out.println("Please enter the repository path where the file should be placed" + displayDefaultPath + ":");
        String path = in.readLine().trim();
        if (path.length() == 0) {
            if (defaultPath == null) {
                throw new IllegalArgumentException("The path \"" + path + "\" is not valid.");
            }
            path = defaultPath;
        }
        return path;
    }

    public void displaySearchResults( List<ContentInfo> contentInfos ) {
        System.out.println();
        if (contentInfos.isEmpty()) {
            System.out.println("No results were found.");
            System.out.println();
            return;
        }
        if (contentInfos.size() == 1) {
            System.out.println("1 result was found:");
        } else {
            System.out.println("" + contentInfos.size() + " results were found:");
        }
        int counter = 1;
        for (ContentInfo info : contentInfos) {
            System.out.println(" " + info.getInfoType() + " " + counter++);
            System.out.println(info.toString());
        }
        System.out.println();
    }

    public String getStatistics( SequencingService.Statistics stats ) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("# nodes sequenced: ").append(stats.getNumberOfNodesSequenced()).append("\n");
        sb.append("# nodes skipped: ").append(stats.getNumberOfNodesSkipped()).append("\n");
        sb.append("\n");
        return sb.toString();
    }

    public void displayError( Exception e ) {
        System.err.println(e.getMessage());
    }
}
