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

    private final Set<String> runRules;

    public SequencerConfig( String name, String description, String classname, String[] classpath, String... runRules ) {
        this(name, description, System.currentTimeMillis(), classname, classpath, runRules);
    }

    public SequencerConfig( String name, String description, long timestamp, String classname, String[] classpath, String... runRules ) {
        super(name, description, timestamp, classname, classpath);
        this.runRules = buildRunRuleSet(runRules);
    }

    /* package */static Set<String> buildRunRuleSet( String... runRules ) {
        Set<String> result = new LinkedHashSet<String>();
        for (String runRule : runRules) {
            if (runRule == null) continue;
            runRule = runRule.trim();
            if (runRule.length() == 0) continue;
            result.add(runRule);
        }
        return Collections.unmodifiableSet(result);
    }

    public Collection<String> getRunRules() {
        return Collections.unmodifiableSet(this.runRules);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof SequencerConfig) {
            SequencerConfig that = (SequencerConfig)obj;
            if (this.isSame(that)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasChanged( SequencerConfig that ) {
        if (super.hasChanged(that)) return true;
        if (!this.getRunRules().equals(that.getRunRules())) return true;
        return false;
    }

}
