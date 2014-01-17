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
 * A simple and limited {@link org.modeshape.common.naming.SingletonInitialContext JNDI naming context} 
 * and {@link org.modeshape.common.naming.SingletonInitialContextFactory InitialContext factory implementation} 
 * that can be used in unit tests or other code that uses JNDI to {@link javax.naming.Context#lookup(String) looks up} 
 * objects.
 */
package org.modeshape.common.naming;
