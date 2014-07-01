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
package org.modeshape.sequencer.testdata;

import java.util.EnumSet;
//CHECKSTYLE:OFF
import java.util.*;
//CHECKSTYLE:ON

/**
 * This is a enum type test class.
 */
public enum EnumType implements Cloneable {

    /**
     * Indicates a waiting state.
     */
    WAITING(0) {

        class TextProvider {
            public String get() {
                return "I'm waiting.";
            }
        }

        /**
         * {@inheritDoc}
         *
         * @see org.modeshape.sequencer.testdata.EnumType#execute()
         */
        @Override
        public void execute() {
            this.executor.execute(new TextProvider().get());
        }
    },

    /**
     * Indicates a ready state.
     */
    READY(1) {

        private String text = "I'm ready.";

        /**
         * {@inheritDoc}
         *
         * @see org.modeshape.sequencer.testdata.EnumType#execute()
         */
        @Override
        public void execute() {
            this.executor.execute(text);
        }
    },

    /**
     * Indicates a skipped state.
     */
    SKIPPED(2) {

        /**
         * {@inheritDoc}
         *
         * @see org.modeshape.sequencer.testdata.EnumType#execute()
         */
        @Override
        public void execute() {
            this.executor.execute("I've been skipped.");
        }
    },

    /**
     * Indicates a done state.
     */
    DONE(3) {

        /**
         * {@inheritDoc}
         *
         * @see org.modeshape.sequencer.testdata.EnumType#execute()
         */
        @Override
        public void execute() {
            this.executor.execute("I'm done.");
        }
    };

    class Executor {
        void execute(String text) {
            System.out.println(text);
        }
    }

    private static final Map<Integer, EnumType> _lookup;

    static {
        _lookup = new HashMap<Integer, EnumType>();

        for (EnumType enumType : EnumSet.allOf(EnumType.class)) {
            _lookup.put(enumType.getCode(), enumType);
        }
    }

    public static EnumType get( int code ) {
        return _lookup.get(code);
    }

    private int code;
    Executor executor;

    private EnumType( int code ) {
        this.code = code;
        this.executor = new Executor();
    }

    public abstract void execute();

    public int getCode() {
        return this.code;
    }

}
