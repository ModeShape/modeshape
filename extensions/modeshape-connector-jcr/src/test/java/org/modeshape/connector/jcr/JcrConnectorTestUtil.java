/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.connector.jcr;

import org.jboss.security.config.IDTrustConfiguration;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.collection.Problem;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrRepository.Option;

public class JcrConnectorTestUtil {

    protected static final String JAAS_CONFIG_FILE_PATH = "security/jaas.conf.xml";
    protected static final String JAAS_POLICY_NAME = "modeshape-jcr";

    public static final String CARS_REPOSITORY_NAME = "Cars";
    public static final String AIRCRAFT_REPOSITORY_NAME = "Aircraft";
    protected static final String CARS_SOURCE_NAME = "Cars Source";
    protected static final String AIRCRAFT_SOURCE_NAME = "Aircraft Source";

    static {
        // Set up the JAAS instance (only need to do this once) ...
        String configFile = JAAS_CONFIG_FILE_PATH;
        IDTrustConfiguration idtrustConfig = new IDTrustConfiguration();

        try {
            idtrustConfig.config(configFile);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Create the ModeShape JCR engine having two in-memory repositories called "Cars" and "Aircraft", and each populated with
     * initial content. Each repository is configured to use the JAAS provider and the "modeshape-jcr" application policy defined
     * in the "security/jaas.conf.xml" file.
     * 
     * @return the running engine; never null
     */
    public static JcrEngine loadEngine() {
        final ClassLoader classLoader = JcrConnectorTestUtil.class.getClassLoader();

        ExecutionContext newContext = new ExecutionContext();
        JcrConfiguration configuration = new JcrConfiguration(newContext);

        // Define the Cars repository ...
        configuration.repositorySource(CARS_SOURCE_NAME)
                     .usingClass(InMemoryRepositorySource.class.getName())
                     .loadedFromClasspath()
                     .setDescription("Repository source with cars")
                     .and()
                     .repository(CARS_REPOSITORY_NAME)
                     .setDescription("JCR Repository with cars")
                     .setSource(CARS_SOURCE_NAME)
                     .addNodeTypes(classLoader.getResource("cars.cnd"))
                     .setOption(Option.JAAS_LOGIN_CONFIG_NAME, JAAS_POLICY_NAME);

        // Define the Cars repository ...
        configuration.repositorySource(AIRCRAFT_SOURCE_NAME)
                     .usingClass(InMemoryRepositorySource.class.getName())
                     .loadedFromClasspath()
                     .setDescription("Repository source with aircraft")
                     .and()
                     .repository(AIRCRAFT_REPOSITORY_NAME)
                     .setDescription("JCR Repository with aircraft")
                     .setSource(AIRCRAFT_SOURCE_NAME)
                     .addNodeTypes(classLoader.getResource("aircraft.cnd"))
                     .setOption(Option.JAAS_LOGIN_CONFIG_NAME, JAAS_POLICY_NAME);
        configuration.save();

        // Now create the JCR engine ...
        JcrEngine engine = configuration.build();
        engine.start();
        if (engine.getProblems().hasProblems()) {
            for (Problem problem : engine.getProblems()) {
                System.err.println(problem.getMessageString());
            }
            throw new RuntimeException("Could not start due to problems");
        }

        // Now import the content for the two in-memory repository sources ...
        try {
            engine.getGraph(CARS_SOURCE_NAME).importXmlFrom(classLoader.getResource("cars.xml").toURI()).into("/");
            engine.getGraph(AIRCRAFT_SOURCE_NAME).importXmlFrom(classLoader.getResource("aircraft.xml").toURI()).into("/");
        } catch (Throwable t) {
            throw new SystemFailureException("Could not import the content into the repositories", t);
        }

        return engine;
    }

}
