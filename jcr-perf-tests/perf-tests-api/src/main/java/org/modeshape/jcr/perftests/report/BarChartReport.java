/*
 * JBoss, Home of Professional Open Source
 * Copyright [2011], Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.perftests.report;

import com.googlecode.charts4j.*;
import freemarker.cache.URLTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.modeshape.jcr.perftests.StatisticalData;
import org.modeshape.jcr.perftests.TestData;
import org.modeshape.jcr.perftests.util.DurationsConverter;
import java.io.PrintWriter;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Class which uses chart4j and freemarker to generate a simple bar graphic of the statistical data of each test run.
 *
 * @author Horia Chiorean
 */
public final class BarChartReport extends TestReportGenerator {

    private static final Configuration FREEMARKER_CONFIG;
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###.#");

    static {
        FREEMARKER_CONFIG = new Configuration();
        FREEMARKER_CONFIG.setDefaultEncoding("UTF-8");
        FREEMARKER_CONFIG.setTemplateLoader(new URLTemplateLoader() {
            @Override
            protected URL getURL( String name ) {
                return this.getClass().getClassLoader().getResource("report/" + name);
            }
        });
        FREEMARKER_CONFIG.setWhitespaceStripping(true);
    }

    public BarChartReport() {
        super("barchart.html", TimeUnit.MILLISECONDS);
    }

    @Override
    public void generateReport( TestData testData ) throws Exception {
        final Map<String, String> reportData = createReportData(testData);
        Template reportTemplate = FREEMARKER_CONFIG.getTemplate("bar-chart-report.ftl");
        Map<String, Object> templateMap = new HashMap<String, Object>();
        templateMap.put("testMap", reportData);
        reportTemplate.process(templateMap, new PrintWriter(getReportFile()));
    }

    private Map<String, String> createReportData( TestData testData ) {
        Set<String> passedTests = testData.getSuccessfulTestNames();
        Map<String, String> reportData = new TreeMap<String, String>();

        for (String testName : passedTests) {
            List<Double> statisticData = getStatisticData(testData, testName);
            Data chartData = Data.newData(statisticData);
            BarChartPlot chartPlot = Plots.newBarChartPlot(chartData, Color.AQUA, testName + "(" + timeUnit.toString() + ")");
            chartPlot.setDataLine(1, Color.RED, Priority.NORMAL);
            for (int i = 0; i < statisticData.size(); i++) {
                chartPlot.addTextMarker(DECIMAL_FORMAT.format(statisticData.get(i)), Color.BLACK, 10, i);
            }
            BarChart chart = GCharts.newBarChart(chartPlot);
            chart.setBarWidth(40);
            chart.addXAxisLabels(AxisLabelsFactory.newAxisLabels("Min", "1st Quartile", "Median", "3rd Quartile", "Max"));
            chart.setSize(700, 300);
            reportData.put(testName, chart.toURLForHTML());
        }

        return reportData;
    }


    private List<Double> getStatisticData( TestData testData, String testName ) {
        List<Double> convertedDurations = DurationsConverter.convertFromNanos(testData.getTestDurationsNanos(testName), timeUnit);
        StatisticalData statisticalData = new StatisticalData(convertedDurations.toArray(new Double[convertedDurations.size()]));

        List<Double> barData = new ArrayList<Double>();
        for (double fiveNumberSummaryData : statisticalData.fiveNumberSummary()) {
            barData.add(fiveNumberSummaryData);
        }
        return barData;
    }
}
