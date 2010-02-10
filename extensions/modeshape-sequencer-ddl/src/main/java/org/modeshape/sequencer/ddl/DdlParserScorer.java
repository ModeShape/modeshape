/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.sequencer.ddl;

import net.jcip.annotations.NotThreadSafe;

/**
 * Interface used by a parser to determine a score describing how well it handles the DDL content.
 */
@NotThreadSafe
public class DdlParserScorer {

    private int score = 0;

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
