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
package org.modeshape.graph.connector.federation;

import java.util.List;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;

/**
 * Information about a node that has been projected into the repository from a source. Each projected node may be followed by a
 * {@link #next() next} projected node.
 */
@NotThreadSafe
abstract class ProjectedNode {
    private final Location location;
    private ProjectedNode next;

    protected ProjectedNode( Location location ) {
        this.location = location;
    }

    public Location location() {
        return location;
    }

    public int size() {
        return next != null ? next.size() + 1 : 1;
    }

    public int numberOfProxies() {
        int base = isProxy() ? 1 : 0;
        return next != null ? next.numberOfProxies() + base : base;
    }

    public boolean hasNext() {
        return next != null;
    }

    public ProjectedNode next() {
        return next;
    }

    /*package*/void add( ProjectedNode next ) {
        if (this.next != null) {
            this.next.add(next);
        } else {
            this.next = next;
        }
    }

    public abstract boolean isPlaceholder();

    public abstract boolean isProxy();

    public abstract PlaceholderNode asPlaceholder();

    public abstract ProxyNode asProxy();
}

@NotThreadSafe
class PlaceholderNode extends ProjectedNode {
    private final Map<Name, Property> properties;
    private final List<ProjectedNode> children;

    protected PlaceholderNode( Location location,
                               Map<Name, Property> properties,
                               List<ProjectedNode> children ) {
        super(location);
        this.properties = properties;
        this.children = children;
    }

    public List<ProjectedNode> children() {
        return children;
    }

    public Map<Name, Property> properties() {
        return properties;
    }

    @Override
    public boolean isPlaceholder() {
        return true;
    }

    @Override
    public PlaceholderNode asPlaceholder() {
        return this;
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    @Override
    public ProxyNode asProxy() {
        return null;
    }

    @Override
    public String toString() {
        return "Placeholder " + location();
    }
}

@NotThreadSafe
class ProxyNode extends ProjectedNode {
    private final Projection projection;
    private final boolean sameLocationAsOriginal;
    private final Location federatedLocation;

    protected ProxyNode( Projection projection,
                         Location locationInSource,
                         Location locationInFederated ) {
        super(locationInSource);
        this.projection = projection;
        this.federatedLocation = locationInFederated;
        this.sameLocationAsOriginal = locationInSource.isSame(locationInFederated);
    }

    protected ProxyNode( Projection projection,
                         Location locationInSource,
                         Location locationInFederated,
                         boolean isSameLocation ) {
        super(locationInSource);
        this.projection = projection;
        this.federatedLocation = locationInFederated;
        this.sameLocationAsOriginal = isSameLocation;
    }

    public String source() {
        return projection.getSourceName();
    }

    public String workspaceName() {
        return projection.getWorkspaceName();
    }

    /*package*/Projection projection() {
        return projection;
    }

    public Location federatedLocation() {
        return federatedLocation;
    }

    @Override
    public boolean isPlaceholder() {
        return false;
    }

    @Override
    public PlaceholderNode asPlaceholder() {
        return null;
    }

    @Override
    public boolean isProxy() {
        return true;
    }

    @Override
    public ProxyNode asProxy() {
        return this;
    }

    /**
     * Determine whether this projected node is a top-level node in the source projected into the federated repository.
     * 
     * @return true if the projected node is a top-level node in the source, or false otherwise
     */
    public boolean isTopLevelNode() {
        return federatedLocation != null && federatedLocation.hasPath() && projection.isTopLevelPath(federatedLocation.getPath());
    }

    public boolean isSameLocationAsOriginal() {
        return this.sameLocationAsOriginal;
    }

    @Override
    public String toString() {
        return "Proxy for " + federatedLocation() + " projected from " + location() + " in workspace \"" + workspaceName()
               + "\" in \"" + source() + "\"";
    }
}
