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
package org.jboss.dna.spi.graph.commands.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.Path.Segment;
import org.jboss.dna.spi.graph.commands.GetNodeCommand;
import org.jboss.dna.spi.graph.impl.BasicPathSegment;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class BasicGetNodeCommand extends BasicGetPropertiesCommand implements GetNodeCommand {

    /**
     */
    private static final long serialVersionUID = 5355669032301356873L;
    private List<Segment> children;

    /**
     * @param path
     */
    public BasicGetNodeCommand( Path path ) {
        super(path);

    }

    /**
     * @return children
     */
    public List<Segment> getChildren() {
        return children;
    }

    /**
     * {@inheritDoc}
     */
    public void setChild( Name nameOfChild ) {
        if (nameOfChild == null) {
            children = Collections.emptyList();
        } else {
            children = Collections.singletonList((Segment)new BasicPathSegment(nameOfChild));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setChildren( Iterator<Segment> namesOfChildren ) {
        if (namesOfChildren == null) {
            children = Collections.emptyList();
        } else {
            children = new ArrayList<Segment>();
            while (namesOfChildren.hasNext()) {
                children.add(namesOfChildren.next());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setChildren( Iterable<Segment> namesOfChildren ) {
        if (namesOfChildren == null) {
            children = Collections.emptyList();
        } else {
            children = new ArrayList<Segment>();
            for (Segment childSegment : namesOfChildren) {
                children.add(childSegment);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setChildren( Segment... namesOfChildren ) {
        if (namesOfChildren == null || namesOfChildren.length == 0) {
            children = Collections.emptyList();
        } else {
            children = new ArrayList<Segment>();
            for (Segment childSegment : namesOfChildren) {
                children.add(childSegment);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNoChildren() {
        children = Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());
        sb.append(" at ");
        sb.append(this.getPath());
        boolean firstProperty = true;
        for (Property property : this.getPropertyIterator()) {
            if (property.isEmpty()) continue;
            if (firstProperty) {
                sb.append(" { ");
                firstProperty = false;
            } else {
                sb.append("; ");
            }
            sb.append(property.getName());
            sb.append("=");
            if (property.isSingle()) {
                sb.append(StringUtil.readableString(property.getValues().next()));
            } else {
                sb.append(StringUtil.readableString(property.getValuesAsArray()));
            }
        }
        if (!firstProperty) {
            sb.append(" }");
        }
        List<Path.Segment> children = this.getChildren();
        if (children != null && children.size() > 0) {
            sb.append(" with ").append(children.size()).append(" children: ");
            sb.append(StringUtil.readableString(children));
        }
        return sb.toString();
    }

}
