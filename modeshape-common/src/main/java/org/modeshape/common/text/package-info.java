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
 * A set of utilities for working with text.  Included is an {@link org.modeshape.common.text.Inflector} class that transforms (English) works into singular,
 * plural, and human-readable forms, and is capable of transforming between camel-case, whitespace-delimited, or underscore-delimited
 * forms.  There is also components that can {@link org.modeshape.common.text.TextEncoder encode} and {@link org.modeshape.common.text.TextDecoder decode} text.
 * Finally, this package defines a simple framework for {@link org.modeshape.common.text.TokenStream tokenizing text} (e.g., from files or stream)
 * making it very easy to create very straightforward parsers.
 */
package org.modeshape.common.text;
