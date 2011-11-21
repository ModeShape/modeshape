package org.modeshape.jcr.perftests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Horia Chiorean
 */
final class RunnerConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunnerConfiguration.class);

    List<String> excludeTestsRegExp = new ArrayList<String>();
    List<String> includeTestsRegExp = new ArrayList<String>();
    List<String> scanSubPackages = new ArrayList<String>();
    int repeatCount = 1;

    RunnerConfiguration( String fileName ) {
        try {
            Properties configParams = new Properties();
            configParams.load(getClass().getClassLoader().getResourceAsStream(fileName));
            initRunner(configParams);
        } catch (IOException e) {
            LOGGER.warn("Cannot load config file. Will use defaults ", e);
        }
    }

    private void initRunner( Properties configParams ) {
        parseMultiValuedString(configParams.getProperty("tests.exclude"), excludeTestsRegExp);
        parseMultiValuedString(configParams.getProperty("tests.include"), includeTestsRegExp);
        parseMultiValuedString(configParams.getProperty("scan.subPackages"), scanSubPackages);
        repeatCount = Integer.valueOf(configParams.getProperty("repeat.count"));
    }

    private void parseMultiValuedString( String multiValueString, List<String> collector ) {
        if (multiValueString == null) {
            return;
        }
        String[] values = multiValueString.split(",");
        for (String value : values) {
            if (!value.trim().isEmpty()) {
                collector.add(value.trim());
            }
        }
    }
}
