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
 * This is a class type test class that is in the default package.
 *
 * @since 2.0
 */
public class DefaultPackageClass {

    /**
     * A constant.
     */
    public static final String CONSTANT = "constant"; // a line comment

    /**
     * A field.
     */
    private String text = "text";

    /**
     * @return the text
     */
    public String get() {
        return this.text;
    }

    /**
     * @param newText the new text
     */
    public final void set(final String newText) {
        this.text = newText;
    }

}
