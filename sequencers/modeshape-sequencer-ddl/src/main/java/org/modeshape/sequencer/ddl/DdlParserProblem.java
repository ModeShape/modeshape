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
package org.modeshape.sequencer.ddl;

import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.sequencer.ddl.DdlConstants.Problems;

/**
 * A special form of {@link Problems} that is also a {@link ParsingException}.
 */
public class DdlParserProblem extends ParsingException implements DdlConstants.Problems {
    private static final long serialVersionUID = 2010539270968770893L;

    private int level = OK;
    private String unusedSource;

    public DdlParserProblem( Position position ) {
        super(position);

    }

    public DdlParserProblem( int level,
                             Position position,
                             String message,
                             Throwable cause ) {
        super(position, message, cause);
        this.level = level;
    }

    public DdlParserProblem( int level,
                             Position position,
                             String message ) {
        super(position, message);
        this.level = level;
    }

    /**
     * @return the unused statement string
     */
    public String getUnusedSource() {
        return this.unusedSource;
    }

    public void setUnusedSource( String unusedSource ) {
        if (unusedSource == null) {
            unusedSource = "";
        }
        this.unusedSource = unusedSource;
    }

    public void appendSource( boolean addSpaceBefore,
                              String value ) {
        if (addSpaceBefore) {
            this.unusedSource = this.unusedSource + DdlConstants.SPACE;
        }
        this.unusedSource = this.unusedSource + value;
    }

    public void appendSource( boolean addSpaceBefore,
                              String value,
                              String... additionalStrs ) {
        if (addSpaceBefore) {
            this.unusedSource = this.unusedSource + DdlConstants.SPACE;
        }
        this.unusedSource = this.unusedSource + value;
    }

    public int getLevel() {
        return level;
    }

    /**
     * @param level Sets level to the specified value.
     */
    public void setLevel( int level ) {
        this.level = level;
    }

    @Override
    public String toString() {
        if (this.level == WARNING) {
            return ("WARNING: " + super.toString());
        } else if (this.level == ERROR) {
            return ("ERROR: " + super.toString());
        }

        return super.toString();
    }

}
