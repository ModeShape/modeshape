/*
 *
 */
package org.jboss.dna.common.i18n;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.Locale;
import org.jboss.dna.common.CommonI18n;
import org.junit.Test;

/**
 * @author John Verhaeg
 */
public class CommonI18nTest {

	@Test
	public void shouldNotHaveLocalizationProblems() {
		for (Locale locale : CommonI18n.getLocalizationProblemLocales()) {
			assertThat(CommonI18n.getLocalizationProblems(locale).isEmpty(), is(true));
		}
	}
}
