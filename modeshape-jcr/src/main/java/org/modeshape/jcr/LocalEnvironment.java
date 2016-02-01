/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jgroups.Channel;
import org.modeshape.common.util.DelegatingClassLoader;
import org.modeshape.common.util.StringURLClassLoader;
import org.modeshape.common.util.StringUtil;

/**
 * An {@link Environment} that can be used with a standalone repository.
 * <p>
 * To use a custom Environment instance, simply create a {@link RepositoryConfiguration} as usual but then call the
 * {@link RepositoryConfiguration#with(Environment)} with the Environment instance and then use the resulting
 * RepositoryConfiguration instance.
 * </p>
 */
public class LocalEnvironment implements Environment {

    public LocalEnvironment() {
    }

    @Override
    public Channel getChannel(String name) throws Exception {
        return null;
    }

    @Override
    public ClassLoader getClassLoader( Object caller,
                                       String... classpathEntries ) {
        caller = Objects.requireNonNull(caller);

        Set<ClassLoader> delegates = new LinkedHashSet<>();

        List<String> urls = Arrays.stream(classpathEntries).filter(StringUtil::notBlank).collect(Collectors.toList());
        if (urls.isEmpty()) {
            StringURLClassLoader urlClassLoader = new StringURLClassLoader(urls);
            // only if any custom urls were parsed add this loader
            if (urlClassLoader.getURLs().length > 0) {
                delegates.add(urlClassLoader);
            }
        }

        ClassLoader currentLoader = getClass().getClassLoader();
        ClassLoader callerLoader = caller.getClass().getClassLoader();
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        
        //add the TCCL to the list if it's not the same as the current loader or the fallback loader
        if (!callerLoader.equals(tccl) && !currentLoader.equals(tccl)) {
            delegates.add(tccl);
        }

        if (!callerLoader.equals(currentLoader)) {
            // if the parent of fallback is the same as the current loader, just use that
            if (currentLoader.equals(callerLoader.getParent())) {
                currentLoader = callerLoader;
            } else {
                delegates.add(callerLoader);
            }
        }

        return delegates.isEmpty() ? currentLoader : new DelegatingClassLoader(currentLoader, delegates);
    }
}
