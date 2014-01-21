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
package org.modeshape.sequencer.ddl.dialect.teiid;

import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.sequencer.ddl.DdlTokenStream;

/**
 * A Teiid parsing error.
 */
public class TeiidDdlParsingException extends ParsingException {

    private static final long serialVersionUID = 1L;

    /**
     * @param tokens the tokens being parsed when the error occurred (cannot be <code>null</code>)
     * @param message the error message (cannot be <code>null</code>)
     */
    public TeiidDdlParsingException( final DdlTokenStream tokens,
                                     final String message ) {
        super((tokens.hasNext() ? tokens.nextPosition() : Position.EMPTY_CONTENT_POSITION), message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Throwable#getLocalizedMessage()
     */
    @Override
    public String getLocalizedMessage() {
        String msg = super.getLocalizedMessage();

        if (getPosition() == Position.EMPTY_CONTENT_POSITION) {
            return msg + " (tokens are empty)";
        }

        return msg + " (position = " + getPosition();
    }

}
