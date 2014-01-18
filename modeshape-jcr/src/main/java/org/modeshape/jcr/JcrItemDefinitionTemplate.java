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

import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.OnParentVersionAction;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * ModeShape convenience implementation to support the JCR 2 NodeDefinitionTemplate and PropertyDefinitionTemplate classes.
 */
@NotThreadSafe
abstract class JcrItemDefinitionTemplate implements ItemDefinition {

    protected static void registerMissingNamespaces( ExecutionContext originalContext,
                                                     ExecutionContext newContext,
                                                     Path path ) {
        for (Segment segment : path) {
            registerMissingNamespaces(originalContext, newContext, segment.getName());
        }
    }

    protected static void registerMissingNamespaces( ExecutionContext originalContext,
                                                     ExecutionContext newContext,
                                                     Name... names ) {
        if (names == null) return;
        NamespaceRegistry newRegistry = newContext.getNamespaceRegistry();
        NamespaceRegistry originalRegistry = originalContext.getNamespaceRegistry();
        for (Name name : names) {
            if (name != null) {
                String uri = name.getNamespaceUri();
                if (!newRegistry.isRegisteredNamespaceUri(uri)) {
                    String prefix = originalRegistry.getPrefixForNamespaceUri(uri, false);
                    newRegistry.register(prefix, uri);
                }
            }
        }
    }

    private final ExecutionContext context;
    private boolean autoCreated = false;
    private boolean mandatory = false;
    private boolean isProtected = false;
    private Name name;
    private int onParentVersion = OnParentVersionAction.COPY;

    JcrItemDefinitionTemplate( ExecutionContext context ) {
        assert context != null;

        this.context = context;
    }

    JcrItemDefinitionTemplate( JcrItemDefinitionTemplate original,
                               ExecutionContext context ) {
        this.context = context;
        this.autoCreated = original.autoCreated;
        this.mandatory = original.mandatory;
        this.isProtected = original.isProtected;
        this.name = original.name;
        this.onParentVersion = original.onParentVersion;
        JcrItemDefinitionTemplate.registerMissingNamespaces(original.context, context, this.name);
    }

    ExecutionContext getExecutionContext() {
        return context;
    }

    @Override
    public NodeType getDeclaringNodeType() {
        return null;
    }

    @Override
    public String getName() {
        if (name == null) return null;
        return name.getString(context.getNamespaceRegistry());
    }

    @Override
    public int getOnParentVersion() {
        return onParentVersion;
    }

    @Override
    public boolean isAutoCreated() {
        return autoCreated;
    }

    @Override
    public boolean isMandatory() {
        return mandatory;
    }

    @Override
    public boolean isProtected() {
        return isProtected;
    }

    public ExecutionContext getContext() {
        return context;
    }

    public void setAutoCreated( boolean autoCreated ) {
        this.autoCreated = autoCreated;
    }

    public void setMandatory( boolean mandatory ) {
        this.mandatory = mandatory;
    }

    public void setProtected( boolean isProtected ) {
        this.isProtected = isProtected;
    }

    public void setName( String name ) throws ConstraintViolationException {
        if (name == null) {
            throw new ConstraintViolationException();
        }
        try {
            this.name = context.getValueFactories().getNameFactory().create(name);
        } catch (ValueFormatException vfe) {
            throw new ConstraintViolationException(vfe);
        }
    }

    public void setOnParentVersion( int onParentVersion ) {
        assert onParentVersion == OnParentVersionAction.ABORT || onParentVersion == OnParentVersionAction.COMPUTE
               || onParentVersion == OnParentVersionAction.COPY || onParentVersion == OnParentVersionAction.IGNORE
               || onParentVersion == OnParentVersionAction.INITIALIZE || onParentVersion == OnParentVersionAction.VERSION;
        this.onParentVersion = onParentVersion;
    }

    @Override
    public String toString() {
        return getName();
    }
}
