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
package org.modeshape.sequencer.classfile.metadata;

import javassist.bytecode.AccessFlag;

public enum Visibility {

    PUBLIC("public"),
    PROTECTED("protected"),
    PACKAGE("package"),
    PRIVATE("private");

    private static final int VISBILITY_MASK = AccessFlag.PUBLIC | AccessFlag.PRIVATE | AccessFlag.PROTECTED;

    private final String description;

    private Visibility( String description ) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    @Override
    public String toString() {
        return description;
    }

    public static Visibility fromAccessFlags( int accessFlags ) {
        switch (accessFlags & VISBILITY_MASK) {
            case AccessFlag.PUBLIC:
                return Visibility.PUBLIC;
            case AccessFlag.PROTECTED:
                return Visibility.PROTECTED;
            case AccessFlag.PRIVATE:
                return Visibility.PRIVATE;
            default:
                return Visibility.PACKAGE;
        }

    }
}
