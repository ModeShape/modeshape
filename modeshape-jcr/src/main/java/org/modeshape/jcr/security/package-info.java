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
/**
 * ModeShape JCR repositories have a pluggable authentication and authorization framework. Out of the box, each repository
 * is configured to support authenticating and authorizing using JAAS, HTTP servlet (if the servlet library is on the classpath), and (if configured) anonymous logins.
 * In addition, Each repository can also be configured with customzied authenticators.
 * <p>
 * Creating a custom authenticator is a matter of properly implementing {@link org.modeshape.jcr.security.AuthenticationProvider}
 * and configuring the repository to use that class. Each authenticator is responsible for authenticating the supplied
 * {@link javax.jcr.Credentials} and returning an ExecutionContext that will represent the user, including
 * its embedded {@link org.modeshape.jcr.security.SecurityContext} (for simple role-based authorization) or {@link org.modeshape.jcr.security.AuthorizationProvider} (for a combination of path- and role-based authorization).
 * </p>
 */

package org.modeshape.jcr.security;

