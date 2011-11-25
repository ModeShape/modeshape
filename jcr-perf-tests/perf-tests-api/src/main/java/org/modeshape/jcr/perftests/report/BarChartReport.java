/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
