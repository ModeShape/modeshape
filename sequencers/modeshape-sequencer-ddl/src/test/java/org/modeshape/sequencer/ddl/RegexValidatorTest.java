/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.sequencer.ddl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 *
 * @author kulikov
 */
public class RegexValidatorTest {
    private String patternText = "(\\s*)(CREATE)(\\s+)";
    
    @Test
    public void test() {
        String s = "  CREATESTW";
        
        Pattern pattern = Pattern.compile(patternText);
        Matcher matcher = pattern.matcher(s);
        
        while (matcher.find()) {
            System.out.println("--");
            System.out.println(matcher.group());
            System.out.println(matcher.end());
        }
    }
}
