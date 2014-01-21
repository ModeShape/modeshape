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
package org.modeshape.jcr;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.value.Name;

/**
 * An exception that captures the error condition that a referenced node type already exists.
 */
@Immutable
public class NodeTypeExistsException extends javax.jcr.nodetype.NodeTypeExistsException {

    /**
     */
    private static final long serialVersionUID = 1L;

    private Name nodeType;

    /**
     * @param nodeType the name of the existing node type
     */
    public NodeTypeExistsException( Name nodeType ) {
        super();
        CheckArg.isNotNull(nodeType, "nodeType");
        this.nodeType = nodeType;
    }

    /**
     * @param nodeType the name of the existing node type
     * @param message
     * @param rootCause
     */
    public NodeTypeExistsException( Name nodeType,
                                    String message,
                                    Throwable rootCause ) {
        super(message, rootCause);
        CheckArg.isNotNull(nodeType, "nodeType");
        this.nodeType = nodeType;
    }

    /**
     * @param nodeType the name of the existing node type
     * @param message
     */
    public NodeTypeExistsException( Name nodeType,
                                    String message ) {
        super(message);
        CheckArg.isNotNull(nodeType, "nodeType");
        this.nodeType = nodeType;
    }

    /**
     * @param nodeType the name of the existing node type
     * @param rootCause
     */
    public NodeTypeExistsException( Name nodeType,
                                    Throwable rootCause ) {
        super(rootCause);
        CheckArg.isNotNull(nodeType, "nodeType");
        this.nodeType = nodeType;
    }

    /**
     * Get the name of the existing node type.
     * 
     * @return the existing node type name
     */
    public Name getNodeType() {
        return nodeType;
    }

}
