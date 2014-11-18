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
package org.modeshape.sequencer.teiid;

import java.io.InputStream;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import org.modeshape.common.util.CheckArg;

/**
 * The Dynamic Vdb Sequencer that reads dynamics VDB files defined wholly by DDL
 * in an xml file
 */
public class VdbDynamicSequencer extends VdbSequencer {

    @Override
    public boolean execute(Property inputProperty, Node outputNode, Context context) throws Exception {
        LOGGER.debug("VdbDynamicSequencer.execute called:outputNode name='{0}', path='{1}'", outputNode.getName(), outputNode.getPath());

        final Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");

        InputStream stream = binaryValue.getStream();
        VdbManifest manifest = readManifest(binaryValue, stream, outputNode, context);
        if (manifest == null) {
            throw new Exception("VdbDynamicSequencer.execute failed. The xml cannot be read.");
        }

        return true;
    }
}
