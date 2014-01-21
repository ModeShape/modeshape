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

package org.modeshape.jcr.cache.change;

import java.util.Set;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;

/**
 * Base class for the changes involving sequencing
 * 
 * @author Horia Chiorean
 */
public abstract class AbstractSequencingChange extends AbstractNodeChange {

    private static final long serialVersionUID = 1L;

    private final String userId;
    private final String selectedPath;
    private final String outputPath;
    private final String sequencerName;

    protected AbstractSequencingChange( NodeKey sequencedNodeKey,
                                        Path sequencedNodePath,
                                        Name sequencedNodePrimaryType,
                                        Set<Name> sequencedNodeMixinTypes,
                                        String outputPath,
                                        String userId,
                                        String selectedPath,
                                        String sequencerName ) {
        super(sequencedNodeKey, sequencedNodePath, sequencedNodePrimaryType, sequencedNodeMixinTypes);
        assert outputPath != null;
        assert userId != null;
        assert selectedPath != null;
        assert sequencerName != null;
        this.outputPath = outputPath;
        this.userId = userId;
        this.selectedPath = selectedPath;
        this.sequencerName = sequencerName;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public String getSelectedPath() {
        return selectedPath;
    }

    public String getSequencerName() {
        return sequencerName;
    }

    public String getUserId() {
        return userId;
    }
}
