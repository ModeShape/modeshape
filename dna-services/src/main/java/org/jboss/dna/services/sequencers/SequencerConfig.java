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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.ClassUtil;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.maven.MavenId;

/**
 * @author Randall Hauch
 */
@Immutable
public class SequencerConfig implements Comparable<SequencerConfig> {

    private final String name;
    private final Set<String> runRules;
    private final String description;
    private final String sequencerClassname;
    private final List<MavenId> classpath;
    private final long timestamp;

    public SequencerConfig( String name, String description, String classname, MavenId[] classpath, String... runRules ) {
        this(name, description, System.currentTimeMillis(), classname, classpath, runRules);
    }

    public SequencerConfig( String name, String description, long timestamp, String classname, MavenId[] classpath, String... runRules ) {
        ArgCheck.isNotEmpty(name, "name");
        this.name = name;
        this.description = description != null ? description.trim() : "";
        this.sequencerClassname = classname;
        this.classpath = buildClasspath(classpath);
        this.runRules = buildRunRuleSet(runRules);
        this.timestamp = timestamp;
        // Check the classname is a valid classname ...
        if (!ClassUtil.isFullyQualifiedClassname(classname)) {
            String msg = "The classname {1} specified for sequencer {2} is not a valid Java classname";
            msg = StringUtil.createString(msg, classname, name);
            throw new IllegalArgumentException(msg);
        }
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

    /* package */static List<MavenId> buildClasspath( MavenId... mavenIds ) {
        List<MavenId> classpath = new ArrayList<MavenId>();
        if (mavenIds != null && mavenIds.length != 0) {
            for (MavenId mavenId : mavenIds) {
                if (!classpath.contains(mavenId)) classpath.add(mavenId);
            }
        }
        return Collections.unmodifiableList(classpath);
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public String getSequencerClassname() {
        return this.sequencerClassname;
    }

    public List<MavenId> getSequencerClasspath() {
        return this.classpath;
    }

    public MavenId[] getSequencerClasspathArray() {
        // Always return a copy to not care about modification of the array ...
        List<MavenId> ids = getSequencerClasspath();
        return ids.toArray(new MavenId[ids.size()]);
    }

    public Collection<String> getRunRules() {
        return Collections.unmodifiableSet(this.runRules);
    }

    /**
     * Get the system timestamp when this configuration object was created.
     * @return timestamp
     */
    public long getTimestamp() {
        return this.timestamp;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( SequencerConfig that ) {
        if (that == this) return 0;
        int diff = this.getName().compareToIgnoreCase(that.getName());
        if (diff != 0) return diff;
        diff = (int)(this.getTimestamp() - that.getTimestamp());
        return diff;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.getName().hashCode();
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

    /**
     * Determine whether this configuration represents the same configuration as that supplied.
     * @param that the other configuration to be compared with this
     * @return true if this configuration and the supplied configuration
     */
    public boolean isSame( SequencerConfig that ) {
        if (that == this) return true;
        if (that == null) return false;
        return this.getName().equalsIgnoreCase(that.getName());
    }

    public boolean hasChanged( SequencerConfig that ) {
        assert this.isSame(that);
        if (!this.getSequencerClassname().equals(that.getSequencerClassname())) return true;
        if (!this.getSequencerClasspath().equals(that.getSequencerClasspath())) return true;
        if (!this.getRunRules().equals(that.getRunRules())) return true;
        return false;
    }

}
