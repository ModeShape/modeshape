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
package org.modeshape.maven;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.modeshape.common.util.CheckArg;

/**
 * The cornerstone of Maven is its dependency list. Most every project depends upon others to build and run correctly, and if all
 * Maven does for you is manage this list for you, you have gained a lot. Maven downloads and links the dependencies for you on
 * compilation and other goals that require them. As an added bonus, Maven brings in the dependencies of those dependencies
 * (transitive dependencies), allowing your list to focus solely on the dependencies your project requires.
 */
public class MavenDependency {

    /**
     * The scope of the dependency - <code>compile</code>, <code>runtime</code>, <code>test</code>, <code>system</code>, and
     * <code>provided</code>. Used to calculate the various classpaths used for compilation, testing, and so on. It also assists
     * in determining which artifacts to include in a distribution of this project. For more information, see <a
     * href="http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html">the dependency mechanism</a>.
     */
    public enum Scope {
        COMPILE("compile"),
        TEST("test"),
        PROVIDED("provided"),
        SYSTEM("system"),
        RUNTIME("runtime");

        private String suffix;

        private Scope( String suffix ) {
            this.suffix = suffix;
        }

        public String getText() {
            return this.suffix;
        }

        public static Scope valueByText( String suffix,
                                         boolean useDefault ) {
            for (Scope type : Scope.values()) {
                if (type.suffix.equalsIgnoreCase(suffix)) return type;
            }
            return useDefault ? getDefault() : null;
        }

        public static Scope getDefault() {
            return COMPILE;
        }

        public static EnumSet<Scope> getRuntimeScopes() {
            return EnumSet.of(COMPILE, RUNTIME);
        }
    }

    public static final String DEFAULT_TYPE = "jar";
    public static final boolean DEFAULT_OPTIONAL = false;

    private MavenId id;
    private String type = DEFAULT_TYPE;
    private Scope scope = Scope.getDefault();
    private String systemPath;
    private boolean optional = DEFAULT_OPTIONAL;
    private final Set<MavenId> exclusions = new LinkedHashSet<MavenId>();

    public MavenDependency( String coordinates ) {
        this.id = new MavenId(coordinates);
    }

    public MavenDependency( MavenId id ) {
        CheckArg.isNotNull(id, "id");
        this.id = id;
    }

    public MavenDependency( String groupId,
                            String artifactId,
                            String version ) {
        this.id = new MavenId(groupId, artifactId, version);
    }

    public MavenDependency( String groupId,
                            String artifactId,
                            String version,
                            String classifier ) {
        this.id = new MavenId(groupId, artifactId, version, classifier);
    }

    /**
     * The identifier of the artifact for this dependency.
     * 
     * @return the identifier
     */
    public MavenId getId() {
        return this.id;
    }

    /**
     * The type of dependency. This defaults to <code>jar</code>. While it usually represents the extension on the filename of the
     * dependency, that is not always the case. A type can be mapped to a different extension and a classifier. The type often
     * correspongs to the packaging used, though this is also not always the case. Some examples are <code>jar</code>,
     * <code>war</code>, <code>ejb-client</code> and <code>test-jar</code>. New types can be defined by plugins that set
     * <code>extensions</code> to <code>true</code>, so this is not a complete list.
     * 
     * @return the dependency type
     */
    public String getType() {
        return this.type;
    }

    /**
     * Set the type of dependency.
     * 
     * @param type the new dependency type. If null, then the type will be set to the {@link #DEFAULT_TYPE default dependency
     *        type}.
     */
    public void setType( String type ) {
        this.type = type != null ? type.trim() : DEFAULT_TYPE;
    }

    /**
     * The scope of the dependency - <code>compile</code>, <code>runtime</code>, <code>test</code>, <code>system</code>, and
     * <code>provided</code>. Used to calculate the various classpaths used for compilation, testing, and so on. It also assists
     * in determining which artifacts to include in a distribution of this project. For more information, see <a
     * href="http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html">the dependency mechanism</a>.
     * 
     * @return the scope
     */
    public Scope getScope() {
        return this.scope;
    }

    /**
     * @param scope Sets scope to the specified value.
     */
    public void setScope( Scope scope ) {
        this.scope = scope != null ? scope : Scope.getDefault();
    }

    public void setScope( String text ) {
        this.scope = Scope.valueByText(text, true);
    }

    /**
     * FOR SYSTEM SCOPE ONLY. Note that use of this property is <b>discouraged</b> and may be replaced in later versions. This
     * specifies the path on the filesystem for this dependency. Requires an absolute path for the value, not relative. Use a
     * property that gives the machine specific absolute path, e.g. <code>${java.home}</code>.
     * 
     * @return systemPath
     */
    public String getSystemPath() {
        return this.systemPath;
    }

    /**
     * @param systemPath Sets systemPath to the specified value.
     */
    public void setSystemPath( String systemPath ) {
        this.systemPath = systemPath != null ? systemPath.trim() : null;
    }

    /**
     * Indicates the dependency is optional for use of this library. While the version of the dependency will be taken into
     * account for dependency calculation if the library is used elsewhere, it will not be passed on transitively.
     * 
     * @return true if this is an optional dependency, or false otherwise
     */
    public boolean isOptional() {
        return this.optional;
    }

    /**
     * @param optional Sets optional to the specified value.
     */
    public void setOptional( boolean optional ) {
        this.optional = optional;
    }

    /**
     * Exclusions explicitly tell Maven that you don't want to include the specified project that is a dependency of this
     * dependency (in other words, its transitive dependency). For example, the maven-embedder requires maven-core , and we do not
     * wish to use it or its dependencies, then we would add it as an exclusion .
     * 
     * @return the set of exclusions
     */
    public Set<MavenId> getExclusions() {
        return this.exclusions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof MavenDependency) {
            MavenDependency that = (MavenDependency)obj;
            return this.id.equals(that.id);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.id.toString();
    }

}
