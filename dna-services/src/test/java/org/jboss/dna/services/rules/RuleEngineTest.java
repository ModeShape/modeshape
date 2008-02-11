package org.jboss.dna.services.rules;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.InputStreamReader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jboss.dna.common.util.TestUtil;
import org.jboss.dna.services.rules.RuleEngine;
import org.jboss.dna.services.rules.RuleEngineConfig;
import org.jboss.dna.services.rules.sequencer.ContentInfo;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author John Verhaeg
 */
public class RuleEngineTest {

    public static final String TEMP_DATA_PATH = TestUtil.TEMP_DATA_PATH + "RuleEngineTest/";

    @Ignore
    @Test
    public void shouldBeConfigurable() throws Exception {
        RuleEngineConfig config = new RuleEngineConfig();
        Set<String> sequencers = new LinkedHashSet<String>();
        config.addGlobal("sequencers", sequencers);
        config.setProperty("source", "drl");
        config.setProperty("dsl", new InputStreamReader(RuleEngineTest.class.getResourceAsStream("/sequencer.dsl")));
        config.addRules(new InputStreamReader(RuleEngineTest.class.getResourceAsStream("/sequencer.dslr")));
        RuleEngine engine = config.createRuleEngine();

        ContentInfo info = new ContentInfo();
        info.setFileName("sequencer.dsl");
        info.setHeader("[when]after \"{value}\"=$info: ContentInfo( sequenced == true ) from ContentInfo( id matches \"{value}\" )");
        info.setMimeType("text");

        List<?> results = engine.execute(info);
        assertThat(results, is(notNullValue()));
        assertThat(results.isEmpty(), is(false));
        Object result = results.get(0);
        assertThat(result, is(instanceOf(ContentInfo.class)));
        assertThat(sequencers.isEmpty(), is(false));
        Object sequencer = sequencers.iterator().next();
        assertThat(sequencer, is(instanceOf(String.class)));
        assertThat(((String)sequencer), is("A"));
    }
}
