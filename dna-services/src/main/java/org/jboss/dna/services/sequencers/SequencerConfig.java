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
package org.jboss.dna.services.sequencers;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.component.ComponentConfig;

/**
 * @author Randall Hauch
 */
@Immutable
public class SequencerConfig extends ComponentConfig {

    private final Set<SequencerPathExpression> pathExpressions;

    public SequencerConfig( String name, String description, String classname, String[] classpath, String... pathExpressions ) {
        this(name, description, System.currentTimeMillis(), classname, classpath, pathExpressions);
    }

    public SequencerConfig( String name, String description, long timestamp, String classname, String[] classpath, String... pathExpressions ) {
        super(name, description, timestamp, classname, classpath);
        this.pathExpressions = buildPathExpressionSet(pathExpressions);
    }

    /* package */static Set<SequencerPathExpression> buildPathExpressionSet( String... pathExpressions ) {
        Set<SequencerPathExpression> result = null;
        if (pathExpressions != null) {
            result = new LinkedHashSet<SequencerPathExpression>();
            for (String pathExpression : pathExpressions) {
                if (pathExpression == null) continue;
                pathExpression = pathExpression.trim();
                if (pathExpression.length() == 0) continue;
                result.add(new SequencerPathExpression(pathExpression));
            }
            result = Collections.unmodifiableSet(result);
        } else {
            result = Collections.emptySet(); // already immutable
        }
        return result;
    }

    public Collection<SequencerPathExpression> getPathExpressions() {
        return Collections.unmodifiableSet(this.pathExpressions);
    }

    public boolean hasChanged( SequencerConfig that ) {
        if (super.hasChanged(that)) return true;
        if (!this.getPathExpressions().equals(that.getPathExpressions())) return true;
        return false;
    }

}
