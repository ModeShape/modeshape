/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.repository.sequencer;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.component.ComponentConfig;

/**
 * @author Randall Hauch
 */
@Immutable
public class SequencerConfig extends ComponentConfig {

    private final Set<SequencerPathExpression> pathExpressions;

    public SequencerConfig( String name, String description, Map<String, Object> properties, String classname, String[] classpath, String... pathExpressions ) {
        this(name, description, System.currentTimeMillis(), properties, classname, classpath, pathExpressions);
    }

    public SequencerConfig( String name, String description, long timestamp, Map<String, Object> properties, String classname, String[] classpath, String... pathExpressions ) {
        super(name, description, timestamp, properties, classname, classpath);
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
                result.add(SequencerPathExpression.compile(pathExpression));
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
