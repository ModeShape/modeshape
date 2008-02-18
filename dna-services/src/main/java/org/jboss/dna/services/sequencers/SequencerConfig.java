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
import java.util.Collections;
import java.util.List;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.ClassUtil;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.maven.MavenId;

/**
 * @author Randall Hauch
 */
public class SequencerConfig implements ISequencerConfig {

    private final String name;
    private final List<String> runRules;
    private String description;
    private String sequencerClassname;
    private final List<MavenId> classpath;

    public SequencerConfig( String name, String classname ) {
        ArgCheck.isNotEmpty(name, "name");
        this.setClassname(classname);
        this.name = name;
        this.runRules = new ArrayList<String>();
        this.classpath = new ArrayList<MavenId>();
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
        return Collections.unmodifiableList(this.classpath);
    }

    public List<String> getRunRules() {
        return Collections.unmodifiableList(this.runRules);
    }

    public SequencerConfig setDescription( String description ) {
        this.description = description != null ? description.trim() : null;
        return this;
    }

    public SequencerConfig setClassname( String classname ) {
        ArgCheck.isNotEmpty(sequencerClassname, "sequencerClassname");
        if (!ClassUtil.isFullyQualifiedClassname(sequencerClassname)) {
            String msg = "The classname {1} specified for sequencer {2} is not a valid Java classname";
            msg = StringUtil.createString(msg, sequencerClassname, name);
            throw new IllegalArgumentException(msg);
        }
        return this;
    }

    public SequencerConfig setClasspath( MavenId... ids ) {
        this.classpath.clear();
        if (ids != null && ids.length != 0) {
            for (MavenId mavenId : ids) {
                if (!this.classpath.contains(mavenId)) this.classpath.add(mavenId);
            }
        }
        return this;
    }

    public SequencerConfig addRunRule( String runRule ) {
        if (runRule != null && runRule.trim().length() != 0) this.runRules.add(runRule);
        return this;
    }

    public SequencerConfig setRunRules( String... runRules ) {
        this.runRules.clear();
        if (runRules != null && runRules.length != 0) {
            for (String runRule : runRules) {
                addRunRule(runRule);
            }
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( ISequencerConfig that ) {
        if (that == this) return 00;
        return this.getName().compareToIgnoreCase(that.getName());
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
            if (this.getName().equalsIgnoreCase(that.getName())) {
                return true;
            }
        }
        return false;
    }

}
