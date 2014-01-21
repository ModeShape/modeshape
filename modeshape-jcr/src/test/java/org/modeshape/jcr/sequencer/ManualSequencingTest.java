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
package org.modeshape.jcr.sequencer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.net.URL;
import javax.jcr.Node;
import javax.jcr.Property;
import org.junit.Test;
import org.modeshape.jcr.Environment;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.SingleUseAbstractTest;
import org.modeshape.jcr.TestSequencersHolder;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.Session;

public class ManualSequencingTest extends SingleUseAbstractTest {

    @Override
    protected RepositoryConfiguration createRepositoryConfiguration( String repositoryName,
                                                                     Environment environment ) throws Exception {
        return RepositoryConfiguration.read("config/repo-config-manual-sequencing.json");
    }

    private JcrTools tools = new JcrTools();

    @Test
    public void shouldManuallySequence() throws Exception {
        URL url = getClass().getClassLoader().getResource("log4j.properties");
        assertNotNull(url);
        Session session = session();
        tools.uploadFile(session, "/files/log4j.properties", url);

        Node propFile = session.getNode("/files/log4j.properties");
        Property content = propFile.getProperty("jcr:content/jcr:data");
        assertNotNull(content);

        Node output = session.getRootNode().addNode("output");
        assertFalse(output.hasNode(TestSequencersHolder.DERIVED_NODE_NAME));

        session.sequence("Counting sequencer", content, output);
        assertTrue(output.hasNode(TestSequencersHolder.DERIVED_NODE_NAME));

        session.refresh(false);

        assertFalse(session.getRootNode().hasNode("files"));
        assertFalse(session.getRootNode().hasNode("output"));
    }

}
