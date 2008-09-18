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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.graph.DateTime;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.Path.Segment;
import org.jboss.dna.spi.graph.commands.GetChildrenCommand;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class BasicGetChildrenCommand extends BasicGraphCommand implements GetChildrenCommand {

    /**
     */
    private static final long serialVersionUID = -8515194602506918337L;
    private final Map<Segment, Property[]> childProperties = new HashMap<Segment, Property[]>();
    private final List<Segment> children = new LinkedList<Segment>();
    private final Path path;
    private CachePolicy cachePolicy;
    private DateTime timeLoaded;

    /**
     * @param path the path to the node; may not be null
     */
    public BasicGetChildrenCommand( Path path ) {
        super();
        assert path != null;
        this.path = path;
    }

    /**
     * {@inheritDoc}
     */
    public void setNoChildren() {
        this.children.clear();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.GetChildrenCommand#addChild(org.jboss.dna.spi.graph.Path.Segment,
     *      org.jboss.dna.spi.graph.Property[])
     */
    public void addChild( Segment nameOfChild,
                          Property... identityProperties ) {
        if (nameOfChild == null) return;
        this.children.add(nameOfChild);
        if (identityProperties != null) {
            if (identityProperties.length == 0) identityProperties = null;
            this.childProperties.put(nameOfChild, identityProperties);
        }
    }

    /**
     * Get the identity properties for the supplied child.
     * 
     * @param child the name of the child
     * @return the array of identity properties for the child, or null if there are none
     */
    public Property[] getChildIdentityProperties( Segment child ) {
        return this.childProperties.get(child);
    }

    /**
     * @return children
     */
    public List<Segment> getChildren() {
        return this.children;
    }

    /**
     * {@inheritDoc}
     */
    public Path getPath() {
        return path;
    }

    /**
     * {@inheritDoc}
     */
    public CachePolicy getCachePolicy() {
        return cachePolicy;
    }

    /**
     * {@inheritDoc}
     */
    public DateTime getTimeLoaded() {
        return timeLoaded;
    }

    /**
     * @param timeLoaded Sets timeLoaded to the specified value.
     */
    public void setTimeLoaded( DateTime timeLoaded ) {
        this.timeLoaded = timeLoaded;
    }

    /**
     * {@inheritDoc}
     */
    public void setCachePolicy( CachePolicy cachePolicy ) {
        this.cachePolicy = cachePolicy;
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
        List<Path.Segment> children = this.getChildren();
        if (children != null && children.size() > 0) {
            sb.append(" with ").append(children.size()).append(" children: ");
            sb.append(StringUtil.readableString(children));
        }
        return sb.toString();
    }
}
