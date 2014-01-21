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
 * This package defines an internal adapter framework for detecting MIME types. The framework generally wraps the Tika library,
 * but is capable of being used without error but with significantly degraded functionality when Tika is not available on the classpath.
 * Thus, Tika can be removed if MIME type detection is not required.
 */

package org.modeshape.jcr.mimetype;

