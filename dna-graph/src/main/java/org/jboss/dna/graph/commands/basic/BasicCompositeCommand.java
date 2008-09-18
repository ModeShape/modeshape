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
package org.jboss.dna.graph.commands.basic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jboss.dna.graph.commands.CompositeCommand;
import org.jboss.dna.graph.commands.GraphCommand;

/**
 * @author Randall Hauch
 */
public class BasicCompositeCommand extends BasicGraphCommand implements CompositeCommand {

    private final List<GraphCommand> commands = new ArrayList<GraphCommand>();

    public BasicCompositeCommand() {
        super();
    }

    public boolean isEmpty() {
        return commands.isEmpty();
    }

    public int size() {
        return commands.size();
    }

    public void add( GraphCommand command ) {
        if (command != null) commands.add(command);
    }

    public GraphCommand getCommand( int index ) {
        return commands.get(index);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<GraphCommand> iterator() {
        return commands.iterator();
    }

    public BasicCompositeCommand clearCommands() {
        this.commands.clear();
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " (containing " + commands.size() + " commands)";
    }

}
