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

import org.modeshape.common.annotation.NotThreadSafe;

/**
 * Interface used by a parser to determine a score describing how well it handles the DDL content.
 */
@NotThreadSafe
public class DdlParserScorer {

    private int score = 0;

    /**
     * Reset the score back to zero.
     */
    public void reset() {
        this.score = 0;
    }

    /**
     * Increment the score because another statement was matched.
     * 
     * @param count the number of statements
     */
    public void scoreStatements( int count ) {
        score += count;
    }

    /**
     * Increment the score if the given text contains any of the supply keywords.
     * 
     * @param text the text to evaluate; may be null
     * @param factor the factor to use for each increment
     * @param keywords the keywords to be found in the text
     */
    public void scoreText( String text,
                           int factor,
                           String... keywords ) {
        if (text != null && keywords != null) {
            // Increment the score once for each keyword that is found within the text ...
            String lowercaseText = text.toLowerCase();
            for (String keyword : keywords) {
                if (keyword == null) continue;
                String lowercaseKeyword = keyword.toLowerCase();
                int index = 0;
                while (true) {
                    index = lowercaseText.indexOf(lowercaseKeyword, index);
                    if (index == -1) break;
                    score += factor;
                    ++index;
                }
            }
        }
    }

    /**
     * Increment the score if the given text contains any of the supply keywords.
     * 
     * @param text the text to evaluate; may be null
     * @param keywords the keywords to be found in the text
     */
    public void scoreText( String text,
                           String... keywords ) {
        scoreText(text, 1, keywords);
    }

    /**
     * Get the score.
     * 
     * @return the score
     */
    public int getScore() {
        return score;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return Integer.toString(score);
    }
}
