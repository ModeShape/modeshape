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
package org.modeshape.schematic.internal.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to help track that a test is verifying the fix for one or more specific issues. To use, simply
 * place this annotation on the test method and reference the JIRA issue number:
 * 
 * <pre>
 *    &#064;FixFor("MODE-123")
 *    &#064;Test
 *    public void shouldVerifyBehavior() {
 *     ...
 *    }
 * </pre>
 * <p>
 * It is also possible to reference multiple JIRA issues if the test is verifying multiple ones:
 * 
 * <pre>
 *    &#064;FixFor({"MODE-123","MODE-456"})
 *    &#064;Test
 *    public void shouldVerifyBehavior() {
 *     ...
 *    }
 * </pre>
 * 
 * </p>
 */
@Documented
@Retention( CLASS )
@Target( METHOD )
public @interface FixFor {
    /**
     * The JIRA issue for which this is a fix. For example, "MODE-123".
     * 
     * @return the issue
     */
    String[] value();
}
